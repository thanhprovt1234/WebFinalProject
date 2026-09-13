package com.example.demo.repository;

import java.util.Optional;

import com.example.demo.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByStoreIdAndProductVariantId(Long storeId, Long productVariantId);
}
