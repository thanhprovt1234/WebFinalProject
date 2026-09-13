package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import com.example.demo.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    Optional<Shipment> findByShopOrderId(Long shopOrderId);

    List<Shipment> findByShipperId(Long shipperId);
}
