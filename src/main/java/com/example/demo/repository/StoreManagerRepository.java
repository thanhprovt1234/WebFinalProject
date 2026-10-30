package com.example.demo.repository;

import com.example.demo.entity.StoreManager;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface StoreManagerRepository extends JpaRepository<StoreManager,Long> {
    @EntityGraph(attributePaths={"user","store","store.shop"})
    @Query("select m from StoreManager m where m.store.shop.id in :shopIds")
    List<StoreManager> findForRevenue(@org.springframework.data.repository.query.Param("shopIds") List<Long> shopIds);
    @EntityGraph(attributePaths={"store","store.shop"})
    List<StoreManager> findByUserId(Long userId);
    boolean existsByUserIdAndStoreId(Long userId,Long storeId);
    Optional<StoreManager> findByUserIdAndStoreId(Long userId,Long storeId);
}
