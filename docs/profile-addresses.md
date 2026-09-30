# Profile and delivery addresses

Signed-in users open `/profile` through the account name in the header. They can
edit their full name and optional phone number. Username and email are displayed
read-only. The update DTO never binds account identity, roles, enabled status or password.

The address book supports creating, editing and deleting multiple delivery addresses,
and choosing a default. The first address is automatically default. Selecting another
default clears the previous choice; deleting the default selects the oldest remaining
address. Editing the default retains it until another address is selected. An account
row lock serializes address writes, including concurrent first-address submissions.

Every lookup includes the authenticated user's ID; requests for another user's address
return 404. Forms use CSRF protection, server-side validation and escaped output.
Deletion asks for confirmation in the browser. Existing purchase address snapshots are
not modified. Checkout integration remains a later step.

Existing `users` and `addresses` tables are reused. Apply
[`sql/profile-permissions.sql`](sql/profile-permissions.sql) with the database owner
to grant the local application access. Applied to `H2T_DB_TEAM_TRIAL_20260917` on
2026-09-30. No schema changes were needed.

Verified: 73 tests pass, including profile validation, protected account fields, address
ownership, HTML escaping, CRUD, default selection/reassignment, concurrent inserts and
CSRF. Also exercised the UI against SQL Server in Chrome at 1440px and 390px.
