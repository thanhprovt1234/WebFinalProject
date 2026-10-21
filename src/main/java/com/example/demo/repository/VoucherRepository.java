package com.example.demo.repository;

import com.example.demo.entity.Voucher;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoucherRepository extends JpaRepository<Voucher,Long> {
    boolean existsBySourceOrderId(Long id);
    Optional<Voucher> findByCodeAndUserUsername(String code,String username);
    List<Voucher> findByUserUsernameAndUsedOrderIsNullOrderByIdDesc(String username);
}
