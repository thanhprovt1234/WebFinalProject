package com.example.demo.service;

import static org.assertj.core.api.Assertions.*;
import com.example.demo.dto.*;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest @ActiveProfiles("test") @Transactional
class CheckoutServiceTests {
    @Autowired CheckoutService checkout;
    @Autowired CartService carts;
    @Autowired UserRepository users;
    @Autowired ProductRepository products;
    @Autowired ShopRepository shops;
    @Autowired CategoryRepository categories;
    @Autowired ProductVariantRepository variants;
    @Autowired StoreRepository stores;
    @Autowired InventoryRepository inventory;
    @Autowired OrderEntityRepository orders;
    record Fixture(User user, Product product,ProductVariant variant,Inventory stock) {}
    private Fixture fixture() {
        var key=UUID.randomUUID().toString();
        var user=new User(); user.setUsername(key); user.setEmail(key+"@example.com"); user.setFullName("COD Test"); user.setPassword("no-login"); users.save(user);
        var shop=new Shop(); shop.setOwner(user); shop.setName("Shop"); shop.setSlug(key); shop.setStatus(ShopStatus.ACTIVE); shops.save(shop);
        var category=new Category(); category.setName(key); categories.save(category);
        var p=new Product(); p.setShop(shop); p.setCategory(category); p.setSlug(key); p.setName("Áo kiểm thử"); p.setBasePrice(new BigDecimal("100000")); products.save(p);
        var v=new ProductVariant(); v.setProduct(p); v.setSku(key); v.setSize("M"); v.setColor("Trắng"); v.setPrice(new BigDecimal("120000")); variants.save(v);
        var store=new Store(); store.setShop(shop); store.setCode(key); store.setName("Store"); store.setAddress("Address"); stores.save(store);
        var stock=new Inventory(); stock.setStore(store); stock.setProductVariant(v); stock.setQuantity(5); inventory.saveAndFlush(stock);
        var add=new CartAddRequest(); add.setProductId(p.getSlug()); add.setColor(v.getColor()); add.setSize(v.getSize()); add.setQuantity(2); carts.add(user.getUsername(),add);
        return new Fixture(user,p,v,stock);
    }
    private CheckoutRequest request(BigDecimal total) {
        var r=new CheckoutRequest(); r.setReceiverName("Người nhận"); r.setReceiverPhone("0901234567");
        r.setShippingAddress("12 Nguyễn Trãi, TP HCM"); r.setToken(UUID.randomUUID().toString()); r.setExpectedTotal(total); return r;
    }
    @Test void codPersistsSnapshotDeductsStockClearsCartAndRetriesWithoutDuplicate() {
        var f=fixture(); var r=request(new BigDecimal("240000"));
        var id=checkout.place(f.user().getUsername(),r);
        assertThat(checkout.place(f.user().getUsername(),r)).isEqualTo(id);
        assertThat(f.stock().getQuantity()).isEqualTo(3); assertThat(carts.get(f.user().getUsername()).items()).isEmpty();
        f.product().setName("Renamed"); f.variant().setPrice(BigDecimal.ONE); products.flush();
        var receipt=checkout.get(f.user().getUsername(),id);
        assertThat(receipt.paymentMethod()).isEqualTo("COD"); assertThat(receipt.paymentStatus()).isEqualTo("UNPAID");
        assertThat(receipt.total()).isEqualByComparingTo("240000");
        assertThat(receipt.shops().getFirst().status()).isEqualTo("NEW");
        assertThat(receipt.shops().getFirst().items().getFirst().name()).isEqualTo("Áo kiểm thử");
    }
    @Test void rejectsEmptyCartAndOtherCustomersCannotReadReceipt() {
        var f=fixture(); var id=checkout.place(f.user().getUsername(),request(new BigDecimal("240000")));
        assertThatThrownBy(()->checkout.get("another-user",id)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->checkout.place(f.user().getUsername(),request(BigDecimal.ZERO))).isInstanceOf(ResponseStatusException.class).hasMessageContaining("trống");
    }
    @Test void refusesStockShortageWithoutCreatingOrderOrClearingCart() {
        var f=fixture(); f.stock().setQuantity(1); inventory.flush();
        assertThatThrownBy(()->checkout.place(f.user().getUsername(),request(new BigDecimal("240000"))))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("tồn kho");
        assertThat(orders.findByUserUsernameOrderByIdDesc(f.user().getUsername())).isEmpty();
        assertThat(carts.get(f.user().getUsername()).items()).hasSize(1); assertThat(f.stock().getQuantity()).isEqualTo(1);
    }
    @Test void refusesHiddenProduct() {
        var f=fixture(); f.product().setEnabled(false); products.flush();
        assertThatThrownBy(()->checkout.place(f.user().getUsername(),request(new BigDecimal("240000"))))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("không còn kinh doanh");
    }
    @Test void splitsOrderAcrossShopsAndUsesLivePrices() {
        var first=fixture(); var second=fixture();
        var add=new CartAddRequest(); add.setProductId(second.product().getSlug()); add.setSize("M"); add.setColor("Trắng"); add.setQuantity(1);
        carts.add(first.user().getUsername(),add);
        var id=checkout.place(first.user().getUsername(),request(new BigDecimal("360000")));
        assertThat(checkout.get(first.user().getUsername(),id).shops()).hasSize(2);
        assertThat(first.stock().getQuantity()).isEqualTo(3); assertThat(second.stock().getQuantity()).isEqualTo(4);
    }
}
