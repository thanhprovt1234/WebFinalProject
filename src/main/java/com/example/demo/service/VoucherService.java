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
        return order!=null && "PAID".equals(order.getPaymentStatus()) && order.getTotalAmount().compareTo(new BigDecimal("500000"))>0
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
        if(!usable(voucher)) throw invalid();
        return voucher;
    }
    public Voucher lockForUse(String username,String code) {
        var voucher=vouchers.lockByCodeAndUsername(code.strip().toUpperCase(java.util.Locale.ROOT),username).orElseThrow(()->invalid());
        if(!usable(voucher)) throw invalid();
        return voucher;
    }
    private boolean usable(Voucher v) {
        return v.getUsedOrder()==null && v.getRevokedAt()==null && (v.getCampaign()!=null
                ? v.getCampaign().getExpiresAt().isAfter(java.time.Instant.now()) : eligible(v.getSourceOrder()));
    }
    private ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.CONFLICT,"Mã giảm giá không hợp lệ, đã dùng hoặc không thuộc tài khoản của bạn.");
    }
    public BigDecimal discount(BigDecimal total) {
        return total.multiply(new BigDecimal("0.10")).setScale(2,RoundingMode.HALF_UP);
    }
    public BigDecimal discount(Voucher voucher,BigDecimal total) {
        var campaign=voucher.getCampaign();
        if(campaign==null) return discount(total);
        if(total.compareTo(campaign.getMinimumTotal())<0)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Đơn hàng chưa đạt giá trị tối thiểu của voucher.");
        var amount="PERCENT".equals(campaign.getDiscountType())
                ? total.multiply(campaign.getDiscountValue()).divide(new BigDecimal("100"),2,RoundingMode.HALF_UP)
                : campaign.getDiscountValue();
        if(campaign.getMaximumDiscount()!=null) amount=amount.min(campaign.getMaximumDiscount());
        return amount.min(total).max(BigDecimal.ZERO).setScale(2,RoundingMode.HALF_UP);
    }
    @Transactional(readOnly=true)
    public BigDecimal preview(String username,String code,BigDecimal total) {
        if(code==null || code.isBlank()) return BigDecimal.ZERO;
        return discount(validate(username,code),total);
    }
    @Transactional(readOnly=true)
    public List<String> available(String username) {
        return vouchers.findByUserUsernameAndUsedOrderIsNullOrderByIdDesc(username).stream()
                .filter(this::usable).map(Voucher::getCode).toList();
    }
    public String date(java.time.Instant instant) {
        return java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")
                .withZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).format(instant);
    }
    public String status(Voucher v) {
        if(v.getUsedOrder()!=null)return "Đã dùng";
        if(v.getRevokedAt()!=null)return "Đã thu hồi";
        if(v.getCampaign()!=null && !v.getCampaign().getExpiresAt().isAfter(java.time.Instant.now()))return "Hết hạn";
        return usable(v)?"Chưa dùng":"Không còn đủ điều kiện";
    }
    @Transactional(readOnly=true)
    public List<java.util.Map<String,Object>> wallet(String username,boolean onlyAvailable) {
        return vouchers.findByUserUsernameOrderByIdDesc(username).stream().filter(v->!onlyAvailable || usable(v)).map(v->{
            var c=v.getCampaign();
            var row=new java.util.HashMap<String,Object>();
            row.put("code",v.getCode());row.put("name",c==null?"Thưởng mua hàng":c.getName());
            row.put("benefit",c==null?"Giảm 10%":"Giảm "+java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("vi-VN")).format(c.getDiscountValue())+ ("PERCENT".equals(c.getDiscountType())?"%":"đ"));
            row.put("minimum",c==null?BigDecimal.ZERO:c.getMinimumTotal());
            row.put("maximum",c==null?null:c.getMaximumDiscount());
            row.put("expires",c==null?"Không giới hạn":date(c.getExpiresAt().minusSeconds(1)));
            row.put("status",status(v));return (java.util.Map<String,Object>)row;
        }).toList();
    }
}
