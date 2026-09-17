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
}