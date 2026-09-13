package com.example.demo.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import com.example.demo.entity.Category;
import com.example.demo.entity.Shop;
import com.example.demo.entity.ShopOrder;
import com.example.demo.entity.RoleName;
import com.example.demo.entity.Inventory;
import com.example.demo.entity.OrderEntity;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.OrderStatus;
import com.example.demo.entity.Product;
import com.example.demo.entity.ProductVariant;
import com.example.demo.entity.Role;
import com.example.demo.entity.Store;
import com.example.demo.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
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
class H2tPersistenceTests {

    @Autowired private ShopRepository shops;
    private Shop testShop;

    @Autowired private CategoryRepository categories;
    @Autowired private ProductRepository products;
    @Autowired private ProductVariantRepository variants;
    @Autowired private StoreRepository stores;
    @Autowired private InventoryRepository inventories;
    @Autowired private UserRepository users;
    @Autowired private RoleRepository roles;
    @Autowired private OrderEntityRepository orders;
    @Autowired private OrderItemRepository orderItems;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void persistsShirtVariantsAndIndependentStockPerStore() {
        var catalog = catalog();
        inventories.save(inventory(catalog.storeA(), catalog.medium(), 10));
        inventories.save(inventory(catalog.storeA(), catalog.large(), 0));
        inventories.saveAndFlush(inventory(catalog.storeB(), catalog.medium(), 5));
        entityManager.clear();

        assertThat(categories.findByName("Áo sơ mi")).singleElement()
                .satisfies(category -> assertThat(category.getId()).isEqualTo(catalog.category().getId()));
        var product = products.findBySlug("ao-so-mi-oxford-regular-fit").orElseThrow();
        assertThat(product.getName()).isEqualTo("Áo sơ mi Oxford Regular Fit");
        assertThat(product.getCategory().getName()).isEqualTo("Áo sơ mi");
        assertThat(product.getBasePrice()).isEqualByComparingTo("399000.00");
        assertThat(product.getVariants()).extracting(ProductVariant::getSku)
                .containsExactlyInAnyOrder("SM-OXF-WHITE-M", "SM-OXF-WHITE-L");
        assertThat(variants.findBySku("SM-OXF-WHITE-M").orElseThrow().getPrice()).isNull();
        var large = variants.findBySku("SM-OXF-WHITE-L").orElseThrow();
        assertThat(large.getSize()).isEqualTo("L");
        assertThat(large.getColor()).isEqualTo("Trắng");
        assertThat(large.getPrice()).isEqualByComparingTo("419000.00");
        assertThat(stores.findByCode("H2T-TD").orElseThrow().getName()).isEqualTo("H2T Thủ Đức");
        assertStock(catalog.storeA(), catalog.medium(), 10);
        assertStock(catalog.storeA(), catalog.large(), 0);
        assertStock(catalog.storeB(), catalog.medium(), 5);
    }

    @Test
    void keepsAssociationsLazyWhenReloaded() {
        var catalog = catalog();
        var stock = inventories.saveAndFlush(inventory(catalog.storeA(), catalog.medium(), 10));
        var order = order(catalog);
        orders.saveAndFlush(order);
        entityManager.clear();

        var persistence = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
        var loadedStock = inventories.findById(stock.getId()).orElseThrow();
        assertThat(persistence.isLoaded(loadedStock, "store")).isFalse();
        assertThat(persistence.isLoaded(loadedStock, "productVariant")).isFalse();
        var loadedProduct = products.findById(catalog.product().getId()).orElseThrow();
        assertThat(persistence.isLoaded(loadedProduct, "category")).isFalse();
        assertThat(persistence.isLoaded(loadedProduct, "variants")).isFalse();
        var loadedOrder = orders.findById(order.getId()).orElseThrow();
        assertThat(persistence.isLoaded(loadedOrder, "user")).isFalse();
        assertThat(persistence.isLoaded(loadedOrder, "shopOrders")).isFalse();
    }

