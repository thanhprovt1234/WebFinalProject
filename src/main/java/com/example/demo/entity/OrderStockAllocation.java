package com.example.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name="order_stock_allocations",uniqueConstraints=@UniqueConstraint(name="uk_order_stock_allocation",columnNames={"shop_order_id","inventory_id"}),
        check=@CheckConstraint(name="ck_order_stock_quantity",constraint="quantity > 0"))
public class OrderStockAllocation extends BaseEntity {
    @NotNull @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="shop_order_id",nullable=false)
    private ShopOrder shopOrder;
    @NotNull @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="inventory_id",nullable=false)
    private Inventory inventory;
    @NotNull @Positive @Column(nullable=false)
    private Integer quantity;
    @Column(nullable=false)
    private boolean restored;
    public ShopOrder getShopOrder() { return shopOrder; }
    public void setShopOrder(ShopOrder value) { shopOrder=value; }
    public Inventory getInventory() { return inventory; }
    public void setInventory(Inventory value) { inventory=value; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer value) { quantity=value; }
    public boolean isRestored() { return restored; }
    public void setRestored(boolean value) { restored=value; }
}
