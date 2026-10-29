package com.example.demo.repository;

import com.example.demo.entity.VoucherCampaign;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;

public interface VoucherCampaignRepository extends JpaRepository<VoucherCampaign,Long> {
    Page<VoucherCampaign> findAllByOrderByIdDesc(Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from VoucherCampaign c where c.id=:id")
    Optional<VoucherCampaign> lockById(Long id);
}
