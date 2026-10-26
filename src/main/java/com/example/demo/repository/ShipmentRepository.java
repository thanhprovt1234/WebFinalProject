package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import com.example.demo.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    @org.springframework.data.jpa.repository.Query("select s.shopOrder.order.id from Shipment s where s.id=:id")
    Optional<Long> parentOrderId(@org.springframework.data.repository.query.Param("id") Long id);

    Optional<Shipment> findByShopOrderId(Long shopOrderId);

    List<Shipment> findByShipperId(Long shipperId);
}
