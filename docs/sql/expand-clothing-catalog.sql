-- Run as database owner after sample-shops.sql. Adds nine items per category.
-- Existing products, prices and inventory are retained on repeated runs.
SET NOCOUNT ON;
SET XACT_ABORT ON;
BEGIN TRANSACTION;
DECLARE @source TABLE(shop_slug VARCHAR(100),category NVARCHAR(100),slug VARCHAR(200),name NVARCHAR(200),price DECIMAL(19,2),image VARCHAR(2048),search_name VARCHAR(200));
INSERT @source VALUES
 ('an-nhien-wear',N'Áo polo','an-nhien-polo-02',N'Áo polo Cotton Basic',219000,'/images/products/ao-polo/an-nhien-polo-02.jpg','ao polo cotton basic'),
 ('an-nhien-wear',N'Áo polo','an-nhien-polo-03',N'Áo polo Pique Classic',259000,'/images/products/ao-polo/an-nhien-polo-03.jpg','ao polo pique classic'),
 ('an-nhien-wear',N'Áo polo','an-nhien-polo-04',N'Áo polo Slim Fit',279000,'/images/products/ao-polo/an-nhien-polo-04.jpg','ao polo slim fit'),
 ('an-nhien-wear',N'Áo polo','an-nhien-polo-05',N'Áo polo Regular Fit',269000,'/images/products/ao-polo/an-nhien-polo-05.jpg','ao polo regular fit'),
 ('an-nhien-wear',N'Áo polo','an-nhien-polo-06',N'Áo polo Cổ phối',289000,'/images/products/ao-polo/an-nhien-polo-06.jpg','ao polo co phoi'),
 ('an-nhien-wear',N'Áo polo','an-nhien-polo-07',N'Áo polo Dệt kim',349000,'/images/products/ao-polo/an-nhien-polo-07.jpg','ao polo det kim'),
 ('an-nhien-wear',N'Áo polo','an-nhien-polo-08',N'Áo polo Thể thao',239000,'/images/products/ao-polo/an-nhien-polo-08.jpg','ao polo the thao'),
 ('an-nhien-wear',N'Áo polo','an-nhien-polo-09',N'Áo polo Kẻ sọc',299000,'/images/products/ao-polo/an-nhien-polo-09.jpg','ao polo ke soc'),
 ('an-nhien-wear',N'Áo polo','an-nhien-polo-10',N'Áo polo Premium',359000,'/images/products/ao-polo/an-nhien-polo-10.jpg','ao polo premium'),
 ('moc-studio',N'Áo sơ mi','moc-so-mi-02',N'Áo sơ mi Cotton Basic',259000,'/images/products/ao-so-mi/moc-so-mi-02.jpg','ao so mi cotton basic'),
 ('moc-studio',N'Áo sơ mi','moc-so-mi-03',N'Áo sơ mi Linen',329000,'/images/products/ao-so-mi/moc-so-mi-03.jpg','ao so mi linen'),
 ('moc-studio',N'Áo sơ mi','moc-so-mi-04',N'Áo sơ mi Oxford Slim Fit',319000,'/images/products/ao-so-mi/moc-so-mi-04.jpg','ao so mi oxford slim fit'),
 ('moc-studio',N'Áo sơ mi','moc-so-mi-05',N'Áo sơ mi Poplin',299000,'/images/products/ao-so-mi/moc-so-mi-05.jpg','ao so mi poplin'),
 ('moc-studio',N'Áo sơ mi','moc-so-mi-06',N'Áo sơ mi Cổ trụ',279000,'/images/products/ao-so-mi/moc-so-mi-06.jpg','ao so mi co tru'),
 ('moc-studio',N'Áo sơ mi','moc-so-mi-07',N'Áo sơ mi Ngắn tay',269000,'/images/products/ao-so-mi/moc-so-mi-07.jpg','ao so mi ngan tay'),
 ('moc-studio',N'Áo sơ mi','moc-so-mi-08',N'Áo sơ mi Kẻ sọc',309000,'/images/products/ao-so-mi/moc-so-mi-08.jpg','ao so mi ke soc'),
 ('moc-studio',N'Áo sơ mi','moc-so-mi-09',N'Áo sơ mi Caro',289000,'/images/products/ao-so-mi/moc-so-mi-09.jpg','ao so mi caro'),
 ('moc-studio',N'Áo sơ mi','moc-so-mi-10',N'Áo sơ mi Premium',379000,'/images/products/ao-so-mi/moc-so-mi-10.jpg','ao so mi premium'),
 ('pho-denim',N'Quần jeans','pho-denim-jeans-02',N'Quần jeans Slim Fit',379000,'/images/products/quan-jeans/pho-denim-jeans-02.jpg','quan jeans slim fit'),
 ('pho-denim',N'Quần jeans','pho-denim-jeans-03',N'Quần jeans Straight Fit',399000,'/images/products/quan-jeans/pho-denim-jeans-03.jpg','quan jeans straight fit'),
 ('pho-denim',N'Quần jeans','pho-denim-jeans-04',N'Quần jeans Regular Fit',389000,'/images/products/quan-jeans/pho-denim-jeans-04.jpg','quan jeans regular fit'),
 ('pho-denim',N'Quần jeans','pho-denim-jeans-05',N'Quần jeans Relaxed Fit',419000,'/images/products/quan-jeans/pho-denim-jeans-05.jpg','quan jeans relaxed fit'),
 ('pho-denim',N'Quần jeans','pho-denim-jeans-06',N'Quần jeans Skinny Fit',369000,'/images/products/quan-jeans/pho-denim-jeans-06.jpg','quan jeans skinny fit'),
 ('pho-denim',N'Quần jeans','pho-denim-jeans-07',N'Quần jeans Ống suông',429000,'/images/products/quan-jeans/pho-denim-jeans-07.jpg','quan jeans ong suong'),
 ('pho-denim',N'Quần jeans','pho-denim-jeans-08',N'Quần jeans Co giãn',409000,'/images/products/quan-jeans/pho-denim-jeans-08.jpg','quan jeans co gian'),
 ('pho-denim',N'Quần jeans','pho-denim-jeans-09',N'Quần jeans Washed',439000,'/images/products/quan-jeans/pho-denim-jeans-09.jpg','quan jeans washed'),
 ('pho-denim',N'Quần jeans','pho-denim-jeans-10',N'Quần jeans Premium',459000,'/images/products/quan-jeans/pho-denim-jeans-10.jpg','quan jeans premium');

