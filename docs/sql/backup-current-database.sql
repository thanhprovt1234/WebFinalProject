-- Run in SSMS with a login that can back up the database and verify backups.
-- The SQL Server service must have write access to the destination directory.
-- Creates a full copy-only backup of the database configured for the local web.
USE [H2T_DB_TEAM_TRIAL_20260917];
GO
SET NOCOUNT ON;

IF HAS_PERMS_BY_NAME(DB_NAME(), N'DATABASE', N'BACKUP DATABASE') <> 1
    THROW 50001, 'This login does not have BACKUP DATABASE permission.', 1;

DECLARE @now datetime2(0) = SYSDATETIME();
DECLARE @path nvarchar(4000) =
    N'E:\FINAL_PROJECT\docs\sql\H2T_DB_TEAM_'
    + CONVERT(char(8), @now, 112) + N'_'
    + REPLACE(CONVERT(char(8), @now, 108), ':', '') + N'_'
    + CONVERT(nvarchar(36), NEWID()) + N'.bak';

BACKUP DATABASE [H2T_DB_TEAM_TRIAL_20260917]
TO DISK = @path
WITH COPY_ONLY, CHECKSUM, STATS = 10;

RESTORE VERIFYONLY FROM DISK = @path WITH CHECKSUM;
SELECT @path AS verified_backup_path;
GO
