-- Additive migration, preserves saved user data. Run as database owner.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.products','search_name') IS NULL
    ALTER TABLE dbo.products ADD search_name VARCHAR(200) NOT NULL CONSTRAINT df_products_search_name DEFAULT '';
IF COL_LENGTH('dbo.cart_items','color') IS NULL
    ALTER TABLE dbo.cart_items ADD color NVARCHAR(50) NOT NULL CONSTRAINT df_cart_items_color DEFAULT N'';
IF EXISTS (SELECT 1 FROM sys.key_constraints WHERE name='uk_cart_items_user_product_size')
    ALTER TABLE dbo.cart_items DROP CONSTRAINT uk_cart_items_user_product_size;
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name='ck_cart_items_size')
    ALTER TABLE dbo.cart_items DROP CONSTRAINT ck_cart_items_size;
IF EXISTS (SELECT 1 FROM sys.key_constraints WHERE name='uk_cart_items_user_product_option')
    ALTER TABLE dbo.cart_items DROP CONSTRAINT uk_cart_items_user_product_option;
ALTER TABLE dbo.cart_items ALTER COLUMN catalog_product_id VARCHAR(200) NOT NULL;
ALTER TABLE dbo.cart_items ALTER COLUMN size VARCHAR(10) NOT NULL;
ALTER TABLE dbo.product_reviews DROP CONSTRAINT uk_product_reviews_catalog_user;
ALTER TABLE dbo.product_reviews ALTER COLUMN catalog_product_id VARCHAR(200) NOT NULL;
ALTER TABLE dbo.product_reviews ADD CONSTRAINT uk_product_reviews_catalog_user UNIQUE(catalog_product_id,user_id);
ALTER TABLE dbo.product_activities DROP CONSTRAINT uk_product_activities_user_product;
ALTER TABLE dbo.product_activities ALTER COLUMN catalog_product_id VARCHAR(200) NOT NULL;
ALTER TABLE dbo.product_activities ADD CONSTRAINT uk_product_activities_user_product UNIQUE(user_id,catalog_product_id);
DECLARE @owner BIGINT=(SELECT TOP 1 u.id FROM dbo.users u JOIN dbo.user_roles ur ON ur.user_id=u.id JOIN dbo.roles r ON r.id=ur.role_id WHERE r.name='ROLE_ADMIN' AND u.enabled=1 ORDER BY u.id);
IF @owner IS NULL THROW 50001, 'An enabled administrator is required to own the imported H2T shop.',1;
IF NOT EXISTS(SELECT 1 FROM dbo.shops WHERE slug='h2t')
    INSERT dbo.shops(owner_id,slug,name,enabled,status,created_at,updated_at) VALUES(@owner,'h2t',N'H2T',1,'ACTIVE',SYSUTCDATETIME(),SYSUTCDATETIME());
DECLARE @shop BIGINT=(SELECT id FROM dbo.shops WHERE slug='h2t');
IF NOT EXISTS(SELECT 1 FROM dbo.categories WHERE name=N'Áo Thun Cổ Tròn')
    INSERT dbo.categories(name,enabled,created_at,updated_at) VALUES(N'Áo Thun Cổ Tròn',1,SYSUTCDATETIME(),SYSUTCDATETIME());
