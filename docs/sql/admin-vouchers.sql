-- Extend existing loyalty-vouchers.sql. Run as database owner; safe to rerun.
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF OBJECT_ID('dbo.voucher_campaigns','U') IS NULL
BEGIN
    CREATE TABLE dbo.voucher_campaigns (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        name NVARCHAR(150) NOT NULL,
        discount_type VARCHAR(10) NOT NULL,
        discount_value DECIMAL(19,2) NOT NULL,
        minimum_total DECIMAL(19,2) NOT NULL,
        maximum_discount DECIMAL(19,2) NULL,
        expires_at DATETIMEOFFSET(6) NOT NULL,
        created_by_id BIGINT NOT NULL REFERENCES dbo.users(id),
        created_at DATETIME2 NOT NULL,
        updated_at DATETIME2 NOT NULL,
        CONSTRAINT ck_voucher_campaign_conditions CHECK (
            discount_type IN ('PERCENT','FIXED') AND discount_value > 0 AND minimum_total >= 0
            AND (maximum_discount IS NULL OR maximum_discount > 0)
            AND (discount_type <> 'PERCENT' OR discount_value <= 100))
    );
END;
IF COL_LENGTH('dbo.vouchers','campaign_id') IS NULL
    ALTER TABLE dbo.vouchers ADD campaign_id BIGINT NULL REFERENCES dbo.voucher_campaigns(id);
IF COL_LENGTH('dbo.vouchers','revoked_at') IS NULL
    ALTER TABLE dbo.vouchers ADD revoked_at DATETIMEOFFSET(6) NULL;
IF COL_LENGTH('dbo.vouchers','issued_by_id') IS NULL
    ALTER TABLE dbo.vouchers ADD issued_by_id BIGINT NULL REFERENCES dbo.users(id);
IF COL_LENGTH('dbo.vouchers','revoked_by_id') IS NULL
    ALTER TABLE dbo.vouchers ADD revoked_by_id BIGINT NULL REFERENCES dbo.users(id);
-- SQL Server's unfiltered unique constraint permits only one NULL source order.
-- Replace single-column source uniqueness with a filtered index for gift vouchers.
DECLARE @dropSql NVARCHAR(MAX)=N'';
SELECT @dropSql=@dropSql+CASE WHEN i.is_unique_constraint=1
    THEN N'ALTER TABLE dbo.vouchers DROP CONSTRAINT '+QUOTENAME(i.name)+N';'
    ELSE N'DROP INDEX '+QUOTENAME(i.name)+N' ON dbo.vouchers;' END
FROM sys.indexes i
WHERE i.object_id=OBJECT_ID('dbo.vouchers') AND i.is_unique=1 AND i.has_filter=0
    AND (SELECT COUNT(*) FROM sys.index_columns ic WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0)=1
    AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id
                WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND c.name='source_order_id');
EXEC sp_executesql @dropSql;
IF EXISTS(SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID('dbo.vouchers') AND name='source_order_id' AND is_nullable=0)
    ALTER TABLE dbo.vouchers ALTER COLUMN source_order_id BIGINT NULL;
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.vouchers') AND name='uk_vouchers_source_not_null')
    EXEC(N'CREATE UNIQUE INDEX uk_vouchers_source_not_null ON dbo.vouchers(source_order_id) WHERE source_order_id IS NOT NULL');
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.vouchers') AND name='uk_voucher_campaign_user')
    EXEC(N'CREATE UNIQUE INDEX uk_voucher_campaign_user ON dbo.vouchers(campaign_id,user_id) WHERE campaign_id IS NOT NULL');
IF DATABASE_PRINCIPAL_ID(N'h2t_user') IS NOT NULL
BEGIN
    GRANT SELECT,INSERT,UPDATE ON dbo.voucher_campaigns TO h2t_user;
    GRANT SELECT,INSERT,UPDATE ON dbo.vouchers TO h2t_user;
END;
COMMIT;
