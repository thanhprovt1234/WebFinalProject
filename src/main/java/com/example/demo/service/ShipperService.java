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
@Transactional(readOnly=true)
@PreAuthorize("hasAnyRole('SHIPPER','ADMIN')")
public class ShipperService {
    private static final ZoneId LOCAL = ZoneId.of("Asia/Ho_Chi_Minh");
    private final ShipmentRepository shipments;
    private final ShopOrderRepository parts;
    private final OrderEntityRepository orders;
    private final UserRepository users;
    private final OrderStatusHistoryRepository history;
    private final OrderStockAllocationRepository allocations;
    private final AccessService access;
    private final PurchaseConfirmationService confirmations;

    public ShipperService(ShipmentRepository shipments, ShopOrderRepository parts, OrderEntityRepository orders,
            UserRepository users, OrderStatusHistoryRepository history, OrderStockAllocationRepository allocations,
            AccessService access, PurchaseConfirmationService confirmations) {
        this.shipments=shipments;this.parts=parts;this.orders=orders;this.users=users;this.history=history;
        this.allocations=allocations;this.access=access;this.confirmations=confirmations;
    }
    private <T> T found(Optional<T> value) { return value.orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND)); }
    private User actor(String name) {
        var u=access.account(name);
        if(!access.has(u,RoleName.ROLE_SHIPPER)&&!access.has(u,RoleName.ROLE_ADMIN))throw new AccessDeniedException("Bạn không có quyền giao hàng.");
        return u;
    }
    private void owner(User u,Shipment s) {
        if(!s.getShipper().getId().equals(u.getId())&&!access.has(u,RoleName.ROLE_ADMIN))throw new AccessDeniedException("Đơn giao không được phân công cho bạn.");
    }
    public static String label(ShipmentStatus status) {
        return switch(status) {
            case ASSIGNED -> "Chờ lấy hàng"; case PICKED_UP -> "Đã nhận hàng"; case SHIPPING -> "Đang giao";
            case FAILED -> "Giao chưa thành công"; case DELIVERED -> "Giao thành công"; case CANCELLED -> "Đã hủy / hoàn hàng";
        };
    }
    private static String time(LocalDateTime t) {
        return t==null?"":t.atOffset(ZoneOffset.UTC).atZoneSameInstant(LOCAL).format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
    }
    private static LocalDate date(LocalDateTime t) { return t.atOffset(ZoneOffset.UTC).atZoneSameInstant(LOCAL).toLocalDate(); }
    private BigDecimal cod(Shipment s) {
        var p=s.getShopOrder();
        if(s.getStatus()==ShipmentStatus.DELIVERED) {
            if(s.getCodCollectedAmount()!=null)return s.getCodCollectedAmount();
            // Existing completed shipments predate the explicit COD snapshot.
            return "COD".equals(p.getOrder().getPaymentMethod())?p.getTotalAmount():BigDecimal.ZERO;
        }
        return "COD".equals(p.getOrder().getPaymentMethod()) && !"PAID".equals(p.getOrder().getPaymentStatus())
                && s.getStatus()!=ShipmentStatus.CANCELLED ? p.getTotalAmount():BigDecimal.ZERO;
    }
    private Map<String,Object> row(Shipment s) {
        var p=s.getShopOrder();var o=p.getOrder();var r=new LinkedHashMap<String,Object>();
        r.put("id",s.getId());r.put("orderId",p.getId());r.put("parentId",o.getId());r.put("shop",p.getShop().getName());
        r.put("shipper",s.getShipper().getFullName());r.put("receiver",o.getReceiverName());r.put("phone",o.getReceiverPhone());
        r.put("address",o.getShippingAddress());r.put("status",s.getStatus().name());r.put("label",label(s.getStatus()));
        r.put("assignedAt",time(s.getAssignedAt()));r.put("pickedUpAt",time(s.getPickedUpAt()));r.put("deliveredAt",time(s.getDeliveredAt()));
        r.put("cod",cod(s));r.put("paymentMethod",o.getPaymentMethod());r.put("failureReason",Objects.toString(s.getFailureReason(),""));
        r.put("settlement",CodSettlementService.describe(s));
        return r;
    }
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String,Object> assignment(Long partId) {
        var shipment=shipments.findByShopOrderId(partId);
        var shippers=users.findAll().stream().filter(User::isEnabled).filter(u->access.has(u,RoleName.ROLE_SHIPPER))
                .map(u->Map.<String,Object>of("id",u.getId(),"name",u.getFullName()+" ("+u.getUsername()+")")).toList();
        return Map.of("shippers",shippers,"assignedShipment",shipment.map(this::row).orElse(Map.of()),
                "canAssign",found(parts.findById(partId)).getStatus()==OrderStatus.CONFIRMED&&shipment.isEmpty());
    }
    @Transactional @PreAuthorize("hasRole('ADMIN')")
    public void assign(String name,Long partId,Long shipperId) {
        if(!access.has(access.account(name),RoleName.ROLE_ADMIN))throw new AccessDeniedException("Cần quyền Admin.");
        found(orders.lockById(found(parts.parentOrderId(partId))));
        var p=found(parts.findById(partId));
        if(p.getStatus()!=OrderStatus.CONFIRMED)throw new IllegalArgumentException("Chỉ phân công đơn đã được shop xác nhận.");
        var existing=shipments.findByShopOrderId(partId);
        if(existing.isPresent()) {
            if(existing.get().getShipper().getId().equals(shipperId))return;
            throw new IllegalArgumentException("Đơn đã được phân công cho shipper.");
        }
        var shipper=found(users.findById(shipperId));
        if(!shipper.isEnabled()||!access.has(shipper,RoleName.ROLE_SHIPPER))throw new IllegalArgumentException("Tài khoản shipper không hoạt động hoặc chưa có quyền giao hàng.");
        var s=new Shipment();s.setShopOrder(p);s.setShipper(shipper);shipments.save(s);
        event(p,"Admin "+name+" phân công giao hàng cho "+shipper.getUsername());
    }
    private void event(ShopOrder p,String note) {
        var e=new OrderStatusHistory();e.setShopOrder(p);e.setStatus(p.getStatus());e.setNote(note);history.save(e);
    }
    public Map<String,Object> dashboard(String name,String q,ShipmentStatus status,LocalDate from,LocalDate to,int page) {
        var u=actor(name);
        if(from!=null&&to!=null&&from.isAfter(to))throw new IllegalArgumentException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
        var all=(access.has(u,RoleName.ROLE_ADMIN)?shipments.findAll():shipments.findByShipperId(u.getId())).stream()
                .filter(s->from==null||!date(s.getAssignedAt()).isBefore(from))
                .filter(s->to==null||!date(s.getAssignedAt()).isAfter(to)).sorted(Comparator.comparing(Shipment::getId).reversed()).toList();
        var counts=new LinkedHashMap<String,Long>();for(var st:ShipmentStatus.values())counts.put(st.name(),all.stream().filter(s->s.getStatus()==st).count());
        var term=q.strip().toLowerCase(Locale.ROOT);
        var filtered=all.stream().filter(s->status==null||s.getStatus()==status).filter(s->{var p=s.getShopOrder();var o=p.getOrder();
            return term.isEmpty()||String.valueOf(p.getId()).contains(term)||o.getReceiverName().toLowerCase(Locale.ROOT).contains(term)||o.getReceiverPhone().contains(term);}).toList();
        int pages=Math.max(1,(filtered.size()+19)/20),current=Math.max(0,Math.min(page,pages-1)),start=current*20;
        var collected=all.stream().filter(s->s.getStatus()==ShipmentStatus.DELIVERED).map(this::cod).reduce(BigDecimal.ZERO,BigDecimal::add);
        var unremitted=all.stream().filter(s->s.getCodRemittedAt()==null).map(CodSettlementService::collected).reduce(BigDecimal.ZERO,BigDecimal::add);
        var awaitingConfirmation=all.stream().filter(s->s.getCodRemittedAt()!=null&&s.getCodConfirmedAt()==null).map(CodSettlementService::collected).reduce(BigDecimal.ZERO,BigDecimal::add);
        var confirmed=all.stream().filter(s->s.getCodConfirmedAt()!=null).map(CodSettlementService::collected).reduce(BigDecimal.ZERO,BigDecimal::add);
        var days=new TreeMap<LocalDate,long[]>();
        for(var s:all){var values=days.computeIfAbsent(date(s.getAssignedAt()),ignored->new long[2]);values[0]++;if(s.getStatus()==ShipmentStatus.DELIVERED)values[1]++;}
        var daily=days.entrySet().stream().map(e->Map.<String,Object>of("day",e.getKey().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),"assigned",e.getValue()[0],"delivered",e.getValue()[1],"percent",e.getValue()[1]*100/e.getValue()[0])).toList();
        var result=new LinkedHashMap<String,Object>();result.put("rows",filtered.subList(start,Math.min(start+20,filtered.size())).stream().map(this::row).toList());
        result.put("counts",counts);result.put("total",all.size());result.put("filteredTotal",filtered.size());result.put("page",current);result.put("pages",pages);
        result.put("collected",collected);result.put("daily",daily);result.put("rate",all.isEmpty()?0:counts.get("DELIVERED")*100/all.size());
        result.put("unremitted",unremitted);result.put("awaitingConfirmation",awaitingConfirmation);result.put("confirmedCod",confirmed);
        return result;
    }
    public Map<String,Object> detail(String name,Long id) {
        var u=actor(name);var s=found(shipments.findById(id));owner(u,s);var p=s.getShopOrder();
        var pickup=new LinkedHashSet<String>();
        allocations.findByShopOrderIdOrderByInventoryIdAsc(p.getId()).forEach(a->{var store=a.getInventory().getStore();pickup.add(store.getName()+" — "+store.getAddress());});
        if(pickup.isEmpty()&&p.getStore()!=null)pickup.add(p.getStore().getName()+" — "+p.getStore().getAddress());
        var items=p.getItems().stream().map(i->Map.<String,Object>of("name",i.getProductName(),"size",i.getSize(),"color",i.getColor(),"quantity",i.getQuantity())).toList();
        var events=history.findByShopOrderIdOrderByChangedAtAscIdAsc(p.getId()).stream().map(e->Map.of("time",time(e.getChangedAt()),"note",Objects.toString(e.getNote(),""),"status",CheckoutService.statusLabel(e.getStatus().name()))).toList();
        return Map.of("shipment",row(s),"pickup",pickup,"items",items,"events",events,"shopPhone",Objects.toString(p.getShop().getPhone(),""));
    }
    @Transactional
    public void update(String name,Long id,ShipmentStatus target,String note,boolean codCollected) {
        var u=actor(name);
        var parent=found(orders.lockById(found(shipments.parentOrderId(id))));
        var s=found(shipments.findById(id));owner(u,s);var p=s.getShopOrder();
        if(s.getStatus()==target)return;
        boolean allowed=switch(s.getStatus()) {
            case ASSIGNED -> target==ShipmentStatus.PICKED_UP;
            case PICKED_UP, FAILED -> target==ShipmentStatus.SHIPPING;
            case SHIPPING -> target==ShipmentStatus.DELIVERED||target==ShipmentStatus.FAILED;
            default -> false;
        };
        var expectedOrderStatus=switch(s.getStatus()) {
            case ASSIGNED -> OrderStatus.CONFIRMED;
            case PICKED_UP -> OrderStatus.PICKED_UP;
            default -> OrderStatus.SHIPPING;
        };
        if(!allowed||p.getStatus()!=expectedOrderStatus)throw new IllegalArgumentException("Không thể chuyển trạng thái giao hàng theo cách này.");
        var reason=note==null?"":note.strip();
        if(reason.length()>1000)throw new IllegalArgumentException("Ghi chú tối đa 1000 ký tự.");
        if(target==ShipmentStatus.FAILED&&reason.isBlank())throw new IllegalArgumentException("Vui lòng nhập lý do giao chưa thành công.");
        var amountDue=cod(s);
        if(target==ShipmentStatus.DELIVERED&&amountDue.signum()>0&&!codCollected)throw new IllegalArgumentException("Cần xác nhận đã thu đủ tiền COD trước khi hoàn tất.");
        boolean completed=confirmations.completed(parent);
        var now=LocalDateTime.now(ZoneOffset.UTC);
        s.setStatus(target);
        if(target==ShipmentStatus.PICKED_UP){s.setPickedUpAt(now);p.setStatus(OrderStatus.PICKED_UP);}
        if(target==ShipmentStatus.SHIPPING)p.setStatus(OrderStatus.SHIPPING);
        if(target==ShipmentStatus.FAILED)s.setFailureReason(reason);
        if(target==ShipmentStatus.DELIVERED){s.setDeliveredAt(now);s.setCodCollectedAmount(amountDue);p.setStatus(OrderStatus.DELIVERED);}
        event(p,"Shipper "+name+": "+label(target)+(target==ShipmentStatus.DELIVERED?"; COD đã thu: "+amountDue.toPlainString()+"đ":"")+(reason.isBlank()?"":" — "+reason));
        if("COD".equals(parent.getPaymentMethod())&&parent.getShopOrders().stream().allMatch(o->o.getStatus()==OrderStatus.DELIVERED||o.getStatus()==OrderStatus.CANCELLED))parent.setPaymentStatus("PAID");
        confirmations.notifyIfCompleted(parent,completed);
    }
}
