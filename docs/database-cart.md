# Account carts

The shared cart now persists in SQL Server `cart_items`, scoped to the authenticated
account. Guests are prompted to sign in. Cart contents survive logout and are shared
between devices. Adding the same catalog product and size increments its quantity;
each line permits 1–99 units. The dialog supports quantity changes and removal.

The application derives product names, images, prices and totals from the database via
`CatalogService`. Client-supplied prices and user IDs are ignored. Lines now distinguish
size and color, use variant prices, and enforce available branch stock. Stable product
slugs preserve old saved records. This does not introduce checkout or stock reservation.
See [database-catalog.md](database-catalog.md) for the newer migration and stock behavior.

The old `h2t-cart-v1` browser storage is no longer read or written. It has no account
ownership, so it is not automatically copied into a signed-in user's cart. Existing
browser data is left untouched; users can add desired products to the account cart.

## Database setup

Apply [sql/cart-items.sql](sql/cart-items.sql) as database owner before starting with
`ddl-auto=validate` or `none`. It creates the table if missing and grants only
SELECT/INSERT/UPDATE/DELETE on that table to the local `h2t_user`. For other deployments,
grant those permissions to the configured application database user.

Applied to local `H2T_DB_TEAM_TRIAL_20260917` on 2026-09-30. Existing application
configuration remains unchanged.

## API

All responses are JSON; mutations return the full current cart. Authentication and
CSRF protection apply. Form-encoded requests are used for POST/PATCH.

| Method | Path | Fields |
| --- | --- | --- |
| GET | `/api/cart` | None |
| POST | `/api/cart/items` | `productId` (slug), `size`, `color`, `quantity` |
| PATCH | `/api/cart/items/{id}` | `quantity` |
| DELETE | `/api/cart/items/{id}` | None |

Item lookups always include the current user's ID. Writes lock that user's row for
the duration of the transaction so concurrent first inserts and increments serialize.
Database uniqueness and CHECK constraints provide additional safeguards.

`CartControllerTests` covers ownership, cross-session persistence, authoritative prices,
merging, separate sizes, updates/removal, limits, CSRF, anonymous access and concurrent adds.

Verified in Chrome against local SQL Server: guest sign-in prompt, add/update/remove,
totals, page reload, recovery after a fresh login session, four concurrent additions,
and dialog layouts at 1440px and 390px. The temporary test account was removed.
