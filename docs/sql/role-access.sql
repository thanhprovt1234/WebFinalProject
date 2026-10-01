SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF OBJECT_ID('dbo.store_managers','U') IS NULL
CREATE TABLE dbo.store_managers (
 id BIGINT IDENTITY PRIMARY KEY,
 user_id BIGINT NOT NULL REFERENCES dbo.users(id),
 store_id BIGINT NOT NULL REFERENCES dbo.stores(id),
 CONSTRAINT uk_store_managers_user_store UNIQUE(user_id,store_id)
);
IF DATABASE_PRINCIPAL_ID(N'h2t_user') IS NOT NULL
BEGIN
 GRANT SELECT,INSERT,UPDATE,DELETE ON dbo.store_managers TO h2t_user;
 GRANT SELECT,INSERT,DELETE ON dbo.user_roles TO h2t_user;
 GRANT SELECT ON dbo.shipments TO h2t_user;
 GRANT SELECT ON dbo.shop_orders TO h2t_user;
END;
COMMIT;
