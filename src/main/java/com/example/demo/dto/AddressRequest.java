package com.example.demo.dto;

import jakarta.validation.constraints.*;

public class AddressRequest {
    @NotBlank(message = "Vui lòng nhập tên người nhận.") @Size(max = 150)
    private String receiverName;
    @NotBlank(message = "Vui lòng nhập số điện thoại.")
    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự.")
    @Pattern(regexp = "\\+?[0-9][0-9 .()-]{7,19}", message = "Số điện thoại không hợp lệ (8–20 ký tự).")
    private String phone;
    @NotBlank(message = "Vui lòng nhập số nhà và tên đường.") @Size(max = 500)
    private String addressLine;
    @NotBlank(message = "Vui lòng nhập phường/xã.") @Size(max = 100)
    private String ward;
    @NotBlank(message = "Vui lòng nhập quận/huyện hoặc khu vực.") @Size(max = 100)
    private String district;
    @NotBlank(message = "Vui lòng nhập tỉnh/thành phố.") @Size(max = 100)
    private String city;
    private boolean defaultAddress;

    private String clean(String value) { return value == null ? null : value.strip(); }
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String value) { receiverName = clean(value); }
    public String getPhone() { return phone; }
    public void setPhone(String value) { phone = clean(value); }
    public String getAddressLine() { return addressLine; }
    public void setAddressLine(String value) { addressLine = clean(value); }
    public String getWard() { return ward; }
    public void setWard(String value) { ward = clean(value); }
    public String getDistrict() { return district; }
    public void setDistrict(String value) { district = clean(value); }
    public String getCity() { return city; }
    public void setCity(String value) { city = clean(value); }
    public boolean isDefaultAddress() { return defaultAddress; }
    public void setDefaultAddress(boolean value) { defaultAddress = value; }
}
