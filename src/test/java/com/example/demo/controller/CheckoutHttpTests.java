package com.example.demo.controller;

import static org.assertj.core.api.Assertions.*;
import com.example.demo.dto.CartAddRequest;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.service.CartService;
import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties="spring.datasource.url=jdbc:h2:mem:codhttp;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class CheckoutHttpTests {
    @Value("${local.server.port}") int port;
    @Autowired UserRepository users;
    @Autowired ProductRepository products;
    @Autowired ShopRepository shops;
    @Autowired CategoryRepository categories;
    @Autowired ProductVariantRepository variants;
    @Autowired StoreRepository stores;
    @Autowired InventoryRepository inventory;
    @Autowired CartService carts;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired com.example.demo.service.CheckoutService checkout;
    @Autowired com.example.demo.service.VoucherService vouchers;
    @Autowired OrderEntityRepository orders;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    final List<HttpClient> clients=new ArrayList<>();
    record Fixture(User user,Product product,Inventory stock) {}
    User account() {
        var user=new User(); var key=UUID.randomUUID().toString(); user.setUsername(key); user.setEmail(key+"@example.com");
        user.setFullName("COD Buyer"); user.setPassword(encoder.encode("test-password")); return users.saveAndFlush(user);
    }
    Fixture fixture(int stockQuantity) {
        var user=account(); var key=user.getUsername();
        var shop=new Shop(); shop.setOwner(user); shop.setName("COD Shop"); shop.setSlug(key); shop.setStatus(ShopStatus.ACTIVE); shops.saveAndFlush(shop);
        var category=new Category(); category.setName(key); categories.saveAndFlush(category);
        var product=new Product(); product.setShop(shop); product.setCategory(category); product.setName("COD Shirt <test>"); product.setSlug(key);
        product.setBasePrice(new BigDecimal("100000")); product.setImageUrl("/images/aosomi.png"); products.saveAndFlush(product);
        var variant=new ProductVariant(); variant.setProduct(product); variant.setSku(key); variant.setSize("M"); variant.setColor("White"); variants.saveAndFlush(variant);
        var store=new Store(); store.setShop(shop); store.setName("Store"); store.setCode(key); store.setAddress("Address"); stores.saveAndFlush(store);
        var stock=new Inventory(); stock.setStore(store); stock.setProductVariant(variant); stock.setQuantity(stockQuantity); inventory.saveAndFlush(stock);
        add(user,product); return new Fixture(user,product,stock);
    }
    void add(User user,Product product) {
        var add=new CartAddRequest(); add.setProductId(product.getSlug()); add.setSize("M"); add.setColor("White"); add.setQuantity(2);
        carts.add(user.getUsername(),add);
    }
    HttpClient client() {
        var client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build(); clients.add(client); return client;
    }
    HttpRequest request(String path,String data,String csrf) {
        var builder=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path));
        if(csrf!=null) builder.header("X-CSRF-TOKEN",csrf);
        return data==null?builder.GET().build():builder.header("Content-Type","application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(data)).build();
    }
    String csrf(String html) {
        var m=Pattern.compile("name=\"csrf-token\" content=\"([^\"]+)\"").matcher(html); assertThat(m.find()).isTrue(); return m.group(1);
    }
    String body(String html) {
        var token=Pattern.compile("name=\"token\"[^>]*value=\"([^\"]+)\"").matcher(html); assertThat(token.find()).isTrue();
        return "receiverName=Buyer&receiverPhone=0901234567&shippingAddress=12+Test+Street&expectedTotal=200000&token="+token.group(1);
    }
    HttpClient login(User user) throws Exception {
        var c=client(); var page=c.send(request("/login",null,null),HttpResponse.BodyHandlers.ofString());
        assertThat(c.send(request("/login","username="+user.getUsername()+"&password=test-password",csrf(page.body())),HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(302);
        return c;
    }
    @AfterEach void cleanup() {
        clients.forEach(HttpClient::close);
        for(String table:List.of("vouchers","order_stock_allocations","order_status_history","order_items","shop_orders","orders","cart_items","inventories","product_variants","products","stores","shops","categories","user_roles","users")) jdbc.update("DELETE FROM "+table);
    }
    @Test void appliesVoucherShowsDiscountAndConsumesItOnSuccessfulCheckout() throws Exception {
        var f=fixture(8); f.product().setBasePrice(new BigDecimal("300000")); products.saveAndFlush(f.product());
        var orderRequest=new com.example.demo.dto.CheckoutRequest();
        orderRequest.setToken(UUID.randomUUID().toString()); orderRequest.setExpectedTotal(new BigDecimal("600000"));
        orderRequest.setReceiverName("Buyer"); orderRequest.setReceiverPhone("0901234567"); orderRequest.setShippingAddress("12 Test Street");
        var source=checkout.place(f.user().getUsername(),orderRequest);
        new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(status->{
            var order=orders.findById(source).orElseThrow();
            order.getShopOrders().forEach(s->s.setStatus(OrderStatus.DELIVERED)); order.setPaymentStatus("PAID"); vouchers.reward(order);
        });
        var code=vouchers.available(f.user().getUsername()).getFirst(); add(f.user(),f.product());
        var c=login(f.user()); var page=c.send(request("/checkout",null,null),HttpResponse.BodyHandlers.ofString());
        var data=body(page.body()).replace("expectedTotal=200000","expectedTotal=600000")+"&voucherCode="+code;
        var preview=c.send(request("/checkout",data+"&action=apply",csrf(page.body())),HttpResponse.BodyHandlers.ofString());
        assertThat(preview.statusCode()).isEqualTo(200);
        assertThat(preview.body()).contains("value=\"540000.00\"");
        var placed=c.send(request("/checkout",data.replace("expectedTotal=600000","expectedTotal=540000"),csrf(preview.body())),HttpResponse.BodyHandlers.ofString());
        assertThat(placed.statusCode()).isEqualTo(302);
        assertThat(vouchers.available(f.user().getUsername())).isEmpty();
        var list=c.send(request("/orders",null,null),HttpResponse.BodyHandlers.ofString());
        assertThat(list.statusCode()).isEqualTo(200); assertThat(list.body()).contains(code);
        assertThat(jdbc.queryForObject("SELECT total_amount FROM orders WHERE voucher_code=?",BigDecimal.class,code)).isEqualByComparingTo("540000");
    }
    @Test void rendersValidatesCsrfCreatesCodAndConcurrentRetryReturnsSameReceipt() throws Exception {
        var f=fixture(5); var c=login(f.user()); var page=c.send(request("/checkout",null,null),HttpResponse.BodyHandlers.ofString());
        assertThat(page.statusCode()).isEqualTo(200); assertThat(page.body()).contains("COD Shirt &lt;test&gt;","Thanh toán khi nhận hàng");
        var data=body(page.body()); var token=csrf(page.body());
        assertThat(page.body()).contains("id=\"voucherCode\"", "name=\"action\" value=\"apply\"");
        var preview=c.send(request("/checkout",data+"&action=apply&voucherCode=INVALID",token),HttpResponse.BodyHandlers.ofString());
        assertThat(preview.statusCode()).isEqualTo(200);
        assertThat(preview.body()).contains("role=\"alert\"", "INVALID");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM orders",Integer.class)).isZero();
        assertThat(c.send(request("/checkout",data,null),HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(403);
        var invalid=c.send(request("/checkout",data.replace("receiverName=Buyer","receiverName="),token),HttpResponse.BodyHandlers.ofString());
        assertThat(invalid.statusCode()).isEqualTo(200); assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM orders",Integer.class)).isZero();
        var first=c.sendAsync(request("/checkout",data,token),HttpResponse.BodyHandlers.ofString());
        var second=c.sendAsync(request("/checkout",data,token),HttpResponse.BodyHandlers.ofString());
        var result=first.get(); assertThat(result.statusCode()).isEqualTo(302);
        assertThat(second.get().headers().firstValue("Location")).isEqualTo(result.headers().firstValue("Location"));
        var location=URI.create(result.headers().firstValue("Location").orElseThrow());
        assertThat(location.getPath()).isEqualTo("/");
        assertThat(location.getQuery()).isNull();
        var home=c.send(request(location.getPath(),null,null),HttpResponse.BodyHandlers.ofString());
        assertThat(home.statusCode()).isEqualTo(200);
        var orderId=jdbc.queryForObject("SELECT id FROM orders WHERE user_id=?",Long.class,f.user().getId());
        var list=c.send(request("/orders?placed="+orderId,null,null),HttpResponse.BodyHandlers.ofString());
        assertThat(list.statusCode()).isEqualTo(200);
        assertThat(list.body()).contains("Đặt hàng thành công!","data-placed-order=\""+orderId+"\"","Chi tiết đơn hàng","Mã đơn hàng:","Ngày đặt:","Địa chỉ giao hàng","12 Test Street","0901234567");
        var path="/orders/"+orderId;
        var receipt=c.send(request(path,null,null),HttpResponse.BodyHandlers.ofString());
        assertThat(receipt.statusCode()).isEqualTo(200); assertThat(receipt.body()).contains("Chưa thanh toán","Chờ xác nhận","COD Shirt &lt;test&gt;");
        assertThat(c.send(request("/orders",null,null),HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM orders",Integer.class)).isEqualTo(1);
        assertThat(inventory.findById(f.stock().getId()).orElseThrow().getQuantity()).isEqualTo(3);
        assertThat(carts.get(f.user().getUsername()).items()).isEmpty();
        var other=login(account()); assertThat(other.send(request(path,null,null),HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(404);
        assertThat(other.send(request("/orders?placed="+orderId,null,null),HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(404);
        assertThat(other.send(request("/orders",null,null),HttpResponse.BodyHandlers.ofString()).body()).doesNotContain("COD Shirt &lt;test&gt;");
        assertThat(client().send(request("/checkout",null,null),HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(302);
    }
    @Test void priceChangeRollsBackStockAndKeepsCart() throws Exception {
        var f=fixture(5); var c=login(f.user()); var page=c.send(request("/checkout",null,null),HttpResponse.BodyHandlers.ofString());
        f.product().setBasePrice(new BigDecimal("150000")); products.saveAndFlush(f.product());
        var rejected=c.send(request("/checkout",body(page.body()),csrf(page.body())),HttpResponse.BodyHandlers.ofString());
        assertThat(rejected.statusCode()).isEqualTo(200); assertThat(rejected.body()).contains("Giá sản phẩm đã thay đổi");
        assertThat(inventory.findById(f.stock().getId()).orElseThrow().getQuantity()).isEqualTo(5);
        assertThat(carts.get(f.user().getUsername()).items()).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM orders",Integer.class)).isZero();
    }
    @Test void guestCartSurvivesLoginAndCanBePurchased() throws Exception {
        var f=fixture(8); var c=client();
        var product=c.send(request("/products/"+f.product().getSlug(),null,null),HttpResponse.BodyHandlers.ofString());
        var added=c.send(request("/api/cart/items","productId="+f.product().getSlug()+"&size=M&color=White&quantity=2",csrf(product.body())),HttpResponse.BodyHandlers.ofString());
        assertThat(added.statusCode()).isEqualTo(200);
        assertThat(c.send(request("/checkout",null,null),HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(302);
        var login=c.send(request("/login",null,null),HttpResponse.BodyHandlers.ofString());
        var loggedIn=c.send(request("/login","username="+f.user().getUsername()+"&password=test-password",csrf(login.body())),HttpResponse.BodyHandlers.ofString());
        assertThat(loggedIn.statusCode()).isEqualTo(302);
        assertThat(URI.create(loggedIn.headers().firstValue("Location").orElseThrow()).getPath()).isEqualTo("/checkout");
        var page=c.send(request("/checkout",null,null),HttpResponse.BodyHandlers.ofString());
        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(carts.get(f.user().getUsername()).count()).isEqualTo(4);
        var placed=c.send(request("/checkout",body(page.body()).replace("expectedTotal=200000","expectedTotal=400000"),csrf(page.body())),HttpResponse.BodyHandlers.ofString());
        assertThat(placed.statusCode()).isEqualTo(302);
        assertThat(URI.create(placed.headers().firstValue("Location").orElseThrow()).getPath()).isEqualTo("/");
        assertThat(jdbc.queryForObject("SELECT SUM(quantity) FROM order_items",Integer.class)).isEqualTo(4);
        assertThat(inventory.findById(f.stock().getId()).orElseThrow().getQuantity()).isEqualTo(4);
        assertThat(carts.get(f.user().getUsername()).items()).isEmpty();
    }
    @Test void concurrentCustomersCannotOversellLastStock() throws Exception {
        var f=fixture(3); var other=account(); add(other,f.product());
        var a=login(f.user()); var b=login(other);
        var pageA=a.send(request("/checkout",null,null),HttpResponse.BodyHandlers.ofString());
        var pageB=b.send(request("/checkout",null,null),HttpResponse.BodyHandlers.ofString());
        var orderA=a.sendAsync(request("/checkout",body(pageA.body()),csrf(pageA.body())),HttpResponse.BodyHandlers.ofString());
        var orderB=b.sendAsync(request("/checkout",body(pageB.body()),csrf(pageB.body())),HttpResponse.BodyHandlers.ofString());
        assertThat(List.of(orderA.get().statusCode(),orderB.get().statusCode())).containsExactlyInAnyOrder(302,200);
        assertThat(inventory.findById(f.stock().getId()).orElseThrow().getQuantity()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM orders",Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cart_items",Integer.class)).isEqualTo(1);
    }
}