    @ParameterizedTest
    @EnumSource(UniqueKey.class)
    void rejectsDuplicateBusinessKeys(UniqueKey key) {
        var catalog = catalog();
        users.saveAndFlush(user("khach.h2t", "khach.h2t@example.test"));
        roles.saveAndFlush(role("ROLE_USER"));
        inventories.saveAndFlush(inventory(catalog.storeA(), catalog.medium(), 10));

        assertThatThrownBy(() -> {
            switch (key) {
                case USERNAME -> users.saveAndFlush(user("khach.h2t", "khach.khac@example.test"));
                case EMAIL -> users.saveAndFlush(user("khach.khac", "khach.h2t@example.test"));
                case ROLE_NAME -> roles.saveAndFlush(role("ROLE_USER"));
                case PRODUCT_SLUG -> products.saveAndFlush(product(catalog.category()));
                case VARIANT_SKU -> variants.saveAndFlush(
                        variant(catalog.product(), "SM-OXF-WHITE-M", "XL", "Đen", null));
                case VARIANT_OPTIONS -> variants.saveAndFlush(
                        variant(catalog.product(), "SM-OXF-WHITE-M-OTHER", "M", "Trắng", null));
                case STORE_CODE -> stores.saveAndFlush(store("H2T Chi nhánh khác", "H2T-TD"));
                case INVENTORY_PAIR -> inventories.saveAndFlush(
                        inventory(catalog.storeA(), catalog.medium(), 99));
            }
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {-1, -10})
    void rejectsMissingOrNegativeInventoryQuantity(Integer quantity) {
        var catalog = catalog();
        assertThatThrownBy(() -> inventories.saveAndFlush(
                inventory(catalog.storeA(), catalog.medium(), quantity)))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void databaseRejectsNegativeInventoryEvenWhenBeanValidationIsBypassed() {
        var catalog = catalog();
        var stock = inventories.saveAndFlush(inventory(catalog.storeA(), catalog.medium(), 10));
        assertThatThrownBy(() -> jdbc.update(
                "update inventories set quantity = -1 where id = ?", stock.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void persistsSharedRolesAndDoesNotDeleteThemWithAUser() {
        var customerRole = roles.save(role("ROLE_USER"));
        var adminRole = roles.save(role("ROLE_ADMIN"));
        var customer = user("khach.h2t", "khach.h2t@example.test");
        customer.addRole(customerRole);
        customer.addRole(adminRole);
        customer.addRole(customerRole);
        users.saveAndFlush(customer);
        entityManager.clear();

        var loaded = users.findByUsername("khach.h2t").orElseThrow();
        assertThat(loaded.getRoles()).extracting(Role::getName)
                .containsExactlyInAnyOrder(RoleName.ROLE_USER, RoleName.ROLE_ADMIN);
        assertThat(users.findByEmail("khach.h2t@example.test").orElseThrow().getId())
                .isEqualTo(loaded.getId());
        assertThat(users.existsByUsername("khach.h2t")).isTrue();
        assertThat(users.existsByEmail("khach.h2t@example.test")).isTrue();
        users.delete(loaded);
        users.flush();
        entityManager.clear();
        assertThat(roles.findByName(RoleName.ROLE_USER)).isPresent();
        assertThat(roles.findByName(RoleName.ROLE_ADMIN)).isPresent();
        assertThat(jdbc.queryForObject("select count(*) from user_roles", Long.class)).isZero();
    }

    @ParameterizedTest
    @EnumSource(OrderStatus.class)
    void persistsOrderWithVariantSnapshotsAndStringStatus(OrderStatus status) {
        var catalog = catalog();
        var order = order(catalog);
        order.getShopOrders().getFirst().setStatus(status);
        orders.saveAndFlush(order);
        var orderId = order.getId();
        var userId = order.getUser().getId();
        entityManager.clear();

        var loaded = orders.findById(orderId).orElseThrow();
        assertThat(loaded.getUser().getId()).isEqualTo(userId);
        assertThat(loaded.getShopOrders().getFirst().getStore().getId()).isEqualTo(catalog.storeA().getId());
        assertThat(loaded.getOrderDate()).isNotNull();
        assertThat(loaded.getShopOrders().getFirst().getStatus()).isEqualTo(status);
        assertThat(jdbc.queryForObject("select status from shop_orders where order_id = ?", String.class, orderId))
                .isEqualTo(status.name());
        assertThat(loaded.getTotalAmount()).isEqualByComparingTo("1217000.00");
        assertThat(loaded.getShopOrders().getFirst().getItems()).hasSize(2);
        assertThat(loaded.getShopOrders().getFirst().getItems()).extracting(item -> item.getProductVariant().getId())
                .containsExactlyInAnyOrder(catalog.medium().getId(), catalog.large().getId());
        var mediumLine = loaded.getShopOrders().getFirst().getItems().stream()
                .filter(item -> item.getVariantSku().equals("SM-OXF-WHITE-M")).findFirst().orElseThrow();
        assertThat(mediumLine.getProductName()).isEqualTo("Áo sơ mi Oxford Regular Fit");
        assertThat(mediumLine.getSize()).isEqualTo("M");
        assertThat(mediumLine.getColor()).isEqualTo("Trắng");
        assertThat(mediumLine.getQuantity()).isEqualTo(2);
        assertThat(mediumLine.getUnitPrice()).isEqualByComparingTo("399000.00");
        assertThat(mediumLine.getSubtotal()).isEqualByComparingTo("798000.00");
        assertThat(orderItems.findById(mediumLine.getId())).isPresent();
    }

    @Test
    void historicalSnapshotsSurviveCatalogChanges() {
        var catalog = catalog();
        var order = orders.saveAndFlush(order(catalog));
        var lineId = order.getShopOrders().getFirst().getItems().getFirst().getId();

        catalog.product().setName("Áo sơ mi Oxford H2T phiên bản mới");
        catalog.product().setBasePrice(new BigDecimal("459000.00"));
        catalog.medium().setSku("SM-OXF-CREAM-XL");
        catalog.medium().setSize("XL");
        catalog.medium().setColor("Kem");
        catalog.medium().setPrice(new BigDecimal("479000.00"));
        entityManager.flush();
        entityManager.clear();

        var snapshot = orderItems.findById(lineId).orElseThrow();
        assertThat(snapshot.getProductVariant().getSku()).isEqualTo("SM-OXF-CREAM-XL");
        assertThat(snapshot.getProductName()).isEqualTo("Áo sơ mi Oxford Regular Fit");
        assertThat(snapshot.getVariantSku()).isEqualTo("SM-OXF-WHITE-M");
        assertThat(snapshot.getSize()).isEqualTo("M");
        assertThat(snapshot.getColor()).isEqualTo("Trắng");
        assertThat(snapshot.getUnitPrice()).isEqualByComparingTo("399000.00");
        assertThat(snapshot.getSubtotal()).isEqualByComparingTo("798000.00");
        assertThat(snapshot.getShopOrder().getOrder().getTotalAmount()).isEqualByComparingTo("1217000.00");
    }

    @ParameterizedTest
    @ValueSource(strings = {"product", "variant", "order"})
    void referencedRecordsCannotCascadeDeleteOrderHistory(String target) {
        var catalog = catalog();
        var order = orders.saveAndFlush(order(catalog));
        entityManager.clear();

        assertThatThrownBy(() -> {
            switch (target) {
                case "product" -> {
                    products.deleteById(catalog.product().getId());
                    products.flush();
                }
                case "variant" -> {
                    variants.deleteById(catalog.medium().getId());
                    variants.flush();
                }
                case "order" -> {
                    orders.deleteById(order.getId());
                    orders.flush();
                }
                default -> throw new IllegalArgumentException(target);
            }
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void initializesAndUpdatesAuditTimestamps() {
        var catalog = catalog();
        var stock = inventories.saveAndFlush(inventory(catalog.storeA(), catalog.medium(), 10));
        entityManager.refresh(catalog.product());
        entityManager.refresh(stock);
        var createdAt = catalog.product().getCreatedAt();
        var updatedAt = stock.getUpdatedAt();
        assertThat(createdAt).isNotNull();
        assertThat(catalog.category().getCreatedAt()).isNotNull();
        assertThat(catalog.storeA().getCreatedAt()).isNotNull();
        assertThat(updatedAt).isNotNull();
        catalog.product().setMaterial("Cotton Oxford");
        stock.setQuantity(12);
        entityManager.flush();
        entityManager.clear();

        var product = products.findById(catalog.product().getId()).orElseThrow();
        assertThat(product.getCreatedAt()).isEqualTo(createdAt);
        assertThat(product.getUpdatedAt()).isAfterOrEqualTo(createdAt);
        assertThat(inventories.findById(stock.getId()).orElseThrow().getUpdatedAt())
                .isAfterOrEqualTo(updatedAt);
    }

    @Test
    void rejectsMoneyWithMoreThanTwoFractionDigits() {
        var category = categories.save(category());
        var product = product(category);
        product.setBasePrice(new BigDecimal("399000.001"));
        assertThatThrownBy(() -> products.saveAndFlush(product))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void rejectsZeroOrderItemQuantity() {
        var order = order(catalog());
        order.getShopOrders().getFirst().getItems().getFirst().setQuantity(0);
        assertThatThrownBy(() -> orders.saveAndFlush(order))
                .isInstanceOf(ConstraintViolationException.class);
    }

    private void assertStock(Store store, ProductVariant variant, int quantity) {
        var stock = inventories.findByStoreIdAndProductVariantId(store.getId(), variant.getId()).orElseThrow();
        assertThat(stock.getQuantity()).isEqualTo(quantity);
        assertThat(stock.getProductVariant().getSize()).isEqualTo(variant.getSize());
    }

    private Catalog catalog() {
        var category = categories.save(category());
        var product = products.save(product(category));
        var medium = variants.save(variant(product, "SM-OXF-WHITE-M", "M", "Trắng", null));
        var large = variants.save(variant(product, "SM-OXF-WHITE-L", "L", "Trắng", new BigDecimal("419000.00")));
        var storeA = stores.save(store("H2T Thủ Đức", "H2T-TD"));
        var storeB = stores.saveAndFlush(store("H2T Quận 1", "H2T-Q1"));
        return new Catalog(category, product, medium, large, storeA, storeB);
    }

    private Shop testShop() {
        if (testShop == null) {
            var vendor = user("vendor_h2t", "vendor_h2t@example.test");
            vendor.addRole(roles.save(role("ROLE_VENDOR")));
            users.save(vendor);
            testShop = new Shop();
            testShop.setName("H2T Official");
            testShop.setSlug("h2t-official");
            testShop.setOwner(vendor);
            shops.save(testShop);
        }
        return testShop;
    }

    private Category category() {
        var category = new Category();
        category.setName("Áo sơ mi");
        return category;
    }

    private Product product(Category category) {
        var product = new Product();
        product.setName("Áo sơ mi Oxford Regular Fit");
        product.setSlug("ao-so-mi-oxford-regular-fit");
        product.setMaterial("Cotton");
        product.setBasePrice(new BigDecimal("399000.00"));
        product.setCategory(category);
        product.setShop(testShop());
        return product;
    }

    private ProductVariant variant(Product product, String sku, String size, String color, BigDecimal price) {
        var variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSku(sku);
        variant.setSize(size);
        variant.setColor(color);
        variant.setPrice(price);
        return variant;
    }

    private Store store(String name, String code) {
        var store = new Store();
        store.setShop(testShop());
        store.setName(name);
        store.setCode(code);
        store.setAddress("Địa chỉ chi nhánh H2T dùng trong kiểm thử");
        return store;
    }

    private Inventory inventory(Store store, ProductVariant variant, Integer quantity) {
        var inventory = new Inventory();
        inventory.setStore(store);
        inventory.setProductVariant(variant);
        inventory.setQuantity(quantity);
        return inventory;
    }

    private Role role(String name) {
        var role = new Role();
        role.setName(RoleName.valueOf(name));
        return role;
    }

    private User user(String username, String email) {
        var user = new User();
        user.setUsername(username);
        user.setEmail(email);
        // Random persistence-only value; authentication is outside these tests.
        user.setPassword(UUID.randomUUID().toString());
        user.setFullName("Khách hàng H2T");
        return user;
    }

    private OrderEntity order(Catalog catalog) {
        var customer = users.save(user("khach.h2t", "khach.h2t@example.test"));
        var order = new OrderEntity();
        order.setUser(customer);
        var part = new ShopOrder();
        part.setShop(catalog.product().getShop());
        part.setStore(catalog.storeA());
        part.setSubtotal(new BigDecimal("1217000.00"));
        part.setTotalAmount(new BigDecimal("1217000.00"));
        order.addShopOrder(part);
        order.setTotalAmount(new BigDecimal("1217000.00"));
        order.setReceiverName("Khách hàng H2T");
        order.setReceiverPhone("0900000000");
        order.setShippingAddress("Địa chỉ nhận áo H2T dùng trong kiểm thử");
        part.addItem(line(catalog.medium(), 2, new BigDecimal("399000.00")));
        part.addItem(line(catalog.large(), 1, new BigDecimal("419000.00")));
        return order;
    }

    private OrderItem line(ProductVariant variant, int quantity, BigDecimal unitPrice) {
        var item = new OrderItem();
        item.setProductVariant(variant);
        item.setProductName(variant.getProduct().getName());
        item.setVariantSku(variant.getSku());
        item.setSize(variant.getSize());
        item.setColor(variant.getColor());
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setSubtotal(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        return item;
    }

    private record Catalog(Category category, Product product, ProductVariant medium,
            ProductVariant large, Store storeA, Store storeB) {
    }

    private enum UniqueKey {
        USERNAME, EMAIL, ROLE_NAME, PRODUCT_SLUG, VARIANT_SKU, VARIANT_OPTIONS, STORE_CODE, INVENTORY_PAIR
    }
}
