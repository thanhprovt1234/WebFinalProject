package com.example.demo.entity;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "shipments",
        uniqueConstraints = @UniqueConstraint(name = "uk_shipments_shop_order", columnNames = "shop_order_id"))
public class Shipment extends BaseEntity {

    // Unidirectional lazy association; UNIQUE allows at most one shipment per shop order.
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_order_id", nullable = false)
    private ShopOrder shopOrder;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipper_id", nullable = false)
    private User shipper;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    @Column(name = "picked_up_at")
    private LocalDateTime pickedUpAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Nationalized
    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String value) { failureReason = value; }

    @jakarta.validation.constraints.DecimalMin("0.00")
    @Column(name = "cod_collected_amount", precision = 19, scale = 2)
    private java.math.BigDecimal codCollectedAmount;

    public java.math.BigDecimal getCodCollectedAmount() { return codCollectedAmount; }
    public void setCodCollectedAmount(java.math.BigDecimal value) { codCollectedAmount = value; }

    @Column(name = "cod_remitted_at")
    private LocalDateTime codRemittedAt;

    @Column(name = "cod_confirmed_at")
    private LocalDateTime codConfirmedAt;

    @Size(max = 500) @Nationalized
    @Column(name = "cod_remittance_note", length = 500)
    private String codRemittanceNote;

    @Size(max = 500) @Nationalized
    @Column(name = "cod_confirmation_note", length = 500)
    private String codConfirmationNote;

    @Size(max = 100) @Nationalized
    @Column(name = "cod_confirmed_by", length = 100)
    private String codConfirmedBy;

    public LocalDateTime getCodRemittedAt() { return codRemittedAt; }
    public void setCodRemittedAt(LocalDateTime value) { codRemittedAt = value; }
    public LocalDateTime getCodConfirmedAt() { return codConfirmedAt; }
    public void setCodConfirmedAt(LocalDateTime value) { codConfirmedAt = value; }
    public String getCodRemittanceNote() { return codRemittanceNote; }
    public void setCodRemittanceNote(String value) { codRemittanceNote = value; }
    public String getCodConfirmationNote() { return codConfirmationNote; }
    public void setCodConfirmationNote(String value) { codConfirmationNote = value; }
    public String getCodConfirmedBy() { return codConfirmedBy; }
    public void setCodConfirmedBy(String value) { codConfirmedBy = value; }

    @Size(max = 150)
    @Nationalized
    @Column(name = "shipping_provider_name", length = 150)
    private String shippingProviderName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShipmentStatus status = ShipmentStatus.ASSIGNED;

    @PrePersist
    protected void initializeAssignedAt() {
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now(ZoneOffset.UTC);
        }
    }

    public ShopOrder getShopOrder() {
        return shopOrder;
    }

    public void setShopOrder(ShopOrder shopOrder) {
        this.shopOrder = shopOrder;
    }

    public User getShipper() {
        return shipper;
    }

    public void setShipper(User shipper) {
        this.shipper = shipper;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public LocalDateTime getPickedUpAt() {
        return pickedUpAt;
    }

    public void setPickedUpAt(LocalDateTime pickedUpAt) {
        this.pickedUpAt = pickedUpAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public String getShippingProviderName() {
        return shippingProviderName;
    }

    public void setShippingProviderName(String shippingProviderName) {
        this.shippingProviderName = shippingProviderName;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public void setStatus(ShipmentStatus status) {
        this.status = status;
    }
}
