package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class VerifyOtpRequest {

    @NotBlank(message = "Mã OTP không được để trống")
    @Pattern(
            regexp = "^\\d{6}$",
            message = "Mã OTP phải gồm đúng 6 chữ số")
    private String otp;

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}