DECLARE @category BIGINT=(SELECT TOP 1 id FROM dbo.categories WHERE name=N'Áo Thun Cổ Tròn' ORDER BY id);
DECLARE @source TABLE(slug VARCHAR(200),name NVARCHAR(200),price DECIMAL(19,2),image_url VARCHAR(2048),color NVARCHAR(50));
INSERT @source VALUES
(N'ao-thun-01',N'Áo Thun Basic UltraComfort Simwood Denim 1724 - VÀNG',99000,N'/images/products/ao-thun-co-tron/ao-thun-01.jpg',N'VÀNG'),
(N'ao-thun-02',N'Áo Thun Basic UltraComfort Simwood Denim 1724 - XANH LÁ',99000,N'/images/products/ao-thun-co-tron/ao-thun-02.jpg',N'XANH LÁ'),
(N'ao-thun-03',N'Áo Thun Basic UltraComfort Simwood Denim 1724 - GHI',99000,N'/images/products/ao-thun-co-tron/ao-thun-03.jpg',N'GHI'),
(N'ao-thun-04',N'Áo Thun Basic UltraComfort Simwood Denim 1724 - GHI ĐẬM',99000,N'/images/products/ao-thun-co-tron/ao-thun-04.jpg',N'GHI ĐẬM'),
(N'ao-thun-05',N'Áo Thun Basic UltraComfort Simwood Denim 1724 - NÂU',99000,N'/images/products/ao-thun-co-tron/ao-thun-05.jpg',N'NÂU'),
(N'ao-thun-06',N'Áo Thun Basic UltraComfort Simwood Denim 1724 - XANH NGỌC',99000,N'/images/products/ao-thun-co-tron/ao-thun-06.jpg',N'XANH NGỌC'),
(N'ao-thun-07',N'Áo Thun Cotton Defoxx 2001 - TRẮNG',199000,N'/images/products/ao-thun-co-tron/ao-thun-07.jpg',N'TRẮNG'),
(N'ao-thun-08',N'Áo Thun Cotton Defoxx 2001 - ĐEN',199000,N'/images/products/ao-thun-co-tron/ao-thun-08.jpg',N'ĐEN')
;
INSERT dbo.products(shop_id,category_id,slug,name,base_price,image_url,enabled,created_at,updated_at)
    SELECT @shop,@category,s.slug,s.name,s.price,s.image_url,1,SYSUTCDATETIME(),SYSUTCDATETIME() FROM @source s WHERE NOT EXISTS(SELECT 1 FROM dbo.products p WHERE p.slug=s.slug);
INSERT dbo.product_variants(product_id,sku,size,color,price,enabled)
    SELECT p.id,CONCAT('H2T-',s.slug,'-',sz.size),sz.size,s.color,NULL,1
    FROM @source s JOIN dbo.products p ON p.slug=s.slug CROSS JOIN (VALUES('S'),('M'),('L'),('XL'),('XXL')) sz(size)
    WHERE NOT EXISTS(SELECT 1 FROM dbo.product_variants v WHERE v.product_id=p.id AND v.size=sz.size AND v.color=s.color);
EXEC(N'UPDATE c SET color=v.color FROM dbo.cart_items c JOIN dbo.products p ON p.slug=c.catalog_product_id JOIN (SELECT product_id,MIN(color) AS color FROM dbo.product_variants GROUP BY product_id HAVING COUNT(DISTINCT color)=1) v ON v.product_id=p.id WHERE c.color=N'''';');
EXEC(N'ALTER TABLE dbo.cart_items ADD CONSTRAINT uk_cart_items_user_product_option UNIQUE(user_id,catalog_product_id,size,color);');
EXEC(N'UPDATE dbo.products SET search_name=LOWER(TRANSLATE(LOWER(name),N''àáâãäåçèéêëìíîïñòóôõöùúûüýÿāăąćĉċčďđēĕėęěĝğġģĥĩīĭįĵķĺļľńņňōŏőŕŗřśŝşšţťũūŭůűųŵŷźżžạảấầẩẫậắằẳẵặẹẻẽếềểễệỉịọỏốồổỗộớờởỡợụủứừửữựỳỵỷỹ'',N''aaaaaaceeeeiiiinooooouuuuyyaaaccccddeeeeegggghiiiijklllnnnooorrrssssttuuuuuuwyzzzaaaaaaaaaaaaeeeeeeeeiioooooooooooouuuuuuuyyyy''));');
-- Do not invent inventory. Existing stock stays unchanged; missing stock means zero.
IF DATABASE_PRINCIPAL_ID(N'h2t_user') IS NOT NULL
BEGIN
    GRANT SELECT ON dbo.products TO h2t_user;
    GRANT SELECT ON dbo.categories TO h2t_user;
    GRANT SELECT ON dbo.shops TO h2t_user;
    GRANT SELECT ON dbo.product_variants TO h2t_user;
    GRANT SELECT ON dbo.inventories TO h2t_user;
    GRANT SELECT ON dbo.stores TO h2t_user;
END;
COMMIT;
