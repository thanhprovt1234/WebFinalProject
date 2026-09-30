-- Apply as database owner to the application's database.
-- Existing schema already contains users and addresses.
IF DATABASE_PRINCIPAL_ID(N'h2t_user') IS NULL
    THROW 50001, 'Application user h2t_user does not exist.', 1;
GRANT SELECT, INSERT, UPDATE, DELETE ON OBJECT::dbo.addresses TO [h2t_user];
GRANT UPDATE (full_name, phone) ON OBJECT::dbo.users TO [h2t_user];
