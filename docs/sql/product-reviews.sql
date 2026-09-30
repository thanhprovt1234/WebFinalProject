-- Run against the application's SQL Server database when ddl-auto=none.
-- Development profile creates this table through Hibernate automatically.
IF OBJECT_ID(N'dbo.product_reviews', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.product_reviews (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        catalog_product_id VARCHAR(100) NOT NULL,
        user_id BIGINT NOT NULL,
        rating INT NOT NULL,
        comment NVARCHAR(2000) NOT NULL,
        created_at DATETIME2 NOT NULL,
        updated_at DATETIME2 NOT NULL,
        CONSTRAINT uk_product_reviews_catalog_user UNIQUE (catalog_product_id, user_id),
        CONSTRAINT ck_product_reviews_rating CHECK (rating BETWEEN 1 AND 5),
        CONSTRAINT fk_product_reviews_user FOREIGN KEY (user_id) REFERENCES dbo.users(id)
    );
END;
