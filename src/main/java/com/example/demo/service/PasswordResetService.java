package com.example.demo.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

import com.example.demo.entity.PasswordResetOtp;
import com.example.demo.entity.User;
import com.example.demo.repository.PasswordResetOtpRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {

    private static final int OTP_BOUND = 1_000_000;
    private static final long OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final SecureRandom secureRandom = new SecureRandom();
    private final UserRepository userRepository;
    private final PasswordResetOtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetOtpRepository otpRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public void requestPasswordReset(String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        var userOptional = userRepository.findByEmail(normalizedEmail);

        if (userOptional.isEmpty()) {
            return;
        }

        User user = userOptional.get();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        var activeOtps = otpRepository.findAllByUserAndUsedAtIsNull(user);

        for (PasswordResetOtp activeOtp : activeOtps) {
            activeOtp.setUsedAt(now);
        }

        otpRepository.saveAll(activeOtps);

        String rawOtp = generateOtp();

        PasswordResetOtp resetOtp = new PasswordResetOtp();
        resetOtp.setUser(user);
        resetOtp.setOtpHash(passwordEncoder.encode(rawOtp));
        resetOtp.setExpiresAt(now.plusMinutes(OTP_EXPIRY_MINUTES));
        resetOtp.setFailedAttempts(0);

        otpRepository.save(resetOtp);

        emailService.sendPasswordResetOtp(user.getEmail(), rawOtp);
    }

    @Transactional
    public boolean verifyOtp(String email, String rawOtp) {
        if (email == null || rawOtp == null) {
            return false;
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        var userOptional = userRepository.findByEmail(normalizedEmail);

        if (userOptional.isEmpty()) {
            return false;
        }

        User user = userOptional.get();

        var otpOptional = otpRepository
                .findFirstByUserAndUsedAtIsNullOrderByCreatedAtDesc(user);

        if (otpOptional.isEmpty()) {
            return false;
        }

        PasswordResetOtp resetOtp = otpOptional.get();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        if (!resetOtp.getExpiresAt().isAfter(now)) {
            resetOtp.setUsedAt(now);
            otpRepository.save(resetOtp);
            return false;
        }

        if (resetOtp.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            resetOtp.setUsedAt(now);
            otpRepository.save(resetOtp);
            return false;
        }

        if (!passwordEncoder.matches(rawOtp, resetOtp.getOtpHash())) {
            int failedAttempts = resetOtp.getFailedAttempts() + 1;
            resetOtp.setFailedAttempts(failedAttempts);

            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                resetOtp.setUsedAt(now);
            }

            otpRepository.save(resetOtp);
            return false;
        }

        if (resetOtp.getVerifiedAt() == null) {
            resetOtp.setVerifiedAt(now);
            otpRepository.save(resetOtp);
        }

        return true;
    }

    @Transactional
    public boolean resetPassword(String email, String newPassword) {
        if (email == null
                || newPassword == null
                || newPassword.isBlank()) {
            return false;
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        var userOptional = userRepository.findByEmail(normalizedEmail);

        if (userOptional.isEmpty()) {
            return false;
        }

        User user = userOptional.get();

        var otpOptional = otpRepository
                .findFirstByUserAndUsedAtIsNullOrderByCreatedAtDesc(user);

        if (otpOptional.isEmpty()) {
            return false;
        }

        PasswordResetOtp resetOtp = otpOptional.get();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        if (resetOtp.getVerifiedAt() == null) {
            return false;
        }

        if (!resetOtp.getExpiresAt().isAfter(now)) {
            resetOtp.setUsedAt(now);
            otpRepository.save(resetOtp);
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        var activeOtps = otpRepository.findAllByUserAndUsedAtIsNull(user);

        for (PasswordResetOtp activeOtp : activeOtps) {
            activeOtp.setUsedAt(now);
        }

        otpRepository.saveAll(activeOtps);

        return true;
    }

    private String generateOtp() {
        int value = secureRandom.nextInt(OTP_BOUND);
        return String.format(Locale.ROOT, "%06d", value);
    }
}