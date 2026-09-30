package com.example.demo.repository;

import com.example.demo.entity.ProductActivity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductActivityRepository extends JpaRepository<ProductActivity, Long> {
    Optional<ProductActivity> findByUserIdAndCatalogProductId(Long userId, String productId);
    Page<ProductActivity> findByUserIdAndFavoriteTrueOrderByFavoritedAtDescIdDesc(Long userId, Pageable pageable);
    Page<ProductActivity> findByUserIdAndViewedAtIsNotNullOrderByViewedAtDescIdDesc(Long userId, Pageable pageable);
}
