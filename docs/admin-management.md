# Quản trị toàn bộ shop

Đăng nhập bằng tài khoản có `ROLE_ADMIN`, chọn **Quản trị** hoặc mở `/admin`. Admin truy cập tất cả shop. Các role khác bị chặn tại URL và service; POST yêu cầu CSRF, quyền được tải lại từ database mỗi request.

- `/admin`: thống kê shop, sản phẩm, đơn theo shop và tài khoản.
- `/admin/shops`: thêm/sửa/tắt shop, chọn chủ sở hữu, duyệt trạng thái shop; thêm/sửa/tắt chi nhánh.
- `/admin/products`: tìm/lọc theo shop, thêm/sửa/xóa sản phẩm, giá, ảnh, mô tả; thêm size/màu, sửa giá riêng hoặc tắt phiên bản; nhập tồn kho theo chi nhánh cùng shop. Ảnh nhận đường dẫn `/images/…` hoặc URL HTTPS.
- `/admin/categories`: thêm/sửa/tắt danh mục.
- `/admin/orders`: tất cả đơn theo shop, địa chỉ, sản phẩm, lịch sử; xác nhận, hủy/từ chối, chuyển giao hàng, xác nhận đã giao/thu COD.
- `/admin/users`: phân quyền và phân công Manager hiện có.

Xóa shop/sản phẩm bằng tắt hoạt động/hiển thị, giữ lịch sử đơn và quan hệ dữ liệu. Có thể khôi phục ở trang sửa. Đường dẫn shop/sản phẩm, shop của sản phẩm và SKU/size/màu đã tạo được giữ ổn định để bảo toàn giỏ hàng; tạo phiên bản mới khi cần đổi thông số này.

## Xử lý đơn

Admin xác nhận sẽ chuyển trực tiếp `NEW → DELIVERED` và ghi nhận đã thanh toán COD khi tất cả phần đơn đã xác nhận hoặc hủy. Các đơn cũ `CONFIRMED`, `PICKED_UP`, `SHIPPING` cũng có thể xác nhận hoàn tất trực tiếp. Có thể hủy từ `NEW` hoặc `CONFIRMED`, bắt buộc lý do; không hủy sau khi đã giao hoặc mở lại đơn đã hủy. Yêu cầu xác nhận `CONFIRMED` từ trang Admin cũ được xử lý thành `DELIVERED`.

Checkout mới ghi lượng hàng đã trừ từng dòng kho vào `order_stock_allocations`. Hủy đơn khóa đơn cha và các dòng kho, hoàn đúng chi nhánh một lần, ghi lịch sử kèm tài khoản Admin, đặt tổng tiền phần đơn về 0 và giảm tổng tiền đơn cha. Giá/dòng sản phẩm ban đầu vẫn giữ để đối soát. COD chỉ đánh dấu `PAID` khi tất cả phần đơn đã giao hoặc hủy và có phần đã giao; nếu tất cả hủy thì `CANCELLED`.

Đơn cũ chưa có phân bổ kho chỉ tự hoàn khi mỗi phiên bản có duy nhất một dòng tồn kho thuộc shop. Nếu không xác định được, hủy bị từ chối với thông báo cần đối soát kho. Không tự đổi trạng thái đơn của khách trong quá trình triển khai.

## Email mua hàng thành công

Khi xác nhận hoàn tất đơn, hệ thống gửi email **H2T - Mua hàng thành công** đến email tài khoản khách, gồm mã đơn, sản phẩm, size/màu, số lượng, tổng tiền và thông tin nhận hàng. Đơn nhiều shop chỉ gửi một lần khi mọi phần đơn đã giao hoặc hủy và có ít nhất một phần đã giao. Không gửi lúc khách vừa đặt đơn, khi toàn bộ đơn bị hủy, hoặc khi bấm xác nhận lại. Khách hủy phần còn chờ sau khi shop khác đã xác nhận cũng hoàn tất đơn và kích hoạt email.

Email chỉ gửi sau khi transaction lưu trạng thái thành công; rollback không gửi. Dùng cùng cấu hình SMTP với OTP: `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` (hoặc `spring.mail.*` trong file cấu hình local). Không cần migration database.

Nếu SMTP lỗi, trạng thái đơn vẫn được giữ và lỗi kèm mã đơn được ghi vào log. Hiện chưa có hàng đợi gửi lại; thao tác xác nhận lặp lại không gửi lại email lỗi. Kiểm thử dùng mail sender giả lập, không gửi email thật.

## Database và tài khoản local

Chạy [sql/admin-management.sql](sql/admin-management.sql) sau migration COD bằng database owner. Thêm bảng phân bổ kho và quyền SQL cho ứng dụng. Đã áp dụng cho `H2T_DB_TEAM_TRIAL_20260917` ngày 15/10/2026.

Tài khoản local mới `h2t_admin` có `ROLE_ADMIN` và `ROLE_USER`. Mật khẩu ngẫu nhiên lưu trong `.build-tools/h2t-admin-account.txt` (Git bỏ qua); database chỉ lưu BCrypt hash. Giữ nguyên mật khẩu `admin` cũ. Không đưa mật khẩu vào script được theo dõi bởi Git.

## Xác minh

`AdminManagementTests` kiểm tra quyền/CSRF, render các màn hình, CRUD, dữ liệu sai, nhập kho khác shop, chuyển trạng thái, hoàn kho một lần, nhiều chi nhánh/shop, tổng COD sau hủy một phần và giữ lịch sử khi xóa sản phẩm. HTTP trên SQL Server thật đã xác nhận đăng nhập mới, tạo shop/chi nhánh/sản phẩm/phiên bản/tồn kho, đặt COD, xác nhận/hủy và hoàn đúng tồn kho. Dữ liệu thử đã xóa; giữ tài khoản Admin mới và dữ liệu người dùng.
