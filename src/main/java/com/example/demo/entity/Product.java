package com.example.demo.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "products", uniqueConstraints = {
        @UniqueConstraint(name = "uk_products_slug", columnNames = {"slug"})
}, check = @CheckConstraint(name = "ck_products_values", constraint = "base_price >= 0"))
public class Product extends AuditableEntity {
    @Column(name = "search_name", nullable = false, length = 200)
    private String searchName = "";

    @jakarta.persistence.PrePersist
    @jakarta.persistence.PreUpdate
    private void indexName() { searchName = normalize(name == null ? "" : name); }

    public static String normalize(String value) {
        return java.text.Normalizer.normalize(value.strip().toLowerCase(java.util.Locale.ROOT), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd').replaceAll("\\s+", " ");
    }

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    public Shop getShop() {
        return shop;
    }

    public void setShop(Shop shop) {
        this.shop = shop;
    }

    @NotBlank
    @Size(max = 200)
    @Nationalized
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @NotBlank
    @Size(max = 200)
    @Column(name = "slug", nullable = false, length = 200)
    private String slug;

    @Size(max = 4000)
    @Nationalized
    @Column(name = "description", length = 4000)
    private String description;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 17, fraction = 2)
    @Column(name = "base_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal basePrice;

    @Size(max = 100)
    @Nationalized
    @Column(name = "material", length = 100)
    private String material;

    @Size(max = 2048)
    @Column(name = "image_url", length = 2048)
    private String imageUrl;

    @Column(nullable = false)
    private boolean enabled = true;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<ProductVariant> variants = new ArrayList<>();

    public List<ProductVariant> getVariants() {
        return Collections.unmodifiableList(variants);
    }

    // Called by the owning side to keep both sides consistent.
    void attachVariant(ProductVariant variant) {
        if (!variants.contains(variant)) {
            variants.add(variant);
        }
    }

    void detachVariant(ProductVariant variant) {
        variants.remove(variant);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }
}
