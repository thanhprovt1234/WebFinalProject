package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import com.example.demo.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUserId(Long userId);
    List<Address> findByUserIdOrderByIdAsc(Long userId);
    Optional<Address> findByIdAndUserId(Long id, Long userId);
}
