package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String senderEmail;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username}") String senderEmail) {
        this.mailSender = mailSender;
        this.senderEmail = senderEmail;
    }

    public void sendPasswordResetOtp(String recipientEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(senderEmail);
        message.setTo(recipientEmail);
        message.setSubject("Mã OTP đặt lại mật khẩu H2T");
        message.setText("""
                Xin chào,

                Mã OTP đặt lại mật khẩu của bạn là: %s

                Mã này có hiệu lực trong 5 phút.
                Không cung cấp mã này cho bất kỳ ai.

                H2T
                """.formatted(otp));

        mailSender.send(message);
    }

    public void sendVoucherGifted(VoucherGifted event) {
        var number = java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("vi-VN"));
        var body = new StringBuilder("Xin chào ").append(event.customerName()).append(",\n\n")
                .append("Bạn vừa nhận được một voucher quà tặng từ H2T!\n")
                .append("Chương trình: ").append(event.campaignName()).append('\n')
                .append("Mã voucher: ").append(event.code()).append('\n')
                .append("Ưu đãi: Giảm ").append(number.format(event.discountValue()))
                .append("PERCENT".equals(event.discountType()) ? "%" : " VNĐ").append('\n')
                .append("Áp dụng cho đơn hàng từ: ").append(number.format(event.minimumTotal())).append(" VNĐ\n");
        if (event.maximumDiscount() != null) {
            body.append("Giảm tối đa: ").append(number.format(event.maximumDiscount())).append(" VNĐ\n");
        }
        body.append("Hạn sử dụng: ").append(event.expiryLabel()).append(" (giờ Việt Nam)\n\n")
                .append("Đăng nhập tài khoản được tặng để xem mã tại Đơn hàng của tôi → Voucher của tôi.\n")
                .append("Nhập mã và chọn Áp dụng mã khi thanh toán. Mỗi mã dùng một lần, chỉ dành cho tài khoản được tặng.\n\n")
                .append("Chúc bạn mua sắm vui vẻ!\nH2T Menswear");
        var message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(event.email());
        message.setSubject("H2T - Bạn vừa nhận được voucher quà tặng!");
        message.setText(body.toString());
        mailSender.send(message);
    }

    public void sendPurchaseCompleted(PurchaseCompleted event) {
        var order = event.order();
        var currency = java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag("vi-VN"));
        var body = new StringBuilder("Xin chào ").append(event.customerName()).append(",\n\n")
                .append("Đơn hàng #").append(order.id()).append(" của bạn đã được xác nhận mua hàng thành công.\n")
                .append("Ngày đặt: ").append(order.placedAt()).append("\n\n");
        for (var shop : order.shops()) {
            if (!"DELIVERED".equals(shop.status())) continue;
            body.append("Shop: ").append(shop.name()).append('\n');
            for (var line : shop.items()) {
                body.append("- ").append(line.name()).append(" | ").append(line.color())
                        .append(" | Size ").append(line.size()).append(" | SL: ").append(line.quantity())
                        .append(" | ").append(currency.format(line.subtotal())).append(" VNĐ\n");
            }
        }
        body.append("\nTổng tiền: ").append(currency.format(order.total())).append(" VNĐ\n")
                .append("Phương thức thanh toán: ").append(order.paymentMethod()).append('\n')
                .append("Người nhận: ").append(order.receiverName()).append('\n')
                .append("Điện thoại: ").append(order.receiverPhone()).append('\n')
                .append("Địa chỉ: ").append(order.shippingAddress()).append("\n\n")
                .append("Cảm ơn bạn đã mua sắm tại H2T!\nH2T Menswear");
        var message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(event.email());
        message.setSubject("H2T - Mua hàng thành công - Đơn hàng #" + order.id());
        message.setText(body.toString());
        mailSender.send(message);
    }
}
