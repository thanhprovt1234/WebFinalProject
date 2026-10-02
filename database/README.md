# Đưa dữ liệu SQL Server Windows vào Docker

Chưa export/restore database local. `init-db.sh` chỉ tạo database rỗng khi chưa có;
Hibernate profile `dev` tạo bảng, seeder hiện có tạo roles/admin. Không tạo sản phẩm giả.
File `docs/sql/H2T_DB_TEAM_20260917.bak` đã có từ trước, không đảm bảo là dữ liệu mới
nhất hoặc schema hiện tại. Không tự động dùng file đó.

## Backup trên Windows (chỉ khi bạn muốn chuyển dữ liệu)

Trong SSMS, kết nối SQL Server Windows, xác nhận database cần sao lưu.
Yêu cầu ban đầu nêu `H2T_DB`, nhưng `application-local.properties` hiện dùng
`H2T_DB_TEAM_TRIAL_20260917`; chọn đúng database chứa dữ liệu muốn chia sẻ.
Nhấp phải database → Tasks → Back Up → Full → Disk, chọn file mới, ví dụ
`C:\SQLBackups\H2T_DB.bak`. Thư mục cần tồn tại và SQL Server service cần quyền ghi.
Dùng copy-only backup để không ảnh hưởng chuỗi backup khác, hoặc chạy trong SSMS:

```sql
BACKUP DATABASE [H2T_DB]
TO DISK = N'C:\SQLBackups\H2T_DB.bak'
WITH COPY_ONLY, CHECKSUM, STATS = 10;
```

Thay tên database nếu bạn muốn backup database team-trial. Không dùng `FORMAT`
hay ghi đè bản backup duy nhất. Copy file kết quả vào `database/H2T_DB.bak`.
Backup bị ignore Git/Docker; gửi riêng qua kênh phù hợp nếu cần chia sẻ dữ liệu.
SQL Server nguồn không được mới hơn SQL Server 2022 đích; backup từ bản mới hơn
không restore được vào 2022. Khi đó cần kế hoạch export schema/data tương thích riêng.

## Restore vào Docker

Quy trình sau thay thế **database Docker** bằng backup. Sao lưu dữ liệu Docker cần
giữ trước, dừng app để tránh ghi đồng thời. Không thực hiện lên SQL Server Windows.

```powershell
docker compose stop app
docker compose up -d db
docker compose cp database/H2T_DB.bak db:/var/opt/mssql/data/H2T_DB.bak
docker compose exec db bash
```

Trong shell Linux của container, chạy:

```bash
export SQLCMDPASSWORD="$MSSQL_SA_PASSWORD"
/opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -C -b
```

Tại prompt SQL, xác minh backup và lấy tên logical:

```sql
RESTORE VERIFYONLY FROM DISK = N'/var/opt/mssql/data/H2T_DB.bak';
RESTORE FILELISTONLY FROM DISK = N'/var/opt/mssql/data/H2T_DB.bak';
GO
```

Thay `LOGICAL_DATA` và `LOGICAL_LOG` dưới đây bằng giá trị cột `LogicalName`
vừa lấy. Nếu backup có nhiều data files, thêm `MOVE` cho từng file tới đường dẫn
Linux riêng. Dùng tên database trong `DB_NAME` nếu khác `H2T_DB`.

```sql
USE master;
ALTER DATABASE [H2T_DB] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
RESTORE DATABASE [H2T_DB]
FROM DISK = N'/var/opt/mssql/data/H2T_DB.bak'
WITH MOVE N'LOGICAL_DATA' TO N'/var/opt/mssql/data/H2T_DB.mdf',
     MOVE N'LOGICAL_LOG' TO N'/var/opt/mssql/data/H2T_DB_log.ldf',
     REPLACE, CHECKSUM, STATS = 10;
ALTER DATABASE [H2T_DB] SET MULTI_USER;
GO
EXIT
```

`REPLACE` ghi đè database Docker đích: chỉ chạy sau khi xác nhận đích và backup.
Nếu restore lỗi, đọc lỗi và đưa database về MULTI_USER trước khi thử lại.
Thoát shell bằng `exit`, sau đó:

```powershell
docker compose up -d
docker compose logs -f app
```

App Docker dùng `sa`, không phụ thuộc login `h2t_user` trên Windows. Admin/password
đã có trong backup giữ nguyên; `ADMIN_SEED_PASSWORD` không reset tài khoản cũ.
Với backup schema cũ, xem `docs/database-design.md`, `docs/database-catalog.md` và
các migration `docs/sql/` trước khi chạy app. Hibernate `update` không sửa mọi CHECK
constraint/cột legacy. Không chạy toàn bộ SQL scripts hàng loạt vì có script thay đổi
schema, script cần tham số hash và script nhập dữ liệu mẫu.

Nếu chỉ có SQL script dữ liệu đã được duyệt, copy vào container rồi dùng `sqlcmd`
với `-d H2T_DB -i /đường/dẫn/script.sql -b`; chuẩn bị schema phù hợp trước.
Không tự tạo/import dữ liệu mẫu khi chưa cần.
