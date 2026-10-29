package com.example.demo.repository;

import com.example.demo.entity.Voucher;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoucherRepository extends JpaRepository<Voucher,Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select v from Voucher v where v.code=:code and v.user.username=:username")
    Optional<Voucher> lockByCodeAndUsername(String code,String username);
    boolean existsByCampaignIdAndUserId(Long campaignId,Long userId);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths={"user","campaign","issuedBy"})
    List<Voucher> findByCampaignIdOrderByIdDesc(Long campaignId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select v from Voucher v where v.id=:id")
    Optional<Voucher> lockById(Long id);
    List<Voucher> findByUserUsernameOrderByIdDesc(String username);
    boolean existsBySourceOrderId(Long id);
    Optional<Voucher> findByCodeAndUserUsername(String code,String username);
    List<Voucher> findByUserUsernameAndUsedOrderIsNullOrderByIdDesc(String username);
}
