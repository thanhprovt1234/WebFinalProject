-- Local demo inventory for the ten crew-neck products, not a real stock receipt.
-- Only insert missing rows; never reset quantities already changed by orders.
SET NOCOUNT ON;
SET XACT_ABORT ON;
BEGIN TRANSACTION;
DECLARE @shop BIGINT=(SELECT id FROM dbo.shops WHERE slug='h2t' AND enabled=1 AND status='ACTIVE');
IF @shop IS NULL THROW 50001,'An active H2T shop is required.',1;
IF EXISTS(SELECT 1 FROM dbo.stores WHERE code='DEMO-h2t' AND shop_id<>@shop)
    THROW 50002,'The sample branch code belongs to another shop.',1;
IF NOT EXISTS(SELECT 1 FROM dbo.stores WITH (UPDLOCK,HOLDLOCK) WHERE code='DEMO-h2t')
    INSERT dbo.stores(shop_id,code,name,address,enabled,created_at,updated_at)
    VALUES(@shop,'DEMO-h2t',N'H2T — Chi nhánh mẫu',N'Chi nhánh mô phỏng dùng cho dữ liệu mẫu',1,SYSUTCDATETIME(),SYSUTCDATETIME());
DECLARE @store BIGINT=(SELECT id FROM dbo.stores WHERE code='DEMO-h2t' AND shop_id=@shop AND enabled=1);
IF @store IS NULL THROW 50003,'The sample branch is disabled.',1;
INSERT dbo.inventories(store_id,product_variant_id,quantity,updated_at)
SELECT @store,v.id,10,SYSUTCDATETIME()
FROM dbo.products p JOIN dbo.product_variants v ON v.product_id=p.id AND v.enabled=1
WHERE p.shop_id=@shop AND p.enabled=1 AND p.slug IN
    ('ao-thun-01','ao-thun-02','ao-thun-03','ao-thun-04','ao-thun-05','ao-thun-06','ao-thun-07','ao-thun-08','ao-thun-09','ao-thun-10')
AND NOT EXISTS(SELECT 1 FROM dbo.inventories i WITH (UPDLOCK,HOLDLOCK) WHERE i.store_id=@store AND i.product_variant_id=v.id);
COMMIT;
SELECT p.slug,v.size,i.quantity FROM dbo.products p JOIN dbo.product_variants v ON v.product_id=p.id
JOIN dbo.inventories i ON i.product_variant_id=v.id AND i.store_id=@store ORDER BY p.slug,v.size;
