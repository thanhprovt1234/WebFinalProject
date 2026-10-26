-- Run as database owner before deploying the shipper workflow.
-- Existing assignments and orders are preserved. Safe to run again.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF OBJECT_ID(N'dbo.shipments', N'U') IS NULL
    THROW 50001, 'Create the base shipments table before running shipper-workflow.sql.', 1;

IF COL_LENGTH(N'dbo.shipments', N'failure_reason') IS NULL
    ALTER TABLE dbo.shipments ADD failure_reason NVARCHAR(1000) NULL;
IF COL_LENGTH(N'dbo.shipments', N'cod_collected_amount') IS NULL
    ALTER TABLE dbo.shipments ADD cod_collected_amount DECIMAL(19,2) NULL;
IF COL_LENGTH(N'dbo.shipments', N'cod_remitted_at') IS NULL
    ALTER TABLE dbo.shipments ADD cod_remitted_at DATETIME2(6) NULL;
IF COL_LENGTH(N'dbo.shipments', N'cod_confirmed_at') IS NULL
    ALTER TABLE dbo.shipments ADD cod_confirmed_at DATETIME2(6) NULL;
IF COL_LENGTH(N'dbo.shipments', N'cod_remittance_note') IS NULL
    ALTER TABLE dbo.shipments ADD cod_remittance_note NVARCHAR(500) NULL;
IF COL_LENGTH(N'dbo.shipments', N'cod_confirmation_note') IS NULL
    ALTER TABLE dbo.shipments ADD cod_confirmation_note NVARCHAR(500) NULL;
IF COL_LENGTH(N'dbo.shipments', N'cod_confirmed_by') IS NULL
    ALTER TABLE dbo.shipments ADD cod_confirmed_by NVARCHAR(100) NULL;

-- Use dynamic SQL so new columns are resolved after ALTER TABLE in this batch.
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.shipments') AND name=N'ck_shipments_cod_confirmation')
    EXEC(N'ALTER TABLE dbo.shipments WITH CHECK ADD CONSTRAINT ck_shipments_cod_confirmation CHECK(cod_confirmed_at IS NULL OR cod_remitted_at IS NOT NULL)');

-- Replace status checks only; FK, unique and timestamp constraints are retained.
DECLARE @drop NVARCHAR(MAX) = N'';
SELECT @drop = @drop + N'ALTER TABLE dbo.shipments DROP CONSTRAINT ' + QUOTENAME(name) + N';'
FROM sys.check_constraints
WHERE parent_object_id = OBJECT_ID(N'dbo.shipments') AND definition LIKE N'%status%';
IF @drop <> N'' EXEC sys.sp_executesql @drop;
ALTER TABLE dbo.shipments WITH CHECK ADD CONSTRAINT ck_shipments_status
    CHECK (status IN ('ASSIGNED','PICKED_UP','SHIPPING','FAILED','DELIVERED','CANCELLED'));

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.shipments') AND name=N'ix_shipments_shipper_assigned')
    CREATE INDEX ix_shipments_shipper_assigned ON dbo.shipments(shipper_id,assigned_at);

IF DATABASE_PRINCIPAL_ID(N'h2t_user') IS NOT NULL
BEGIN
    GRANT SELECT, INSERT, UPDATE ON dbo.shipments TO h2t_user;
    GRANT SELECT, INSERT ON dbo.order_status_history TO h2t_user;
END;
COMMIT;
