package com.example.demo.dto;

import jakarta.validation.constraints.*;

public class CartQuantityRequest {
    @NotNull @Min(1) @Max(99)
    private Integer quantity;

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
