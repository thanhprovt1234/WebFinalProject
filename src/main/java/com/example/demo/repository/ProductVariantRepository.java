package com.example.demo.repository;

import java.util.Optional;

import com.example.demo.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    Optional<ProductVariant> findBySku(String sku);
    java.util.List<ProductVariant> findByProductIdAndEnabledTrueOrderByIdAsc(Long productId);
}
