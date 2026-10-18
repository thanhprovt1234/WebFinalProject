-- Add COD checkout metadata without changing existing order records or stock.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.orders','payment_method') IS NULL
    ALTER TABLE dbo.orders ADD payment_method VARCHAR(20) NOT NULL CONSTRAINT df_orders_payment_method DEFAULT 'COD';
IF COL_LENGTH('dbo.orders','payment_status') IS NULL
    ALTER TABLE dbo.orders ADD payment_status VARCHAR(20) NOT NULL CONSTRAINT df_orders_payment_status DEFAULT 'UNPAID';
IF COL_LENGTH('dbo.orders','checkout_token') IS NULL
    ALTER TABLE dbo.orders ADD checkout_token VARCHAR(36) NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.orders') AND name='uk_orders_checkout_token')
    EXEC(N'CREATE UNIQUE INDEX uk_orders_checkout_token ON dbo.orders(user_id,checkout_token) WHERE checkout_token IS NOT NULL');
IF DATABASE_PRINCIPAL_ID(N'h2t_user') IS NOT NULL
BEGIN
    GRANT SELECT,INSERT ON dbo.orders TO h2t_user;
    GRANT SELECT,INSERT ON dbo.shop_orders TO h2t_user;
    GRANT SELECT,INSERT ON dbo.order_items TO h2t_user;
    GRANT SELECT,INSERT ON dbo.order_status_history TO h2t_user;
    GRANT SELECT,UPDATE ON dbo.inventories TO h2t_user;
END;
COMMIT;
