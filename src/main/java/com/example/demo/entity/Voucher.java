package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name="vouchers", uniqueConstraints=@UniqueConstraint(name="uk_voucher_campaign_user", columnNames={"campaign_id","user_id"}))
public class Voucher extends AuditableEntity {
    @Column(nullable=false, unique=true, length=40)
    private String code;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="user_id", nullable=false)
    private User user;
    @OneToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="source_order_id", unique=true)
    private OrderEntity sourceOrder;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="used_order_id")
    private OrderEntity usedOrder;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="campaign_id")
    private VoucherCampaign campaign;
    @Column(name="revoked_at")
    private java.time.Instant revokedAt;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="issued_by_id")
    private User issuedBy;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="revoked_by_id")
    private User revokedBy;
    public VoucherCampaign getCampaign(){return campaign;}
    public void setCampaign(VoucherCampaign v){campaign=v;}
    public java.time.Instant getRevokedAt(){return revokedAt;}
    public void setRevokedAt(java.time.Instant v){revokedAt=v;}
    public User getIssuedBy(){return issuedBy;}
    public void setIssuedBy(User v){issuedBy=v;}
    public User getRevokedBy(){return revokedBy;}
    public void setRevokedBy(User v){revokedBy=v;}
    public String getCode() { return code; }
    public void setCode(String value) { code=value; }
    public User getUser() { return user; }
    public void setUser(User value) { user=value; }
    public OrderEntity getSourceOrder() { return sourceOrder; }
    public void setSourceOrder(OrderEntity value) { sourceOrder=value; }
    public OrderEntity getUsedOrder() { return usedOrder; }
    public void setUsedOrder(OrderEntity value) { usedOrder=value; }
}
