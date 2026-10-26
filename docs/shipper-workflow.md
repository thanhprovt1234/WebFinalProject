# Giao hàng và thống kê shipper

Chạy `docs/sql/shipper-workflow.sql` bằng tài khoản chủ database trước khi chạy phiên bản mới trên SQL Server. Script bổ sung lý do giao thất bại, số COD thực thu, cập nhật CHECK trạng thái và quyền ghi shipments. Hibernate không tự cập nhật CHECK cũ, kể cả khi dùng profile dev.

1. Khách đặt hàng: `ShopOrder.NEW`.
2. Seller/Admin xác nhận: `CONFIRMED`, chưa thu COD và chưa thưởng loyalty.
3. Admin mở `/admin/orders/{id}`, chọn shipper đang hoạt động và phân công. Mỗi đơn shop có tối đa một shipment.
4. Shipper vào `/shipper`, mở chi tiết, xác nhận nhận đủ hàng rồi bắt đầu giao: `PICKED_UP` → `SHIPPING`.
5. Giao thành công: xác nhận thu đủ COD nếu có. Shipment và đơn shop chuyển `DELIVERED` trong cùng transaction. Khi toàn bộ phần đơn đã giao/hủy, cập nhật COD và xử lý thông báo/loyalty hiện có đúng một lần.
6. Giao chưa thành công: bắt buộc ghi lý do, shipment `FAILED`, đơn shop giữ `SHIPPING`. Shipper có thể bấm giao lại. Nếu hoàn hàng, Admin chỉ chọn hủy/hoàn kho sau khi đã nhận lại đủ hàng; shipment chuyển `CANCELLED`, tồn kho được hoàn đúng một lần.

Trước khi shipper lấy hàng, Seller/Admin vẫn có thể hủy với lý do; shipment đã phân công cũng được đóng. Khách chỉ tự hủy phần đơn còn `NEW`. Không cho Seller/Admin đánh dấu giao thành công qua biểu mẫu trạng thái đơn cũ.

`/shipper/statistics` mặc định tháng hiện tại, hỗ trợ hôm nay hoặc khoảng ngày. Thống kê theo **ngày phân công**, dùng múi giờ Việt Nam và bao gồm cả hai ngày đầu/cuối. Các ô thống kê và biểu đồ dùng toàn bộ nhóm đơn trong kỳ, không thay đổi theo tìm kiếm/trạng thái của danh sách. Tỷ lệ = số đơn hiện đã giao thành công / tổng đơn được phân công trong kỳ (bao gồm đơn đã hủy). COD đã thu tính từ các đơn COD giao thành công của nhóm đó; không phải tiền lương hoặc xác nhận đã nộp tiền về shop. Biểu đồ nhóm theo ngày phân công, không phải ngày hoàn tất.

Shipper chỉ xem/sửa/thống kê đơn được giao cho tài khoản mình. Admin có quyền xem toàn bộ và hỗ trợ cập nhật. Danh sách có tìm kiếm, lọc, phân trang 20 đơn; chi tiết hiển thị kho lấy hàng thực tế từ phân bổ tồn kho, sản phẩm, người nhận, bản đồ và lịch sử. Đơn lấy từ nhiều kho cần nhận đủ hàng trước khi xác nhận đã lấy.

Phiên bản này chưa có tự động phân công, thay shipper, định vị GPS hoặc ảnh giao hàng. Đơn cũ đã `DELIVERED` được giữ nguyên.

Số COD thực thu được lưu khi giao thành công, độc lập với trạng thái thanh toán của đơn cha về sau. Đơn đã thanh toán trước có COD cần thu bằng 0 kể cả nếu phương thức ban đầu là COD. Với shipment COD đã hoàn tất từ phiên bản cũ, thống kê dùng giá trị đơn shop khi chưa có số thu hộ được lưu riêng.

## Bàn giao và shop xác nhận nhận COD

Chạy lại `docs/sql/shipper-workflow.sql` khi cập nhật phiên bản đối soát để thêm các cột thời điểm bàn giao, xác nhận, ghi chú và người xác nhận. Script không tự xác nhận tiền cho đơn cũ.

1. Shipper giao thành công và thu đủ COD. Đơn khách được ghi nhận đã thanh toán; email/loyalty vẫn xử lý khi giao hàng hoàn tất như trước.
2. Sau khi thực tế trả tiền mặt hoặc chuyển khoản cho shop, shipper mở chi tiết đơn, chọn **Đã bàn giao COD · Gửi shop xác nhận**, đánh dấu đã bàn giao đủ tiền, có thể ghi người nhận hoặc mã giao dịch. Trạng thái đối soát chuyển **Chờ shop xác nhận**.
3. Seller mở **Đơn hàng của shop → Chi tiết → Đối soát tiền COD**, kiểm tra tiền thực nhận rồi chọn **Xác nhận shop đã nhận đủ COD**. Chỉ chủ shop của phần đơn đó hoặc Admin được xác nhận.
4. Shipper thấy trạng thái shop đã nhận COD cùng thời điểm và ghi chú. Thống kê có số tiền chờ bàn giao, đã bàn giao/chờ xác nhận và shop đã xác nhận, theo nhóm đơn trong kỳ phân công đang lọc.

Mỗi lần bàn giao là toàn bộ số COD thực thu của một đơn shop, chưa hỗ trợ nộp một phần hoặc gộp nhiều đơn. Hệ thống ghi nhận đối soát, không tự chuyển tiền ngân hàng. Không cho bàn giao trước khi giao thành công, xác nhận trước khi bàn giao, đổi số tiền hoặc cập nhật trùng lần hai. Thao tác đối soát không thay đổi giá trị đơn, tồn kho, trạng thái khách thanh toán và không gửi lại email/voucher.

Với đơn cũ không có số COD thực thu lưu riêng, cần đối soát ngoài hệ thống trước; không tự suy ra số tiền đã bàn giao/nhận từ trạng thái giao hàng. Đơn trả trước hoặc COD bằng 0 không có bước đối soát tiền thu hộ.
