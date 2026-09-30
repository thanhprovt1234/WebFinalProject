package com.example.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "cart_items", uniqueConstraints = @UniqueConstraint(name = "uk_cart_items_user_product_size",
        columnNames = {"user_id", "catalog_product_id", "size"}), check = {
        @CheckConstraint(name = "ck_cart_items_quantity", constraint = "quantity between 1 and 99"),
        @CheckConstraint(name = "ck_cart_items_size", constraint = "size in ('S','M','L','XL','XXL')")})
public class CartItem extends AuditableEntity {
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank @Size(max = 100)
    @Column(name = "catalog_product_id", nullable = false, length = 100)
    private String catalogProductId;

    @NotNull @Pattern(regexp = "S|M|L|XL|XXL")
    @Column(nullable = false, length = 5)
    private String size;

    @Min(1) @Max(99)
    @Column(nullable = false)
    private int quantity;

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getCatalogProductId() { return catalogProductId; }
    public void setCatalogProductId(String value) { catalogProductId = value; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
