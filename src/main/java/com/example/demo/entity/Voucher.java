package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name="vouchers")
public class Voucher extends AuditableEntity {
    @Column(nullable=false, unique=true, length=40)
    private String code;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="user_id", nullable=false)
    private User user;
    @OneToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="source_order_id", nullable=false, unique=true)
    private OrderEntity sourceOrder;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="used_order_id")
    private OrderEntity usedOrder;
    public String getCode() { return code; }
    public void setCode(String value) { code=value; }
    public User getUser() { return user; }
    public void setUser(User value) { user=value; }
    public OrderEntity getSourceOrder() { return sourceOrder; }
    public void setSourceOrder(OrderEntity value) { sourceOrder=value; }
    public OrderEntity getUsedOrder() { return usedOrder; }
    public void setUsedOrder(OrderEntity value) { usedOrder=value; }
}
