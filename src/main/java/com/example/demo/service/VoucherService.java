package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.VoucherRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VoucherService {
    private final VoucherRepository vouchers;
    public VoucherService(VoucherRepository vouchers) { this.vouchers=vouchers; }
    private boolean eligible(OrderEntity order) {
        return "PAID".equals(order.getPaymentStatus()) && order.getTotalAmount().compareTo(new BigDecimal("500000"))>0
                && order.getShopOrders().stream().anyMatch(s->s.getStatus()==OrderStatus.DELIVERED)
                && order.getShopOrders().stream().allMatch(s->s.getStatus()==OrderStatus.DELIVERED || s.getStatus()==OrderStatus.CANCELLED);
    }
    // Called while holding the parent order lock, in the delivery transaction.
    public void reward(OrderEntity order) {
        if(!eligible(order) || vouchers.existsBySourceOrderId(order.getId())) return;
        var voucher=new Voucher();
        voucher.setCode("H2T-"+UUID.randomUUID().toString().replace("-", "").toUpperCase(java.util.Locale.ROOT));
        voucher.setUser(order.getUser()); voucher.setSourceOrder(order); vouchers.save(voucher);
    }
    public Voucher validate(String username,String code) {
        var voucher=vouchers.findByCodeAndUserUsername(code.strip().toUpperCase(java.util.Locale.ROOT),username)
                .orElseThrow(()->invalid());
        if(voucher.getUsedOrder()!=null || !eligible(voucher.getSourceOrder())) throw invalid();
        return voucher;
    }
    private ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.CONFLICT,"Mã giảm giá không hợp lệ, đã dùng hoặc không thuộc tài khoản của bạn.");
    }
    public BigDecimal discount(BigDecimal total) {
        return total.multiply(new BigDecimal("0.10")).setScale(2,RoundingMode.HALF_UP);
    }
    @Transactional(readOnly=true)
    public BigDecimal preview(String username,String code,BigDecimal total) {
        if(code==null || code.isBlank()) return BigDecimal.ZERO;
        validate(username,code); return discount(total);
    }
    @Transactional(readOnly=true)
    public List<String> available(String username) {
        return vouchers.findByUserUsernameAndUsedOrderIsNullOrderByIdDesc(username).stream()
                .filter(v->eligible(v.getSourceOrder())).map(Voucher::getCode).toList();
    }
}
