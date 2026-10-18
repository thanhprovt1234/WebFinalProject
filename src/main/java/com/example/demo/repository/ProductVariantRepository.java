package com.example.demo.repository;

import java.util.Optional;

import com.example.demo.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select v from ProductVariant v where v.id=:id")
    Optional<ProductVariant> lockById(@org.springframework.data.repository.query.Param("id") Long id);

    Optional<ProductVariant> findBySku(String sku);
    java.util.List<ProductVariant> findByProductIdAndEnabledTrueOrderByIdAsc(Long productId);
}
