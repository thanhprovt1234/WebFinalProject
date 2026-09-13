package com.example.demo.repository;

import java.util.Optional;
import java.util.List;

import com.example.demo.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySlug(String slug);

    List<Product> findByShopId(Long shopId);
}
