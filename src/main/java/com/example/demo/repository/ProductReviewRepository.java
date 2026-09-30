package com.example.demo.repository;

import com.example.demo.entity.ProductReview;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
    @EntityGraph(attributePaths = "user")
    List<ProductReview> findByCatalogProductIdOrderByUpdatedAtDescIdDesc(String productId);

    Optional<ProductReview> findByCatalogProductIdAndUserId(String productId, Long userId);
}
