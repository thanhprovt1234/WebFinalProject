package com.example.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.entity.User;
import com.example.demo.repository.ProductReviewRepository;
import com.example.demo.repository.UserRepository;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProductReviewTests {
    @Value("${local.server.port}") int port;
    @Autowired UserRepository users;
    @Autowired ProductReviewRepository reviews;
    @Autowired PasswordEncoder encoder;

    private URI uri(String path) { return URI.create("http://localhost:" + port + path); }
    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).build(), HttpResponse.BodyHandlers.ofString());
    }
    private HttpResponse<String> post(HttpClient client, String path, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    private String csrf(String html) {
        var matcher = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"").matcher(html);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

    @Test
    void authenticatedReviewsPersistUpdateAndRenderSafely() throws Exception {
        String username = "review-" + UUID.randomUUID();
        var user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setFullName("Khách đánh giá");
        user.setPassword(encoder.encode("review-password"));
        users.saveAndFlush(user);
        String path = "/products/ao-thun-01";
        try (var client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build()) {
            String loginCsrf = csrf(get(client, "/login").body());
            assertThat(post(client, "/login", "username=" + username + "&password=review-password&_csrf=" + loginCsrf)
                    .statusCode()).isEqualTo(302);
            var page = get(client, path);
            assertThat(page.statusCode()).isEqualTo(200);
            assertThat(page.body()).contains("name=\"rating\"", "name=\"comment\"");
            String token = csrf(page.body());
            String comment = "Áo đẹp <script>alert(1)</script>";
            assertThat(post(client, path + "/reviews", "rating=5&comment=" + encode(comment) + "&_csrf=" + token)
                    .statusCode()).isEqualTo(302);
            var stored = reviews.findByCatalogProductIdAndUserId("ao-thun-01", user.getId()).orElseThrow();
            assertThat(stored.getRating()).isEqualTo(5);
            assertThat(stored.getComment()).isEqualTo(comment);
            String html = get(client, path).body();
            assertThat(html).contains("5.0", "100%", "Khách đánh giá", "&lt;script&gt;alert(1)&lt;/script&gt;")
                    .doesNotContain("<script>alert(1)</script>");
            assertThat(get(client, "/products/ao-thun-02").body()).doesNotContain("alert(1)");
            assertThat(post(client, path + "/reviews", "rating=1&comment=" + encode("Cập nhật") + "&_csrf=" + token)
                    .statusCode()).isEqualTo(302);
            var updated = reviews.findByCatalogProductIdAndUserId("ao-thun-01", user.getId()).orElseThrow();
            assertThat(updated.getId()).isEqualTo(stored.getId());
            assertThat(updated.getRating()).isEqualTo(1);
            for (String invalid : new String[] {"rating=0&comment=test", "rating=6&comment=test",
                    "rating=x&comment=test", "comment=test", "rating=3&comment=%20%20", "rating=3&comment=" + "a".repeat(2001)}) {
                var response = post(client, path + "/reviews", invalid + "&_csrf=" + token);
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.body()).contains("role=\"alert\"");
                assertThat(reviews.findByCatalogProductIdAndUserId("ao-thun-01", user.getId()).orElseThrow().getRating())
                        .isEqualTo(1);
            }
            assertThat(post(client, path + "/reviews", "rating=4&comment=test").statusCode()).isEqualTo(403);
            assertThat(post(client, "/products/missing/reviews", "rating=4&comment=test&_csrf=" + token).statusCode())
                    .isEqualTo(404);
        } finally {
            reviews.findByCatalogProductIdAndUserId("ao-thun-01", user.getId()).ifPresent(reviews::delete);
            users.deleteById(user.getId());
        }
    }

    @Test
    void anonymousVisitorsCanReadButCannotSubmit() throws Exception {
        try (var client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build()) {
            var page = get(client, "/products/ao-thun-01");
            assertThat(page.statusCode()).isEqualTo(200);
            assertThat(page.body()).contains("để viết nhận xét").doesNotContain("class=\"review-form\"");
            String token = csrf(get(client, "/login").body());
            var response = post(client, "/products/ao-thun-01/reviews", "rating=5&comment=test&_csrf=" + token);
            assertThat(response.statusCode()).isEqualTo(302);
            assertThat(response.headers().firstValue("Location").orElseThrow()).endsWith("/login");
        }
    }
}