IF EXISTS (SELECT 1 FROM @source a WHERE NOT EXISTS (
    SELECT 1 FROM dbo.shops s WHERE s.slug=a.shop_slug AND s.enabled=1 AND s.status='ACTIVE'))
    THROW 50001,'Required active sample shops are missing.',1;
IF EXISTS (SELECT 1 FROM @source a WHERE NOT EXISTS (
    SELECT 1 FROM dbo.categories c WHERE c.name=a.category AND c.enabled=1))
    THROW 50002,'Required enabled categories are missing.',1;
IF EXISTS (SELECT 1 FROM @source a JOIN dbo.products p ON p.slug=a.slug
    JOIN dbo.shops s ON s.id=p.shop_id JOIN dbo.categories c ON c.id=p.category_id
    WHERE s.slug<>a.shop_slug OR c.name<>a.category)
    THROW 50003,'Product slug belongs to another shop or category.',1;
IF EXISTS (SELECT 1 FROM @source a WHERE NOT EXISTS (
    SELECT 1 FROM dbo.stores st JOIN dbo.shops s ON s.id=st.shop_id
    WHERE s.slug=a.shop_slug AND st.code=CONCAT('DEMO-',a.shop_slug) AND st.enabled=1))
    THROW 50004,'Required sample branches are missing.',1;

DECLARE @added TABLE(id BIGINT);
INSERT dbo.products(shop_id,category_id,slug,name,base_price,image_url,description,search_name,enabled,created_at,updated_at)
OUTPUT inserted.id INTO @added
SELECT s.id,(SELECT TOP 1 id FROM dbo.categories WHERE name=a.category AND enabled=1 ORDER BY id),
    a.slug,a.name,a.price,a.image,N'Sản phẩm mẫu để trải nghiệm mua sắm đa shop. Hình ảnh minh họa.',a.search_name,1,SYSUTCDATETIME(),SYSUTCDATETIME()
FROM @source a JOIN dbo.shops s ON s.slug=a.shop_slug
WHERE NOT EXISTS (SELECT 1 FROM dbo.products p WITH (UPDLOCK,HOLDLOCK) WHERE p.slug=a.slug);

INSERT dbo.product_variants(product_id,sku,size,color,enabled)
SELECT p.id,CONCAT('DEMO-',p.slug,'-',sz.size),sz.size,N'Mặc định',1
FROM @added n JOIN dbo.products p ON p.id=n.id CROSS JOIN (VALUES('M'),('L'),('XL')) sz(size);

INSERT dbo.inventories(store_id,product_variant_id,quantity,updated_at)
SELECT st.id,v.id,10,SYSUTCDATETIME()
FROM @added n JOIN dbo.products p ON p.id=n.id JOIN dbo.shops s ON s.id=p.shop_id
JOIN dbo.stores st ON st.shop_id=s.id AND st.code=CONCAT('DEMO-',s.slug)
JOIN dbo.product_variants v ON v.product_id=p.id;

-- Reject unexpected catalog state instead of committing a partial expansion.
IF EXISTS (SELECT a.category FROM (SELECT DISTINCT category FROM @source) a
    LEFT JOIN dbo.categories c ON c.name=a.category AND c.enabled=1
    LEFT JOIN dbo.products p ON p.category_id=c.id AND p.enabled=1
    LEFT JOIN dbo.shops s ON s.id=p.shop_id AND s.enabled=1 AND s.status='ACTIVE'
    GROUP BY a.category HAVING COUNT(s.id)<>10)
    THROW 50005,'Expected exactly ten visible products per requested category.',1;
COMMIT;
SELECT c.name,COUNT(p.id) AS product_count,MIN(p.base_price) AS min_price,MAX(p.base_price) AS max_price
FROM dbo.categories c JOIN dbo.products p ON p.category_id=c.id AND p.enabled=1
JOIN dbo.shops s ON s.id=p.shop_id AND s.enabled=1 AND s.status='ACTIVE'
WHERE c.name IN (N'Áo polo',N'Áo sơ mi',N'Quần jeans') GROUP BY c.name;
