package com.example.demo.repository;

import java.util.List;

import com.example.demo.entity.ShopOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long> {
    @org.springframework.data.jpa.repository.Query("select s.order.id from ShopOrder s where s.id=:id")
    java.util.Optional<Long> parentOrderId(@org.springframework.data.repository.query.Param("id") Long id);

    org.springframework.data.domain.Page<ShopOrder> findByShopOwnerId(Long ownerId,org.springframework.data.domain.Pageable pageable);

    List<ShopOrder> findByShopId(Long shopId);

    List<ShopOrder> findByOrderId(Long orderId);
}
