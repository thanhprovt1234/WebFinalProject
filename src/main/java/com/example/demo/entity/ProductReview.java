package com.example.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "product_reviews", uniqueConstraints = @UniqueConstraint(
        name = "uk_product_reviews_catalog_user", columnNames = {"catalog_product_id", "user_id"}),
        check = @CheckConstraint(name = "ck_product_reviews_rating", constraint = "rating between 1 and 5"))
public class ProductReview extends AuditableEntity {
    // The display catalog uses stable string IDs, independently of database products.
    @NotBlank
    @Column(name = "catalog_product_id", nullable = false, length = 200)
    private String catalogProductId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Min(1) @Max(5)
    @Column(nullable = false)
    private int rating;

    @NotBlank @Size(max = 2000)
    @Nationalized
    @Column(nullable = false, length = 2000)
    private String comment;

    public String getCatalogProductId() { return catalogProductId; }
    public void setCatalogProductId(String value) { catalogProductId = value; }
    public User getUser() { return user; }
    public void setUser(User value) { user = value; }
    public int getRating() { return rating; }
    public void setRating(int value) { rating = value; }
    public String getComment() { return comment; }
    public void setComment(String value) { comment = value; }
}
