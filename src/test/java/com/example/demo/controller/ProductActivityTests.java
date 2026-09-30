package com.example.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import com.example.demo.entity.User;
import com.example.demo.entity.ProductActivity;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.ProductActivityRepository;
import java.net.*;
import java.net.http.*;
import java.time.*;
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
class ProductActivityTests {
    @Value("${local.server.port}") int port;
    @Autowired UserRepository users;
    @Autowired ProductActivityRepository activities;
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
    private Session login(User existing) throws Exception {
        var user = existing;
        if (user == null) {
            user = new User(); user.setUsername("activity-" + UUID.randomUUID());
            user.setEmail(user.getUsername()+"@example.com"); user.setFullName("Activity test");
            user.setPassword(encoder.encode("test-password")); users.saveAndFlush(user); accounts.add(user);
        }
        var client = HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build(); clients.add(client);
        String csrf = token(request(client,"/login",null,null).body());
        assertThat(request(client,"/login","username="+user.getUsername()+"&password=test-password",csrf).statusCode()).isEqualTo(302);
        return new Session(user,client,token(request(client,"/",null,null).body()));
    }
    private HttpResponse<String> post(Session s,String path,String data) throws Exception {
        return request(s.client(),path,data,s.csrf());
    }
    private String get(Session s,String path) throws Exception {
        var response=request(s.client(),path,null,null); assertThat(response.statusCode()).isEqualTo(200); return response.body();
    }
    @AfterEach void cleanup() {
        clients.forEach(HttpClient::close);
        for(var user:accounts) {
            activities.deleteAll(activities.findAll().stream().filter(a->a.getUser().getId().equals(user.getId())).toList());
            users.deleteById(user.getId());
        }
    }
    @Test void favoritesPersistAcrossSessionsAndRemainPrivate() throws Exception {
        var owner=login(null); var other=login(null);
        assertThat(post(owner,"/products/ao-thun-01/favorite","saved=true&source=favorites&userId="+other.user().getId()).statusCode()).isEqualTo(302);
        var first=activities.findByUserIdAndCatalogProductId(owner.user().getId(),"ao-thun-01").orElseThrow();
        assertThat(first.isFavorite()).isTrue(); assertThat(first.getViewedAt()).isNull();
        assertThat(post(owner,"/products/ao-thun-01/favorite","saved=true").statusCode()).isEqualTo(302);
        assertThat(activities.findByUserIdAndCatalogProductId(owner.user().getId(),"ao-thun-01").orElseThrow().getFavoritedAt()).isEqualTo(first.getFavoritedAt());
        assertThat(get(login(owner.user()),"/favorites")).contains("data-activity-product=\"ao-thun-01\"");
        assertThat(get(other,"/favorites")).doesNotContain("data-activity-product=\"ao-thun-01\"");
        assertThat(post(other,"/products/ao-thun-01/favorite","saved=false&userId="+owner.user().getId()).statusCode()).isEqualTo(302);
        assertThat(activities.findByUserIdAndCatalogProductId(owner.user().getId(),"ao-thun-01")).isPresent();
        assertThat(post(owner,"/products/ao-thun-01/favorite","saved=false").statusCode()).isEqualTo(302);
        assertThat(get(owner,"/favorites")).doesNotContain("data-activity-product=\"ao-thun-01\"");
    }
    @Test void revisitsMoveToFrontAndUnfavoritingKeepsHistory() throws Exception {
        var owner=login(null);
        for(String id:List.of("ao-thun-01","ao-thun-02","ao-thun-01"))
            assertThat(post(owner,"/api/products/"+id+"/viewed","").statusCode()).isEqualTo(204);
        var history=get(owner,"/recently-viewed");
        assertThat(history.indexOf("data-activity-product=\"ao-thun-01\""))
                .isLessThan(history.indexOf("data-activity-product=\"ao-thun-02\""));
        assertThat(history.split("data-activity-product=",-1)).hasSize(3);
        post(owner,"/products/ao-thun-01/favorite","saved=true");
        post(owner,"/products/ao-thun-01/favorite","saved=false");
        assertThat(get(owner,"/recently-viewed")).contains("data-activity-product=\"ao-thun-01\"");
        assertThat(get(login(null),"/recently-viewed")).doesNotContain("data-activity-product=");
    }
    @Test void authenticationCsrfUnknownProductsAndPagination() throws Exception {
        try(var guest=HttpClient.newHttpClient()) {
            assertThat(request(guest,"/favorites",null,null).statusCode()).isEqualTo(302);
            assertThat(request(guest,"/recently-viewed",null,null).statusCode()).isEqualTo(302);
        }
        var owner=login(null);
        assertThat(request(owner.client(),"/products/ao-thun-01/favorite","saved=true",null).statusCode()).isEqualTo(403);
        assertThat(request(owner.client(),"/api/products/ao-thun-01/viewed","",null).statusCode()).isEqualTo(403);
        assertThat(post(owner,"/products/missing/favorite","saved=true").statusCode()).isEqualTo(404);
        assertThat(post(owner,"/api/products/missing/viewed","").statusCode()).isEqualTo(404);
        for(int i=0;i<25;i++) {
            var activity=new ProductActivity(); activity.setUser(owner.user()); activity.setCatalogProductId("retired-"+i);
            activity.setFavorite(true); activity.setFavoritedAt(LocalDateTime.now(ZoneOffset.UTC).plusSeconds(i));
            activity.setViewedAt(activity.getFavoritedAt()); activities.saveAndFlush(activity);
        }
        assertThat(get(owner,"/favorites").split("data-activity-product=",-1)).hasSize(21);
        assertThat(get(owner,"/favorites?page=1").split("data-activity-product=",-1)).hasSize(6);
        assertThat(get(owner,"/recently-viewed").split("data-activity-product=",-1)).hasSize(21);
        assertThat(get(owner,"/favorites?page=999").split("data-activity-product=",-1)).hasSize(6);
        assertThat(post(owner,"/products/retired-0/favorite","saved=false").statusCode()).isEqualTo(302);
        assertThat(activities.findByUserIdAndCatalogProductId(owner.user().getId(),"retired-0").orElseThrow().isFavorite()).isFalse();
    }
    @Test void concurrentFavoriteAndViewWritesShareOneRecord() throws Exception {
        var owner=login(null);
        try(var pool=Executors.newFixedThreadPool(4)) {
            List<Callable<Integer>> tasks=List.of(
                ()->post(owner,"/products/ao-thun-01/favorite","saved=true").statusCode(),
                ()->post(owner,"/products/ao-thun-01/favorite","saved=true").statusCode(),
                ()->post(owner,"/api/products/ao-thun-01/viewed","").statusCode(),
                ()->post(owner,"/api/products/ao-thun-01/viewed","").statusCode());
            for(var result:pool.invokeAll(tasks)) assertThat(result.get()).isIn(204,302);
        }
        var activity=activities.findByUserIdAndCatalogProductId(owner.user().getId(),"ao-thun-01").orElseThrow();
        assertThat(activity.isFavorite()).isTrue(); assertThat(activity.getViewedAt()).isNotNull();
    }
}
