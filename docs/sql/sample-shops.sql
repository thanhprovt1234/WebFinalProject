-- Run with SQLCMD variables MOC_HASH, AN_HASH, PHO_HASH, MANAGER_HASH, SHIPPER_HASH.
-- Supply BCrypt hashes generated locally; never put plaintext passwords in this file.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
DECLARE @accounts TABLE(username NVARCHAR(100),full_name NVARCHAR(150),password VARCHAR(255),role_name VARCHAR(32));
INSERT @accounts VALUES
 ('demo_moc',N'Seller Mộc Studio','$(MOC_HASH)','ROLE_VENDOR'),
 ('demo_an_nhien',N'Seller An Nhiên Wear','$(AN_HASH)','ROLE_VENDOR'),
 ('demo_pho_denim',N'Seller Phố Denim','$(PHO_HASH)','ROLE_VENDOR'),
 ('demo_manager',N'Quản lý mẫu Mộc Studio','$(MANAGER_HASH)','ROLE_MANAGER'),
 ('demo_shipper',N'Giao hàng mẫu','$(SHIPPER_HASH)','ROLE_SHIPPER');
-- Only grant roles to accounts created by this invocation.
DECLARE @new TABLE(id BIGINT,username NVARCHAR(100));
INSERT dbo.users(username,email,password,full_name,enabled,created_at,updated_at)
 OUTPUT inserted.id,inserted.username INTO @new
 SELECT a.username,CONCAT(a.username,'@example.invalid'),a.password,a.full_name,1,SYSUTCDATETIME(),SYSUTCDATETIME()
 FROM @accounts a WHERE NOT EXISTS(SELECT 1 FROM dbo.users u WHERE u.username=a.username);
INSERT dbo.user_roles(user_id,role_id)
 SELECT n.id,r.id FROM @new n JOIN @accounts a ON a.username=n.username JOIN dbo.roles r ON r.name IN ('ROLE_USER',a.role_name);
DECLARE @samples TABLE(username NVARCHAR(100),shop_slug VARCHAR(200),shop_name NVARCHAR(150),slug VARCHAR(200),name NVARCHAR(200),category NVARCHAR(150),price DECIMAL(19,2),image VARCHAR(2048),search_name VARCHAR(200));
INSERT @samples VALUES
 ('demo_moc','moc-studio',N'Mộc Studio','moc-so-mi-oxford',N'Áo sơ mi Oxford Mộc',N'Áo sơ mi',289000,'/images/products/ao-so-mi/moc-so-mi-oxford.jpg','ao so mi oxford moc'),
 ('demo_an_nhien','an-nhien-wear',N'An Nhiên Wear','an-nhien-polo',N'Áo polo An Nhiên',N'Áo polo',249000,'/images/products/ao-polo/an-nhien-polo.jpg','ao polo an nhien'),
 ('demo_pho_denim','pho-denim',N'Phố Denim','pho-denim-quan-jeans',N'Quần jeans Phố Denim',N'Quần jeans',399000,'/images/products/quan-jeans/pho-denim-quan-jeans.jpg','quan jeans pho denim');
-- Abort on name collisions with unrelated existing accounts or shops.
IF EXISTS(SELECT 1 FROM @samples a JOIN dbo.users u ON u.username=a.username
 WHERE u.email<>CONCAT(a.username,'@example.invalid')
 OR NOT EXISTS(SELECT 1 FROM dbo.user_roles ur JOIN dbo.roles r ON r.id=ur.role_id WHERE ur.user_id=u.id AND r.name='ROLE_VENDOR'))
 THROW 50002,'Sample seller account collision. No changes were applied.',1;
IF EXISTS(SELECT 1 FROM @samples a JOIN dbo.shops s ON s.slug=a.shop_slug JOIN dbo.users u ON u.username=a.username WHERE s.owner_id<>u.id)
 THROW 50003,'Sample shop ownership collision. No changes were applied.',1;
INSERT dbo.shops(owner_id,slug,name,enabled,status,created_at,updated_at)
 SELECT u.id,a.shop_slug,a.shop_name,1,'ACTIVE',SYSUTCDATETIME(),SYSUTCDATETIME() FROM @samples a JOIN dbo.users u ON u.username=a.username
 WHERE NOT EXISTS(SELECT 1 FROM dbo.shops s WHERE s.slug=a.shop_slug);
INSERT dbo.categories(name,enabled,created_at,updated_at)
 SELECT a.category,1,SYSUTCDATETIME(),SYSUTCDATETIME() FROM @samples a WHERE NOT EXISTS(SELECT 1 FROM dbo.categories c WHERE c.name=a.category);
INSERT dbo.products(shop_id,category_id,slug,name,base_price,image_url,description,search_name,enabled,created_at,updated_at)
 SELECT s.id,(SELECT TOP 1 id FROM dbo.categories WHERE name=a.category ORDER BY id),a.slug,a.name,a.price,a.image,
 N'Sản phẩm mẫu để trải nghiệm mua sắm đa shop. Hình ảnh minh họa.',a.search_name,1,SYSUTCDATETIME(),SYSUTCDATETIME()
 FROM @samples a JOIN dbo.shops s ON s.slug=a.shop_slug WHERE NOT EXISTS(SELECT 1 FROM dbo.products p WHERE p.slug=a.slug);
INSERT dbo.stores(shop_id,code,name,address,enabled,created_at,updated_at)
 SELECT s.id,CONCAT('DEMO-',a.shop_slug),CONCAT(a.shop_name,N' — Chi nhánh mẫu'),N'Chi nhánh mô phỏng dùng cho dữ liệu mẫu',1,SYSUTCDATETIME(),SYSUTCDATETIME()
 FROM @samples a JOIN dbo.shops s ON s.slug=a.shop_slug WHERE NOT EXISTS(SELECT 1 FROM dbo.stores st WHERE st.code=CONCAT('DEMO-',a.shop_slug));
INSERT dbo.product_variants(product_id,sku,size,color,enabled)
 SELECT p.id,CONCAT('DEMO-',a.slug,'-',sz.size),sz.size,N'Mặc định',1
 FROM @samples a JOIN dbo.products p ON p.slug=a.slug JOIN dbo.shops s ON p.shop_id=s.id AND s.slug=a.shop_slug CROSS JOIN(VALUES('M'),('L'),('XL'))sz(size)
 WHERE NOT EXISTS(SELECT 1 FROM dbo.product_variants v WHERE v.sku=CONCAT('DEMO-',a.slug,'-',sz.size));
INSERT dbo.inventories(store_id,product_variant_id,quantity,updated_at)
 SELECT st.id,v.id,10,SYSUTCDATETIME() FROM @samples a JOIN dbo.products p ON p.slug=a.slug JOIN dbo.shops s ON s.id=p.shop_id AND s.slug=a.shop_slug
 JOIN dbo.stores st ON st.shop_id=s.id AND st.code=CONCAT('DEMO-',a.shop_slug) JOIN dbo.product_variants v ON v.product_id=p.id
 WHERE v.sku LIKE 'DEMO-%' AND NOT EXISTS(SELECT 1 FROM dbo.inventories i WHERE i.store_id=st.id AND i.product_variant_id=v.id);
INSERT dbo.store_managers(user_id,store_id)
 SELECT n.id,s.id FROM @new n CROSS JOIN dbo.stores s WHERE n.username='demo_manager' AND s.code='DEMO-moc-studio';
COMMIT;
