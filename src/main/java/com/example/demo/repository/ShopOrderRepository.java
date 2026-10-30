package com.example.demo.repository;

import java.util.List;

import com.example.demo.entity.ShopOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long> {
    @org.springframework.data.jpa.repository.Query("""
        select s.shop.id, s.status, count(s), sum(s.totalAmount)
        from ShopOrder s
        where s.shop.id in :shopIds and s.order.orderDate >= :start and s.order.orderDate < :end
        group by s.shop.id, s.status
        """)
    List<Object[]> revenueSummary(@org.springframework.data.repository.query.Param("shopIds") List<Long> shopIds,
            @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
            @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);
    @org.springframework.data.jpa.repository.Query("select s.order.id from ShopOrder s where s.id=:id")
    java.util.Optional<Long> parentOrderId(@org.springframework.data.repository.query.Param("id") Long id);

    org.springframework.data.domain.Page<ShopOrder> findByShopOwnerId(Long ownerId,org.springframework.data.domain.Pageable pageable);

    List<ShopOrder> findByShopId(Long shopId);

    List<ShopOrder> findByOrderId(Long orderId);
}
