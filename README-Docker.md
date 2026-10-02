# Chạy H2T bằng Docker

Yêu cầu: Docker Desktop đang chạy **Linux containers**, máy Windows x64 và đủ RAM
(nên dành ít nhất 4 GB cho Docker). Không cần cài Java, Maven hay SQL Server trên host.
Lần build đầu cần Internet để tải image, Maven Wrapper và dependencies.

## Khởi động

1. Clone/copy project rồi mở PowerShell tại thư mục chứa `compose.yaml`.
2. Chạy `Copy-Item .env.example .env` (chỉ làm khi chưa có `.env`).
3. Điền `MSSQL_SA_PASSWORD` và `ADMIN_SEED_PASSWORD` bằng mật khẩu riêng.
   Mật khẩu SQL Server dài ít nhất 8 ký tự, có ít nhất 3 nhóm: chữ hoa, chữ thường,
   chữ số, ký tự đặc biệt. Nếu giá trị chứa `$` hoặc `#`, đặt trong dấu nháy đơn
   trong `.env`. Không commit/chia sẻ `.env` chứa thông tin thật.
4. Mở Docker Desktop, chạy:

   ```powershell
   docker compose up --build
   ```

5. Khi log báo `Started DemoApplication`, mở **http://localhost:8080**.
   Tài khoản `admin` được seeder hiện có tạo khi chưa tồn tại, dùng
   `ADMIN_SEED_PASSWORD`. Đổi biến này không đổi mật khẩu admin đã lưu.

Database mới chưa có sản phẩm, tồn kho hoặc tài khoản từ máy Windows.
Web vẫn chạy với danh mục trống; muốn dữ liệu cũ, xem [backup/restore](database/README.md).
Không tự động restore file `.bak` đang có trong `docs/sql`.

## Kiến trúc và cấu hình

`Browser → localhost:8080 → app (Java 26 / Spring Boot 4.1.1) → db:1433 (SQL Server 2022)`.
`db-init` chạy một lần sau khi SQL Server healthy, chỉ tạo `H2T_DB` nếu chưa tồn tại;
`app` chờ bước này thành công. Dockerfile build bằng Maven Wrapper 3.9.16,
giữ executable WAR và JSP/JSTL, sau đó chạy `java -jar` bằng Java 26 JRE.

Compose bật profile `dev` có sẵn: Hibernate `update`, lỗi schema làm startup thất bại.
Cấu hình gốc vẫn `none`; không sửa `application.properties`, cấu hình local,
Entity, Repository, Service hoặc Controller. Hibernate update không thay thế mọi
migration cũ; đọc hướng dẫn database trước khi restore schema phiên bản trước.

Ứng dụng Docker dùng `sa` và `MSSQL_SA_PASSWORD` để đơn giản hóa đồ án nhóm;
đây là cấu hình phát triển với SQL Server Developer, không dành cho production.
Datasource được override bằng `SPRING_DATASOURCE_*`, không dùng localhost.
File `application-local.properties`, `.env` và backup không vào build context/image.

`MAIL_USERNAME` và `MAIL_PASSWORD` cần tài khoản SMTP/app password của nhóm nếu muốn
gửi OTP. Bỏ trống vẫn khởi động web, nhưng chức năng gửi email sẽ không hoạt động.
`MAIL_HOST`/`MAIL_PORT` có thể đổi. JWT/Cloudinary hiện chỉ có cấu hình dự phòng;
chưa có triển khai gọi dịch vụ. Không thấy cấu hình VNPay/MoMo trong source hiện tại.
Ảnh sản phẩm trỏ HTTPS trong database vẫn cần Internet; ảnh local nằm trong WAR.

Nếu cổng host bị chiếm, đổi `.env`: `APP_HOST_PORT=8081`, `DB_HOST_PORT=1434`, rồi
chạy lại Compose. URL khi đó là `http://localhost:8081`; port trong container vẫn
8080 và 1433. SQL Server Windows trên máy này đang chiếm 1433, nên dùng 1434.
SQL Server Docker chỉ được publish ở `127.0.0.1`; SSMS kết nối `localhost,1434`
(hoặc cổng bạn chọn), SQL authentication `sa`, bật Trust server certificate.

## Lệnh thường dùng

```powershell
docker compose config --quiet  # kiểm tra cấu hình, không in secret
docker compose build
docker compose up --build
docker compose up -d --build
docker compose ps --all
docker compose logs -f
docker compose logs -f app db
docker compose restart
docker compose down            # dừng và giữ database
```

`docker compose config` cũng hợp lệ nhưng sẽ in giá trị secret đã thay thế;
không gửi output đó vào Git/chat. `db-init` trạng thái `Exited (0)` là bình thường.

Dữ liệu nằm trong named volume `<tên-project>_sqlserver_data`, mount vào
`/var/opt/mssql`. Xem tên chính xác bằng `docker volume ls`. Restart, rebuild app
và `docker compose down` không xóa volume. Giữ cùng thư mục/tên Compose project
để dùng lại volume; đổi tên project sẽ chọn volume khác.

**`docker compose down -v` xóa volume SQL Server và toàn bộ dữ liệu trong đó.**
Chỉ dùng khi chủ động muốn xóa dữ liệu và đã backup cần thiết.
Đổi `MSSQL_SA_PASSWORD` trong `.env` không tự đổi mật khẩu trên volume đã khởi tạo;
cần đổi login trong SQL Server rồi đồng bộ `.env`, không xóa volume để sửa lỗi login.

## Chạy trực tiếp trên Windows

Tiếp tục dùng `application-local.properties` và các lệnh Maven như trước.
Compose không sửa hoặc kết nối SQL Server Windows. Cấu hình local hiện trỏ
`H2T_DB_TEAM_TRIAL_20260917`, profile `team-trial`, `ddl-auto=validate`.
Các credentials, SMTP và dữ liệu local không được tự chuyển sang container.
Thành viên khác chỉ cần `.env` riêng + Docker Desktop + `docker compose up --build`;
thêm bước restore nếu muốn cùng dữ liệu hiện tại.

Image Java và công cụ SQL Server tham khảo tài liệu chính thức:
[Temurin](https://hub.docker.com/_/eclipse-temurin),
[SQL Server containers](https://learn.microsoft.com/en-us/sql/linux/quickstart-install-connect-docker?view=sql-server-ver16).

## Kết quả kiểm tra ngày 02/10/2026

- Compose config hợp lệ; multi-stage build từ source thành công với Maven Wrapper.
- Runtime Java 26.0.2.1, Spring Boot 4.1.1, executable WAR; user `10001:10001`.
- SQL Server healthy; `db-init` thoát 0; JDBC kết nối `db:1433/H2T_DB` thành công.
- Trang chủ, login, register, forgot-password, collections, CSS, JS và ảnh trả HTTP 200.
- Đăng nhập admin và trang `/admin/users` trả HTTP 200.
- Sau `down` rồi `up -d`, 20 bảng, 5 roles và admin (ID/thời điểm tạo) giữ nguyên;
  app khởi động lại, bước init không tạo lại database.
- `.env` kiểm thử riêng đã được tạo và Git ignore, dùng web 8080 / database host 1434.
  Không export/restore hoặc chỉnh dữ liệu SQL Server Windows. Không chạy gửi email
  thật hoặc toàn bộ test nghiệp vụ; Docker build dùng `-DskipTests`.
