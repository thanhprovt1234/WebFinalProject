package com.example.demo.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResetPasswordRequest {

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 8, max = 72, message = "Mật khẩu phải từ 8 đến 72 ký tự")
    private String newPassword;

    @NotBlank(message = "Bạn phải nhập lại mật khẩu")
    private String confirmPassword;

    @AssertTrue(message = "Mật khẩu xác nhận không khớp")
    public boolean isPasswordMatching() {
        if (newPassword == null || confirmPassword == null) {
            return true;
        }

        return newPassword.equals(confirmPassword);
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}