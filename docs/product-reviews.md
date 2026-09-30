# Product reviews

The product detail page supports public reviews and a signed-in form with a required
1–5 star rating and a nonblank comment (maximum 2,000 characters). Submitting again
updates the current account's review. The author comes from the authenticated account;
the form cannot select another author. CSRF protection remains enabled and JSP escapes
comments and names. Dates are displayed in Vietnam time.

Reviews persist in `product_reviews`, with a unique `(catalog_product_id, user_id)`
constraint and a database check for the rating range. The current display catalog
uses TSV IDs such as `ao-thun-01`, so reviews reference that stable key, not the
independent database `products` table. Keep these IDs stable when editing the catalog.

With the `dev` profile, Hibernate can create the new table if the database login has
DDL permissions. Environments using `ddl-auto=none` or `validate` must apply
[`sql/product-reviews.sql`](sql/product-reviews.sql) to their application database
before running the updated application.

The local `team-trial` configuration uses `validate` and an application login without
`CREATE TABLE` permission. Apply the script with a database administrator account;
keep the application's existing validation setting. The review migration was applied
to the local `H2T_DB_TEAM_TRIAL_20260917` database on 2026-09-30.

The application also needs `SELECT`, `INSERT`, and `UPDATE` permissions on the new
table. For the local `h2t_user` database user, apply
[`sql/product-reviews-permissions.sql`](sql/product-reviews-permissions.sql) as the
database owner. Creating the table does not automatically grant write access to a
login with permissions assigned to individual tables.

`ProductReviewTests` covers authenticated submission, persistence, update, product
isolation, HTML escaping, invalid ratings/comments, unknown products, anonymous access,
and CSRF rejection. Tests use the isolated H2 database, not SQL Server.
