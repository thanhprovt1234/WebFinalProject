package com.example.demo.dto;

import jakarta.validation.constraints.*;

public class CartAddRequest extends CartQuantityRequest {
    @NotBlank @Size(max = 200)
    private String productId;
    @NotBlank @Size(max = 10)
    private String size;
    @Size(max = 50)
    private String color;
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
}
