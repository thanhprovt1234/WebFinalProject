# Danh mục Áo Thun Cổ Tròn

## Trang chi tiết

Nhấn ảnh hoặc tên sản phẩm để mở `/products/{slug}` (ví dụ `/products/ao-thun-01`). Slug giữ nguyên sau khi chuyển sang database. Sản phẩm không tồn tại hoặc ngừng bán trả về HTTP 404.

Trang chi tiết lấy sản phẩm và phiên bản màu/size từ database. Khi chọn phiên bản, giá và tồn kho cập nhật tương ứng. Giỏ hàng kiểm tra tồn kho phía server. Đánh giá, yêu thích và lịch sử xem được lưu theo tài khoản.

Giỏ hàng lưu trong database theo tài khoản, gộp theo sản phẩm + size, hỗ trợ đổi số lượng và xóa mặt hàng. Giá và tổng tiền lấy từ danh mục phía server. Người dùng cần đăng nhập; chưa có đặt hàng/thanh toán hoặc giữ tồn kho. Xem [giỏ hàng database](database-cart.md) để biết API và cách tạo bảng/cấp quyền.

Đã kiểm tra Chrome 1440px/390px: mở chi tiết từ danh sách, phóng to, bắt buộc chọn size, thêm/xóa giỏ, tổng tiền, đổi màu và giữ giỏ. Kiểm thử tích hợp kiểm tra đủ 8 trang chi tiết, nhóm màu, gợi ý và HTTP 404.

## Danh sách

- Truy cập `/collections/ao-thun-co-tron` hoặc chọn **Danh mục → Áo Thun Cổ Tròn**. Không yêu cầu đăng nhập.
- 8 sản phẩm cũ đã được nhập vào database. Danh mục chung `/collections` hỗ trợ thêm các sản phẩm khác theo dữ liệu thực tế.
- Sắp xếp bằng `?sort=price-asc` hoặc `?sort=price-desc`. Giá trị không hợp lệ trở về thứ tự nổi bật.
- Tìm tên sản phẩm bằng ô tìm kiếm (`?q=...`), hỗ trợ chữ hoa/thường và tiếng Việt không dấu. Nhấn Enter hoặc **Áp dụng** để tìm và sắp xếp cùng lúc; xóa từ khóa rồi áp dụng để xem lại tất cả. Số lượng phản ánh kết quả tìm kiếm, có thông báo khi không có kết quả.
- Tên, ảnh, giá và phiên bản được đọc từ database; sửa file TSV không còn thay đổi danh mục chạy thực tế.
- Ảnh nằm trong `src/main/webapp/images/products/ao-thun-co-tron/`. File `SOURCES.md` trong thư mục này ghi nguồn từng ảnh.

Nguồn 8 sản phẩm ban đầu là danh mục H2T Store tham khảo ngày 2026-09-29. Hiện đã chuyển sang database, lọc theo shop/danh mục và phân trang 20 sản phẩm. Xem [database-catalog.md](database-catalog.md) để biết migration và cách tính tồn kho. Chưa có đặt hàng/thanh toán.

Kiểm tra: `./mvnw.cmd test`. `CatalogControllerTests` kiểm tra truy cập ẩn danh, JSP hiển thị giá VNĐ, tải đủ ảnh cục bộ và sắp xếp giá. Đã kiểm tra thao tác menu và sắp xếp trên Chrome ở chiều rộng 1440px / 390px, không tràn ngang.
