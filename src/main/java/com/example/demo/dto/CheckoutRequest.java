package com.example.demo.dto;

import jakarta.validation.constraints.*;

public class CheckoutRequest {
    @NotBlank @Size(max=150)
    private String receiverName;
    @NotBlank @Pattern(regexp="\\+?[0-9][0-9 .()-]{7,19}") @Size(max=20)
    private String receiverPhone;
    @NotBlank @Size(max=500)
    private String shippingAddress;
    @NotBlank @Pattern(regexp="[0-9a-fA-F-]{36}")
    private String token;
    @NotNull @DecimalMin("0.00")
    private java.math.BigDecimal expectedTotal;
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String s) { receiverName=s==null?null:s.strip(); }
    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String s) { receiverPhone=s==null?null:s.strip(); }
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String s) { shippingAddress=s==null?null:s.strip(); }
    public String getToken() { return token; }
    public void setToken(String s) { token=s; }
    public java.math.BigDecimal getExpectedTotal() { return expectedTotal; }
    public void setExpectedTotal(java.math.BigDecimal value) { expectedTotal=value; }
}
