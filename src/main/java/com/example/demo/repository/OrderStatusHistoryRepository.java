package com.example.demo.repository;

import java.util.List;

import com.example.demo.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

    List<OrderStatusHistory> findByShopOrderIdOrderByChangedAtAscIdAsc(Long shopOrderId);
}
