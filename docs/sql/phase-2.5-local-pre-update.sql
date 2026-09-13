-- Run once before starting phase 2.5 with the dev profile; stop older app instances first.
-- This migration supports the verified empty phase-2 catalog/order baseline only.
-- It deliberately refuses populated legacy data: ownership and order splitting need
-- an explicit business-approved backfill, not invented vendors or guessed statuses.
-- No tables, columns or rows are deleted. Safe to rerun after upgrading.
SET XACT_ABORT ON;
IF DB_NAME() <> N'H2T_DB'
    THROW 51000, 'Migration requires H2T_DB.', 1;

BEGIN TRY
    BEGIN TRANSACTION;

    IF COL_LENGTH('dbo.products', 'shop_id') IS NULL
       AND EXISTS (SELECT 1 FROM dbo.products WITH (TABLOCKX, HOLDLOCK))
        THROW 51001, 'Existing products need an explicit Shop ownership backfill.', 1;
    IF COL_LENGTH('dbo.stores', 'shop_id') IS NULL
       AND EXISTS (SELECT 1 FROM dbo.stores WITH (TABLOCKX, HOLDLOCK))
        THROW 51002, 'Existing stores need an explicit Shop ownership backfill.', 1;
    IF COL_LENGTH('dbo.orders', 'created_at') IS NULL
       AND EXISTS (SELECT 1 FROM dbo.orders WITH (TABLOCKX, HOLDLOCK))
        THROW 51003, 'Existing orders need ShopOrder splitting, status and audit backfill.', 1;
    IF COL_LENGTH('dbo.order_items', 'shop_order_id') IS NULL
       AND EXISTS (SELECT 1 FROM dbo.order_items WITH (TABLOCKX, HOLDLOCK))
        THROW 51004, 'Existing order items need an explicit ShopOrder backfill.', 1;

    -- Hibernate update does not retire unmapped NOT NULL columns. Keep these legacy
    -- columns and their foreign keys, but allow new JPA inserts to omit them.
    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.orders')
               AND name = 'store_id' AND is_nullable = 0)
        ALTER TABLE dbo.orders ALTER COLUMN store_id bigint NULL;
    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.orders')
               AND name = 'status' AND is_nullable = 0)
        ALTER TABLE dbo.orders ALTER COLUMN status varchar(20) NULL;
    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.order_items')
               AND name = 'order_id' AND is_nullable = 0)
        ALTER TABLE dbo.order_items ALTER COLUMN order_id bigint NULL;

    -- Hibernate update does not replace an existing named CHECK constraint.
    IF EXISTS (SELECT 1 FROM sys.check_constraints
               WHERE parent_object_id = OBJECT_ID('dbo.roles') AND name = 'ck_roles_values')
        ALTER TABLE dbo.roles DROP CONSTRAINT ck_roles_values;
    ALTER TABLE dbo.roles WITH CHECK ADD CONSTRAINT ck_roles_values
        CHECK (name IN ('ROLE_USER', 'ROLE_VENDOR', 'ROLE_MANAGER', 'ROLE_ADMIN', 'ROLE_SHIPPER'));

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
