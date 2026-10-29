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
    @Autowired VoucherService vouchers;
    @Autowired VoucherRepository voucherRepository;
    @Autowired PurchaseConfirmationService confirmations;
    @Autowired CustomerOrderService customerOrders;
    @Autowired AdminVoucherService adminVouchers;
    @Autowired RoleRepository roles;
    @Autowired VoucherCampaignRepository campaigns;
    private Long giftCampaign(Fixture buyer,String type,String value,String minimum,String maximum) {
        for(var name:java.util.List.of(RoleName.ROLE_USER,RoleName.ROLE_ADMIN)) {
            var role=roles.findByName(name).orElseGet(()->{var r=new Role();r.setName(name);return roles.save(r);});
            buyer.user().addRole(role);
        }
        users.flush();
        var form=new VoucherCampaignForm();form.setName("Lucky gift");form.setDiscountType(type);
        form.setDiscountValue(new BigDecimal(value));form.setMinimumTotal(new BigDecimal(minimum));
        form.setMaximumDiscount(maximum==null?null:new BigDecimal(maximum));
        form.setExpiryDate(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(3));
        var id=adminVouchers.create(buyer.user().getUsername(),form);
        adminVouchers.issue(buyer.user().getUsername(),id,java.util.List.of(buyer.user().getId()));return id;
    }
    @Test void fixedGiftChecksMinimumAndDistributesExactDiscountAcrossShops() {
        var buyer=fixture();var second=fixture();
        var campaignId=giftCampaign(buyer,"FIXED","50000","300000",null);
        var code=vouchers.available(buyer.user().getUsername()).getFirst();
        assertThatThrownBy(()->vouchers.preview(buyer.user().getUsername(),code,new BigDecimal("240000")))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("tối thiểu");
        var add=new CartAddRequest();add.setProductId(second.product().getSlug());add.setSize("M");add.setColor("Trắng");add.setQuantity(1);
        carts.add(buyer.user().getUsername(),add);
        var req=request(new BigDecimal("310000"));req.setVoucherCode(code);
        var order=checkout.get(buyer.user().getUsername(),checkout.place(buyer.user().getUsername(),req));
        assertThat(order.discount()).isEqualByComparingTo("50000");
        assertThat(order.shops().stream().map(CheckoutService.ShopView::total).reduce(BigDecimal.ZERO,BigDecimal::add))
                .isEqualByComparingTo("310000");
        assertThatThrownBy(()->adminVouchers.revoke(buyer.user().getUsername(),campaignId,
                voucherRepository.findByCampaignIdOrderByIdDesc(campaignId).getFirst().getId()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("đã sử dụng");
    }
    @Test void percentGiftHonorsCapExpiryRevocationAndCannotExceedOrderTotal() {
        var buyer=fixture();var id=giftCampaign(buyer,"PERCENT","20","0","40000");
        var name=buyer.user().getUsername();var gift=voucherRepository.findByCampaignIdOrderByIdDesc(id).getFirst();
        assertThat(vouchers.preview(name,gift.getCode(),new BigDecimal("360000"))).isEqualByComparingTo("40000");
        var c=campaigns.findById(id).orElseThrow();c.setExpiresAt(java.time.Instant.now().minusSeconds(1));
        assertThat(vouchers.available(name)).isEmpty();
        assertThatThrownBy(()->vouchers.preview(name,gift.getCode(),new BigDecimal("360000"))).isInstanceOf(ResponseStatusException.class);
        c.setExpiresAt(java.time.Instant.now().plusSeconds(3600));adminVouchers.revoke(name,id,gift.getId());
        assertThatThrownBy(()->vouchers.preview(name,gift.getCode(),new BigDecimal("360000"))).isInstanceOf(ResponseStatusException.class);
        var fixedId=giftCampaign(buyer,"FIXED","999999","0",null);
        var fixed=voucherRepository.findByCampaignIdOrderByIdDesc(fixedId).getFirst();
        assertThat(vouchers.preview(name,fixed.getCode(),new BigDecimal("240000"))).isEqualByComparingTo("240000");
        var req=request(BigDecimal.ZERO);req.setVoucherCode(fixed.getCode());
        assertThat(checkout.get(name,checkout.place(name,req)).total()).isEqualByComparingTo("0");
    }
    @Test void zeroPricedProductsDoNotCauseVoucherAllocationDivisionByZero() {
        var buyer=fixture();buyer.variant().setPrice(BigDecimal.ZERO);variants.flush();
        giftCampaign(buyer,"FIXED","50000","0",null);
        var name=buyer.user().getUsername();var req=request(BigDecimal.ZERO);
        req.setVoucherCode(vouchers.available(name).getFirst());
        var receipt=checkout.get(name,checkout.place(name,req));
        assertThat(receipt.total()).isEqualByComparingTo("0");assertThat(receipt.discount()).isEqualByComparingTo("0");
    }
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

    private Long purchase(Fixture f,String unitPrice) {
        f.variant().setPrice(new BigDecimal(unitPrice)); variants.flush();
        return checkout.place(f.user().getUsername(),request(new BigDecimal(unitPrice).multiply(BigDecimal.valueOf(2))));
    }
    private void deliver(Long id) {
        var order=orders.findById(id).orElseThrow();
        order.getShopOrders().forEach(s->s.setStatus(OrderStatus.DELIVERED));
        order.setPaymentStatus("PAID");
        confirmations.notifyIfCompleted(order,false);
    }
    private void addAgain(Fixture f) {
        var add=new CartAddRequest(); add.setProductId(f.product().getSlug()); add.setSize("M"); add.setColor("Trắng"); add.setQuantity(2);
        carts.add(f.user().getUsername(),add);
    }
    @Test void rewardsOnlyCompletedOrdersStrictlyAboveThresholdAndOnlyOnce() {
        var atThreshold=fixture(); var exact=purchase(atThreshold,"250000"); deliver(exact);
        assertThat(vouchers.available(atThreshold.user().getUsername())).isEmpty();
        var above=fixture(); var qualifying=purchase(above,"250001");
        assertThat(vouchers.available(above.user().getUsername())).isEmpty();
        deliver(qualifying); deliver(qualifying);
        assertThat(vouchers.available(above.user().getUsername())).hasSize(1);
        var cancelled=fixture(); var cancelledId=purchase(cancelled,"300000");
        customerOrders.cancelPending(cancelled.user().getUsername(),cancelledId,"");
        assertThat(vouchers.available(cancelled.user().getUsername())).isEmpty();
    }
    @Test void voucherIsOwnedSingleUseAndDiscountIsPersistedAcrossShopsAndCancellation() {
        var buyer=fixture(); var source=purchase(buyer,"300000"); deliver(source);
        var name=buyer.user().getUsername(); var code=vouchers.available(name).getFirst();
        var other=fixture();
        assertThatThrownBy(()->vouchers.preview(other.user().getUsername(),code,new BigDecimal("100000")))
                .isInstanceOf(ResponseStatusException.class);
        addAgain(buyer);
        var extra=new CartAddRequest(); extra.setProductId(other.product().getSlug()); extra.setSize("M"); extra.setColor("Trắng"); extra.setQuantity(1);
        carts.add(name,extra);
        assertThat(vouchers.preview(name,code.toLowerCase(java.util.Locale.ROOT),new BigDecimal("720000"))).isEqualByComparingTo("72000");
        var r=request(new BigDecimal("648000")); r.setVoucherCode(code);
        var id=checkout.place(name,r);
        assertThat(checkout.place(name,r)).isEqualTo(id);
        var receipt=checkout.get(name,id);
        assertThat(receipt.total()).isEqualByComparingTo("648000");
        assertThat(receipt.discount()).isEqualByComparingTo("72000");
        assertThat(receipt.voucherCode()).isEqualTo(code);
        assertThat(receipt.shops().stream().map(CheckoutService.ShopView::total).reduce(BigDecimal.ZERO,BigDecimal::add))
                .isEqualByComparingTo(receipt.total());
        assertThat(vouchers.available(name)).isEmpty();
        // Cancelling one shop subtracts its discounted total, leaving the other payable amount.
        var order=orders.findById(id).orElseThrow();
        var kept=order.getShopOrders().getFirst(); kept.setStatus(OrderStatus.CONFIRMED);
        customerOrders.cancelPending(name,id,"");
        assertThat(order.getTotalAmount()).isEqualByComparingTo(kept.getTotalAmount());
        assertThatThrownBy(()->vouchers.preview(name,code,BigDecimal.TEN)).isInstanceOf(ResponseStatusException.class);
        assertThat(voucherRepository.existsBySourceOrderId(source)).isTrue();
    }
}
