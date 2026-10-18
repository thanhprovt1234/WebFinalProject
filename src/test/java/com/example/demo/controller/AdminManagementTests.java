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
        for(var table:List.of("order_stock_allocations","order_status_history","order_items","shop_orders","orders","cart_items","inventories","product_variants","products","stores","shops","categories","user_roles","users"))jdbc.update("DELETE FROM "+table);
    }
    Session vendor() throws Exception {
        var session=session(false);
        var role=roles.findByName(RoleName.ROLE_VENDOR).orElseGet(()->{var r=new Role();r.setName(RoleName.ROLE_VENDOR);return roles.saveAndFlush(r);});
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES (?,?)",session.user().getId(),role.getId());
        return session;
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
        assertThat(orders.findById(idA).orElseThrow().getPaymentStatus()).isEqualTo("PAID");
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
        post(admin,"/admin/orders/"+partA.getId()+"/status",Map.of("status","DELIVERED"));
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
            try { return post(admin,"/admin/orders/"+part.getId()+"/status",Map.of("status","DELIVERED")).statusCode(); }
            catch(Exception ex) { throw new java.util.concurrent.CompletionException(ex); }
        });
        assertThat(cancel.get()).isEqualTo(302);assertThat(approve.get()).isEqualTo(302);
        var status=shopOrders.findById(part.getId()).orElseThrow().getStatus();
        assertThat(status).isIn(OrderStatus.CANCELLED,OrderStatus.DELIVERED);
        assertThat(inventory.findById(f.stock().getId()).orElseThrow().getQuantity()).isEqualTo(status==OrderStatus.CANCELLED?5:3);
        assertThat(orders.findById(id).orElseThrow().getPaymentStatus()).isEqualTo(status==OrderStatus.CANCELLED?"CANCELLED":"PAID");
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
    @Test void adminConfirmationImmediatelyMarksDeliveredAndCodPaidWithoutDeductingStockAgain() throws Exception {
        var admin=session(true);var buyer=session(false);var f=fixture(buyer);var id=place(buyer,f);var so=shopOrders.findByOrderId(id).getFirst();var path="/admin/orders/"+so.getId()+"/status";
        assertThat(get(admin,"/admin/orders/"+so.getId()).body()).contains("Xác nhận đã giao &amp; đã thanh toán");
        post(admin,path,Map.of("status","SHIPPING"));assertThat(shopOrders.findById(so.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.NEW);
        verifyNoInteractions(mailSender);
        post(admin,path,Map.of("status","CONFIRMED","note","Approved"));
        assertThat(shopOrders.findById(so.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(get(buyer,"/orders/"+id).body()).contains("Đã giao hàng","Đã thanh toán");
        post(admin,path,Map.of("status","DELIVERED","note","Duplicate"));
        assertThat(orders.findById(id).orElseThrow().getPaymentStatus()).isEqualTo("PAID");
        assertThat(inventory.findById(f.stock().getId()).orElseThrow().getQuantity()).isEqualTo(3);
        assertThat(get(admin,"/admin/orders/"+so.getId()).body()).contains("Approved","Xác nhận đã giao và đã thanh toán COD.");
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
        assertThat(post(admin,path,Map.of("status","DELIVERED")).statusCode()).isEqualTo(302);
        assertThat(orders.findById(id).orElseThrow().getPaymentStatus()).isEqualTo("PAID");
        assertThat(post(admin,path,Map.of("status","DELIVERED")).statusCode()).isEqualTo(302);
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
        assertThat(post(admin,"/admin/orders/"+parts.getFirst().getId()+"/status",Map.of("status","DELIVERED")).statusCode()).isEqualTo(302);
        verifyNoInteractions(mailSender);
        var lastPath="/admin/orders/"+parts.getLast().getId()+"/status";
        assertThat(post(admin,lastPath,Map.of("status","DELIVERED")).statusCode()).isEqualTo(302);
        post(admin,lastPath,Map.of("status","DELIVERED"));
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
        post(admin,"/admin/orders/"+partB.getId()+"/status",Map.of("status","DELIVERED"));
        assertThat(orders.findById(id).orElseThrow().getPaymentStatus()).isEqualTo("PAID");
    }
}
