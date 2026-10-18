package com.example.demo.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "orders",
        check = @CheckConstraint(name = "ck_orders_values", constraint = "total_amount >= 0"))
public class OrderEntity extends AuditableEntity {

    @Column(name = "payment_method", nullable = false, length = 20)
    private String paymentMethod = "COD";

    @Column(name = "payment_status", nullable = false, length = 20)
    private String paymentStatus = "UNPAID";

    @Column(name = "checkout_token", length = 36)
    private String checkoutToken;

    public String getPaymentMethod() { return paymentMethod; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String value) { paymentStatus=value; }
    public String getCheckoutToken() { return checkoutToken; }
    public void setCheckoutToken(String value) { checkoutToken = value; }

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "order_date", nullable = false, updatable = false)
    private LocalDateTime orderDate;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 17, fraction = 2)
    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @NotBlank
    @Size(max = 150)
    @Nationalized
    @Column(name = "receiver_name", nullable = false, length = 150)
    private String receiverName;

    @NotBlank
    @Size(max = 20)
    @Column(name = "receiver_phone", nullable = false, length = 20)
    private String receiverPhone;

    @NotBlank
    @Size(max = 500)
    @Nationalized
    @Column(name = "shipping_address", nullable = false, length = 500)
    private String shippingAddress;

    // Persist/merge an order together with its shop orders, but never cascade deletion.
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<ShopOrder> shopOrders = new ArrayList<>();

    @PrePersist
    protected void initializeOrderDate() {
        orderDate = LocalDateTime.now(ZoneOffset.UTC);
    }

    public List<ShopOrder> getShopOrders() {
        return Collections.unmodifiableList(shopOrders);
    }

    public void addShopOrder(ShopOrder shopOrder) {
        Objects.requireNonNull(shopOrder).setOrder(this);
    }

    void attachShopOrder(ShopOrder shopOrder) {
        if (!shopOrders.contains(shopOrder)) {
            shopOrders.add(shopOrder);
        }
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverPhone() {
        return receiverPhone;
    }

    public void setReceiverPhone(String receiverPhone) {
        this.receiverPhone = receiverPhone;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }
}
