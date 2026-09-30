-- Run as the database owner after product-reviews.sql.
-- h2t_user is the local application's database user.
IF DATABASE_PRINCIPAL_ID(N'h2t_user') IS NULL
    THROW 50001, 'Application database user h2t_user does not exist.', 1;

GRANT SELECT, INSERT, UPDATE ON OBJECT::dbo.product_reviews TO [h2t_user];
