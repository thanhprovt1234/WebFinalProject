package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import com.example.demo.entity.PasswordResetOtp;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetOtpRepository
        extends JpaRepository<PasswordResetOtp, Long> {

    Optional<PasswordResetOtp> findFirstByUserAndUsedAtIsNullOrderByCreatedAtDesc(User user);

    List<PasswordResetOtp> findAllByUserAndUsedAtIsNull(User user);
}