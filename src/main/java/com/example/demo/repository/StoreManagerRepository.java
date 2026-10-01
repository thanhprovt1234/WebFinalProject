package com.example.demo.repository;

import com.example.demo.entity.StoreManager;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface StoreManagerRepository extends JpaRepository<StoreManager,Long> {
    @EntityGraph(attributePaths={"store","store.shop"})
    List<StoreManager> findByUserId(Long userId);
    boolean existsByUserIdAndStoreId(Long userId,Long storeId);
    Optional<StoreManager> findByUserIdAndStoreId(Long userId,Long storeId);
}
