# Đặt hàng COD

## Voucher mua lần sau

Chạy [sql/loyalty-vouchers.sql](sql/loyalty-vouchers.sql) bằng database owner trước khi khởi động phiên bản có voucher.
Đã áp dụng và xác minh bảng/cột cùng quyền SELECT, INSERT, UPDATE của `h2t_user` trên `H2T_DB_TEAM_TRIAL_20260917` (`localhost:1433`, instance `SQLEXPRESS`) ngày 21/10/2026.
Đơn mới giao và thanh toán thành công có tổng thực trả **lớn hơn 500.000đ** được tặng một mã giảm 10%. Đơn đúng 500.000đ không được tặng; phần đã hủy không được tính. Không cấp bù tự động cho đơn cũ.
Mã gắn với tài khoản, dùng một lần, chưa đặt hạn sử dụng. Xem mã khả dụng tại **Đơn hàng của tôi** hoặc trang thanh toán. Nhập mã và bấm **Áp dụng mã** để xem số tiền giảm trước khi xác nhận. Mỗi đơn dùng tối đa một mã; mã đã dùng không được hoàn lại khi hủy đơn.
Giảm giá được tính trên tiền hàng, làm tròn đến hai chữ số thập phân và chia cho từng shop để hủy một phần đơn vẫn trừ đúng số tiền thực trả.

Từ giỏ hàng, chọn **Đặt hàng COD**, kiểm tra địa chỉ nhận hàng và tổng tiền rồi xác nhận. Địa chỉ mặc định trong tài khoản được điền sẵn và có thể sửa trước khi đặt. Trong phiên bản này, phí giao hàng là 0đ và được hiển thị trước khi xác nhận.

Sau khi xác nhận thành công, ứng dụng chuyển sang `/orders?placed={id}`: danh sách đơn của tài khoản và tự mở hộp chi tiết đơn vừa đặt. Mỗi đơn có nút **Xem chi tiết**, hiển thị mã `H2T-…`, ngày giờ đặt theo múi giờ Việt Nam, trạng thái, phương thức/trạng thái thanh toán, người nhận, số điện thoại, địa chỉ, sản phẩm và tổng tiền. Trạng thái `NEW` được hiển thị là **Chờ xác nhận**. Đơn nhiều shop có trạng thái khác nhau hiển thị trạng thái của từng shop. Khi JavaScript không khả dụng, nút chi tiết mở trang `/orders/{id}` với cùng thông tin.

- `/checkout`: yêu cầu đăng nhập; nhận họ tên, số điện thoại và địa chỉ đầy đủ.
- `/orders`: các đơn của tài khoản đang đăng nhập.
- `/orders/{id}`: thông tin nhận hàng, sản phẩm, tổng tiền và trạng thái từng shop. Tài khoản khác không đọc được đơn.

Đơn mới lưu `payment_method=COD`, `payment_status=UNPAID`; đơn theo shop ở trạng thái `NEW`. Việc đặt đơn chưa xác nhận đã thu tiền. [Trang quản trị](admin-management.md) cho phép Admin xác nhận, giao hàng, ghi nhận đã giao/thu COD và hủy trước khi giao kèm hoàn tồn kho.

Giá và lựa chọn size/màu được lấy từ database, lưu lại trên từng dòng đơn. Tổng tiền người dùng đã xem phải khớp giá hiện tại; nếu giá đổi, trang yêu cầu xem lại trước khi xác nhận. Sản phẩm ngừng kinh doanh hoặc không đủ tồn kho không thể đặt.

Tạo đơn, trừ tồn kho, ghi lịch sử và xóa giỏ hàng thực hiện trong cùng transaction. Khóa tài khoản tuần tự hóa thay đổi giỏ hàng/đặt đơn. Khóa hàng tồn kho theo thứ tự variant và ID ngăn khách mua đồng thời làm âm tồn kho. Chỉ sử dụng tồn kho của chi nhánh đang hoạt động, đúng shop của sản phẩm. Nếu cần nhiều chi nhánh, số lượng được trừ lần lượt; đơn shop chưa được phân công một chi nhánh giao hàng cụ thể.

Token đặt hàng do trang checkout cấp, kiểm tra theo session và lưu theo tài khoản. Gửi lại cùng yêu cầu đã thành công trả về đơn cũ. Form POST giữ bảo vệ CSRF; các nội dung người dùng/sản phẩm được escape trong JSP.

## Database

Chạy [sql/cod-checkout.sql](sql/cod-checkout.sql) bằng tài khoản database owner trước khi chạy phiên bản này. Script thêm metadata COD, chỉ mục token và quyền tối thiểu cho `h2t_user`. Đã áp dụng cho `H2T_DB_TEAM_TRIAL_20260917` ngày 15/10/2026.

Để thử giỏ hàng/COD với áo thun, [sql/sample-crew-neck-stock.sql](sql/sample-crew-neck-stock.sql) tạo chi nhánh mô phỏng `DEMO-h2t` và bổ sung 10 chiếc mỗi size/màu cho 10 sản phẩm áo thun. Đã áp dụng ngày 15/10/2026 và kiểm tra thêm giỏ hàng thành công trên SQL Server. Đây là tồn kho mẫu cục bộ; script chỉ thêm dòng tồn kho còn thiếu, không đặt lại số lượng đã thay đổi bởi đơn hàng.

## Kiểm tra

`CheckoutServiceTests` và `CheckoutHttpTests` kiểm tra lưu giá tại thời điểm đặt, tách đơn nhiều shop, quyền sở hữu, CSRF, validation, giá đổi rollback, thiếu tồn kho và hai khách đặt đồng thời. Kiểm tra HTTP thực tế trên SQL Server local đã xác nhận lưu đơn, trừ tồn kho, xem đơn và chống gửi lặp. Toàn bộ tài khoản/sản phẩm/đơn kiểm thử SQL Server đã được xóa sau khi kiểm tra.

## Khách hàng hủy đơn chờ duyệt

Khách có thể hủy ở danh sách hoặc chi tiết đơn hàng. Chỉ phần đơn có trạng thái NEW (Chờ xác nhận) được hủy.
Nếu đơn mua nhiều shop, phần đã duyệt/giao giữ nguyên; tổng tiền chỉ trừ phần bị hủy.
Tồn kho được hoàn về đúng chi nhánh, chỉ một lần. Đơn hủy toàn bộ có thanh toán CANCELLED.
Hủy và duyệt dùng chung khóa đơn cha, tránh hủy đồng thời với xác nhận.
POST yêu cầu đăng nhập, đúng chủ đơn và CSRF; lý do hủy không bắt buộc, tối đa 1000 ký tự.

Thông báo được lưu trong lịch sử trạng thái với nội dung “Khách hàng hủy đơn”.
Admin thấy tối đa 20 thông báo mới nhất ở trang tổng quan và danh sách đơn.
Chủ shop thấy thông báo thuộc shop mình tại /seller/orders; mở chi tiết để xem lịch sử và lý do hủy.
Thông báo hiển thị khi tải lại trang, không gửi email hay thông báo đẩy.
Không cần thay đổi schema database.
