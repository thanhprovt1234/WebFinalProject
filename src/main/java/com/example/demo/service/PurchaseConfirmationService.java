package com.example.demo.service;

import com.example.demo.entity.OrderEntity;
import com.example.demo.entity.OrderStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class PurchaseConfirmationService {
    private final ApplicationEventPublisher events;
    private final CheckoutService checkout;
    private final VoucherService vouchers;

    public PurchaseConfirmationService(ApplicationEventPublisher events, CheckoutService checkout, VoucherService vouchers) {
        this.events = events;
        this.checkout = checkout;
        this.vouchers = vouchers;
    }

    public boolean completed(OrderEntity order) {
        return order.getShopOrders().stream().anyMatch(s -> s.getStatus() == OrderStatus.DELIVERED)
                && order.getShopOrders().stream().allMatch(s -> s.getStatus() == OrderStatus.DELIVERED || s.getStatus() == OrderStatus.CANCELLED);
    }

    public void notifyIfCompleted(OrderEntity order, boolean alreadyCompleted) {
        if (!alreadyCompleted && completed(order)) {
            vouchers.reward(order);
            events.publishEvent(new PurchaseCompleted(order.getUser().getEmail(), order.getUser().getFullName(),
                    checkout.get(order.getUser().getUsername(), order.getId())));
        }
    }
}
