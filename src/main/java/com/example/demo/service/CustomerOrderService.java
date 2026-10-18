package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CustomerOrderService {
    private final OrderEntityRepository orders;
    private final OrderStatusHistoryRepository history;
    private final OrderStockService stock;
    private final AccessService access;
    private final PurchaseConfirmationService confirmations;
    public CustomerOrderService(OrderEntityRepository orders,OrderStatusHistoryRepository history,OrderStockService stock,AccessService access,PurchaseConfirmationService confirmations) {
        this.orders=orders;this.history=history;this.stock=stock;this.access=access;
        this.confirmations=confirmations;
    }
    @Transactional
    public void cancelPending(String actor,Long id,String reason) {
        var user=access.account(actor);
        // The first order read takes the same parent lock used by admin status changes.
        var order=orders.lockByIdAndUserUsername(id,user.getUsername())
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        var parts=order.getShopOrders();
        boolean alreadyCompleted = confirmations.completed(order);
        var pending=parts.stream().filter(s->s.getStatus()==OrderStatus.NEW)
                .sorted(java.util.Comparator.comparing(ShopOrder::getId)).toList();
        if(pending.isEmpty()) {
            if(parts.stream().allMatch(s->s.getStatus()==OrderStatus.CANCELLED)) return;
            throw new IllegalArgumentException("Đơn đã được duyệt, bạn không thể tự hủy. Vui lòng liên hệ shop.");
        }
        var note=reason==null?"":reason.strip();
        if(note.length()>1000) throw new IllegalArgumentException("Lý do hủy tối đa 1000 ký tự.");
        for(var part:pending) {
            stock.restore(part);
            order.setTotalAmount(order.getTotalAmount().subtract(part.getTotalAmount()));
            part.setTotalAmount(BigDecimal.ZERO);part.setStatus(OrderStatus.CANCELLED);
            var event=new OrderStatusHistory();event.setShopOrder(part);event.setStatus(OrderStatus.CANCELLED);
            event.setNote("Khách hàng hủy đơn: "+user.getUsername()+(note.isBlank()?"":" — "+note));history.save(event);
        }
        if(parts.stream().allMatch(s->s.getStatus()==OrderStatus.CANCELLED)) order.setPaymentStatus("CANCELLED");
        else if("COD".equals(order.getPaymentMethod()) && parts.stream().allMatch(s->s.getStatus()==OrderStatus.DELIVERED||s.getStatus()==OrderStatus.CANCELLED))
            order.setPaymentStatus("PAID");
        confirmations.notifyIfCompleted(order, alreadyCompleted);
    }
}
