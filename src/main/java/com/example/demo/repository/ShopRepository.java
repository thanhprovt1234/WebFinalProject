package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import com.example.demo.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopRepository extends JpaRepository<Shop, Long> {

    Optional<Shop> findBySlug(String slug);

    List<Shop> findByOwnerId(Long ownerId);
}
