package com.example.demo.repository;

import java.util.Optional;
import java.util.List;

import com.example.demo.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByCode(String code);

    List<Store> findByShopId(Long shopId);
}
