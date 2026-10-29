-- Apply after product-reviews.sql and database-catalog.sql, before starting the app.
-- Historical reviews are assigned to the latest qualifying order where possible.
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.product_reviews', 'order_id') IS NULL
    ALTER TABLE dbo.product_reviews ADD order_id BIGINT NULL;
EXEC(N'
UPDATE r SET order_id = (
    SELECT MAX(o.id) FROM dbo.orders o
    JOIN dbo.shop_orders s ON s.order_id = o.id
    JOIN dbo.order_items i ON i.shop_order_id = s.id
    JOIN dbo.product_variants v ON v.id = i.product_variant_id
    JOIN dbo.products p ON p.id = v.product_id
    WHERE o.user_id = r.user_id AND p.slug = r.catalog_product_id
      AND s.status = ''DELIVERED''
) FROM dbo.product_reviews r WHERE r.order_id IS NULL;
');
IF EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = 'uk_product_reviews_catalog_user'
           AND parent_object_id = OBJECT_ID('dbo.product_reviews'))
    ALTER TABLE dbo.product_reviews DROP CONSTRAINT uk_product_reviews_catalog_user;
-- Keep unmatched legacy reviews visible; NULL order IDs are not new review slots.
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uk_product_reviews_catalog_order'
               AND object_id = OBJECT_ID('dbo.product_reviews'))
    EXEC(N'CREATE UNIQUE INDEX uk_product_reviews_catalog_order ON dbo.product_reviews (catalog_product_id, order_id) WHERE order_id IS NOT NULL');
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_product_reviews_order'
               AND parent_object_id = OBJECT_ID('dbo.product_reviews'))
    EXEC(N'ALTER TABLE dbo.product_reviews ADD CONSTRAINT fk_product_reviews_order FOREIGN KEY (order_id) REFERENCES dbo.orders(id)');
COMMIT;
