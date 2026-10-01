# Phân quyền và dữ liệu đa shop

Một tài khoản có thể giữ nhiều vai trò. Guest là người chưa đăng nhập, không có tài khoản riêng. Đăng ký công khai chỉ cấp ROLE_USER; quyền cao hơn do Admin cấp.

| Vai trò | Đường dẫn | Phạm vi đã triển khai |
| --- | --- | --- |
| Guest | /, /collections, /products/{slug} | Xem sản phẩm công khai |
| Các vai trò đăng nhập | /profile, /workspace | Các chức năng khách hàng hiện có |
| Vendor (Seller) | /seller, /seller/shops/{id} | Xem shop do tài khoản sở hữu và danh sách sản phẩm |
| Manager | /manager, /manager/stores/{id} | Xem chi nhánh được phân công và tồn kho |
| Shipper | /shipper, /shipper/shipments/{id} | Xem đơn giao được phân công |
| Admin | /admin/users, /admin/users/{id} | Tìm tài khoản, cấp/thu hồi vai trò, phân công/gỡ Manager; truy cập toàn bộ các trang trên |

Đăng nhập rồi chọn **Tài khoản → Khu vực làm việc**. Các trang Seller, Manager, Shipper trong đợt này là trang xem dữ liệu theo phạm vi. CRUD sản phẩm, xử lý trạng thái đơn, doanh thu và quy trình đăng ký/duyệt shop chưa được bổ sung trong đợt này.

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

Ở máy phát triển hiện tại, thông tin đăng nhập ngẫu nhiên lưu tại `.build-tools/demo-accounts.txt` (bị Git bỏ qua). Admin hiện có giữ nguyên mật khẩu.

## Xác minh

`RoleAccessTests` chạy HTTP/JSP trên H2 riêng: ma trận vai trò, chặn URL trực tiếp, khác chủ shop/chi nhánh/người giao; bảo vệ CSRF; cấp/thu hồi quyền ngay trong phiên hiện tại; gỡ phân công và chống tự hạ quyền Admin.

Đã kiểm tra thêm trên SQL Server local: trang chi tiết ba sản phẩm, đăng nhập năm tài khoản mẫu, trang theo vai trò, form cấp quyền và phân công Manager. Tài khoản Admin kiểm thử tạm được xóa sau khi kiểm tra.
