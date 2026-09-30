-- Apply as database owner before starting the updated application.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF OBJECT_ID(N'dbo.product_activities', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.product_activities (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        user_id BIGINT NOT NULL,
        catalog_product_id VARCHAR(100) NOT NULL,
        favorite BIT NOT NULL,
        favorited_at DATETIME2 NULL,
        viewed_at DATETIME2 NULL,
        created_at DATETIME2 NOT NULL,
        updated_at DATETIME2 NOT NULL,
        CONSTRAINT uk_product_activities_user_product UNIQUE (user_id, catalog_product_id),
        CONSTRAINT fk_product_activities_user FOREIGN KEY (user_id) REFERENCES dbo.users(id)
    );
    CREATE INDEX ix_product_activities_favorites ON dbo.product_activities(user_id, favorite, favorited_at DESC, id DESC);
    CREATE INDEX ix_product_activities_views ON dbo.product_activities(user_id, viewed_at DESC, id DESC);
END;
IF DATABASE_PRINCIPAL_ID(N'h2t_user') IS NOT NULL
    GRANT SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.product_activities TO [h2t_user];
COMMIT;
