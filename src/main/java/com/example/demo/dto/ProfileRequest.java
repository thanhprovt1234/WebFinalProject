package com.example.demo.dto;

import jakarta.validation.constraints.*;

public class ProfileRequest {
    @NotBlank(message = "Vui lòng nhập họ tên.")
    @Size(max = 150, message = "Họ tên tối đa 150 ký tự.")
    private String fullName;
    @Pattern(regexp = "^$|\\+?[0-9][0-9 .()-]{7,19}", message = "Số điện thoại không hợp lệ (8–20 ký tự).")
    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự.")
    private String phone;

    public String getFullName() { return fullName; }
    public void setFullName(String value) { fullName = value == null ? null : value.strip(); }
    public String getPhone() { return phone; }
    public void setPhone(String value) { phone = value == null ? null : value.strip(); }
}
