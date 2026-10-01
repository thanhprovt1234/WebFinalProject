package com.example.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "cart_items", uniqueConstraints = @UniqueConstraint(name = "uk_cart_items_user_product_option",
        columnNames = {"user_id", "catalog_product_id", "size", "color"}), check = {
        @CheckConstraint(name = "ck_cart_items_quantity", constraint = "quantity between 1 and 99")})
public class CartItem extends AuditableEntity {
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank @Size(max = 200)
    @Column(name = "catalog_product_id", nullable = false, length = 200)
    private String catalogProductId;

    @NotBlank @Size(max = 10)
    @Column(nullable = false, length = 10)
    private String size;

    @NotNull @Size(max = 50)
    @org.hibernate.annotations.Nationalized
    @Column(nullable = false, length = 50)
    private String color = "";
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

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
