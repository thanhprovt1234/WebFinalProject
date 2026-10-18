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
