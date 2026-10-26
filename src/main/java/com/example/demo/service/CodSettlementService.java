package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class CodSettlementService {
    private final ShipmentRepository shipments;
    private final ShopOrderRepository parts;
    private final OrderEntityRepository orders;
    private final OrderStatusHistoryRepository history;
    private final AccessService access;

    public CodSettlementService(ShipmentRepository shipments,ShopOrderRepository parts,OrderEntityRepository orders,
            OrderStatusHistoryRepository history,AccessService access) {
        this.shipments=shipments;this.parts=parts;this.orders=orders;this.history=history;this.access=access;
    }
    private <T> T found(Optional<T> value) { return value.orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND)); }
    private User actor(String name,RoleName role) {
        var u=access.account(name);
        if(!access.has(u,role)&&!access.has(u,RoleName.ROLE_ADMIN))throw new AccessDeniedException("Bạn không có quyền đối soát COD.");
        return u;
    }
    public static BigDecimal collected(Shipment s) {
        return s!=null&&s.getStatus()==ShipmentStatus.DELIVERED&&s.getCodCollectedAmount()!=null?s.getCodCollectedAmount():BigDecimal.ZERO;
    }
    private static String time(LocalDateTime t) {
        return t==null?"":t.atOffset(ZoneOffset.UTC).atZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
    }
    public static Map<String,Object> describe(Shipment s) {
        var amount=collected(s);String state="NOT_REQUIRED",label="Không có COD cần đối soát";
        if(s!=null&&s.getStatus()!=ShipmentStatus.DELIVERED&&s.getStatus()!=ShipmentStatus.CANCELLED
                &&"COD".equals(s.getShopOrder().getOrder().getPaymentMethod())&&!"PAID".equals(s.getShopOrder().getOrder().getPaymentStatus())) {
            state="AWAITING_DELIVERY";label="Chưa thu COD";
        }
        if(s!=null&&s.getStatus()==ShipmentStatus.DELIVERED&&s.getCodCollectedAmount()==null&&"COD".equals(s.getShopOrder().getOrder().getPaymentMethod())) {
            state="NEEDS_REVIEW";label="Cần đối soát số COD của đơn cũ";
        }
        if(amount.signum()>0) {
            if(s.getCodConfirmedAt()!=null){state="CONFIRMED";label="Shop đã xác nhận nhận COD";}
            else if(s.getCodRemittedAt()!=null){state="REMITTED";label="Đã bàn giao COD · Chờ shop xác nhận";}
            else {state="COLLECTED";label="Đã thu COD · Chờ bàn giao cho shop";}
        }
        var r=new LinkedHashMap<String,Object>();r.put("state",state);r.put("label",label);r.put("amount",amount);
        r.put("remittedAt",s==null?"":time(s.getCodRemittedAt()));r.put("confirmedAt",s==null?"":time(s.getCodConfirmedAt()));
        r.put("remittanceNote",s==null?"":Objects.toString(s.getCodRemittanceNote(),""));
        r.put("confirmationNote",s==null?"":Objects.toString(s.getCodConfirmationNote(),""));
        r.put("confirmedBy",s==null?"":Objects.toString(s.getCodConfirmedBy(),""));return r;
    }
    private String validate(Shipment s,BigDecimal amount,String note,boolean confirmed) {
        if(s.getStatus()!=ShipmentStatus.DELIVERED||s.getShopOrder().getStatus()!=OrderStatus.DELIVERED||collected(s).signum()<=0)
            throw new IllegalArgumentException("Chỉ đối soát đơn đã giao thành công và đã ghi nhận số COD thực thu.");
        if(amount==null||amount.compareTo(collected(s))!=0)throw new IllegalArgumentException("Số tiền đối soát phải bằng số COD thực thu của đơn.");
        if(!confirmed)throw new IllegalArgumentException("Vui lòng xác nhận đã bàn giao hoặc nhận đủ tiền COD thực tế.");
        var text=note==null?"":note.strip();
        if(text.length()>500)throw new IllegalArgumentException("Ghi chú đối soát tối đa 500 ký tự.");
        return text;
    }
    private void event(Shipment s,String text) {
        var e=new OrderStatusHistory();e.setShopOrder(s.getShopOrder());e.setStatus(s.getShopOrder().getStatus());e.setNote(text);history.save(e);
    }
    @PreAuthorize("hasAnyRole('SHIPPER','ADMIN')")
    public void remit(String name,Long shipmentId,BigDecimal amount,String note,boolean confirmed) {
        var u=actor(name,RoleName.ROLE_SHIPPER);
        found(orders.lockById(found(shipments.parentOrderId(shipmentId))));
        var s=found(shipments.findById(shipmentId));
        if(!s.getShipper().getId().equals(u.getId())&&!access.has(u,RoleName.ROLE_ADMIN))throw new AccessDeniedException("Đơn giao không được phân công cho bạn.");
        var text=validate(s,amount,note,confirmed);
        if(s.getCodRemittedAt()!=null)return;
        s.setCodRemittedAt(LocalDateTime.now(ZoneOffset.UTC));s.setCodRemittanceNote(text);
        event(s,"Bàn giao COD — "+name+": "+collected(s).toPlainString()+"đ"+(text.isBlank()?"":" — "+text));
    }
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    public void confirm(String name,Long partId,BigDecimal amount,String note,boolean confirmed) {
        var u=actor(name,RoleName.ROLE_VENDOR);
        found(orders.lockById(found(parts.parentOrderId(partId))));
        var p=found(parts.findById(partId));
        if(!p.getShop().getOwner().getId().equals(u.getId())&&!access.has(u,RoleName.ROLE_ADMIN))throw new AccessDeniedException("Đơn hàng không thuộc shop của bạn.");
        var s=found(shipments.findByShopOrderId(partId));var text=validate(s,amount,note,confirmed);
        if(s.getCodRemittedAt()==null)throw new IllegalArgumentException("Shipper chưa ghi nhận bàn giao COD cho shop.");
        if(s.getCodConfirmedAt()!=null)return;
        s.setCodConfirmedAt(LocalDateTime.now(ZoneOffset.UTC));s.setCodConfirmedBy(name);s.setCodConfirmationNote(text);
        event(s,"Shop xác nhận nhận COD — "+name+": "+collected(s).toPlainString()+"đ"+(text.isBlank()?"":" — "+text));
    }
}
