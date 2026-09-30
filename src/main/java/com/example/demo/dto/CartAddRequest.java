package com.example.demo.dto;

import jakarta.validation.constraints.*;

public class CartAddRequest extends CartQuantityRequest {
    @NotBlank @Size(max = 100)
    private String productId;
    @NotNull @Pattern(regexp = "S|M|L|XL|XXL")
    private String size;

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
}
