package com.example.demo.repository;

import java.util.Optional;

import com.example.demo.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByStoreIdAndProductVariantId(Long storeId, Long productVariantId);
    @org.springframework.data.jpa.repository.Query("select coalesce(sum(i.quantity),0) from Inventory i where i.productVariant.id = :variantId and i.store.enabled = true and i.store.shop = i.productVariant.product.shop")
    long availableQuantity(@org.springframework.data.repository.query.Param("variantId") Long variantId);
    java.util.List<Inventory> findByStoreId(Long storeId);
}
