package com.example.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_activities", uniqueConstraints = @UniqueConstraint(
        name = "uk_product_activities_user_product", columnNames = {"user_id", "catalog_product_id"}))
public class ProductActivity extends AuditableEntity {
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @NotBlank @Size(max = 200)
    @Column(name = "catalog_product_id", nullable = false, length = 200)
    private String catalogProductId;
    @Column(nullable = false)
    private boolean favorite;
    @Column(name = "favorited_at")
    private LocalDateTime favoritedAt;
    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getCatalogProductId() { return catalogProductId; }
    public void setCatalogProductId(String id) { catalogProductId = id; }
    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean value) { favorite = value; }
    public LocalDateTime getFavoritedAt() { return favoritedAt; }
    public void setFavoritedAt(LocalDateTime value) { favoritedAt = value; }
    public LocalDateTime getViewedAt() { return viewedAt; }
    public void setViewedAt(LocalDateTime value) { viewedAt = value; }
}
