# Phân quyền và dữ liệu đa shop

Một tài khoản có thể giữ nhiều vai trò. Guest là người chưa đăng nhập, không có tài khoản riêng. Đăng ký công khai chỉ cấp ROLE_USER; quyền cao hơn do Admin cấp.

| Vai trò | Đường dẫn | Phạm vi đã triển khai |
| --- | --- | --- |
| Guest | /, /collections, /products/{slug} | Xem sản phẩm công khai |
| Các vai trò đăng nhập | /profile, /workspace | Các chức năng khách hàng hiện có |
| Vendor (Admin shop) | /seller, /seller/shops/{id}, /seller/orders | Xem shop, sản phẩm và xử lý đơn của shop do tài khoản sở hữu |
| Manager | /manager, /manager/stores/{id} | Xem chi nhánh được phân công và tồn kho |
| Shipper | /shipper, /shipper/shipments/{id} | Xem đơn giao được phân công |
| Admin | /admin/users, /admin/users/{id} | Tìm tài khoản, cấp/thu hồi vai trò, phân công/gỡ Manager; truy cập toàn bộ các trang trên |

Đăng nhập rồi chọn **Tài khoản → Khu vực làm việc**. Seller được xem dữ liệu và xử lý đơn của shop mình; Manager, Shipper xem dữ liệu theo phạm vi được phân công. Admin có trang `/admin` để quản lý shop/chi nhánh, sản phẩm/phiên bản/tồn kho, danh mục, duyệt và xử lý đơn COD. Xem [Quản trị toàn bộ shop](admin-management.md). Doanh thu và quy trình đăng ký shop của Seller chưa được bổ sung.

## Kiểm soát truy cập

- Kiểm tra URL và vai trò tại service; kiểm tra chủ sở hữu/phân công khi truy cập từng bản ghi.
- Mỗi request đăng nhập tải lại quyền từ database. Thu hồi vai trò và khóa tài khoản áp dụng với cả phiên đang đăng nhập.
- Form thay đổi quyền/phân công yêu cầu CSRF; vai trò giới hạn bằng enum.
- Không cho Admin tự bỏ quyền Admin. Thay đổi quyền được tuần tự hóa bằng khóa hàng ROLE_ADMIN và kiểm tra lại người thực hiện trong transaction.
- Bỏ quyền Manager đồng thời gỡ phân công. Cấp Vendor không tự chuyển quyền sở hữu shop.
- Trái quyền trả HTTP 403; chưa đăng nhập chuyển tới đăng nhập.

## Database và dữ liệu mẫu

Chạy `docs/sql/role-access.sql` bằng tài khoản có quyền DDL trước khi khởi động với `ddl-auto=validate`.

`docs/sql/sample-shops.sql` yêu cầu năm biến SQLCMD chứa BCrypt hash của mật khẩu tự tạo: MOC_HASH, AN_HASH, PHO_HASH, MANAGER_HASH, SHIPPER_HASH. Không ghi mật khẩu thật vào source control. Script không đặt lại mật khẩu tài khoản cũ hay cộng lại tồn kho đã có.

| Shop | Sản phẩm | Giá |
| --- | --- | --- |
| Mộc Studio | Áo sơ mi Oxford Mộc | 289.000 VNĐ |
| An Nhiên Wear | Áo polo An Nhiên | 249.000 VNĐ |
| Phố Denim | Quần jeans Phố Denim | 399.000 VNĐ |

Mỗi shop có Seller riêng (`demo_moc`, `demo_an_nhien`, `demo_pho_denim`), một chi nhánh mẫu và một sản phẩm. Mỗi sản phẩm có size M/L/XL, tồn kho mẫu 10 mỗi size. Hình minh họa dùng tài nguyên có sẵn. `demo_manager` được phân công chi nhánh Mộc Studio; `demo_shipper` chưa có đơn giao. Không tạo đơn hàng hoặc số bán giả.

Ngày 15/10/2026, đã chạy `docs/sql/expand-clothing-catalog.sql` trên database local để bổ sung 9 sản phẩm cho mỗi mục áo polo, áo sơ mi và quần jeans, nâng mỗi mục lên 10 sản phẩm. Giá các sản phẩm mới lần lượt là 219.000–359.000đ, 259.000–379.000đ và 369.000–459.000đ. Sản phẩm mới có size M/L/XL và tồn kho mẫu 10 mỗi size. Cả 30 sản phẩm trong ba mục dùng ảnh riêng tải từ H2T, lưu tại `src/main/webapp/images/products/`; nguồn từng ảnh nằm trong `SOURCES.md` của mỗi thư mục. Script `docs/sql/clothing-product-images.sql` cập nhật ảnh cho database đã nhập sản phẩm trước đó. Script yêu cầu chạy sau `sample-shops.sql` bằng tài khoản có quyền ghi; chạy lại không đặt lại giá hay tồn kho đã có.

Ở máy phát triển hiện tại, thông tin đăng nhập ngẫu nhiên lưu tại `.build-tools/demo-accounts.txt` (bị Git bỏ qua). Admin hiện có giữ nguyên mật khẩu.

## Xác minh

`RoleAccessTests` chạy HTTP/JSP trên H2 riêng: ma trận vai trò, chặn URL trực tiếp, khác chủ shop/chi nhánh/người giao; bảo vệ CSRF; cấp/thu hồi quyền ngay trong phiên hiện tại; gỡ phân công và chống tự hạ quyền Admin.

Đã kiểm tra thêm trên SQL Server local: trang chi tiết ba sản phẩm, đăng nhập năm tài khoản mẫu, trang theo vai trò, form cấp quyền và phân công Manager. Tài khoản Admin kiểm thử tạm được xóa sau khi kiểm tra.

## Cấp quyền quản lý đơn hàng theo shop

Admin shop dùng ROLE_VENDOR; ROLE_ADMIN là quản trị toàn hệ thống, có quyền trên mọi shop.
Tại /admin/users, mở tài khoản đã có và cấp ROLE_VENDOR (giữ ROLE_USER).
Tại /admin/shops, sửa shop tương ứng và chọn tài khoản đó làm chủ shop.
Nếu tài khoản chỉ quản lý một shop đang có ROLE_ADMIN, admin hệ thống cần thu hồi vai trò này.

Ba tài khoản mẫu demo_moc, demo_an_nhien, demo_pho_denim đã được liên kết với shop và cấp Vendor bởi sample-shops.sql.
Chủ shop đăng nhập → Khu vực làm việc → Quản lý đơn hàng shop (/seller/orders).
Danh sách lọc theo chủ shop tại database; xem chi tiết và cập nhật trạng thái đều kiểm tra chủ sở hữu.
Đơn mua nhiều shop chỉ hiển thị sản phẩm và tổng tiền thuộc phần đơn của shop mình.
Hủy đơn chỉ hoàn kho phần đó. Form cập nhật yêu cầu CSRF.
