package com.example.demo.repository;

import java.util.List;

import com.example.demo.entity.ShopOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long> {

    List<ShopOrder> findByShopId(Long shopId);

    List<ShopOrder> findByOrderId(Long orderId);
}
