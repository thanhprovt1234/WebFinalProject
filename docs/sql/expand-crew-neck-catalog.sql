-- Run as database owner after database-catalog.sql.
-- Add two crew-neck products; preserve existing prices, variants and inventory.
SET NOCOUNT ON;
SET XACT_ABORT ON;
BEGIN TRANSACTION;
DECLARE @shop BIGINT=(SELECT id FROM dbo.shops WHERE slug='h2t' AND enabled=1 AND status='ACTIVE');
DECLARE @category BIGINT=(SELECT TOP 1 id FROM dbo.categories WHERE name=N'Áo Thun Cổ Tròn' AND enabled=1 ORDER BY id);
IF @shop IS NULL OR @category IS NULL
    THROW 50001,'An active H2T shop and crew-neck category are required.',1;
DECLARE @source TABLE(slug VARCHAR(200),name NVARCHAR(200),price DECIMAL(19,2),color NVARCHAR(50),search_name VARCHAR(200));
INSERT @source VALUES
 ('ao-thun-09',N'Áo Thun Textured Piping 1996 - TRẮNG',229000,N'TRẮNG','ao thun textured piping 1996 - trang'),
 ('ao-thun-10',N'Áo Thun Loang Trừu Tượng 1991 - TRẮNG',249000,N'TRẮNG','ao thun loang truu tuong 1991 - trang');
IF EXISTS (SELECT 1 FROM @source a JOIN dbo.products p ON p.slug=a.slug
    WHERE p.shop_id<>@shop OR p.category_id<>@category)
    THROW 50002,'A product slug belongs to another shop or category.',1;
DECLARE @added TABLE(id BIGINT);
INSERT dbo.products(shop_id,category_id,slug,name,base_price,image_url,description,search_name,enabled,created_at,updated_at)
OUTPUT inserted.id INTO @added
SELECT @shop,@category,a.slug,a.name,a.price,CONCAT('/images/products/ao-thun-co-tron/',a.slug,'.jpg'),
    N'Sản phẩm mẫu áo thun cổ tròn. Hình ảnh minh họa từ H2T Store.',a.search_name,1,SYSUTCDATETIME(),SYSUTCDATETIME()
FROM @source a WHERE NOT EXISTS (SELECT 1 FROM dbo.products p WITH (UPDLOCK,HOLDLOCK) WHERE p.slug=a.slug);
INSERT dbo.product_variants(product_id,sku,size,color,price,enabled)
SELECT p.id,CONCAT('H2T-',p.slug,'-',sz.size),sz.size,a.color,NULL,1
FROM @added n JOIN dbo.products p ON p.id=n.id JOIN @source a ON a.slug=p.slug
CROSS JOIN (VALUES('S'),('M'),('L'),('XL'),('XXL')) sz(size);
-- H2T has no branch inventory configured; retain the existing catalog convention.
IF (SELECT COUNT(*) FROM dbo.products p JOIN dbo.shops s ON s.id=p.shop_id
    WHERE p.category_id=@category AND p.enabled=1 AND s.enabled=1 AND s.status='ACTIVE')<>10
    THROW 50003,'Expected exactly ten visible crew-neck products.',1;
COMMIT;
SELECT COUNT(*) AS product_count,COUNT(DISTINCT image_url) AS distinct_images
FROM dbo.products p JOIN dbo.shops s ON s.id=p.shop_id
WHERE p.category_id=@category AND p.enabled=1 AND s.enabled=1 AND s.status='ACTIVE';
SELECT p.slug,p.base_price,COUNT(v.id) AS sizes
FROM dbo.products p JOIN dbo.product_variants v ON v.product_id=p.id
WHERE p.slug IN ('ao-thun-09','ao-thun-10') GROUP BY p.slug,p.base_price;
