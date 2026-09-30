package com.example.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.entity.User;
import com.example.demo.repository.CartItemRepository;
import com.example.demo.repository.UserRepository;
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
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CartControllerTests {
    @Value("${local.server.port}") int port;
    @Autowired UserRepository users;
    @Autowired CartItemRepository items;
    @Autowired PasswordEncoder encoder;
    private final List<User> accounts = new ArrayList<>();
    private final List<HttpClient> clients = new ArrayList<>();
    private final JsonMapper json = JsonMapper.builder().build();
    record Session(HttpClient client, String csrf) {}

    private User account() {
        var user = new User();
        user.setUsername("cart-test-" + UUID.randomUUID());
        user.setEmail(user.getUsername() + "@example.com");
        user.setFullName("Cart test");
        user.setPassword(encoder.encode("cart-password"));
        users.saveAndFlush(user);
        accounts.add(user);
        return user;
    }

    private HttpClient client() {
        var client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        clients.add(client);
        return client;
    }

    private HttpResponse<String> send(HttpClient client, String method, String path, String body, String csrf) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (csrf != null) request.header("X-CSRF-TOKEN", csrf);
        request.header("Content-Type", "application/x-www-form-urlencoded");
        return client.send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private String csrf(String html) {
        var matcher = Pattern.compile("name=\"csrf-token\" content=\"([^\"]+)\"").matcher(html);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private Session login(User user) throws Exception {
        var client = client();
        String token = csrf(send(client, "GET", "/login", null, null).body());
        assertThat(send(client, "POST", "/login", "username=" + user.getUsername() + "&password=cart-password", token)
                .statusCode()).isEqualTo(302);
        return new Session(client, csrf(send(client, "GET", "/", null, null).body()));
    }

    private HttpResponse<String> cart(Session session, String method, String suffix, String body) throws Exception {
        return send(session.client(), method, "/api/cart" + suffix, body, session.csrf());
    }

    @AfterEach
    void cleanup() {
        clients.forEach(HttpClient::close);
        for (var user : accounts) {
            items.deleteAll(items.findByUserIdOrderByIdAsc(user.getId()));
            users.deleteById(user.getId());
        }
    }

    @Test
    void persistsMergesAndSharesOnlyWithinTheSameAccount() throws Exception {
        var owner = account();
        var first = login(owner);
        var secondDevice = login(owner);
        var other = login(account());
        assertThat(cart(first, "GET", "", null).body()).contains("\"count\":0");
        var added = cart(first, "POST", "/items", "productId=ao-thun-01&size=M&quantity=2&price=1&userId=0");
        assertThat(added.statusCode()).isEqualTo(200);
        var body = json.readTree(added.body());
        long id = body.path("items").get(0).path("id").asLong();
        assertThat(body.path("total").asLong()).isEqualTo(198000);
        assertThat(cart(secondDevice, "GET", "", null).body()).isEqualTo(added.body());
        assertThat(cart(other, "GET", "", null).body()).contains("\"count\":0");
        assertThat(cart(other, "PATCH", "/items/" + id, "quantity=8").statusCode()).isEqualTo(404);
        assertThat(cart(other, "DELETE", "/items/" + id, null).statusCode()).isEqualTo(404);
        assertThat(cart(first, "POST", "/items", "productId=ao-thun-01&size=M&quantity=3").statusCode()).isEqualTo(200);
        assertThat(items.findByUserIdOrderByIdAsc(owner.getId())).singleElement()
                .satisfies(item -> assertThat(item.getQuantity()).isEqualTo(5));
        var updated = cart(secondDevice, "PATCH", "/items/" + id, "quantity=1");
        assertThat(updated.statusCode()).isEqualTo(200);
        assertThat(json.readTree(updated.body()).path("total").asLong()).isEqualTo(99000);
        assertThat(cart(first, "POST", "/items", "productId=ao-thun-01&size=L&quantity=1").statusCode()).isEqualTo(200);
        assertThat(items.findByUserIdOrderByIdAsc(owner.getId())).hasSize(2);
        assertThat(cart(first, "DELETE", "/items/" + id, null).statusCode()).isEqualTo(200);
        assertThat(items.findById(id)).isEmpty();
        assertThat(cart(login(owner), "GET", "", null).body()).contains("\"size\":\"L\"", "\"count\":1");
    }

    @Test
    void rejectsInvalidInputAnonymousWritesAndMissingCsrf() throws Exception {
        var anonymous = client();
        assertThat(send(anonymous, "GET", "/api/cart", null, null).statusCode()).isEqualTo(401);
        var session = login(account());
        assertThat(send(session.client(), "POST", "/api/cart/items", "productId=ao-thun-01&size=M&quantity=1", null)
                .statusCode()).isEqualTo(403);
        for (String data : List.of("size=M&quantity=1", "productId=ao-thun-01&quantity=1",
                "productId=ao-thun-01&size=BAD&quantity=1", "productId=ao-thun-01&size=M&quantity=0",
                "productId=ao-thun-01&size=M&quantity=100", "productId=ao-thun-01&size=M&quantity=x",
                "productId=ao-thun-01&size=M&quantity=1.5")) {
            assertThat(cart(session, "POST", "/items", data).statusCode()).as(data).isEqualTo(400);
        }
        assertThat(cart(session, "POST", "/items", "productId=missing&size=M&quantity=1").statusCode()).isEqualTo(404);
        var added = cart(session, "POST", "/items", "productId=ao-thun-01&size=M&quantity=99");
        long id = json.readTree(added.body()).path("items").get(0).path("id").asLong();
        assertThat(cart(session, "POST", "/items", "productId=ao-thun-01&size=M&quantity=1").statusCode()).isEqualTo(400);
        assertThat(cart(session, "PATCH", "/items/" + id, "quantity=-1").statusCode()).isEqualTo(400);
        assertThat(items.findById(id).orElseThrow().getQuantity()).isEqualTo(99);
    }

    @Test
    void concurrentAddsDoNotLoseQuantitiesOrCreateDuplicateLines() throws Exception {
        var owner = account();
        var session = login(owner);
        try (var pool = Executors.newFixedThreadPool(4)) {
            var tasks = new ArrayList<Callable<Integer>>();
            for (int i = 0; i < 8; i++) tasks.add(() -> cart(session, "POST", "/items",
                    "productId=ao-thun-02&size=XL&quantity=1").statusCode());
            for (var result : pool.invokeAll(tasks)) assertThat(result.get()).isEqualTo(200);
        }
        assertThat(items.findByUserIdOrderByIdAsc(owner.getId())).singleElement()
                .satisfies(item -> assertThat(item.getQuantity()).isEqualTo(8));
    }
}
