package com.example.demo.repository;

import java.util.List;

import com.example.demo.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

    @org.springframework.data.jpa.repository.Query("""
        select h from OrderStatusHistory h
        join fetch h.shopOrder s join fetch s.shop shop
        where h.status=com.example.demo.entity.OrderStatus.CANCELLED
          and h.note like 'Khách hàng hủy đơn:%'
          and (:ownerId is null or shop.owner.id=:ownerId)
        order by h.changedAt desc,h.id desc
        """)
    List<OrderStatusHistory> customerCancellations(@org.springframework.data.repository.query.Param("ownerId") Long ownerId,
            org.springframework.data.domain.Pageable pageable);

    List<OrderStatusHistory> findByShopOrderIdOrderByChangedAtAscIdAsc(Long shopOrderId);
}
