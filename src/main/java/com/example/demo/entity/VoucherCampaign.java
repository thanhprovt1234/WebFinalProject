package com.example.demo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "voucher_campaigns")
public class VoucherCampaign extends AuditableEntity {
    @Nationalized @Column(nullable=false, length=150)
    private String name;
    @Column(name="discount_type", nullable=false, length=10)
    private String discountType;
    @Column(name="discount_value", nullable=false, precision=19, scale=2)
    private BigDecimal discountValue;
    @Column(name="minimum_total", nullable=false, precision=19, scale=2)
    private BigDecimal minimumTotal;
    @Column(name="maximum_discount", precision=19, scale=2)
    private BigDecimal maximumDiscount;
    @Column(name="expires_at", nullable=false)
    private Instant expiresAt;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="created_by_id", nullable=false)
    private User createdBy;
    public String getName(){return name;}
    public void setName(String v){name=v;}
    public String getDiscountType(){return discountType;}
    public void setDiscountType(String v){discountType=v;}
    public BigDecimal getDiscountValue(){return discountValue;}
    public void setDiscountValue(BigDecimal v){discountValue=v;}
    public BigDecimal getMinimumTotal(){return minimumTotal;}
    public void setMinimumTotal(BigDecimal v){minimumTotal=v;}
    public BigDecimal getMaximumDiscount(){return maximumDiscount;}
    public void setMaximumDiscount(BigDecimal v){maximumDiscount=v;}
    public Instant getExpiresAt(){return expiresAt;}
    public boolean isActive(){return expiresAt.isAfter(Instant.now());}
    public String getExpiryLabel(){return java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").withZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).format(expiresAt.minusSeconds(1));}
    public void setExpiresAt(Instant v){expiresAt=v;}
    public User getCreatedBy(){return createdBy;}
    public void setCreatedBy(User v){createdBy=v;}
}
