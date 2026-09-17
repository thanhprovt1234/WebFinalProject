package com.example.demo.controller;

import java.util.Locale;

import com.example.demo.dto.ForgotPasswordRequest;
import com.example.demo.service.PasswordResetService;
import com.example.demo.dto.VerifyOtpRequest;
import com.example.demo.dto.ResetPasswordRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class PasswordResetController {

        private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetController.class);
        private static final String RESET_EMAIL_SESSION_ATTRIBUTE = "passwordResetEmail";
        private static final String RESET_VERIFIED_SESSION_ATTRIBUTE = "passwordResetVerified";

        private final PasswordResetService passwordResetService;

        public PasswordResetController(PasswordResetService passwordResetService) {
                this.passwordResetService = passwordResetService;
        }

        @GetMapping("/forgot-password")
        public String showForgotPasswordForm(Model model) {
                model.addAttribute(
                                "forgotPasswordRequest",
                                new ForgotPasswordRequest());
                return "auth/forgot-password";
        }

        @PostMapping("/forgot-password")
        public String requestPasswordReset(
                        @Valid @ModelAttribute("forgotPasswordRequest") ForgotPasswordRequest request,
                        BindingResult bindingResult,
                        HttpSession session) {

                if (bindingResult.hasErrors()) {
                        return "auth/forgot-password";
                }

                String normalizedEmail = request.getEmail()
                                .trim()
                                .toLowerCase(Locale.ROOT);

                try {
                        passwordResetService.requestPasswordReset(normalizedEmail);
                } catch (MailException exception) {
                        LOGGER.error("Could not send password reset OTP", exception);
                        bindingResult.reject(
                                        "passwordReset.emailDeliveryFailed",
                                        "Không thể gửi email lúc này. Vui lòng thử lại sau.");
                        return "auth/forgot-password";
                }

                session.setAttribute(
                                RESET_EMAIL_SESSION_ATTRIBUTE,
                                normalizedEmail);

                session.removeAttribute(RESET_VERIFIED_SESSION_ATTRIBUTE);
                return "redirect:/verify-otp?sent";
        }

        @GetMapping("/verify-otp")
        public String showVerifyOtpForm(
                        Model model,
                        HttpSession session) {

                if (session.getAttribute(RESET_EMAIL_SESSION_ATTRIBUTE) == null) {
                        return "redirect:/forgot-password";
                }

                model.addAttribute("verifyOtpRequest", new VerifyOtpRequest());
                return "auth/verify-otp";
        }

        @PostMapping("/verify-otp")
        public String verifyOtp(
                        @Valid @ModelAttribute("verifyOtpRequest") VerifyOtpRequest request,
                        BindingResult bindingResult,
                        HttpSession session) {

                Object emailAttribute = session.getAttribute(RESET_EMAIL_SESSION_ATTRIBUTE);

                if (!(emailAttribute instanceof String email)) {
                        return "redirect:/forgot-password";
                }

                if (bindingResult.hasErrors()) {
                        return "auth/verify-otp";
                }

                boolean verified = passwordResetService.verifyOtp(email, request.getOtp());

                if (!verified) {
                        bindingResult.reject(
                                        "passwordReset.invalidOtp",
                                        "Mã OTP không đúng, đã hết hạn hoặc đã vượt quá số lần thử.");
                        return "auth/verify-otp";
                }

                session.setAttribute(
                                RESET_VERIFIED_SESSION_ATTRIBUTE,
                                Boolean.TRUE);

                return "redirect:/reset-password";
        }

        @GetMapping("/reset-password")
        public String showResetPasswordForm(
                        Model model,
                        HttpSession session) {

                boolean verified = Boolean.TRUE.equals(
                                session.getAttribute(RESET_VERIFIED_SESSION_ATTRIBUTE));

                if (session.getAttribute(RESET_EMAIL_SESSION_ATTRIBUTE) == null
                                || !verified) {
                        return "redirect:/forgot-password";
                }

                model.addAttribute(
                                "resetPasswordRequest",
                                new ResetPasswordRequest());

                return "auth/reset-password";
        }

        @PostMapping("/reset-password")
        public String resetPassword(
                        @Valid @ModelAttribute("resetPasswordRequest") ResetPasswordRequest request,
                        BindingResult bindingResult,
                        HttpSession session) {

                Object emailAttribute = session.getAttribute(RESET_EMAIL_SESSION_ATTRIBUTE);

                boolean verified = Boolean.TRUE.equals(
                                session.getAttribute(RESET_VERIFIED_SESSION_ATTRIBUTE));

                if (!(emailAttribute instanceof String email) || !verified) {
                        return "redirect:/forgot-password";
                }

                if (bindingResult.hasErrors()) {
                        return "auth/reset-password";
                }

                boolean passwordChanged = passwordResetService.resetPassword(
                                email,
                                request.getNewPassword());

                if (!passwordChanged) {
                        session.removeAttribute(RESET_EMAIL_SESSION_ATTRIBUTE);
                        session.removeAttribute(RESET_VERIFIED_SESSION_ATTRIBUTE);

                        bindingResult.reject(
                                        "passwordReset.expired",
                                        "Phiên đặt lại mật khẩu đã hết hạn. "
                                                        + "Vui lòng yêu cầu mã OTP mới.");

                        return "auth/reset-password";
                }

                session.removeAttribute(RESET_EMAIL_SESSION_ATTRIBUTE);
                session.removeAttribute(RESET_VERIFIED_SESSION_ATTRIBUTE);

                return "redirect:/login?resetSuccess";
        }
}
