# Database catalog

The storefront now reads `products`, `shops`, `categories`, `product_variants` and
`inventories` through JPA. The TSV is retained only as an import reference and H2 test
fixture; it is no longer loaded by the running application's catalog service.

## Browsing

- `/collections` lists active products of enabled, ACTIVE shops and enabled categories.
- Optional `category` (numeric ID), `shop` (slug), `q`, `sort`, and zero-based `page` filters.
- Pagination is executed in the database at 20 products per page.
- Sorts: stable default order, `newest`, `price-asc`, `price-desc` (base/list price).
- Name search uses normalized `search_name`, including Vietnamese accents. JPA lifecycle
  callbacks maintain this field when products are created/renamed. Direct SQL writers
  must also maintain the normalized name. `%` and `_` in queries are literal characters.
- `/collections/ao-thun-co-tron` remains a compatible category shortcut.
- The home page displays up to 20 newest active products. This is not a sales ranking.

Product URLs and saved user records retain the stable product slug (`ao-thun-01`, etc.).
Treat slugs as immutable once published. This preserves existing cart, review, favorite
and viewed-product data without changing their account ownership. Those services now
resolve product information against database products. Inactive/unavailable products
cannot be added; old saved entries remain removable.

## Variants and stock

Each enabled variant supplies its size, color and optional price override. Otherwise the
base product price applies. Stock is the sum at enabled branches of that product's shop;
stock accidentally assigned to another shop is excluded. No inventory row means zero.

Cart lines are unique by account + product slug + size + color. Totals use BigDecimal
and database prices. Add/update rejects quantities above available stock or 99 units.
Existing lines above current stock remain visible with a warning. Adding to a cart does
not reserve inventory; checkout must validate and deduct stock transactionally later.

## Local migration

Apply [sql/database-catalog.sql](sql/database-catalog.sql) as database owner before
starting with schema validation. It adds normalized names and cart colors, widens slug
references, updates cart uniqueness, grants catalog reads, and imports the eight existing
display products into an H2T shop with their former IDs and current prices. Existing
product records are not overwritten. The H2T shop uses an enabled administrator as the
initial owner; assigning a business seller is a later administration step.

Applied to `H2T_DB_TEAM_TRIAL_20260917` on 2026-10-01. The database initially contained
no products, shops, categories, branches or inventories. The migration creates five
sizes per imported color, but **does not fabricate branch inventory**. Supply actual
branch inventory before these imported products can be added to a cart. Old cart lines,
reviews and activity records are retained. Historical crossed-out sample prices are
not presented as active promotions.

## Verification

81 tests pass. New tests cover database filtering/sorting, 20-item pages, normalized
search, hidden shops/categories/products, live price changes, color-separated cart lines,
variant prices, stock limits, disabled branches and cross-shop inventory exclusion.
Chrome checks against SQL Server cover filters, paging, actual variant stock/pricing,
cart limits, favorites/history and responsive layouts at 1440px and 390px. Temporary
test products, inventory and account data are removed after verification.
