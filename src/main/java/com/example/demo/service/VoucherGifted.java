package com.example.demo.service;

import java.math.BigDecimal;

/** Email snapshot captured while the issued voucher and recipient are available. */
public record VoucherGifted(Long voucherId, String email, String customerName, String campaignName,
        String code, String discountType, BigDecimal discountValue, BigDecimal minimumTotal,
        BigDecimal maximumDiscount, String expiryLabel) {}
