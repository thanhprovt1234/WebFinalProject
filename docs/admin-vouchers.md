# Admin vouchers and gifts

Open `/admin/vouchers` using an enabled admin account. Create an immutable program
with a name, fixed VND or percentage discount, minimum order total, optional percentage
cap, and expiry date (valid through the end of that date in Vietnam).

Open a program to search customers by name, username, or email and select up to 100
recipients per submission. Only enabled accounts with `ROLE_USER` can receive gifts.
The random picker draws without replacement from customers who have not received
that program, then shows a preview. No vouchers are issued until the admin submits
the displayed recipients. The search filter applies to manual selection; random
selection uses all eligible customers.

After an issuance transaction commits, each newly gifted recipient is sent an email
at their account email address with the program name, private code, discount,
minimum total, optional cap, expiry in Vietnam time, and redemption instructions.
Previewing a draw, rolling back issuance, and submitting already-issued recipients
do not send emails. The existing SMTP configuration is used. SMTP errors are logged
per voucher and do not undo committed gifts or stop notifications to other recipients.
There is no automatic email retry; recipients can still find their vouchers in their wallet.

Each recipient gets a unique private code, visible in `/orders#vouchers` and at
checkout. An admin can issue more batches, but each customer receives at most one
code per program, even after revocation or use. Duplicate submissions skip existing
recipients. Issuance locks the program, with a unique database index as a second guard.
The ledger shows recipient, issuer, issue time, and status. Unused codes can be
revoked; used codes cannot. Issue and revoke operations record the acting admin.

Checkout enforces ownership, expiry, revocation, minimum total, and one-time use.
Discount is capped at the order total and distributed proportionally across shops
with cumulative rounding so shop totals equal the parent total. Checkout and revocation
lock the same voucher row. Cancelling an order does not restore a used voucher,
matching existing reward voucher behavior. One voucher is allowed per order.

Existing automatic 10% reward vouchers remain supported, including eligibility
checks against their source orders. Gift vouchers do not require a source purchase.

Apply [sql/admin-vouchers.sql](sql/admin-vouchers.sql) after
[sql/loyalty-vouchers.sql](sql/loyalty-vouchers.sql), before running the updated app,
including existing databases using Hibernate update. The script preserves existing
vouchers, creates `voucher_campaigns`, and replaces source-order uniqueness with a
filtered unique index so multiple gifts can have no source order. It grants the
application user access to the new table. It is safe to rerun.

`AdminVoucherTests` exercises real HTTP requests for admin permissions, CSRF,
creation, validation, escaping, email search, random preview, unique private issue,
duplicate submissions, wallet isolation, and revocation.
It also checks notification contents, recipient isolation, commit/rollback behavior,
and continued processing when SMTP fails. `CheckoutServiceTests`
covers fixed gifts across shops, percentage caps, minimum total, expiry, revocation,
discounts capped at the order total, and compatibility with automatic rewards.
