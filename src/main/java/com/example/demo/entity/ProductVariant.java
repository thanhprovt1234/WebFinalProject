package com.example.demo.entity;

import java.math.BigDecimal;
import java.util.Objects;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "product_variants", uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_variants_sku", columnNames = {"sku"}),
        @UniqueConstraint(name = "uk_product_variants_options", columnNames = {"product_id", "size", "color"})
}, check = @CheckConstraint(name = "ck_product_variants_values", constraint = "price is null or price >= 0"))
public class ProductVariant extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotBlank
    @Size(max = 64)
    @Column(name = "sku", nullable = false, length = 64)
    private String sku;

    @NotBlank
    @Size(max = 10)
    @Column(name = "size", nullable = false, length = 10)
    private String size;

    @NotBlank
    @Size(max = 50)
    @Nationalized
    @Column(name = "color", nullable = false, length = 50)
    private String color;

    @DecimalMin("0.00")
    @Digits(integer = 17, fraction = 2)
    @Column(name = "price", precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean enabled = true;

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        Objects.requireNonNull(product);
        if (this.product != product) {
            if (this.product != null) {
                this.product.detachVariant(this);
            }
            this.product = product;
            product.attachVariant(this);
        }
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
