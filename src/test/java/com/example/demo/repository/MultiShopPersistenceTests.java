package com.example.demo.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.example.demo.entity.*;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MultiShopPersistenceTests {

    @Autowired private UserRepository users;
    @Autowired private RoleRepository roles;
    @Autowired private ShopRepository shops;
    @Autowired private ProductRepository products;
    @Autowired private StoreRepository stores;
    @Autowired private AddressRepository addresses;
    @Autowired private OrderEntityRepository orders;
    @Autowired private ShopOrderRepository shopOrders;
    @Autowired private OrderStatusHistoryRepository histories;
    @Autowired private ShipmentRepository shipments;
    @Autowired private EntityManager em;
    @Autowired private JdbcTemplate jdbc;

    @ParameterizedTest
    @EnumSource(RoleName.class)
    void persistsEverySystemRoleAsString(RoleName name) {
        var user = user("h2t-" + name, name);
        em.flush();
        em.clear();
        assertThat(users.findById(user.getId()).orElseThrow().getRoles())
                .extracting(Role::getName).containsExactly(name);
        assertThat(jdbc.queryForObject("select name from roles where id = ?", String.class,
                roles.findByName(name).orElseThrow().getId())).isEqualTo(name.name());
    }

    @ParameterizedTest
    @EnumSource(ShopStatus.class)
    void vendorOwnsShopsWithIndependentBranchesCatalogAndStock(ShopStatus status) {
        var vendor = user("vendor_h2t", RoleName.ROLE_VENDOR);
        var shop = shop(vendor, "h2t-official");
        shop.setStatus(status);
        shop(vendor, "h2t-outlet");
        var branchA = store(shop, "H2T-TD", "H2T Thủ Đức");
        var branchB = store(shop, "H2T-Q1", "H2T Quận 1");
        var variant = variant(shop, "SM-OXF-WHITE-M");
        variant(shop, "SM-LINEN-WHITE-M");
        var large = new ProductVariant();
        large.setProduct(variant.getProduct());
        large.setSku("SM-OXF-WHITE-L");
        large.setSize("L");
        large.setColor("Trắng");
        em.persist(large);
        stock(branchA, variant, 10);
        stock(branchB, variant, 3);
        em.flush();
        em.clear();

        var loaded = shops.findBySlug("h2t-official").orElseThrow();
        assertThat(loaded.getOwner().getRoles()).extracting(Role::getName).contains(RoleName.ROLE_VENDOR);
        assertThat(loaded.getStatus()).isEqualTo(status);
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(shops.findByOwnerId(vendor.getId())).hasSize(2);
        assertThat(products.findByShopId(shop.getId())).hasSize(2)
                .allSatisfy(p -> assertThat(p.getShop().getId()).isEqualTo(shop.getId()));
        assertThat(stores.findByShopId(shop.getId())).hasSize(2)
                .allSatisfy(s -> assertThat(s.getShop().getId()).isEqualTo(shop.getId()));
        assertThat(em.find(ProductVariant.class, variant.getId()).getProduct().getVariants()).hasSize(2);
        var stocks = em.createQuery("select i from Inventory i", Inventory.class).getResultList();
        assertThat(stocks).extracting(Inventory::getQuantity).containsExactlyInAnyOrder(10, 3);
        assertThat(stocks).allSatisfy(i -> assertThat(i.getStore().getShop().getId())
                .isEqualTo(i.getProductVariant().getProduct().getShop().getId()));
    }

    @Test
    void userKeepsMultipleAddressesAndOrderRetainsItsShippingSnapshot() {
        var user = user("customer_h2t", RoleName.ROLE_USER);
        var home = address(user, "Nhà riêng H2T", true);
        address(user, "Văn phòng H2T", false);
        var order = order(user);
        order.setShippingAddress(home.getAddressLine());
        orders.saveAndFlush(order);
        home.setAddressLine("Địa chỉ mới");
        em.flush();
        em.clear();
        assertThat(addresses.findByUserId(user.getId())).hasSize(2)
                .allSatisfy(a -> assertThat(a.getCreatedAt()).isNotNull());
        assertThat(addresses.findByUserId(user.getId()).stream().filter(Address::isDefault)).hasSize(1);
        assertThat(orders.findById(order.getId()).orElseThrow().getShippingAddress()).isEqualTo("Nhà riêng H2T");
    }

    @Test
    void orderSplitsAcrossShopsWithLinesHistoryAndAssignedShipper() {
        var vendor = user("vendor_h2t", RoleName.ROLE_VENDOR);
        var official = shop(vendor, "h2t-official");
        var other = shop(user("vendor_second", RoleName.ROLE_VENDOR), "h2t-second");
        var order = order(user("customer_h2t", RoleName.ROLE_USER));
        var first = part(order, official, "FIRST");
        first.setStore(store(official, "H2T-TD", "H2T Thủ Đức"));
        var second = part(order, other, "SECOND");
        orders.saveAndFlush(order);
        var history = new OrderStatusHistory();
        history.setShopOrder(first);
        history.setStatus(OrderStatus.NEW);
        history.setNote("Khách đặt áo H2T");
        history.setChangedAt(LocalDateTime.of(2026, 9, 13, 1, 0));
        histories.save(history);
        var confirmed = new OrderStatusHistory();
        confirmed.setShopOrder(first);
        confirmed.setStatus(OrderStatus.CONFIRMED);
        confirmed.setChangedAt(history.getChangedAt().plusMinutes(1));
        histories.save(confirmed);
        first.setStatus(OrderStatus.CONFIRMED);
        var shipper = user("shipper_h2t", RoleName.ROLE_SHIPPER);
        var shipment = shipment(first, shipper);
        shipments.saveAndFlush(shipment);
        official.setEnabled(false);
        em.flush();
        em.clear();

        var loaded = orders.findById(order.getId()).orElseThrow();
        assertThat(loaded.getShopOrders()).hasSize(2);
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getUpdatedAt()).isNotNull();
        assertThat(loaded.getOrderDate()).isNotNull();
        assertThat(loaded.getTotalAmount()).isEqualByComparingTo("1656000.00");
        assertThat(shopOrders.findByOrderId(order.getId())).hasSize(2);
        assertThat(shopOrders.findByShopId(official.getId())).singleElement().satisfies(p -> {
            assertThat(p.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
            assertThat(p.getShop().isEnabled()).isFalse();
            assertThat(p.getStore().getShop().getId()).isEqualTo(p.getShop().getId());
        });
        assertThat(shopOrders.findById(second.getId()).orElseThrow().getStore()).isNull();
        assertThat(loaded.getShopOrders()).allSatisfy(p -> {
            assertThat(p.getItems()).hasSize(2).allSatisfy(i -> {
                assertThat(i.getShopOrder().getId()).isEqualTo(p.getId());
                assertThat(i.getProductVariant().getProduct().getShop().getId()).isEqualTo(p.getShop().getId());
            });
            assertThat(p.getSubtotal()).isEqualByComparingTo("798000.00");
            assertThat(p.getShippingFee()).isEqualByComparingTo("40000.00");
            assertThat(p.getDiscountAmount()).isEqualByComparingTo("10000.00");
            assertThat(p.getTotalAmount()).isEqualByComparingTo("828000.00");
        });
        assertThat(histories.findByShopOrderIdOrderByChangedAtAscIdAsc(first.getId()))
                .extracting(OrderStatusHistory::getStatus).containsExactly(OrderStatus.NEW, OrderStatus.CONFIRMED);
        var assigned = shipments.findByShopOrderId(first.getId()).orElseThrow();
        assertThat(assigned.getShipper().getRoles()).extracting(Role::getName).contains(RoleName.ROLE_SHIPPER);
        assertThat(assigned.getAssignedAt()).isNotNull();
        assertThat(assigned.getStatus()).isEqualTo(ShipmentStatus.ASSIGNED);
        assertThat(shipments.findByShipperId(shipper.getId())).hasSize(1);
    }

    @ParameterizedTest
    @EnumSource(ShipmentStatus.class)
    void shipmentStatusAndTimestampsRoundTrip(ShipmentStatus status) {
        var shop = shop(user("vendor_h2t", RoleName.ROLE_VENDOR), "h2t-official");
        var order = order(user("customer_h2t", RoleName.ROLE_USER));
        var part = part(order, shop, "SM");
        orders.saveAndFlush(order);
        var assignment = shipment(part, user("shipper_h2t", RoleName.ROLE_SHIPPER));
        assignment.setStatus(status);
        assignment.setPickedUpAt(LocalDateTime.of(2026, 9, 13, 2, 0));
        assignment.setDeliveredAt(LocalDateTime.of(2026, 9, 13, 3, 0));
        assignment.setShippingProviderName("H2T Delivery");
        shipments.saveAndFlush(assignment);
        em.clear();
        var loaded = shipments.findById(assignment.getId()).orElseThrow();
        assertThat(loaded.getStatus()).isEqualTo(status);
        assertThat(loaded.getDeliveredAt()).isAfter(loaded.getPickedUpAt());
        assertThat(loaded.getShippingProviderName()).isEqualTo("H2T Delivery");
        assertThat(jdbc.queryForObject("select status from shipments where id = ?", String.class,
                assignment.getId())).isEqualTo(status.name());
    }

    @ParameterizedTest
    @ValueSource(strings = {"slug", "orderShop", "shipment"})
    void rejectsDuplicateShopAndAssignmentKeys(String key) {
        var vendor = user("vendor_h2t", RoleName.ROLE_VENDOR);
        var shop = shop(vendor, "h2t-official");
        var order = order(user("customer_h2t", RoleName.ROLE_USER));
        var part = part(order, shop, "SM");
        orders.saveAndFlush(order);
        var shipper = user("shipper_h2t", RoleName.ROLE_SHIPPER);
        shipments.saveAndFlush(shipment(part, shipper));
        assertThatThrownBy(() -> {
            switch (key) {
                case "slug" -> shops.saveAndFlush(shop(vendor, "h2t-official"));
                case "orderShop" -> shopOrders.saveAndFlush(part(order, shop, "DUP"));
                case "shipment" -> shipments.saveAndFlush(shipment(part, shipper));
                default -> throw new IllegalArgumentException(key);
            }
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"shop", "shopOrder", "shipper"})
    void foreignKeysProtectOrderAndShipmentHistory(String target) {
        var shop = shop(user("vendor_h2t", RoleName.ROLE_VENDOR), "h2t-official");
        var order = order(user("customer_h2t", RoleName.ROLE_USER));
        var part = part(order, shop, "SM");
        orders.saveAndFlush(order);
        var shipper = user("shipper_h2t", RoleName.ROLE_SHIPPER);
        shipments.saveAndFlush(shipment(part, shipper));
        em.clear();
        assertThatThrownBy(() -> {
            switch (target) {
                case "shop" -> { shops.deleteById(shop.getId()); shops.flush(); }
                case "shopOrder" -> { shopOrders.deleteById(part.getId()); shopOrders.flush(); }
                case "shipper" -> { users.deleteById(shipper.getId()); users.flush(); }
                default -> throw new IllegalArgumentException(target);
            }
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"product", "store"})
    void shopOwnershipIsRequired(String target) {
        var shop = shop(user("vendor_h2t", RoleName.ROLE_VENDOR), "h2t-official");
        var product = variant(shop, "SM").getProduct();
        var store = store(shop, "H2T-TD", "H2T Thủ Đức");
        em.flush();
        if (target.equals("product")) product.setShop(null);
        else store.setShop(null);
        assertThatThrownBy(em::flush).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void newAssociationsLoadLazily() {
        var shop = shop(user("vendor_h2t", RoleName.ROLE_VENDOR), "h2t-official");
        var order = order(user("customer_h2t", RoleName.ROLE_USER));
        var part = part(order, shop, "SM");
        var branch = store(shop, "H2T-TD", "H2T Thủ Đức");
        part.setStore(branch);
        orders.saveAndFlush(order);
        var history = new OrderStatusHistory();
        history.setShopOrder(part);
        histories.save(history);
        var shipper = user("shipper_h2t", RoleName.ROLE_SHIPPER);
        var shipment = shipments.save(shipment(part, shipper));
        var address = address(order.getUser(), "Nhà H2T", true);
        em.flush();
        var itemId = part.getItems().getFirst().getId();
        var productId = part.getItems().getFirst().getProductVariant().getProduct().getId();
        em.clear();
        var util = em.getEntityManagerFactory().getPersistenceUnitUtil();
        assertThat(util.isLoaded(histories.findById(history.getId()).orElseThrow(), "shopOrder")).isFalse();
        assertThat(util.isLoaded(addresses.findById(address.getId()).orElseThrow(), "user")).isFalse();
        var loadedShipment = shipments.findById(shipment.getId()).orElseThrow();
        assertThat(util.isLoaded(loadedShipment, "shipper")).isFalse();
        assertThat(util.isLoaded(loadedShipment, "shopOrder")).isFalse();
        assertThat(util.isLoaded(em.find(OrderItem.class, itemId), "shopOrder")).isFalse();
        var loadedPart = shopOrders.findById(part.getId()).orElseThrow();
        for (var field : new String[]{"order", "shop", "store", "items"}) {
            assertThat(util.isLoaded(loadedPart, field)).as(field).isFalse();
        }
        assertThat(util.isLoaded(products.findById(productId).orElseThrow(), "shop")).isFalse();
        assertThat(util.isLoaded(stores.findById(branch.getId()).orElseThrow(), "shop")).isFalse();
        assertThat(util.isLoaded(shops.findById(shop.getId()).orElseThrow(), "owner")).isFalse();
    }

    private User user(String name, RoleName roleName) {
        var role = roles.findByName(roleName).orElseGet(() -> {
            var r = new Role(); r.setName(roleName); return roles.save(r);
        });
        var user = new User();
        user.setUsername(name);
        user.setEmail(name + "@example.test");
        user.setFullName("Người dùng H2T");
        user.setPassword(UUID.randomUUID().toString());
        user.addRole(role);
        return users.save(user);
    }

    private Shop shop(User vendor, String slug) {
        var shop = new Shop();
        shop.setOwner(vendor);
        shop.setName("H2T Official");
        shop.setSlug(slug);
        return shops.save(shop);
    }

    private Store store(Shop shop, String code, String name) {
        var store = new Store();
        store.setShop(shop);
        store.setCode(code);
        store.setName(name);
        store.setAddress("Địa chỉ chi nhánh H2T");
        return stores.save(store);
    }

    private ProductVariant variant(Shop shop, String sku) {
        var category = new Category(); category.setName("Áo sơ mi"); em.persist(category);
        var product = new Product();
        product.setShop(shop); product.setCategory(category);
        product.setName("Áo sơ mi Oxford Regular Fit"); product.setSlug(sku.toLowerCase());
        product.setBasePrice(new BigDecimal("399000.00")); em.persist(product);
        var variant = new ProductVariant();
        variant.setProduct(product); variant.setSku(sku); variant.setSize("M"); variant.setColor("Trắng");
        em.persist(variant);
        return variant;
    }

    private void stock(Store store, ProductVariant variant, int quantity) {
        var stock = new Inventory();
        stock.setStore(store); stock.setProductVariant(variant); stock.setQuantity(quantity); em.persist(stock);
    }

    private Address address(User user, String line, boolean isDefault) {
        var address = new Address();
        address.setUser(user); address.setReceiverName("Khách H2T"); address.setPhone("0900000000");
        address.setAddressLine(line); address.setWard("Phường kiểm thử");
        address.setDistrict("Thủ Đức"); address.setCity("TP Hồ Chí Minh"); address.setDefault(isDefault);
        return addresses.save(address);
    }

    private OrderEntity order(User user) {
        var order = new OrderEntity();
        order.setUser(user); order.setTotalAmount(new BigDecimal("1656000.00"));
        order.setReceiverName("Khách H2T"); order.setReceiverPhone("0900000000");
        order.setShippingAddress("Địa chỉ nhận áo H2T");
        return order;
    }

    private ShopOrder part(OrderEntity order, Shop shop, String prefix) {
        var part = new ShopOrder(); part.setShop(shop);
        part.setSubtotal(new BigDecimal("798000.00")); part.setShippingFee(new BigDecimal("40000.00"));
        part.setDiscountAmount(new BigDecimal("10000.00")); part.setTotalAmount(new BigDecimal("828000.00"));
        for (int i = 0; i < 2; i++) {
            var variant = variant(shop, prefix + "-" + i);
            var item = new OrderItem(); item.setProductVariant(variant);
            item.setProductName(variant.getProduct().getName()); item.setVariantSku(variant.getSku());
            item.setSize(variant.getSize()); item.setColor(variant.getColor()); item.setQuantity(1);
            item.setUnitPrice(new BigDecimal("399000.00")); item.setSubtotal(new BigDecimal("399000.00"));
            part.addItem(item);
        }
        order.addShopOrder(part);
        return part;
    }

    private Shipment shipment(ShopOrder part, User shipper) {
        var shipment = new Shipment(); shipment.setShopOrder(part); shipment.setShipper(shipper);
        return shipment;
    }
}
