package com.example.demo.controller;

import static org.assertj.core.api.Assertions.*;
import com.example.demo.dto.*;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.service.*;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties="spring.datasource.url=jdbc:h2:mem:adminmanagement;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class AdminManagementTests {
    @MockitoBean JavaMailSender mailSender;
    @Value("${local.server.port}") int port;
    @Autowired UserRepository users; @Autowired RoleRepository roles; @Autowired ShopRepository shops;
    @Autowired CategoryRepository categories; @Autowired ProductRepository products; @Autowired ProductVariantRepository variants;
    @Autowired StoreRepository stores; @Autowired InventoryRepository inventory; @Autowired OrderEntityRepository orders;
    @Autowired ShopOrderRepository shopOrders; @Autowired OrderStockAllocationRepository allocations;
    @Autowired CartService carts; @Autowired CheckoutService checkout; @Autowired PasswordEncoder encoder; @Autowired JdbcTemplate jdbc;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    @Autowired PurchaseConfirmationService confirmations;
    @Autowired ShipmentRepository shipments;
    @Autowired ShipperService shipping;
    final List<HttpClient> clients=new ArrayList<>();
    record Session(User user,HttpClient client,String csrf) {}
    record Fixture(Shop shop,Product product,ProductVariant variant,Store store,Inventory stock) {}
    HttpResponse<String> request(HttpClient c,String path,String data,String csrf) throws Exception {
        var b=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path));
        if(csrf!=null)b.header("X-CSRF-TOKEN",csrf);
        if(data==null)b.GET();else b.header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(data));
        return c.send(b.build(),HttpResponse.BodyHandlers.ofString());
    }
    String csrf(String html) { var m=Pattern.compile("name=\"csrf-token\" content=\"([^\"]+)\"").matcher(html);assertThat(m.find()).isTrue();return m.group(1); }
    String data(Map<String,?> values) { return values.entrySet().stream().map(e->URLEncoder.encode(e.getKey(),java.nio.charset.StandardCharsets.UTF_8)+"="+URLEncoder.encode(String.valueOf(e.getValue()),java.nio.charset.StandardCharsets.UTF_8)).collect(java.util.stream.Collectors.joining("&")); }
    Session session(boolean admin) throws Exception {
        var key=UUID.randomUUID().toString();var u=new User();u.setUsername(key);u.setEmail(key+"@example.com");u.setFullName("Admin test");u.setPassword(encoder.encode("test-password"));
        var roleName=admin?RoleName.ROLE_ADMIN:RoleName.ROLE_USER;
        var role=roles.findByName(roleName).orElseGet(()->{var r=new Role();r.setName(roleName);return roles.saveAndFlush(r);});u.addRole(role);users.saveAndFlush(u);
        var c=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();clients.add(c);
        var token=csrf(request(c,"/login",null,null).body());assertThat(request(c,"/login","username="+key+"&password=test-password",token).statusCode()).isEqualTo(302);
        return new Session(u,c,csrf(request(c,"/",null,null).body()));
    }
    HttpResponse<String> get(Session s,String path) throws Exception {return request(s.client(),path,null,null);}
    HttpResponse<String> post(Session s,String path,Map<String,?> values) throws Exception {return request(s.client(),path,data(values),s.csrf());}
    Fixture fixture(Session owner) {
        var key=UUID.randomUUID().toString();var shop=new Shop();shop.setOwner(owner.user());shop.setName("Other owner's shop");shop.setSlug(key);shop.setStatus(ShopStatus.ACTIVE);shops.saveAndFlush(shop);
        var category=new Category();category.setName(key);categories.saveAndFlush(category);
        var p=new Product();p.setName("Admin Shirt <test>");p.setSlug(key);p.setShop(shop);p.setCategory(category);p.setBasePrice(new BigDecimal("100000"));p.setImageUrl("/images/aosomi.png");products.saveAndFlush(p);
        var v=new ProductVariant();v.setProduct(p);v.setSku(key);v.setSize("M");v.setColor("White");variants.saveAndFlush(v);
        var st=new Store();st.setShop(shop);st.setName("Branch");st.setCode(key);st.setAddress("Test");stores.saveAndFlush(st);
        var i=new Inventory();i.setStore(st);i.setProductVariant(v);i.setQuantity(5);inventory.saveAndFlush(i);return new Fixture(shop,p,v,st,i);
    }
    Long place(Session buyer,Fixture f) {
        var add=new CartAddRequest();add.setProductId(f.product().getSlug());add.setSize("M");add.setColor("White");add.setQuantity(2);carts.add(buyer.user().getUsername(),add);
        var r=new CheckoutRequest();r.setReceiverName("Buyer");r.setReceiverPhone("0901234567");r.setShippingAddress("12 Test Street");r.setToken(UUID.randomUUID().toString());r.setExpectedTotal(new BigDecimal("200000"));
        return checkout.place(buyer.user().getUsername(),r);
    }
    @AfterEach void cleanup() {
        clients.forEach(HttpClient::close);
        for(var table:List.of("store_managers","vouchers","shipments","order_stock_allocations","order_status_history","order_items","shop_orders","orders","cart_items","inventories","product_variants","products","stores","shops","categories","user_roles","users"))jdbc.update("DELETE FROM "+table);
    }
    Session vendor() throws Exception {
        var session=session(false);
        var role=roles.findByName(RoleName.ROLE_VENDOR).orElseGet(()->{var r=new Role();r.setName(RoleName.ROLE_VENDOR);return roles.saveAndFlush(r);});
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES (?,?)",session.user().getId(),role.getId());
        return session;
    }
    Session courier() throws Exception {
        var s=session(false);
        var role=roles.findByName(RoleName.ROLE_SHIPPER).orElseGet(()->{var r=new Role();r.setName(RoleName.ROLE_SHIPPER);return roles.saveAndFlush(r);});
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES (?,?)",s.user().getId(),role.getId());return s;
    }
    Shipment assign(Session admin,Session shipper,Long partId) throws Exception {
        post(admin,"/admin/orders/"+partId+"/status",Map.of("status","CONFIRMED"));
        assertThat(post(admin,"/admin/orders/"+partId+"/assign",Map.of("shipperId",shipper.user().getId())).statusCode()).isEqualTo(302);
        return shipments.findByShopOrderId(partId).orElseThrow();
    }
    HttpResponse<String> progress(Session s,Shipment shipment,String status,boolean cod,String note) throws Exception {
        return post(s,"/shipper/shipments/"+shipment.getId()+"/status",Map.of("status",status,"codCollected",cod,"note",note));
    }
    HttpResponse<String> deliver(Session admin,Long partId) throws Exception {
        var shipment=shipments.findByShopOrderId(partId);
        if(shipment.isPresent())return progress(admin,shipment.get(),"DELIVERED",true,"");
        var s=courier();var assigned=assign(admin,s,partId);
        progress(s,assigned,"PICKED_UP",false,"");progress(s,assigned,"SHIPPING",false,"");
        return progress(s,assigned,"DELIVERED",true,"");
    }
    Map<String,Object> courierDashboard(Session s) {
        var context=org.springframework.security.core.context.SecurityContextHolder.getContext();var previous=context.getAuthentication();
        context.setAuthentication(org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(
                s.user().getUsername(),"",List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SHIPPER"))));
        try{return shipping.dashboard(s.user().getUsername(),"",null,null,null,0);}
        finally{context.setAuthentication(previous);}
    }
    @Test void revenueIsScopedByShopAndManagerAndCountsOnlyDeliveredOrders() throws Exception {
        var admin = session(true); var seller = vendor(); var outsider = vendor(); var buyer = session(false);
        var manager = session(false);
        var managerRole = roles.findByName(RoleName.ROLE_MANAGER).orElseGet(() -> {
            var r = new Role(); r.setName(RoleName.ROLE_MANAGER); return roles.saveAndFlush(r);
        });
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES (?,?)", manager.user().getId(), managerRole.getId());
        var f = fixture(seller); var other = fixture(outsider);
        jdbc.update("UPDATE shops SET name=? WHERE id=?", "Revenue Alpha", f.shop().getId());
        jdbc.update("UPDATE shops SET name=? WHERE id=?", "Revenue Secret", other.shop().getId());
        jdbc.update("INSERT INTO store_managers(user_id,store_id) VALUES (?,?)", manager.user().getId(), f.store().getId());
        var delivered = place(buyer, f); var pending = place(buyer, f); var hidden = place(buyer, other);
        jdbc.update("UPDATE shop_orders SET status='DELIVERED' WHERE order_id=?", delivered);
        jdbc.update("UPDATE shop_orders SET status='DELIVERED' WHERE order_id=?", hidden);
        jdbc.update("UPDATE orders SET order_date='2026-10-01 17:00:00' WHERE id=?", delivered);
        jdbc.update("UPDATE orders SET order_date='2026-10-02 16:59:59' WHERE id=?", pending);
        var range = "?from=2026-10-02&to=2026-10-02";
        var page = get(admin, "/admin/revenue" + range + "&shopId=" + f.shop().getId());
        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.body()).contains("Revenue Alpha", "200,000", ">2</td><td>1</td><td>1</td><td>0</td>")
                .doesNotContain("Revenue Secret</td>");
        var sellerPage = get(seller, "/seller/revenue" + range);
        assertThat(sellerPage.statusCode()).isEqualTo(200);
        assertThat(sellerPage.body()).contains("Revenue Alpha", "200,000").doesNotContain("Revenue Secret");
        var managerPage = get(manager, "/manager/revenue" + range);
        assertThat(managerPage.statusCode()).isEqualTo(200);
        assertThat(managerPage.body()).contains("Revenue Alpha", "200,000").doesNotContain("Revenue Secret");
        assertThat(get(manager, "/manager/revenue" + range + "&shopId=" + other.shop().getId()).statusCode()).isEqualTo(403);
        assertThat(get(seller, "/seller/revenue" + range + "&shopId=" + other.shop().getId()).statusCode()).isEqualTo(403);
        assertThat(get(buyer, "/admin/revenue").statusCode()).isEqualTo(403);
        assertThat(get(buyer, "/manager/revenue").statusCode()).isEqualTo(403);
        assertThat(get(admin, "/admin/revenue?from=2026-10-03&to=2026-10-01").statusCode()).isEqualTo(400);
        // Both local-day boundaries are exclusive outside this range.
        var nextDay = get(seller, "/seller/revenue?from=2026-10-03&to=2026-10-03");
        assertThat(nextDay.statusCode()).isEqualTo(200);
        assertThat(nextDay.body()).contains(">0</td><td>0</td><td>0</td><td>0</td>");
        jdbc.update("UPDATE shop_orders SET status='CANCELLED',total_amount=0 WHERE order_id=?", delivered);
        var cancelled = get(seller, "/seller/revenue" + range);
        assertThat(cancelled.body()).contains(">2</td><td>0</td><td>1</td><td>1</td>").doesNotContain("200,000");
    }
    @Test void courierHandsOverCodAndShopConfirmsWithoutPayingTwiceOrResendingMail() throws Exception {
        var admin=session(true);var seller=vendor();var buyer=session(false);var s=courier();var f=fixture(seller);
        var id=place(buyer,f);var part=shopOrders.findByOrderId(id).getFirst();var sh=assign(admin,s,part.getId());
        progress(s,sh,"PICKED_UP",false,"");progress(s,sh,"SHIPPING",false,"");progress(s,sh,"DELIVERED",true,"");
        var remit="/shipper/shipments/"+sh.getId()+"/cod/remit";var confirm="/seller/orders/"+part.getId()+"/cod/confirm";
        assertThat(get(seller,"/seller/orders/"+part.getId()).body()).contains("Đã thu COD · Chờ bàn giao cho shop").doesNotContain("action=\""+confirm+"\"");
        assertThat((BigDecimal)courierDashboard(s).get("unremitted")).isEqualByComparingTo("200000");
        assertThat(post(s,remit,Map.of("amount","200000","confirmed",true,"note","Cash <script>")).statusCode()).isEqualTo(302);
        var handedOver=shipments.findById(sh.getId()).orElseThrow();assertThat(handedOver.getCodRemittedAt()).isNotNull();assertThat(handedOver.getCodConfirmedAt()).isNull();
        assertThat(get(seller,"/seller/orders/"+part.getId()).body()).contains(confirm,"Cash &lt;script&gt;").doesNotContain("Cash <script>");
        assertThat(get(seller,"/seller/orders").body()).contains("Chờ shop xác nhận");
        var pending=courierDashboard(s);assertThat((BigDecimal)pending.get("unremitted")).isEqualByComparingTo("0");
        assertThat((BigDecimal)pending.get("awaitingConfirmation")).isEqualByComparingTo("200000");
        assertThat(post(seller,confirm,Map.of("amount","200000","confirmed",true,"note","Received <test>")).statusCode()).isEqualTo(302);
        var finished=shipments.findById(sh.getId()).orElseThrow();assertThat(finished.getCodConfirmedAt()).isNotNull();assertThat(finished.getCodConfirmedBy()).isEqualTo(seller.user().getUsername());
        assertThat(get(s,"/shipper/shipments/"+sh.getId()).body()).contains("Shop đã xác nhận nhận COD","Received &lt;test&gt;").doesNotContain("Received <test>");
        var settled=courierDashboard(s);assertThat((BigDecimal)settled.get("awaitingConfirmation")).isEqualByComparingTo("0");
        assertThat((BigDecimal)settled.get("confirmedCod")).isEqualByComparingTo("200000");
        assertThat(orders.findById(id).orElseThrow().getPaymentStatus()).isEqualTo("PAID");
        assertThat(shopOrders.findById(part.getId()).orElseThrow().getTotalAmount()).isEqualByComparingTo("200000");
        assertThat(inventory.findById(f.stock().getId()).orElseThrow().getQuantity()).isEqualTo(3);
        post(s,remit,Map.of("amount","200000","confirmed",true,"note","Duplicate"));
        post(seller,confirm,Map.of("amount","200000","confirmed",true,"note","Duplicate"));
        var repeated=shipments.findById(sh.getId()).orElseThrow();assertThat(repeated.getCodRemittedAt()).isEqualTo(handedOver.getCodRemittedAt());
        assertThat(repeated.getCodConfirmedAt()).isEqualTo(finished.getCodConfirmedAt());assertThat(repeated.getCodConfirmationNote()).isEqualTo("Received <test>");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM order_status_history WHERE shop_order_id=? AND note LIKE ?",Integer.class,part.getId(),"Bàn giao COD — %")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM order_status_history WHERE shop_order_id=? AND note LIKE ?",Integer.class,part.getId(),"Shop xác nhận nhận COD — %")).isEqualTo(1);
        verify(mailSender,times(1)).send(any(SimpleMailMessage.class));
    }
    @Test void codSettlementBlocksWrongRolesOtherOwnersCsrfMissingMoneyAndEarlyConfirmation() throws Exception {
        var admin=session(true);var seller=vendor();var outsider=vendor();var buyer=session(false);var s=courier();var other=courier();var f=fixture(seller);
        var id=place(buyer,f);var part=shopOrders.findByOrderId(id).getFirst();var sh=assign(admin,s,part.getId());
        var remit="/shipper/shipments/"+sh.getId()+"/cod/remit";var confirm="/seller/orders/"+part.getId()+"/cod/confirm";
        var valid=Map.<String,Object>of("amount","200000","confirmed",true);
        post(s,remit,valid);post(seller,confirm,valid);assertThat(shipments.findById(sh.getId()).orElseThrow().getCodRemittedAt()).isNull();
        progress(s,sh,"PICKED_UP",false,"");progress(s,sh,"SHIPPING",false,"");progress(s,sh,"DELIVERED",true,"");
        assertThat(post(other,remit,valid).statusCode()).isEqualTo(403);assertThat(post(buyer,remit,valid).statusCode()).isEqualTo(403);
        assertThat(post(s,confirm,valid).statusCode()).isEqualTo(403);assertThat(post(outsider,confirm,valid).statusCode()).isEqualTo(403);
        assertThat(request(s.client(),remit,"amount=200000&confirmed=true",null).statusCode()).isEqualTo(403);
        assertThat(request(seller.client(),confirm,"amount=200000&confirmed=true",null).statusCode()).isEqualTo(403);
        post(s,remit,Map.of("amount","1","confirmed",true));post(s,remit,Map.of("amount","200000"));
        post(s,remit,Map.of("amount","200000","confirmed",true,"note","x".repeat(501)));
        assertThat(shipments.findById(sh.getId()).orElseThrow().getCodRemittedAt()).isNull();
        post(seller,confirm,valid);assertThat(shipments.findById(sh.getId()).orElseThrow().getCodConfirmedAt()).isNull();
        post(s,remit,valid);
        post(seller,confirm,Map.of("amount","1","confirmed",true));post(seller,confirm,Map.of("amount","200000"));
        assertThat(shipments.findById(sh.getId()).orElseThrow().getCodConfirmedAt()).isNull();
        post(admin,"/admin/orders/"+part.getId()+"/cod/confirm",valid);
        assertThat(shipments.findById(sh.getId()).orElseThrow().getCodConfirmedBy()).isEqualTo(admin.user().getUsername());
    }
    @Test void multiShopCodIsSettledPerShopEvenWhenOtherPartIsStillBeingDelivered() throws Exception {
        var admin=session(true);var a=vendor();var b=vendor();var buyer=session(false);var s=courier();var fa=fixture(a);var fb=fixture(b);
        for(var f:List.of(fa,fb)){var add=new CartAddRequest();add.setProductId(f.product().getSlug());add.setSize("M");add.setColor("White");add.setQuantity(2);carts.add(buyer.user().getUsername(),add);}
        var r=new CheckoutRequest();r.setReceiverName("Buyer");r.setReceiverPhone("0901234567");r.setShippingAddress("12 Test Street");r.setToken(UUID.randomUUID().toString());r.setExpectedTotal(new BigDecimal("400000"));
        var id=checkout.place(buyer.user().getUsername(),r);var parts=shopOrders.findByOrderId(id);
        var pa=parts.stream().filter(p->p.getShop().getId().equals(fa.shop().getId())).findFirst().orElseThrow();
        var pb=parts.stream().filter(p->p.getShop().getId().equals(fb.shop().getId())).findFirst().orElseThrow();
        var sa=assign(admin,s,pa.getId());var sb=assign(admin,s,pb.getId());
        progress(s,sa,"PICKED_UP",false,"");progress(s,sa,"SHIPPING",false,"");progress(s,sa,"DELIVERED",true,"");
        var valid=Map.<String,Object>of("amount","200000","confirmed",true);
        post(s,"/shipper/shipments/"+sa.getId()+"/cod/remit",valid);
        assertThat(post(b,"/seller/orders/"+pa.getId()+"/cod/confirm",valid).statusCode()).isEqualTo(403);
        post(a,"/seller/orders/"+pa.getId()+"/cod/confirm",valid);
        assertThat(shipments.findById(sa.getId()).orElseThrow().getCodConfirmedAt()).isNotNull();
        assertThat(shipments.findById(sb.getId()).orElseThrow().getCodConfirmedAt()).isNull();
        assertThat(orders.findById(id).orElseThrow().getPaymentStatus()).isEqualTo("UNPAID");verifyNoInteractions(mailSender);
        progress(s,sb,"PICKED_UP",false,"");progress(s,sb,"SHIPPING",false,"");progress(s,sb,"DELIVERED",true,"");
        assertThat(orders.findById(id).orElseThrow().getPaymentStatus()).isEqualTo("PAID");
        assertThat((BigDecimal)courierDashboard(s).get("unremitted")).isEqualByComparingTo("200000");
        assertThat((BigDecimal)courierDashboard(s).get("confirmedCod")).isEqualByComparingTo("200000");verify(mailSender,times(1)).send(any(SimpleMailMessage.class));
    }
    @Test void legacyShipmentWithoutRecordedCodDoesNotCreateAnAssumedSettlement() throws Exception {
        var admin=session(true);var seller=vendor();var buyer=session(false);var s=courier();var f=fixture(seller);
        var id=place(buyer,f);var part=shopOrders.findByOrderId(id).getFirst();var sh=assign(admin,s,part.getId());
        progress(s,sh,"PICKED_UP",false,"");progress(s,sh,"SHIPPING",false,"");progress(s,sh,"DELIVERED",true,"");
        jdbc.update("UPDATE shipments SET cod_collected_amount=NULL WHERE id=?",sh.getId());
        post(s,"/shipper/shipments/"+sh.getId()+"/cod/remit",Map.of("amount","200000","confirmed",true));
        assertThat(shipments.findById(sh.getId()).orElseThrow().getCodRemittedAt()).isNull();
        assertThat(get(seller,"/seller/orders/"+part.getId()).body()).contains("Cần đối soát số COD của đơn cũ");
        assertThat((BigDecimal)courierDashboard(s).get("unremitted")).isEqualByComparingTo("0");
    }
    @Test void assignmentRequiresApprovalActiveShipperAdminAndCsrfAndCannotDuplicate() throws Exception {
        var admin=session(true);var buyer=session(false);var s=courier();var other=courier();var f=fixture(buyer);
        var id=place(buyer,f);var part=shopOrders.findByOrderId(id).getFirst();var path="/admin/orders/"+part.getId()+"/assign";
        post(admin,path,Map.of("shipperId",s.user().getId()));assertThat(shipments.count()).isZero();
        post(admin,"/admin/orders/"+part.getId()+"/status",Map.of("status","CONFIRMED"));
        post(admin,path,Map.of("shipperId",buyer.user().getId()));assertThat(shipments.count()).isZero();
        assertThat(post(s,path,Map.of("shipperId",s.user().getId())).statusCode()).isEqualTo(403);
        assertThat(request(admin.client(),path,"shipperId="+s.user().getId(),null).statusCode()).isEqualTo(403);
        jdbc.update("UPDATE users SET enabled=0 WHERE id=?",s.user().getId());
        post(admin,path,Map.of("shipperId",s.user().getId()));assertThat(shipments.count()).isZero();
        jdbc.update("UPDATE users SET enabled=1 WHERE id=?",s.user().getId());
        var first=java.util.concurrent.CompletableFuture.supplyAsync(()->{try{return post(admin,path,Map.of("shipperId",s.user().getId())).statusCode();}catch(Exception e){throw new RuntimeException(e);}});
        var second=java.util.concurrent.CompletableFuture.supplyAsync(()->{try{return post(admin,path,Map.of("shipperId",other.user().getId())).statusCode();}catch(Exception e){throw new RuntimeException(e);}});
        assertThat(first.get()).isEqualTo(302);assertThat(second.get()).isEqualTo(302);assertThat(shipments.count()).isEqualTo(1);
        verifyNoInteractions(mailSender);
    }
    @Test void shipperWorkflowRejectsSkipsRequiresCodAndSupportsFailureAndRetry() throws Exception {
        var admin=session(true);var buyer=session(false);var s=courier();var stranger=courier();var f=fixture(buyer);
        var id=place(buyer,f);var part=shopOrders.findByOrderId(id).getFirst();var sh=assign(admin,s,part.getId());
        assertThat(get(s,"/shipper").statusCode()).isEqualTo(200);
        assertThat(get(s,"/shipper/shipments/"+sh.getId()).body()).contains("Branch","Test","0901234567","google.com/maps","Admin Shirt &lt;test&gt;");
        assertThat(get(stranger,"/shipper/shipments/"+sh.getId()).statusCode()).isEqualTo(403);
        assertThat(progress(stranger,sh,"PICKED_UP",false,"").statusCode()).isEqualTo(403);
        assertThat(request(s.client(),"/shipper/shipments/"+sh.getId()+"/status","status=PICKED_UP",null).statusCode()).isEqualTo(403);
        progress(s,sh,"DELIVERED",true,"");assertThat(shipments.findById(sh.getId()).orElseThrow().getStatus()).isEqualTo(ShipmentStatus.ASSIGNED);
        progress(s,sh,"PICKED_UP",false,"");assertThat(shopOrders.findById(part.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PICKED_UP);
        post(admin,"/admin/orders/"+part.getId()+"/status",Map.of("status","CANCELLED","note","Cannot cancel after pickup"));
        assertThat(shopOrders.findById(part.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PICKED_UP);
        progress(s,sh,"SHIPPING",false,"");progress(s,sh,"FAILED",false,"");
        assertThat(shipments.findById(sh.getId()).orElseThrow().getStatus()).isEqualTo(ShipmentStatus.SHIPPING);
        progress(s,sh,"FAILED",false,"Customer unavailable <script>");
        assertThat(shipments.findById(sh.getId()).orElseThrow().getStatus()).isEqualTo(ShipmentStatus.FAILED);
        assertThat(shopOrders.findById(part.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.SHIPPING);
        assertThat(get(s,"/shipper/shipments/"+sh.getId()).body()).contains("Customer unavailable &lt;script&gt;").doesNotContain("Customer unavailable <script>");
        assertThat(inventory.findById(f.stock().getId()).orElseThrow().getQuantity()).isEqualTo(3);
        progress(s,sh,"SHIPPING",false,"Retry");progress(s,sh,"DELIVERED",false,"");
        assertThat(orders.findById(id).orElseThrow().getPaymentStatus()).isEqualTo("UNPAID");
        verifyNoInteractions(mailSender);
        progress(s,sh,"DELIVERED",true,"");progress(s,sh,"DELIVERED",true,"");
        var finished=shipments.findById(sh.getId()).orElseThrow();assertThat(finished.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(finished.getPickedUpAt()).isNotNull();assertThat(finished.getDeliveredAt()).isNotNull();
        assertThat(orders.findById(id).orElseThrow().getPaymentStatus()).isEqualTo("PAID");
        assertThat(inventory.findById(f.stock().getId()).orElseThrow().getQuantity()).isEqualTo(3);
        verify(mailSender,times(1)).send(any(SimpleMailMessage.class));
        assertThat(get(s,"/shipper/statistics").statusCode()).isEqualTo(200);
        jdbc.update("DELETE FROM user_roles WHERE user_id=? AND role_id IN (SELECT id FROM roles WHERE name='ROLE_SHIPPER')",s.user().getId());
        assertThat(get(s,"/shipper").statusCode()).isEqualTo(403);
        assertThat(progress(s,sh,"SHIPPING",false,"").statusCode()).isEqualTo(403);
    }
    @Test void failedReturnRequiresAdminAndRestoresStockExactlyOnce() throws Exception {
        var admin=session(true);var seller=vendor();var buyer=session(false);var s=courier();var f=fixture(seller);
        var id=place(buyer,f);var part=shopOrders.findByOrderId(id).getFirst();var sh=assign(admin,s,part.getId());
        progress(s,sh,"PICKED_UP",false,"");progress(s,sh,"SHIPPING",false,"");progress(s,sh,"FAILED",false,"Customer refused");
        post(seller,"/seller/orders/"+part.getId()+"/status",Map.of("status","CANCELLED","note","Not allowed"));
        assertThat(shipments.findById(sh.getId()).orElseThrow().getStatus()).isEqualTo(ShipmentStatus.FAILED);
        var path="/admin/orders/"+part.getId()+"/status";
        post(admin,path,Map.of("status","CANCELLED"));assertThat(inventory.findById(f.stock().getId()).orElseThrow().getQuantity()).isEqualTo(3);
        post(admin,path,Map.of("status","CANCELLED","note","Received returned parcel"));
        post(admin,path,Map.of("status","CANCELLED","note","Duplicate"));progress(s,sh,"SHIPPING",false,"");
        assertThat(shipments.findById(sh.getId()).orElseThrow().getStatus()).isEqualTo(ShipmentStatus.CANCELLED);
        assertThat(inventory.findById(f.stock().getId()).orElseThrow().getQuantity()).isEqualTo(5);
        assertThat(orders.findById(id).orElseThrow().getPaymentStatus()).isEqualTo("CANCELLED");verifyNoInteractions(mailSender);
    }
    @Test void assignedCancellationClosesShipmentAndPickupCannotReopenIt() throws Exception {
        var admin=session(true);var buyer=session(false);var s=courier();var f=fixture(buyer);
        var id=place(buyer,f);var part=shopOrders.findByOrderId(id).getFirst();var sh=assign(admin,s,part.getId());
        post(admin,"/admin/orders/"+part.getId()+"/status",Map.of("status","CANCELLED","note","Before pickup"));
        progress(s,sh,"PICKED_UP",false,"");
        assertThat(shipments.findById(sh.getId()).orElseThrow().getStatus()).isEqualTo(ShipmentStatus.CANCELLED);
        assertThat(shopOrders.findById(part.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(inventory.findById(f.stock().getId()).orElseThrow().getQuantity()).isEqualTo(5);
    }
    @Test void legacyAssignmentCannotSkipShopConfirmation() throws Exception {
        var admin=session(true);var buyer=session(false);var s=courier();var f=fixture(buyer);
        var id=place(buyer,f);var part=shopOrders.findByOrderId(id).getFirst();var sh=assign(admin,s,part.getId());
        jdbc.update("UPDATE shop_orders SET status='NEW' WHERE id=?",part.getId());
        progress(s,sh,"PICKED_UP",false,"");
        assertThat(shipments.findById(sh.getId()).orElseThrow().getStatus()).isEqualTo(ShipmentStatus.ASSIGNED);
        assertThat(shopOrders.findById(part.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.NEW);
        verifyNoInteractions(mailSender);
    }
    @Test void statisticsUseVietnamAssignmentDatesAndNeverIncludeAnotherShippersOrders() throws Exception {
        var admin=session(true);var buyer=session(false);var s=courier();var other=courier();var f=fixture(buyer);
        var id=place(buyer,f);var part=shopOrders.findByOrderId(id).getFirst();var sh=assign(admin,s,part.getId());
        progress(s,sh,"PICKED_UP",false,"");progress(s,sh,"SHIPPING",false,"");progress(s,sh,"DELIVERED",true,"");
        // 17:00 UTC is the start of the next day in Vietnam.
        jdbc.update("UPDATE shipments SET assigned_at=? WHERE id=?",java.time.LocalDateTime.of(2026,10,25,17,0),sh.getId());
        var id2=place(buyer,f);var part2=shopOrders.findByOrderId(id2).getFirst();assign(admin,other,part2.getId());
        var auth=org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(s.user().getUsername(),"",List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SHIPPER")));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            var day=java.time.LocalDate.of(2026,10,26);
            var stats=shipping.dashboard(s.user().getUsername(),"",null,day,day,0);
            assertThat(stats.get("total")).isEqualTo(1);assertThat((BigDecimal)stats.get("collected")).isEqualByComparingTo("200000");
            assertThat(stats.get("rate")).isEqualTo(100L);
            assertThat(shipping.dashboard(s.user().getUsername(),"",null,day.minusDays(1),day.minusDays(1),0).get("total")).isEqualTo(0);
            var searched=shipping.dashboard(s.user().getUsername(),"no match",ShipmentStatus.FAILED,day,day,99);
            assertThat(searched.get("filteredTotal")).isEqualTo(0);assertThat(searched.get("total")).isEqualTo(1);
        } finally {org.springframework.security.core.context.SecurityContextHolder.clearContext();}
        assertThat(get(s,"/shipper?from=2026-10-26&to=2026-10-26").body()).contains("Đơn shop #"+part.getId()).doesNotContain("Đơn shop #"+part2.getId());
        assertThat(get(s,"/shipper?from=2026-10-27&to=2026-10-26").statusCode()).isEqualTo(200);
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"BANK","COD"})
    void prepaidDeliveryDoesNotRequireCodAndLoyaltyIsIssuedOnceAfterDelivery(String method) throws Exception {
        var admin=session(true);var buyer=session(false);var s=courier();var f=fixture(buyer);
        var id=place(buyer,f);var part=shopOrders.findByOrderId(id).getFirst();
        // Simulate an order settled by a payment provider before shipping.
        jdbc.update("UPDATE orders SET payment_method=?, payment_status='PAID', total_amount=600000 WHERE id=?",method,id);
        jdbc.update("UPDATE shop_orders SET subtotal=600000,total_amount=600000 WHERE id=?",part.getId());
        var sh=assign(admin,s,part.getId());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM vouchers",Integer.class)).isZero();
        progress(s,sh,"PICKED_UP",false,"");progress(s,sh,"SHIPPING",false,"");progress(s,sh,"DELIVERED",false,"");
        progress(s,sh,"DELIVERED",false,"");
        assertThat(shipments.findById(sh.getId()).orElseThrow().getCodCollectedAmount()).isEqualByComparingTo("0");
        post(s,"/shipper/shipments/"+sh.getId()+"/cod/remit",Map.of("amount","600000","confirmed",true));
        assertThat(shipments.findById(sh.getId()).orElseThrow().getCodRemittedAt()).isNull();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM vouchers WHERE source_order_id=?",Integer.class,id)).isEqualTo(1);
        verify(mailSender,times(1)).send(any(SimpleMailMessage.class));
    }
    @Test void sellerCanManageOwnOrdersButCannotReadOrChangeAnotherShop() throws Exception {
        var a=vendor();var b=vendor();var buyer=session(false);var fa=fixture(a);var fb=fixture(b);
        var idA=place(buyer,fa);var idB=place(buyer,fb);
        var partA=shopOrders.findByOrderId(idA).getFirst();var partB=shopOrders.findByOrderId(idB).getFirst();
        var list=get(a,"/seller/orders");assertThat(list.statusCode()).isEqualTo(200);
        assertThat(list.body()).contains("/seller/orders/"+partA.getId()).doesNotContain("/seller/orders/"+partB.getId());
        assertThat(get(a,"/seller/orders/"+partA.getId()).statusCode()).isEqualTo(200);
        assertThat(get(a,"/seller/orders/"+partB.getId()).statusCode()).isEqualTo(403);
        assertThat(post(a,"/seller/orders/"+partB.getId()+"/status",Map.of("status","CANCELLED","note","Unauthorized")).statusCode()).isEqualTo(403);
        assertThat(shopOrders.findById(partB.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(inventory.findById(fb.stock().getId()).orElseThrow().getQuantity()).isEqualTo(3);
        assertThat(request(a.client(),"/seller/orders/"+partA.getId()+"/status","status=DELIVERED",null).statusCode()).isEqualTo(403);
        assertThat(post(a,"/seller/orders/"+partA.getId()+"/status",Map.of("status","DELIVERED")).statusCode()).isEqualTo(302);
        assertThat(shopOrders.findById(partA.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.NEW);
        post(a,"/seller/orders/"+partA.getId()+"/status",Map.of("status","CONFIRMED"));
        assertThat(shopOrders.findById(partA.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(orders.findById(idA).orElseThrow().getPaymentStatus()).isEqualTo("UNPAID");
        assertThat(get(a,"/admin/orders").statusCode()).isEqualTo(403);
        assertThat(get(buyer,"/seller/orders").statusCode()).isEqualTo(403);
        jdbc.update("DELETE FROM user_roles WHERE user_id=? AND role_id IN (SELECT id FROM roles WHERE name='ROLE_VENDOR')",a.user().getId());
        assertThat(get(a,"/seller/orders").statusCode()).isEqualTo(403);
    }
    @Test void sellerMultiShopReceiptContainsOnlyOwnItemsAndTotal() throws Exception {
        var a=vendor();var b=vendor();var buyer=session(false);var fa=fixture(a);var fb=fixture(b);
        fb.product().setName("PRIVATE OTHER SHOP ITEM");products.saveAndFlush(fb.product());
        var add=new CartAddRequest();add.setProductId(fb.product().getSlug());add.setSize("M");add.setColor("White");add.setQuantity(1);carts.add(buyer.user().getUsername(),add);
        add=new CartAddRequest();add.setProductId(fa.product().getSlug());add.setSize("M");add.setColor("White");add.setQuantity(1);carts.add(buyer.user().getUsername(),add);
        var r=new CheckoutRequest();r.setReceiverName("Buyer");r.setReceiverPhone("0901234567");r.setShippingAddress("12 Test Street");r.setToken(UUID.randomUUID().toString());r.setExpectedTotal(new BigDecimal("200000"));
        var id=checkout.place(buyer.user().getUsername(),r);
        var part=shopOrders.findByOrderId(id).stream().filter(o->o.getShop().getId().equals(fa.shop().getId())).findFirst().orElseThrow();
        var page=get(a,"/seller/orders/"+part.getId());assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.body()).contains("Admin Shirt &lt;test&gt;","100,000").doesNotContain("PRIVATE OTHER SHOP ITEM","200,000");
        post(a,"/seller/orders/"+part.getId()+"/status",Map.of("status","CANCELLED","note","Cancel own part"));
        assertThat(inventory.findById(fa.stock().getId()).orElseThrow().getQuantity()).isEqualTo(5);
        assertThat(inventory.findById(fb.stock().getId()).orElseThrow().getQuantity()).isEqualTo(4);
        assertThat(orders.findById(id).orElseThrow().getTotalAmount()).isEqualByComparingTo("100000");
    }
    @Test void customerCancelsPendingOrderAndNotifiesOnlyItsShopWithExactlyOneRestock() throws Exception {
        var admin=session(true);var seller=vendor();var outsider=vendor();var buyer=session(false);var stranger=session(false);
        var f=fixture(seller);var id=place(buyer,f);var part=shopOrders.findByOrderId(id).getFirst();var path="/orders/"+id+"/cancel";
        assertThat(get(buyer,"/orders").body()).contains(path,"Hủy phần đơn đang chờ duyệt");
        assertThat(get(buyer,"/orders/"+id).body()).contains(path);
        assertThat(post(stranger,path,Map.of()).statusCode()).isEqualTo(404);
        assertThat(request(buyer.client(),path,"reason=test",null).statusCode()).isEqualTo(403);
        assertThat(inventory.findById(f.stock().getId()).orElseThrow().getQuantity()).isEqualTo(3);
        assertThat(post(buyer,path,Map.of("reason","Changed mind <script>")).statusCode()).isEqualTo(302);
        assertThat(post(buyer,path,Map.of("reason","Repeat")).statusCode()).isEqualTo(302);
        assertThat(shopOrders.findById(part.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(inventory.findById(f.stock().getId()).orElseThrow().getQuantity()).isEqualTo(5);
        assertThat(orders.findById(id).orElseThrow().getTotalAmount()).isEqualByComparingTo("0");
        assertThat(orders.findById(id).orElseThrow().getPaymentStatus()).isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM order_status_history WHERE shop_order_id=? AND status='CANCELLED'",Integer.class,part.getId())).isEqualTo(1);
        for(var page:List.of(get(admin,"/admin"),get(admin,"/admin/orders"),get(seller,"/seller/orders"))) {
            assertThat(page.statusCode()).isEqualTo(200);
            assertThat(page.body()).contains("Khách hàng hủy đơn","Changed mind &lt;script&gt;").doesNotContain("Changed mind <script>");
        }
        assertThat(get(outsider,"/seller/orders").body()).doesNotContain("Changed mind");
        assertThat(get(buyer,"/orders/"+id).body()).doesNotContain(path);
        post(admin,"/admin/orders/"+part.getId()+"/status",Map.of("status","DELIVERED"));
        assertThat(shopOrders.findById(part.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
    @Test void customerCanCancelOnlyPendingPartOfMultiShopOrder() throws Exception {
        var admin=session(true);var a=vendor();var b=vendor();var buyer=session(false);var fa=fixture(a);var fb=fixture(b);
        for(var f:List.of(fa,fb)) {
            var add=new CartAddRequest();add.setProductId(f.product().getSlug());add.setSize("M");add.setColor("White");add.setQuantity(2);carts.add(buyer.user().getUsername(),add);
        }
        var r=new CheckoutRequest();r.setReceiverName("Buyer");r.setReceiverPhone("0901234567");r.setShippingAddress("12 Test Street");
        r.setToken(UUID.randomUUID().toString());r.setExpectedTotal(new BigDecimal("400000"));var id=checkout.place(buyer.user().getUsername(),r);
        var parts=shopOrders.findByOrderId(id);var partA=parts.stream().filter(o->o.getShop().getId().equals(fa.shop().getId())).findFirst().orElseThrow();
        var partB=parts.stream().filter(o->o.getShop().getId().equals(fb.shop().getId())).findFirst().orElseThrow();
        deliver(admin,partA.getId());
        verifyNoInteractions(mailSender);
        assertThat(post(buyer,"/orders/"+id+"/cancel",Map.of("reason","Cancel pending shop")).statusCode()).isEqualTo(302);
        assertThat(shopOrders.findById(partA.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(shopOrders.findById(partB.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(inventory.findById(fa.stock().getId()).orElseThrow().getQuantity()).isEqualTo(3);
        assertThat(inventory.findById(fb.stock().getId()).orElseThrow().getQuantity()).isEqualTo(5);
        assertThat(orders.findById(id).orElseThrow().getTotalAmount()).isEqualByComparingTo("200000");
        assertThat(orders.findById(id).orElseThrow().getPaymentStatus()).isEqualTo("PAID");
        verify(mailSender,times(1)).send(any(SimpleMailMessage.class));
        assertThat(get(a,"/seller/orders").body()).doesNotContain("Cancel pending shop");
        assertThat(get(b,"/seller/orders").body()).contains("Cancel pending shop");
        // A stale customer form cannot cancel a part after shop approval.
        post(buyer,"/orders/"+id+"/cancel",Map.of());
        assertThat(shopOrders.findById(partA.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }
    @Test void customerCancelAndAdminApprovalAreSerialized() throws Exception {
        var admin=session(true);var seller=vendor();var buyer=session(false);var f=fixture(seller);var id=place(buyer,f);
        var part=shopOrders.findByOrderId(id).getFirst();
        var cancel=java.util.concurrent.CompletableFuture.supplyAsync(()->{
            try { return post(buyer,"/orders/"+id+"/cancel",Map.of()).statusCode(); }
            catch(Exception ex) { throw new java.util.concurrent.CompletionException(ex); }
        });
        var approve=java.util.concurrent.CompletableFuture.supplyAsync(()->{
            try { return post(admin,"/admin/orders/"+part.getId()+"/status",Map.of("status","CONFIRMED")).statusCode(); }
            catch(Exception ex) { throw new java.util.concurrent.CompletionException(ex); }
        });
        assertThat(cancel.get()).isEqualTo(302);assertThat(approve.get()).isEqualTo(302);
        var status=shopOrders.findById(part.getId()).orElseThrow().getStatus();
        assertThat(status).isIn(OrderStatus.CANCELLED,OrderStatus.CONFIRMED);
        assertThat(inventory.findById(f.stock().getId()).orElseThrow().getQuantity()).isEqualTo(status==OrderStatus.CANCELLED?5:3);
        assertThat(orders.findById(id).orElseThrow().getPaymentStatus()).isEqualTo(status==OrderStatus.CANCELLED?"CANCELLED":"UNPAID");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM order_status_history WHERE shop_order_id=?",Integer.class,part.getId())).isEqualTo(2);
    }
    @Test void allAdminPagesRenderAndCustomerCannotMutateAnyShop() throws Exception {
        var admin=session(true);var owner=session(false);var f=fixture(owner);var id=place(owner,f);var so=shopOrders.findByOrderId(id).getFirst();
        for(var path:List.of("/admin","/admin/shops","/admin/shops/new","/admin/shops/"+f.shop().getId()+"/edit","/admin/products","/admin/products/new","/admin/products/"+f.product().getId()+"/edit","/admin/categories","/admin/orders","/admin/orders/"+so.getId())) {
            var page=get(admin,path);assertThat(page.statusCode()).as(path).isEqualTo(200);assertThat(page.body()).doesNotContain("Exception");
            assertThat(get(owner,path).statusCode()).as(path).isEqualTo(403);
        }
        var path="/admin/products/"+f.product().getId()+"/delete";
        assertThat(post(owner,path,Map.of()).statusCode()).isEqualTo(403);
        assertThat(request(admin.client(),path,"",null).statusCode()).isEqualTo(403);
        assertThat(products.findById(f.product().getId()).orElseThrow().isEnabled()).isTrue();
    }
    @Test void adminCreatesShopCategoryProductVariantBranchAndStockAndCanArchiveProduct() throws Exception {
        var admin=session(true);var owner=session(false);
        assertThat(post(admin,"/admin/categories",Map.of("name","New category","enabled",true)).statusCode()).isEqualTo(302);
        var category=categories.findByName("New category").getFirst();
        var shopData=Map.of("name","New Shop","slug","new-shop","ownerId",owner.user().getId(),"status","ACTIVE","enabled",true);
        assertThat(post(admin,"/admin/shops",shopData).statusCode()).isEqualTo(302);var shop=shops.findBySlug("new-shop").orElseThrow();
        assertThat(post(admin,"/admin/shops/"+shop.getId()+"/stores",Map.of("code","NEW","name","Branch","address","Test Street","enabled",true)).statusCode()).isEqualTo(302);
        var branch=stores.findByCode("NEW").orElseThrow();
        var pData=new HashMap<String,Object>(Map.of("name","New Product","slug","new-product","shopId",shop.getId(),"categoryId",category.getId(),"price","123000","image","/images/aosomi.png","enabled",true));
        assertThat(post(admin,"/admin/products",pData).statusCode()).isEqualTo(302);var product=products.findBySlug("new-product").orElseThrow();
        assertThat(post(admin,"/admin/products/"+product.getId()+"/variants",Map.of("sku","NEW-M","size","M","color","White","enabled",true)).statusCode()).isEqualTo(302);
        var variant=variants.findBySku("NEW-M").orElseThrow();
        post(admin,"/admin/products/"+product.getId()+"/stock",Map.of("variantId",variant.getId(),"storeId",branch.getId(),"quantity",15));
        assertThat(inventory.availableQuantity(variant.getId())).isEqualTo(15);
        pData.put("name","Edited Product");pData.put("price","99900");post(admin,"/admin/products/"+product.getId(),pData);
        assertThat(products.findById(product.getId()).orElseThrow().getBasePrice()).isEqualByComparingTo("99900");
        pData.put("price","-1");post(admin,"/admin/products/"+product.getId(),pData);
        assertThat(products.findById(product.getId()).orElseThrow().getBasePrice()).isEqualByComparingTo("99900");
        post(admin,"/admin/products/"+product.getId()+"/delete",Map.of());
        assertThat(products.findById(product.getId()).orElseThrow().isEnabled()).isFalse();
        post(admin,"/admin/shops/"+shop.getId()+"/delete",Map.of());assertThat(shops.findById(shop.getId()).orElseThrow().isEnabled()).isFalse();
    }
    @Test void adminCannotPutStockInAnotherShopsBranch() throws Exception {
        var admin=session(true);var owner=session(false);var a=fixture(owner);var b=fixture(owner);
        post(admin,"/admin/products/"+a.product().getId()+"/stock",Map.of("variantId",a.variant().getId(),"storeId",b.store().getId(),"quantity",100));
        assertThat(inventory.findByStoreIdAndProductVariantId(b.store().getId(),a.variant().getId())).isEmpty();
        assertThat(inventory.availableQuantity(a.variant().getId())).isEqualTo(5);
    }
    @Test void confirmationWaitsForShipperAndDeliveryPaysCodWithoutDeductingStockAgain() throws Exception {
        var admin=session(true);var buyer=session(false);var f=fixture(buyer);var id=place(buyer,f);var so=shopOrders.findByOrderId(id).getFirst();var path="/admin/orders/"+so.getId()+"/status";
        assertThat(get(admin,"/admin/orders/"+so.getId()).body()).contains("Xác nhận đơn").doesNotContain("value=\"DELIVERED\"");
        post(admin,path,Map.of("status","SHIPPING"));assertThat(shopOrders.findById(so.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.NEW);
        verifyNoInteractions(mailSender);
        post(admin,path,Map.of("status","CONFIRMED","note","Approved"));
        assertThat(shopOrders.findById(so.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(orders.findById(id).orElseThrow().getPaymentStatus()).isEqualTo("UNPAID");
        verifyNoInteractions(mailSender);
        deliver(admin,so.getId());
        assertThat(shopOrders.findById(so.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(get(buyer,"/orders/"+id).body()).contains("Đã giao hàng","Đã thanh toán");
        post(admin,path,Map.of("status","DELIVERED","note","Duplicate"));
        assertThat(orders.findById(id).orElseThrow().getPaymentStatus()).isEqualTo("PAID");
        assertThat(inventory.findById(f.stock().getId()).orElseThrow().getQuantity()).isEqualTo(3);
        assertThat(get(admin,"/admin/orders/"+so.getId()).body()).contains("Approved","Giao thành công");
        post(admin,path,Map.of("status","CANCELLED","note","Invalid late cancellation"));assertThat(shopOrders.findById(so.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.DELIVERED);
        var mail=org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender,times(1)).send(mail.capture());
        assertThat(mail.getValue().getTo()).containsExactly(buyer.user().getEmail());
        assertThat(mail.getValue().getSubject()).contains("Mua hàng thành công",id.toString());
        assertThat(mail.getValue().getText()).contains("Admin Shirt <test>","200.000 VNĐ","12 Test Street","SL: 2");
    }
    @Test void mailFailureDoesNotUndoConfirmedOrderOrResendOnDuplicate() throws Exception {
        var admin=session(true);var buyer=session(false);var f=fixture(buyer);var id=place(buyer,f);
        var part=shopOrders.findByOrderId(id).getFirst();
        doThrow(new org.springframework.mail.MailSendException("SMTP unavailable")).when(mailSender).send(any(SimpleMailMessage.class));
        var path="/admin/orders/"+part.getId()+"/status";
        assertThat(deliver(admin,part.getId()).statusCode()).isEqualTo(302);
        assertThat(orders.findById(id).orElseThrow().getPaymentStatus()).isEqualTo("PAID");
        assertThat(deliver(admin,part.getId()).statusCode()).isEqualTo(302);
        verify(mailSender,times(1)).send(any(SimpleMailMessage.class));
    }
    @Test void rolledBackConfirmationDoesNotSendEmail() throws Exception {
        var buyer=session(false);var f=fixture(buyer);var id=place(buyer,f);
        new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(tx -> {
            var order=orders.lockById(id).orElseThrow();
            order.getShopOrders().getFirst().setStatus(OrderStatus.DELIVERED);
            confirmations.notifyIfCompleted(order,false);
            tx.setRollbackOnly();
        });
        verifyNoInteractions(mailSender);
        assertThat(shopOrders.findByOrderId(id).getFirst().getStatus()).isEqualTo(OrderStatus.NEW);
    }
    @Test void multiShopConfirmationSendsOneEmailOnlyAfterLastShopCompletes() throws Exception {
        var admin=session(true);var buyer=session(false);var a=fixture(buyer);var b=fixture(buyer);
        for(var f:List.of(a,b)) {
            var add=new CartAddRequest();add.setProductId(f.product().getSlug());add.setSize("M");add.setColor("White");add.setQuantity(2);
            carts.add(buyer.user().getUsername(),add);
        }
        var r=new CheckoutRequest();r.setReceiverName("Buyer");r.setReceiverPhone("0901234567");r.setShippingAddress("12 Test Street");
        r.setToken(UUID.randomUUID().toString());r.setExpectedTotal(new BigDecimal("400000"));
        var id=checkout.place(buyer.user().getUsername(),r);var parts=shopOrders.findByOrderId(id);
        assertThat(deliver(admin,parts.getFirst().getId()).statusCode()).isEqualTo(302);
        verifyNoInteractions(mailSender);
        var lastPath="/admin/orders/"+parts.getLast().getId()+"/status";
        assertThat(deliver(admin,parts.getLast().getId()).statusCode()).isEqualTo(302);
        deliver(admin,parts.getLast().getId());
        var mail=org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender,times(1)).send(mail.capture());
        assertThat(mail.getValue().getTo()).containsExactly(buyer.user().getEmail());
        assertThat(mail.getValue().getText()).contains("400.000 VNĐ");
    }
    @Test void cancellationRestoresOriginalStockExactlyOnceAndKeepsSnapshot() throws Exception {
        var admin=session(true);var buyer=session(false);var f=fixture(buyer);var id=place(buyer,f);var so=shopOrders.findByOrderId(id).getFirst();var path="/admin/orders/"+so.getId()+"/status";
        assertThat(allocations.findByShopOrderIdOrderByInventoryIdAsc(so.getId())).hasSize(1);
        post(admin,path,Map.of("status","CANCELLED"));assertThat(inventory.findById(f.stock().getId()).orElseThrow().getQuantity()).isEqualTo(3);
        post(admin,path,Map.of("status","CANCELLED","note","Customer requested cancellation"));
        post(admin,path,Map.of("status","CANCELLED","note","Duplicate"));
        verifyNoInteractions(mailSender);
        assertThat(inventory.findById(f.stock().getId()).orElseThrow().getQuantity()).isEqualTo(5);
        assertThat(orders.findById(id).orElseThrow().getPaymentStatus()).isEqualTo("CANCELLED");
        assertThat(orders.findById(id).orElseThrow().getTotalAmount()).isEqualByComparingTo("0");
        post(admin,"/admin/products/"+f.product().getId()+"/delete",Map.of());
        assertThat(get(buyer,"/orders/"+id).body()).contains("Admin Shirt &lt;test&gt;","Đã hủy");
    }
    @Test void legacyOrderCanBeCancelledWhenOriginalSingleBranchIsUnambiguous() throws Exception {
        var admin=session(true);var buyer=session(false);var f=fixture(buyer);var id=place(buyer,f);var so=shopOrders.findByOrderId(id).getFirst();
        allocations.deleteAll(allocations.findByShopOrderIdOrderByInventoryIdAsc(so.getId()));
        post(admin,"/admin/orders/"+so.getId()+"/status",Map.of("status","CANCELLED","note","Legacy cancellation"));
        assertThat(inventory.findById(f.stock().getId()).orElseThrow().getQuantity()).isEqualTo(5);
        assertThat(shopOrders.findById(so.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
    @Test void cancellationRestoresEachOriginalBranchAndMultiShopTotalExcludesCancelledPart() throws Exception {
        var admin=session(true);var buyer=session(false);var a=fixture(buyer);var b=fixture(buyer);
        a.stock().setQuantity(1);inventory.saveAndFlush(a.stock());
        var branch=new Store();branch.setShop(a.shop());branch.setCode(UUID.randomUUID().toString());branch.setName("Second branch");branch.setAddress("Test");stores.saveAndFlush(branch);
        var extra=new Inventory();extra.setStore(branch);extra.setProductVariant(a.variant());extra.setQuantity(5);inventory.saveAndFlush(extra);
        for(var f:List.of(a,b)){var add=new CartAddRequest();add.setProductId(f.product().getSlug());add.setSize("M");add.setColor("White");add.setQuantity(2);carts.add(buyer.user().getUsername(),add);}
        var r=new CheckoutRequest();r.setReceiverName("Buyer");r.setReceiverPhone("0901234567");r.setShippingAddress("Test address");r.setToken(UUID.randomUUID().toString());r.setExpectedTotal(new BigDecimal("400000"));
        var id=checkout.place(buyer.user().getUsername(),r);var parts=shopOrders.findByOrderId(id);
        var partA=parts.stream().filter(s->s.getShop().getId().equals(a.shop().getId())).findFirst().orElseThrow();
        var partB=parts.stream().filter(s->s.getShop().getId().equals(b.shop().getId())).findFirst().orElseThrow();
        assertThat(allocations.findByShopOrderIdOrderByInventoryIdAsc(partA.getId())).hasSize(2);
        post(admin,"/admin/orders/"+partA.getId()+"/status",Map.of("status","CANCELLED","note","Cancel first shop only"));
        assertThat(inventory.findById(a.stock().getId()).orElseThrow().getQuantity()).isEqualTo(1);
        assertThat(inventory.findById(extra.getId()).orElseThrow().getQuantity()).isEqualTo(5);
        assertThat(orders.findById(id).orElseThrow().getTotalAmount()).isEqualByComparingTo("200000");
        assertThat(orders.findById(id).orElseThrow().getPaymentStatus()).isEqualTo("UNPAID");
        deliver(admin,partB.getId());
        assertThat(orders.findById(id).orElseThrow().getPaymentStatus()).isEqualTo("PAID");
    }
}
