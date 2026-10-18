package com.example.demo.dto;

import com.example.demo.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public final class AdminForms {
    private AdminForms() {}
    private static String clean(String s) { return s==null?null:s.strip(); }
    public record ShopForm(@NotBlank @Size(max=150) String name,
            @NotBlank @Pattern(regexp="[a-z0-9]+(?:-[a-z0-9]+)*") @Size(max=200) String slug,
            @NotNull Long ownerId,@Size(max=2000) String description,@Size(max=20) String phone,
            @Email @Size(max=254) String email,@NotNull ShopStatus status,boolean enabled) {
        public ShopForm { name=clean(name); slug=clean(slug); description=clean(description); phone=clean(phone); email=clean(email); }
    }
    public record ProductForm(@NotNull Long shopId,@NotNull Long categoryId,
            @NotBlank @Size(max=200) String name,
            @NotBlank @Pattern(regexp="[a-z0-9]+(?:-[a-z0-9]+)*") @Size(max=200) String slug,
            @NotNull @DecimalMin("0.00") @Digits(integer=17,fraction=2) BigDecimal price,
            @NotBlank @Size(max=2048) @Pattern(regexp="(?:/images/|https://)[^\\s]+") String image,
            @Size(max=4000) String description,boolean enabled) {
        public ProductForm { name=clean(name); slug=clean(slug); image=clean(image); description=clean(description); }
    }
    public record VariantForm(@NotBlank @Size(max=64) String sku,@NotBlank @Size(max=10) String size,
            @NotBlank @Size(max=50) String color,@DecimalMin("0.00") @Digits(integer=17,fraction=2) BigDecimal price,boolean enabled) {
        public VariantForm { sku=clean(sku); size=clean(size); color=clean(color); }
    }
    public record StoreForm(@NotBlank @Size(max=50) String code,@NotBlank @Size(max=150) String name,
            @NotBlank @Size(max=500) String address,boolean enabled) {
        public StoreForm { code=clean(code); name=clean(name); address=clean(address); }
    }
    public record StockForm(@NotNull Long variantId,@NotNull Long storeId,@NotNull @Min(0) @Max(1000000) Integer quantity) {}
    public record StatusForm(@NotNull OrderStatus status,@Size(max=2000) String note) {
        public StatusForm { note=clean(note); }
    }
    public record CategoryForm(@NotBlank @Size(max=100) String name,boolean enabled) {
        public CategoryForm { name=clean(name); }
    }
}
