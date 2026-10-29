# Product reviews

The product detail page supports public reviews and a signed-in form with a required
1–5 star rating and a nonblank comment (maximum 2,000 characters). Submitting again
updates the current account's review for the selected order. Buying the product in
another delivered order allows a separate review. Multiple units or variants of the
same product in one order share one review. The author comes from the authenticated account;
the form cannot select another author. CSRF protection remains enabled and JSP escapes
comments and names. Dates are displayed in Vietnam time.

Only enabled accounts with a `DELIVERED` shop order containing the exact product
may submit or update a review. Other signed-in visitors see an eligibility message
instead of the form. Direct submissions without a qualifying purchase return HTTP 403.

In `/orders` and each order's details, delivered shop items include a “Đánh giá sản phẩm”
link to `/products/{slug}?orderId={orderId}#review-form`. This opens the exact purchased product at its
rating/comment form, including when other shops in the same order are still pending.
Items in other shop statuses do not display the shortcut.

Reviews persist in `product_reviews`, with a unique `(catalog_product_id, order_id)`
constraint and a database check for the rating range. Reviews resolve stable product
slugs such as `ao-thun-01` against the database catalog. Keep these slugs stable when
editing products. See [database-catalog.md](database-catalog.md) for the migration.

With the `dev` profile, Hibernate can create the new table if the database login has
DDL permissions. Environments using `ddl-auto=none` or `validate` must apply
[`sql/product-reviews.sql`](sql/product-reviews.sql) to their application database
before running the updated application. Also apply
[`sql/product-reviews-per-order.sql`](sql/product-reviews-per-order.sql), including
on existing development databases: Hibernate update does not remove the old unique
constraint. This migration links historical reviews to the latest delivered matching
order when available, and keeps unmatched historical reviews visible without an order.
The product page selects an unreviewed delivered purchase by default and provides
links to choose another purchase. POST requires an explicit order ID, checked against
the signed-in owner, product, and delivered shop status; another eligible order never
grants permission to review an ineligible selected order.

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
