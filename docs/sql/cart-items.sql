-- Run as database owner against the application's database before deploying.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF OBJECT_ID(N'dbo.cart_items', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.cart_items (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        user_id BIGINT NOT NULL,
        catalog_product_id VARCHAR(100) NOT NULL,
        size VARCHAR(5) NOT NULL,
        quantity INT NOT NULL,
        created_at DATETIME2 NOT NULL,
        updated_at DATETIME2 NOT NULL,
        CONSTRAINT uk_cart_items_user_product_size UNIQUE (user_id, catalog_product_id, size),
        CONSTRAINT ck_cart_items_quantity CHECK (quantity BETWEEN 1 AND 99),
        CONSTRAINT ck_cart_items_size CHECK (size IN ('S','M','L','XL','XXL')),
        CONSTRAINT fk_cart_items_user FOREIGN KEY (user_id) REFERENCES dbo.users(id)
    );
END;
IF DATABASE_PRINCIPAL_ID(N'h2t_user') IS NOT NULL
    GRANT SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.cart_items TO [h2t_user];
COMMIT;
