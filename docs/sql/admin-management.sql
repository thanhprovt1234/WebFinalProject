-- Run as database owner before starting the administration/stock-allocation update.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF OBJECT_ID(N'dbo.order_stock_allocations',N'U') IS NULL
BEGIN
    CREATE TABLE dbo.order_stock_allocations (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        shop_order_id BIGINT NOT NULL,
        inventory_id BIGINT NOT NULL,
        quantity INT NOT NULL,
        restored BIT NOT NULL CONSTRAINT df_order_stock_restored DEFAULT 0,
        CONSTRAINT uk_order_stock_allocation UNIQUE(shop_order_id,inventory_id),
        CONSTRAINT ck_order_stock_quantity CHECK(quantity>0),
        CONSTRAINT fk_order_stock_shop_order FOREIGN KEY(shop_order_id) REFERENCES dbo.shop_orders(id),
        CONSTRAINT fk_order_stock_inventory FOREIGN KEY(inventory_id) REFERENCES dbo.inventories(id)
    );
END;
IF DATABASE_PRINCIPAL_ID(N'h2t_user') IS NOT NULL
BEGIN
    GRANT SELECT,INSERT,UPDATE ON dbo.shops TO h2t_user;
    GRANT SELECT,INSERT,UPDATE ON dbo.stores TO h2t_user;
    GRANT SELECT,INSERT,UPDATE ON dbo.products TO h2t_user;
    GRANT SELECT,INSERT,UPDATE ON dbo.categories TO h2t_user;
    GRANT SELECT,INSERT,UPDATE ON dbo.product_variants TO h2t_user;
    GRANT SELECT,INSERT,UPDATE ON dbo.inventories TO h2t_user;
    GRANT SELECT,INSERT,UPDATE ON dbo.orders TO h2t_user;
    GRANT SELECT,INSERT,UPDATE ON dbo.shop_orders TO h2t_user;
    GRANT SELECT,INSERT ON dbo.order_items TO h2t_user;
    GRANT SELECT,INSERT ON dbo.order_status_history TO h2t_user;
    GRANT SELECT,INSERT,UPDATE ON dbo.order_stock_allocations TO h2t_user;
END;
COMMIT;
