# Danh mục Áo Thun Cổ Tròn

## Trang chi tiết

Nhấn ảnh hoặc tên của bất kỳ mặt hàng nào để mở `/products/{id}` (ví dụ `/products/ao-thun-01`). ID dựa trên tên file ảnh nên không thay đổi khi sắp xếp hoặc tìm kiếm. ID không tồn tại trả về HTTP 404.

Trang chi tiết gồm ảnh đầy đủ/phóng to từ ảnh hiện có, các màu thuộc cùng mẫu, giá, chọn size bắt buộc, số lượng 1–99, sản phẩm gợi ý và phần đánh giá chưa có dữ liệu. Không tạo ảnh góc chụp hoặc đánh giá giả. Các size là lựa chọn giao diện, chưa kiểm tra tồn kho.

Giỏ hàng lưu trong database theo tài khoản, gộp theo sản phẩm + size, hỗ trợ đổi số lượng và xóa mặt hàng. Giá và tổng tiền lấy từ danh mục phía server. Người dùng cần đăng nhập; chưa có đặt hàng/thanh toán hoặc giữ tồn kho. Xem [giỏ hàng database](database-cart.md) để biết API và cách tạo bảng/cấp quyền.

Đã kiểm tra Chrome 1440px/390px: mở chi tiết từ danh sách, phóng to, bắt buộc chọn size, thêm/xóa giỏ, tổng tiền, đổi màu và giữ giỏ. Kiểm thử tích hợp kiểm tra đủ 8 trang chi tiết, nhóm màu, gợi ý và HTTP 404.

## Danh sách

- Truy cập `/collections/ao-thun-co-tron` hoặc chọn **ÁO → Áo Thun Cổ Tròn**. Không yêu cầu đăng nhập.
- Hiển thị 8 mẫu màu áo với ảnh cục bộ, tên, giá gốc và giá giảm; 4 cột desktop, 3 cột tablet, 2 cột điện thoại.
- Sắp xếp bằng `?sort=price-asc` hoặc `?sort=price-desc`. Giá trị không hợp lệ trở về thứ tự nổi bật.
- Tìm tên sản phẩm bằng ô tìm kiếm (`?q=...`), hỗ trợ chữ hoa/thường và tiếng Việt không dấu. Nhấn Enter hoặc **Áp dụng** để tìm và sắp xếp cùng lúc; xóa từ khóa rồi áp dụng để xem lại tất cả. Số lượng phản ánh kết quả tìm kiếm, có thông báo khi không có kết quả.
- Sửa tên, ảnh và giá trong `src/main/resources/catalog/crew-neck.tsv` (UTF-8, các cột phân cách bằng tab), sau đó build/chạy lại ứng dụng.
- Ảnh nằm trong `src/main/webapp/images/products/ao-thun-co-tron/`. File `SOURCES.md` trong thư mục này ghi nguồn từng ảnh.

Đây là dữ liệu mẫu phục vụ giao diện danh mục, lấy từ H2T Store ngày 2026-09-29; chưa liên kết với sản phẩm, tồn kho hoặc thanh toán trong database. Nhãn miễn phí ship dùng điều kiện đơn từ 500.000 VNĐ của nguồn tham khảo. Những mục danh mục khác chưa được triển khai trong thay đổi này.

Kiểm tra: `./mvnw.cmd test`. `CatalogControllerTests` kiểm tra truy cập ẩn danh, JSP hiển thị giá VNĐ, tải đủ ảnh cục bộ và sắp xếp giá. Đã kiểm tra thao tác menu và sắp xếp trên Chrome ở chiều rộng 1440px / 390px, không tràn ngang.
