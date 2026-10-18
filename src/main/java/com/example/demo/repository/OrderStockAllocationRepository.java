package com.example.demo.repository;

import com.example.demo.entity.OrderStockAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStockAllocationRepository extends JpaRepository<OrderStockAllocation,Long> {
    java.util.List<OrderStockAllocation> findByShopOrderIdOrderByInventoryIdAsc(Long shopOrderId);
}
