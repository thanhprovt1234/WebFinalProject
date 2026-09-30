package com.example.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import com.example.demo.entity.User;
import com.example.demo.entity.Address;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.AddressRepository;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProfileControllerTests {
    @Value("${local.server.port}") int port;
    @Autowired UserRepository users;
    @Autowired AddressRepository addresses;
    @Autowired PasswordEncoder encoder;
    private final List<User> accounts = new ArrayList<>();
    private final List<HttpClient> clients = new ArrayList<>();
    record Session(User user, HttpClient client, String csrf) {}

    private HttpResponse<String> request(HttpClient client, String path, String data, String token) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (data != null) builder.header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(data));
        if (token != null) builder.header("X-CSRF-TOKEN", token);
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
    private String token(String html) {
        var matcher = Pattern.compile("name=\"csrf-token\" content=\"([^\"]+)\"").matcher(html);
        assertThat(matcher.find()).isTrue(); return matcher.group(1);
    }
    private Session login() throws Exception {
        var user = new User();
        user.setUsername("profile-" + UUID.randomUUID()); user.setEmail(user.getUsername()+"@example.com");
        user.setFullName("Profile test"); user.setPassword(encoder.encode("test-password"));
        users.saveAndFlush(user); accounts.add(user);
        var client = HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        clients.add(client);
        String csrf = token(request(client,"/login",null,null).body());
        assertThat(request(client,"/login","username="+user.getUsername()+"&password=test-password",csrf).statusCode()).isEqualTo(302);
        return new Session(user,client,token(request(client,"/profile",null,null).body()));
    }
    private HttpResponse<String> post(Session session,String path,String data) throws Exception {
        return request(session.client(),path,data,session.csrf());
    }
    private String address(String name) {
        return "receiverName="+name+"&phone=0901234567&addressLine=123+Test&ward=Ward&district=Area&city=City";
    }
    private List<Address> list(Session session) { return addresses.findByUserIdOrderByIdAsc(session.user().getId()); }
    @AfterEach void cleanup() {
        clients.forEach(HttpClient::close);
        for (var user:accounts) { addresses.deleteAll(addresses.findByUserId(user.getId())); users.deleteById(user.getId()); }
    }

    @Test void profileUpdatesOnlyEditableFieldsAndValidatesInput() throws Exception {
        var session=login();
        assertThat(post(session,"/profile","fullName=Updated&phone=0901234567&email=hacker%40example.com&enabled=false&username=hacker&password=hacked").statusCode()).isEqualTo(302);
        var stored=users.findById(session.user().getId()).orElseThrow();
        assertThat(stored.getFullName()).isEqualTo("Updated");
        assertThat(stored.getPhone()).isEqualTo("0901234567");
        assertThat(stored.getEmail()).isEqualTo(session.user().getEmail());
        assertThat(stored.getUsername()).isEqualTo(session.user().getUsername());
        assertThat(stored.isEnabled()).isTrue();
        assertThat(encoder.matches("test-password",stored.getPassword())).isTrue();
        assertThat(post(session,"/profile","fullName=%20&phone=bad").statusCode()).isEqualTo(200);
        assertThat(users.findById(stored.getId()).orElseThrow().getFullName()).isEqualTo("Updated");
        assertThat(request(session.client(),"/profile","fullName=NoCsrf",null).statusCode()).isEqualTo(403);
    }

    @Test void addressLifecycleOwnershipEscapingAndDefaultSelection() throws Exception {
        var owner=login(); var other=login();
        assertThat(post(owner,"/profile/addresses",address("First")).statusCode()).isEqualTo(302);
        long first=list(owner).getFirst().getId();
        assertThat(list(owner).getFirst().isDefault()).isTrue();
        assertThat(post(owner,"/profile/addresses",address("Second")+"&defaultAddress=true").statusCode()).isEqualTo(302);
        long second=list(owner).getLast().getId();
        assertThat(list(owner).stream().filter(Address::isDefault).toList()).singleElement()
                .satisfies(a->assertThat(a.getId()).isEqualTo(second));
        for(String suffix:List.of("", "/default", "/delete")) {
            assertThat(post(other,"/profile/addresses/"+first+suffix,address("Intruder")).statusCode()).isEqualTo(404);
        }
        assertThat(request(other.client(),"/profile/addresses/"+first+"/edit",null,null).statusCode()).isEqualTo(404);
        assertThat(request(owner.client(),"/profile/addresses/"+first+"/edit",null,null).body()).contains("value=\"First\"");
        assertThat(post(owner,"/profile/addresses/"+first,address("%3Cscript%3Etest%3C%2Fscript%3E")).statusCode()).isEqualTo(302);
        assertThat(request(owner.client(),"/profile",null,null).body()).contains("&lt;script&gt;test&lt;/script&gt;")
                .doesNotContain("<script>test</script>");
        assertThat(post(owner,"/profile/addresses/"+first+"/default","").statusCode()).isEqualTo(302);
        assertThat(post(owner,"/profile/addresses/"+first+"/delete","").statusCode()).isEqualTo(302);
        assertThat(list(owner)).singleElement().satisfies(a->assertThat(a.isDefault()).isTrue());
        assertThat(post(owner,"/profile/addresses/"+second+"/delete","").statusCode()).isEqualTo(302);
        assertThat(list(owner)).isEmpty();
        var invalid=post(owner,"/profile/addresses","receiverName=Test&phone=bad");
        assertThat(invalid.statusCode()).isEqualTo(200);
        assertThat(invalid.body()).contains("text-danger");
        assertThat(list(owner)).isEmpty();
    }

    @Test void concurrentFirstAddressesHaveExactlyOneDefaultAndGuestsMustLogin() throws Exception {
        try(var guest=HttpClient.newHttpClient()) {
            assertThat(request(guest,"/profile",null,null).statusCode()).isEqualTo(302);
        }
        var session=login();
        try(var pool=Executors.newFixedThreadPool(3)) {
            List<Callable<Integer>> tasks=new ArrayList<>();
            for(int i=0;i<3;i++) tasks.add(()->post(session,"/profile/addresses",address("Concurrent")).statusCode());
            for(var result:pool.invokeAll(tasks)) assertThat(result.get()).isEqualTo(302);
        }
        assertThat(list(session)).hasSize(3);
        assertThat(list(session).stream().filter(Address::isDefault).count()).isEqualTo(1);
    }
}
