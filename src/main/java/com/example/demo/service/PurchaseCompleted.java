package com.example.demo.service;

/** Immutable email data captured before the order transaction closes. */
public record PurchaseCompleted(String email, String customerName, CheckoutService.Receipt order) {}
