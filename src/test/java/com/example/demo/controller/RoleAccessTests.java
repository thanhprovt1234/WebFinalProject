package com.example.demo.controller;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties="spring.datasource.url=jdbc:h2:mem:roleaccess;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class RoleAccessTests {
    @Value("${local.server.port}") int port;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired ShopRepository shops;
    @Autowired StoreRepository stores;
    @Autowired StoreManagerRepository assignments;
    @Autowired PasswordEncoder encoder;
    @Autowired ShipmentRepository shipments;
    @Autowired ShopOrderRepository shopOrders;
    @Autowired OrderEntityRepository orders;
    private final List<User> accounts=new ArrayList<>();
    private final List<HttpClient> clients=new ArrayList<>();
    private final List<Shop> createdShops=new ArrayList<>();
    private final List<Store> createdStores=new ArrayList<>();
    private final List<Shipment> createdShipments=new ArrayList<>();
    private final List<ShopOrder> createdShopOrders=new ArrayList<>();
    private final List<OrderEntity> createdOrders=new ArrayList<>();
    record Session(User user,HttpClient client,String csrf){}
    private HttpResponse<String> request(HttpClient client,String path,String data,String csrf)throws Exception {
        var b=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path));
        if(data!=null)b.header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(data));
        if(csrf!=null)b.header("X-CSRF-TOKEN",csrf);
        return client.send(b.build(),HttpResponse.BodyHandlers.ofString());
    }
    private String token(String body){
        var m=Pattern.compile("name=\"csrf-token\" content=\"([^\"]+)\"").matcher(body);
        assertThat(m.find()).as("CSRF token in rendered page").isTrue();return m.group(1);
    }
    private Session login(RoleName role)throws Exception {
        for(var name:RoleName.values())if(roles.findByName(name).isEmpty()){
            var r=new Role();r.setName(name);roles.saveAndFlush(r);
        }
        var u=new User();u.setUsername("access-"+UUID.randomUUID());u.setEmail(u.getUsername()+"@example.com");
        u.setFullName("Access test");u.setPassword(encoder.encode("test-password"));
        u.addRole(roles.findByName(RoleName.ROLE_USER).orElseThrow());
        if(role!=RoleName.ROLE_USER)u.addRole(roles.findByName(role).orElseThrow());
        users.saveAndFlush(u);accounts.add(u);
        var c=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();clients.add(c);
        var csrf=token(request(c,"/login",null,null).body());
        assertThat(request(c,"/login","username="+u.getUsername()+"&password=test-password",csrf).statusCode()).isEqualTo(302);
        var page=request(c,"/workspace",null,null);assertThat(page.statusCode()).isEqualTo(200);
        return new Session(u,c,token(page.body()));
    }
    private HttpResponse<String> get(Session s,String path)throws Exception{return request(s.client(),path,null,null);}
    private HttpResponse<String> post(Session s,String path,String data)throws Exception{return request(s.client(),path,data,s.csrf());}
    private Shop shop(Session s){
        var shop=new Shop();shop.setOwner(s.user());shop.setSlug("access-"+UUID.randomUUID());shop.setName(shop.getSlug());
        shops.saveAndFlush(shop);createdShops.add(shop);return shop;
    }
    private Store store(Shop shop){
        var s=new Store();s.setShop(shop);s.setCode("A-"+UUID.randomUUID());s.setName(s.getCode());s.setAddress("Test address");
        stores.saveAndFlush(s);createdStores.add(s);return s;
    }
    @AfterEach void cleanup(){
        clients.forEach(HttpClient::close);
        createdShipments.forEach(shipments::delete);
        createdShopOrders.forEach(shopOrders::delete);
        createdOrders.forEach(o->orders.deleteById(o.getId()));
        accounts.forEach(u->assignments.deleteAll(assignments.findByUserId(u.getId())));
        createdStores.forEach(stores::delete);createdShops.forEach(shops::delete);accounts.forEach(users::delete);
    }
    @Test void roleMatrixBlocksDirectUrlsAndPreservesCustomerAccess()throws Exception {
        try(var guest=HttpClient.newHttpClient()){
            for(var path:List.of("/workspace","/admin","/seller","/manager","/shipper"))
                assertThat(request(guest,path,null,null).statusCode()).isEqualTo(302);
        }
        for(var role:RoleName.values()){
            var s=login(role);
            assertThat(get(s,"/profile").statusCode()).isEqualTo(200);
            var paths=Map.of("/admin",RoleName.ROLE_ADMIN,"/seller",RoleName.ROLE_VENDOR,"/manager",RoleName.ROLE_MANAGER,"/shipper",RoleName.ROLE_SHIPPER);
            for(var entry:paths.entrySet())assertThat(get(s,entry.getKey()).statusCode())
                .as(role+" "+entry.getKey()).isEqualTo(role==RoleName.ROLE_ADMIN||role==entry.getValue()?200:403);
        }
    }
    @Test void sellerAndManagerCannotReadAnotherShopOrStore()throws Exception{
        var a=login(RoleName.ROLE_VENDOR);var b=login(RoleName.ROLE_VENDOR);
        var first=shop(a);var second=shop(b);
        assertThat(get(a,"/seller").body()).contains(first.getName()).doesNotContain(second.getName());
        assertThat(get(a,"/seller/shops/"+first.getId()).statusCode()).isEqualTo(200);
        assertThat(get(a,"/seller/shops/"+second.getId()).statusCode()).isEqualTo(403);
        var manager=login(RoleName.ROLE_MANAGER);var own=store(first);var other=store(second);
        var assignment=new StoreManager();assignment.setUser(manager.user());assignment.setStore(own);assignments.saveAndFlush(assignment);
        assertThat(get(manager,"/manager").body()).contains(own.getName()).doesNotContain(other.getName());
        assertThat(get(manager,"/manager/stores/"+own.getId()).statusCode()).isEqualTo(200);
        assertThat(get(manager,"/manager/stores/"+other.getId()).statusCode()).isEqualTo(403);
    }
    @Test void onlyAdminCanGrantRolesAndRevocationAppliesToExistingSession()throws Exception{
        var admin=login(RoleName.ROLE_ADMIN);var user=login(RoleName.ROLE_USER);var seller=login(RoleName.ROLE_VENDOR);
        var path="/admin/users/"+user.user().getId()+"/roles";
        assertThat(post(user,path,"roles=ROLE_ADMIN").statusCode()).isEqualTo(403);
        assertThat(request(admin.client(),path,"roles=ROLE_ADMIN",null).statusCode()).isEqualTo(403);
        assertThat(post(admin,path,"roles=ROLE_MANAGER").statusCode()).isEqualTo(302);
        assertThat(get(user,"/manager").statusCode()).isEqualTo(200);
        var store=store(shop(seller));
        assertThat(post(admin,"/admin/users/"+user.user().getId()+"/stores","storeId="+store.getId()+"&assigned=true").statusCode()).isEqualTo(302);
        assertThat(get(user,"/manager/stores/"+store.getId()).statusCode()).isEqualTo(200);
        assertThat(get(admin,"/admin/users/"+user.user().getId()).statusCode()).isEqualTo(200);
        assertThat(post(admin,path,"roles=ROLE_USER").statusCode()).isEqualTo(302);
        assertThat(get(user,"/manager").statusCode()).isEqualTo(403);
        assertThat(assignments.findByUserId(user.user().getId())).isEmpty();
        assertThat(post(admin,"/admin/users/"+admin.user().getId()+"/roles","roles=ROLE_USER").statusCode()).isEqualTo(302);
        assertThat(get(admin,"/admin").statusCode()).isEqualTo(200);
    }
    @Test void shipperCannotReadAnotherShippersAssignment()throws Exception {
        var a=login(RoleName.ROLE_SHIPPER);var b=login(RoleName.ROLE_SHIPPER);
        var shop=shop(a);
        var order=new OrderEntity();order.setUser(a.user());order.setReceiverName("Test");
        order.setReceiverPhone("0901234567");order.setShippingAddress("Test address");order.setTotalAmount(java.math.BigDecimal.TEN);
        orders.saveAndFlush(order);createdOrders.add(order);
        var sub=new ShopOrder();sub.setOrder(order);sub.setShop(shop);sub.setSubtotal(java.math.BigDecimal.TEN);sub.setTotalAmount(java.math.BigDecimal.TEN);
        shopOrders.saveAndFlush(sub);createdShopOrders.add(sub);
        var shipment=new Shipment();shipment.setShopOrder(sub);shipment.setShipper(a.user());shipments.saveAndFlush(shipment);createdShipments.add(shipment);
        assertThat(get(a,"/shipper/shipments/"+shipment.getId()).statusCode()).isEqualTo(200);
        assertThat(get(b,"/shipper/shipments/"+shipment.getId()).statusCode()).isEqualTo(403);
        assertThat(get(a,"/shipper").body()).contains(shop.getName());
        assertThat(get(b,"/shipper").body()).doesNotContain(shop.getName());
    }
}
