package com.example.demo.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.junit.jupiter.api.Test;

class SqlServerSchemaTests {

    @Test
    void generatesSqlServerDdlWithoutConnectingToADatabase() throws Exception {
        var script = new StringWriter();
        Map<String, Object> settings = new HashMap<>();
        settings.put("hibernate.dialect", "org.hibernate.dialect.SQLServerDialect");
        settings.put("hibernate.boot.allow_jdbc_metadata_access", false);
        settings.put("jakarta.persistence.schema-generation.database.action", "none");
        settings.put("jakarta.persistence.schema-generation.scripts.action", "create");
        settings.put("jakarta.persistence.schema-generation.scripts.create-target", script);
        settings.put("hibernate.hbm2ddl.delimiter", ";");

        var registry = new StandardServiceRegistryBuilder().applySettings(settings).build();
        try {
            var sources = new MetadataSources(registry);
            List.of(User.class, Role.class, Category.class, Product.class, ProductVariant.class,
                    Store.class, StoreManager.class, Inventory.class, OrderEntity.class, OrderItem.class, Shop.class,
                    ShopOrder.class, OrderStatusHistory.class, Shipment.class, Address.class, ProductReview.class, CartItem.class, ProductActivity.class)
                    .forEach(sources::addAnnotatedClass);
            var metadata = sources.buildMetadata();
            SchemaManagementToolCoordinator.process(metadata, registry, settings, action -> {
                throw new IllegalStateException("This test must not schedule database schema changes");
            });
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }

        var ddl = script.toString().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        for (var table : List.of("users", "roles", "user_roles", "categories", "products",
                "product_variants", "stores", "store_managers", "inventories", "orders", "order_items", "shops",
                "shop_orders", "order_status_history", "shipments", "addresses", "product_reviews", "cart_items", "product_activities")) {
            assertThat(ddl).contains("create table " + table + " (");
        }
        assertThat(ddl).contains("identity", "nvarchar(200)", "nvarchar(50)", "datetime2", "bit");
        for (var column : List.of("base_price", "price", "total_amount", "unit_price", "subtotal", "shipping_fee", "discount_amount")) {
            assertThat(ddl).containsPattern(column + " (numeric|decimal)\\(19,2\\)");
        }
        assertThat(ddl).contains(
                "constraint uk_users_username unique (username)",
                "constraint uk_users_email unique (email)",
                "constraint uk_roles_name unique (name)",
                "constraint uk_products_slug unique (slug)",
                "constraint uk_product_variants_sku unique (sku)",
                "constraint uk_product_variants_options unique (product_id, size, color)",
                "constraint uk_stores_code unique (code)",
                "constraint uk_inventories_store_variant unique (store_id, product_variant_id)",
                "constraint ck_inventories_values check (quantity >= 0)",
                "foreign key (product_variant_id) references product_variants",
                "primary key (user_id, role_id)",
                "constraint uk_shops_slug unique (slug)",
                "constraint uk_shop_orders_order_shop unique (order_id, shop_id)",
                "foreign key (shop_id) references shops",
                "foreign key (owner_id) references users",
                "foreign key (shop_order_id) references shop_orders",
                "foreign key (shipper_id) references users");
        assertThat(ddl).doesNotContain("create table user (", "create table order (", "on delete cascade");
        assertThat(ddl).contains("constraint uk_product_reviews_catalog_user unique (catalog_product_id, user_id)",
                "constraint ck_product_reviews_rating check (rating between 1 and 5)", "comment nvarchar(2000)");
        assertThat(ddl).contains("constraint uk_cart_items_user_product_option unique (user_id, catalog_product_id, size, color)",
                "constraint ck_cart_items_quantity check (quantity between 1 and 99)");
        assertThat(ddl).contains("status varchar(20)", "'new'", "'delivered'", "'return_requested'", "'refunded'", "'role_vendor'", "'role_shipper'");

        // Reviewable artifact only. No statement is sent to SQL Server.
        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target", "sqlserver-schema.sql"),
                "-- H2T schema generated for review only; not executed against SQL Server.\n"
                        + script, StandardCharsets.UTF_8);
    }
}
