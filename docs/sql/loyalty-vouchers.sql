-- Run as database owner before starting the updated application. Safe to rerun.
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.orders','voucher_code') IS NULL
    ALTER TABLE dbo.orders ADD voucher_code VARCHAR(40) NULL;
IF COL_LENGTH('dbo.orders','discount_amount') IS NULL
    ALTER TABLE dbo.orders ADD discount_amount DECIMAL(19,2) NOT NULL CONSTRAINT df_orders_discount DEFAULT 0;
IF OBJECT_ID('dbo.vouchers','U') IS NULL
BEGIN
    CREATE TABLE dbo.vouchers (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        code VARCHAR(40) NOT NULL CONSTRAINT uk_vouchers_code UNIQUE,
        user_id BIGINT NOT NULL REFERENCES dbo.users(id),
        source_order_id BIGINT NOT NULL CONSTRAINT uk_vouchers_source UNIQUE REFERENCES dbo.orders(id),
        used_order_id BIGINT NULL REFERENCES dbo.orders(id),
        created_at DATETIME2 NOT NULL,
        updated_at DATETIME2 NOT NULL
    );
    CREATE INDEX ix_vouchers_user ON dbo.vouchers(user_id,used_order_id);
END;
IF DATABASE_PRINCIPAL_ID(N'h2t_user') IS NOT NULL
    GRANT SELECT,INSERT,UPDATE ON dbo.vouchers TO h2t_user;
COMMIT;
