package com.example.demo.service;

import static org.assertj.core.api.Assertions.*;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.dto.CartAddRequest;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest @ActiveProfiles("test") @Transactional
class DatabaseCatalogTests {
    @Autowired CatalogService catalog;
    @Autowired CartService carts;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired ShopRepository shops;
    @Autowired UserRepository users;
    @Autowired StoreRepository stores;
    @Autowired ProductVariantRepository variants;
    @Autowired InventoryRepository inventory;
    @Autowired OrderEntityRepository orders;
    record Fixture(User owner,Shop shop,Category category,Store store) {}
    private Fixture fixture() {
        String key=UUID.randomUUID().toString();
        var user=new User();user.setUsername(key);user.setEmail(key+"@example.com");user.setFullName("Test");user.setPassword("no-login");users.save(user);
        var shop=new Shop();shop.setOwner(user);shop.setName("Shop "+key);shop.setSlug(key);shop.setStatus(ShopStatus.ACTIVE);shops.save(shop);
        var category=new Category();category.setName("Category "+key);categories.save(category);
        var store=new Store();store.setShop(shop);store.setName("Branch");store.setCode(key);store.setAddress("Test");stores.save(store);
        return new Fixture(user,shop,category,store);
    }
    private Product product(Fixture f,String name,int price) {
        var p=new Product();p.setShop(f.shop());p.setCategory(f.category());p.setName(name);p.setSlug(UUID.randomUUID().toString());
        p.setBasePrice(BigDecimal.valueOf(price));p.setImageUrl("/images/aosomi.png");return products.saveAndFlush(p);
    }
    private ProductVariant variant(Product p,String color,int price,int quantity,Store store) {
        var v=new ProductVariant();v.setProduct(p);v.setSku(UUID.randomUUID().toString());v.setSize("M");v.setColor(color);
        v.setPrice(BigDecimal.valueOf(price));variants.save(v);
        var stock=new Inventory();stock.setStore(store);stock.setProductVariant(v);stock.setQuantity(quantity);inventory.saveAndFlush(stock);
        return v;
    }
    private void sale(Fixture f,ProductVariant variant,int quantity,OrderStatus status,String payment) {
        var order=new OrderEntity(); order.setUser(f.owner()); order.setReceiverName("Buyer");
        order.setReceiverPhone("0901234567"); order.setShippingAddress("Test address");
        var total=variant.getPrice().multiply(BigDecimal.valueOf(quantity));
        order.setTotalAmount(total); order.setPaymentStatus(payment);
        var shopOrder=new ShopOrder(); shopOrder.setOrder(order); shopOrder.setShop(f.shop());
        shopOrder.setStatus(status); shopOrder.setSubtotal(total); shopOrder.setTotalAmount(total);
        var item=new OrderItem(); item.setShopOrder(shopOrder); item.setProductVariant(variant);
        item.setProductName(variant.getProduct().getName()); item.setVariantSku(variant.getSku());
        item.setSize(variant.getSize()); item.setColor(variant.getColor()); item.setQuantity(quantity);
        item.setUnitPrice(variant.getPrice()); item.setSubtotal(total);
        orders.saveAndFlush(order);
    }
    @Test void bestsellersAggregateVariantsAndExcludeUnpaidCancelledAndHiddenProducts() {
        var f=fixture(); var first=product(f,"Bestseller",100); var second=product(f,"Runner up",100);
        var red=variant(first,"Red",100,10,f.store()); var blue=variant(first,"Blue",100,10,f.store());
        var other=variant(second,"White",100,10,f.store());
        sale(f,red,3,OrderStatus.DELIVERED,"PAID"); sale(f,blue,2,OrderStatus.DELIVERED,"PAID");
        sale(f,other,4,OrderStatus.DELIVERED,"PAID");
        sale(f,other,100,OrderStatus.CANCELLED,"PAID"); sale(f,other,100,OrderStatus.DELIVERED,"UNPAID");
        sale(f,other,100,OrderStatus.NEW,"PAID");
        assertThat(catalog.bestsellers()).extracting(p->p.get("id")).containsExactly(first.getSlug(),second.getSlug());
        assertThat(catalog.bestsellers().getFirst().get("soldQuantity")).isEqualTo(5L);
        first.setEnabled(false); products.flush();
        assertThat(catalog.bestsellers()).extracting(p->p.get("id")).containsExactly(second.getSlug());
        f.category().setEnabled(false); categories.flush(); assertThat(catalog.bestsellers()).isEmpty();
        f.category().setEnabled(true); f.shop().setStatus(ShopStatus.SUSPENDED); shops.flush();
        assertThat(catalog.bestsellers()).isEmpty();
    }
    @Test void databaseFilteringSortingAndTwentyItemPages() {
        var f=fixture();
        for(int i=0;i<25;i++) product(f,"Áo thun đặc biệt "+i,10000+i);
        var page=catalog.search("price-desc","AO THUN DAC BIET",f.category().getId(),f.shop().getSlug(),0);
        assertThat(page.getTotalElements()).isEqualTo(25);assertThat(page.getContent()).hasSize(20);
        assertThat((BigDecimal)page.getContent().getFirst().get("price")).isEqualByComparingTo("10024");
        assertThat(catalog.search("price-desc","",f.category().getId(),f.shop().getSlug(),1).getContent()).hasSize(5);
        assertThat(catalog.search("featured","%",f.category().getId(),"",0).getContent()).isEmpty();
        assertThat(catalog.search("featured","",f.category().getId(),"missing",0).getContent()).isEmpty();
        f.shop().setStatus(ShopStatus.SUSPENDED);shops.flush();
        assertThat(catalog.search("featured","",f.category().getId(),"",0).getContent()).isEmpty();
    }
    @Test void hiddenProductsAndCategoriesAreNotPublicAndPricesUpdateImmediately() {
        var f=fixture();var p=product(f,"Live database product",100);
        assertThat(catalog.findProduct(p.getSlug())).isPresent();
        p.setBasePrice(new BigDecimal("123.45"));products.flush();
        assertThat((BigDecimal)catalog.findProduct(p.getSlug()).orElseThrow().get("price")).isEqualByComparingTo("123.45");
        f.category().setEnabled(false);categories.flush();assertThat(catalog.findProduct(p.getSlug())).isEmpty();
        f.category().setEnabled(true);p.setEnabled(false);products.flush();assertThat(catalog.findProduct(p.getSlug())).isEmpty();
    }
    @Test void cartSeparatesColorsUsesVariantPricesAndEnforcesRealStock() {
        var f=fixture();var p=product(f,"Multi color",100);
        variant(p,"Red",120,3,f.store());variant(p,"Blue",140,2,f.store());
        var request=new CartAddRequest();request.setProductId(p.getSlug());request.setSize("M");request.setColor("Red");request.setQuantity(2);
        var cart=carts.add(f.owner().getUsername(),request);
        assertThat(cart.total()).isEqualByComparingTo("240");
        request.setColor("Blue");request.setQuantity(1);cart=carts.add(f.owner().getUsername(),request);
        assertThat(cart.items()).hasSize(2);assertThat(cart.total()).isEqualByComparingTo("380");
        request.setColor("Red");request.setQuantity(2);
        assertThatThrownBy(()->carts.add(f.owner().getUsername(),request)).isInstanceOf(ResponseStatusException.class);
    }
    @Test void onlyEnabledMatchingBranchesContributeStock() {
        var f=fixture();var other=fixture();var p=product(f,"Stock test",100);
        var v=variant(p,"Green",100,4,f.store());
        var wrong=new Inventory();wrong.setProductVariant(v);wrong.setStore(other.store());wrong.setQuantity(500);inventory.saveAndFlush(wrong);
        assertThat(catalog.option(p.getSlug(),"M","Green").get("stock")).isEqualTo(4L);
        f.store().setEnabled(false);stores.flush();
        assertThat(catalog.option(p.getSlug(),"M","Green").get("stock")).isEqualTo(0L);
        v.setEnabled(false);variants.flush();assertThat(catalog.options(p.getSlug())).isEmpty();
    }
}
