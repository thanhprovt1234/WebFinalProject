package com.example.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.entity.User;
import com.example.demo.entity.OrderEntity;
import com.example.demo.entity.ShopOrder;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.OrderStatus;
import com.example.demo.repository.OrderEntityRepository;
import com.example.demo.repository.ProductVariantRepository;
import java.math.BigDecimal;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProductReviewTests {
    @Value("${local.server.port}") int port;
    @Autowired UserRepository users;
    @Autowired ProductReviewRepository reviews;
    @Autowired PasswordEncoder encoder;
    @Autowired OrderEntityRepository orders;
    @Autowired JdbcTemplate jdbc;
    @Autowired ProductVariantRepository variants;
    @Autowired PlatformTransactionManager transactions;

    private Long purchase(User user, String sku, OrderStatus status) {
        return new TransactionTemplate(transactions).execute(tx -> {
            var variant = variants.findBySku(sku).orElseThrow();
            var order = new OrderEntity();
            order.setUser(user);
            order.setReceiverName("Review buyer");
            order.setReceiverPhone("0901234567");
            order.setShippingAddress("Test address");
            order.setTotalAmount(BigDecimal.ONE);
            var shopOrder = new ShopOrder();
            shopOrder.setShop(variant.getProduct().getShop());
            shopOrder.setSubtotal(BigDecimal.ONE);
            shopOrder.setTotalAmount(BigDecimal.ONE);
            shopOrder.setStatus(status);
            order.addShopOrder(shopOrder);
            var item = new OrderItem();
            item.setProductVariant(variant);
            item.setProductName(variant.getProduct().getName());
            item.setVariantSku(variant.getSku());
            item.setSize(variant.getSize());
            item.setColor(variant.getColor());
            item.setQuantity(1);
            item.setUnitPrice(BigDecimal.ONE);
            item.setSubtotal(BigDecimal.ONE);
            shopOrder.addItem(item);
            return orders.saveAndFlush(order).getId();
        });
    }

    private void setStatus(Long id, OrderStatus status) {
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            var order = orders.findById(id).orElseThrow();
            order.getShopOrders().forEach(s -> s.setStatus(status));
            orders.flush();
        });
    }

    private void deletePurchase(Long id) {
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            jdbc.update("delete from order_items where shop_order_id in (select id from shop_orders where order_id = ?)", id);
            jdbc.update("delete from shop_orders where order_id = ?", id);
            jdbc.update("delete from orders where id = ?", id);
        });
    }

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
        Long purchaseId = purchase(user, "ao-thun-01-S", OrderStatus.NEW);
        Long otherPurchaseId = purchase(users.findByUsername("catalog-fixture").orElseThrow(),
                "ao-thun-01-M", OrderStatus.DELIVERED);
        Long repeatPurchaseId = purchase(user, "ao-thun-01-M", OrderStatus.NEW);
        String path = "/products/ao-thun-01";
        try (var client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build()) {
            String loginCsrf = csrf(get(client, "/login").body());
            assertThat(post(client, "/login", "username=" + username + "&password=review-password&_csrf=" + loginCsrf)
                    .statusCode()).isEqualTo(302);
            var page = get(client, path);
            assertThat(page.statusCode()).isEqualTo(200);
            String token = csrf(page.body());
            for (var status : OrderStatus.values()) {
                if (status == OrderStatus.DELIVERED) continue;
                setStatus(purchaseId, status);
                assertThat(get(client, "/orders").body()).doesNotContain("class=\"order-review-link\"");
                assertThat(get(client, "/orders/" + purchaseId).body()).doesNotContain("class=\"order-review-link\"");
                assertThat(get(client, path).body()).doesNotContain("class=\"review-form\"")
                        .contains("Bạn chỉ có thể đánh giá sản phẩm đã mua");
                assertThat(post(client, path + "/reviews", "orderId=" + purchaseId + "&rating=5&comment=test&_csrf=" + token)
                        .statusCode()).isEqualTo(403);
                assertThat(reviews.findByCatalogProductIdAndOrderId("ao-thun-01", purchaseId)).isEmpty();
            }
            setStatus(purchaseId, OrderStatus.DELIVERED);
            for (String orderPath : new String[] {"/orders", "/orders/" + purchaseId}) {
                var orderPage = get(client, orderPath);
                assertThat(orderPage.statusCode()).isEqualTo(200);
                assertThat(orderPage.body()).contains("class=\"order-review-link\"", "href=\"" + path + "?orderId=" + purchaseId + "#review-form\"");
            }
            page = get(client, path);
            assertThat(page.body()).contains("id=\"review-form\"", "name=\"rating\"", "name=\"comment\"");
            token = csrf(page.body());
            assertThat(get(client, "/products/ao-thun-02").body()).doesNotContain("class=\"review-form\"");
            assertThat(post(client, "/products/ao-thun-02/reviews", "orderId=" + purchaseId + "&rating=5&comment=test&_csrf=" + token)
                    .statusCode()).isEqualTo(403);
            String comment = "Áo đẹp <script>alert(1)</script>";
            assertThat(post(client, path + "/reviews", "orderId=" + purchaseId + "&rating=5&comment=" + encode(comment) + "&_csrf=" + token)
                    .statusCode()).isEqualTo(302);
            var stored = reviews.findByCatalogProductIdAndOrderId("ao-thun-01", purchaseId).orElseThrow();
            assertThat(stored.getRating()).isEqualTo(5);
            assertThat(stored.getComment()).isEqualTo(comment);
            String html = get(client, path).body();
            assertThat(html).contains("5.0", "100%", "Khách đánh giá", "&lt;script&gt;alert(1)&lt;/script&gt;")
                    .doesNotContain("<script>alert(1)</script>");
            assertThat(get(client, "/products/ao-thun-02").body()).doesNotContain("alert(1)");
            assertThat(post(client, path + "/reviews", "orderId=" + purchaseId + "&rating=1&comment=" + encode("Cập nhật") + "&_csrf=" + token)
                    .statusCode()).isEqualTo(302);
            var updated = reviews.findByCatalogProductIdAndOrderId("ao-thun-01", purchaseId).orElseThrow();
            assertThat(updated.getId()).isEqualTo(stored.getId());
            assertThat(updated.getRating()).isEqualTo(1);
            setStatus(repeatPurchaseId, OrderStatus.DELIVERED);
            assertThat(get(client, path).body())
                    .contains("name=\"orderId\" value=\"" + repeatPurchaseId + "\"");
            assertThat(post(client, path + "/reviews", "rating=5&comment=Missing-order&_csrf=" + token)
                    .statusCode()).isEqualTo(200);
            assertThat(reviews.findByCatalogProductIdAndUserId("ao-thun-01", user.getId())).hasSize(1);
            assertThat(post(client, path + "/reviews", "orderId=" + repeatPurchaseId
                    + "&rating=5&comment=Second-purchase&_csrf=" + token).statusCode()).isEqualTo(302);
            assertThat(reviews.findByCatalogProductIdAndUserId("ao-thun-01", user.getId())).hasSize(2);
            assertThat(reviews.findByCatalogProductIdAndOrderId("ao-thun-01", purchaseId).orElseThrow().getRating())
                    .isEqualTo(1);
            assertThat(get(client, path).body()).contains("3.0", "Second-purchase");
            assertThat(get(client, path + "?orderId=" + repeatPurchaseId).body())
                    .contains("name=\"orderId\" value=\"" + repeatPurchaseId + "\"", "Second-purchase");
            assertThat(post(client, path + "/reviews", "orderId=" + otherPurchaseId
                    + "&rating=5&comment=Forged&_csrf=" + token).statusCode()).isEqualTo(403);
            assertThat(post(client, path + "/reviews", "orderId=" + repeatPurchaseId
                    + "&rating=4&comment=Updated-second&_csrf=" + token).statusCode()).isEqualTo(302);
            assertThat(reviews.findByCatalogProductIdAndUserId("ao-thun-01", user.getId())).hasSize(2);
            assertThat(reviews.findByCatalogProductIdAndOrderId("ao-thun-01", purchaseId).orElseThrow().getRating())
                    .isEqualTo(1);
            setStatus(purchaseId, OrderStatus.RETURNED);
            assertThat(post(client, path + "/reviews", "orderId=" + purchaseId + "&rating=5&comment=test&_csrf=" + token)
                    .statusCode()).isEqualTo(403);
            assertThat(reviews.findByCatalogProductIdAndOrderId("ao-thun-01", purchaseId).orElseThrow().getRating())
                    .isEqualTo(1);
            setStatus(purchaseId, OrderStatus.DELIVERED);
            for (String invalid : new String[] {"orderId=" + purchaseId + "&rating=0&comment=test", "orderId=" + purchaseId + "&rating=6&comment=test",
                    "orderId=" + purchaseId + "&rating=x&comment=test", "orderId=" + purchaseId + "&comment=test", "orderId=" + purchaseId + "&rating=3&comment=%20%20", "orderId=" + purchaseId + "&rating=3&comment=" + "a".repeat(2001)}) {
                var response = post(client, path + "/reviews", invalid + "&_csrf=" + token);
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.body()).contains("role=\"alert\"");
                assertThat(reviews.findByCatalogProductIdAndOrderId("ao-thun-01", purchaseId).orElseThrow().getRating())
                        .isEqualTo(1);
            }
            assertThat(post(client, path + "/reviews", "orderId=" + purchaseId + "&rating=4&comment=test").statusCode()).isEqualTo(403);
            assertThat(post(client, "/products/missing/reviews", "orderId=" + purchaseId + "&rating=4&comment=test&_csrf=" + token).statusCode())
                    .isEqualTo(404);
        } finally {
            reviews.findByCatalogProductIdAndOrderId("ao-thun-01", purchaseId).ifPresent(reviews::delete);
            reviews.findByCatalogProductIdAndOrderId("ao-thun-01", repeatPurchaseId).ifPresent(reviews::delete);
            deletePurchase(repeatPurchaseId);
            deletePurchase(purchaseId);
            deletePurchase(otherPurchaseId);
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
