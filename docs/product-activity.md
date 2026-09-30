# Favorites and recently viewed products

Signed-in customers can save/unsave a product using the heart button on its detail
page. Lists are accessible from `/profile` and product detail links:

- `/favorites`: most recently saved first.
- `/recently-viewed`: most recently viewed first; revisiting moves the product up.

Both lists paginate at 20 products. The application derives names, images and prices
from the current catalog. Removed products are shown as unavailable and can still be
removed from favorites. This feature uses the current stable catalog string IDs;
it does not migrate the catalog to database products.

`product_activities` stores one row per account/product, with separate favorite and
view timestamps. Removing a favorite preserves its view history. Favorite writes are
idempotent (explicit desired state rather than toggle) and account locking serializes
concurrent updates. All queries use the authenticated account, never a submitted user ID.

The detail-page script posts the viewed event with CSRF protection after loading;
it also records returns from browser back/forward cache. Anonymous views are not stored.
When JavaScript is disabled, favorites still work through forms, but views are not recorded.

Apply [sql/product-activities.sql](sql/product-activities.sql) as database owner before
starting with schema validation. The script creates the table and indexes, and grants
SELECT/INSERT/UPDATE/DELETE for the local `h2t_user`. Applied to local
`H2T_DB_TEAM_TRIAL_20260917` on 2026-09-30.

Validation: all 77 tests pass. Activity tests cover authentication, CSRF, ownership,
idempotence, revisits, concurrent writes, pagination and unavailable products. Chrome
checks against SQL Server cover favorite/unfavorite, automatic view recording, ordering,
fresh-login persistence and 1440px/390px layouts. Temporary test data was removed.
