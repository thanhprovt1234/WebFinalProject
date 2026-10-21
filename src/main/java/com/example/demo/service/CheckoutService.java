package com.example.demo.service;

import com.example.demo.dto.CheckoutRequest;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@org.springframework.validation.annotation.Validated
public class CheckoutService {
    private final UserRepository users;
    private final CartItemRepository carts;
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final InventoryRepository inventory;
    private final OrderEntityRepository orders;
    private final OrderStatusHistoryRepository history;
    private final OrderStockAllocationRepository allocations;
    private final VoucherService vouchers;
    public CheckoutService(UserRepository users, CartItemRepository carts, ProductRepository products,
            ProductVariantRepository variants, InventoryRepository inventory, OrderEntityRepository orders,
            OrderStatusHistoryRepository history,OrderStockAllocationRepository allocations,VoucherService vouchers) {
        this.users=users; this.carts=carts; this.products=products; this.variants=variants;
        this.inventory=inventory; this.orders=orders; this.history=history;
        this.allocations=allocations; this.vouchers=vouchers;
    }
    private ResponseStatusException rejected(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT,message);
    }
    record Selected(CartItem cart, ProductVariant variant) {}

    @Transactional
    public Long place(String username, @jakarta.validation.Valid CheckoutRequest request) {
        var user=users.findForCartUpdate(username).filter(User::isEnabled)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        var previous=orders.findByUserIdAndCheckoutToken(user.getId(),request.getToken());
        if(previous.isPresent()) return previous.get().getId();
        var voucher=request.getVoucherCode()==null || request.getVoucherCode().isBlank()?null:
                vouchers.validate(username,request.getVoucherCode());
        var cart=carts.findByUserIdOrderByIdAsc(user.getId());
        if(cart.isEmpty()) throw rejected("Giỏ hàng đang trống.");
        var selected=new ArrayList<Selected>();
        for(var line:cart) {
            var product=products.findBySlug(line.getCatalogProductId()).orElseThrow(()->rejected("Sản phẩm không còn kinh doanh."));
            if(!product.isEnabled() || !product.getCategory().isEnabled() || !product.getShop().isEnabled()
                    || product.getShop().getStatus()!=ShopStatus.ACTIVE)
                throw rejected("Sản phẩm không còn kinh doanh: "+product.getName());
            var variant=variants.findByProductIdAndEnabledTrueOrderByIdAsc(product.getId()).stream()
                    .filter(v->v.getSize().equals(line.getSize()) && v.getColor().equals(line.getColor())).findFirst()
                    .orElseThrow(()->rejected("Size hoặc màu không còn kinh doanh: "+product.getName()));
            selected.add(new Selected(line,variant));
        }
        // Global variant order avoids deadlocks between customers buying multiple items.
        selected.sort(Comparator.comparing(s->s.variant().getId()));
        var order=new OrderEntity(); order.setUser(user); order.setCheckoutToken(request.getToken());
        order.setReceiverName(request.getReceiverName()); order.setReceiverPhone(request.getReceiverPhone());
        order.setShippingAddress(request.getShippingAddress());
        var byShop=new LinkedHashMap<Long,ShopOrder>();
        var reserved=new ArrayList<OrderStockAllocation>();
        BigDecimal total=BigDecimal.ZERO;
        for(var selection:selected) {
            var line=selection.cart(); var variant=selection.variant(); var product=variant.getProduct();
            var stock=inventory.lockAvailable(variant.getId());
            if(stock.stream().mapToLong(Inventory::getQuantity).sum()<line.getQuantity())
                throw rejected("Không đủ tồn kho: "+product.getName()+" ("+line.getSize()+"). Vui lòng cập nhật giỏ hàng.");
            var shopOrder=byShop.computeIfAbsent(product.getShop().getId(),key->{
                var value=new ShopOrder(); value.setShop(product.getShop()); value.setOrder(order);
                value.setSubtotal(BigDecimal.ZERO); value.setTotalAmount(BigDecimal.ZERO); return value;
            });
            int remaining=line.getQuantity();
            for(var row:stock) {
                int take=Math.min(remaining,row.getQuantity());
                if(take>0) {
                    row.setQuantity(row.getQuantity()-take); remaining-=take;
                    var allocation=new OrderStockAllocation(); allocation.setShopOrder(shopOrder);
                    allocation.setInventory(row); allocation.setQuantity(take); reserved.add(allocation);
                }
                if(remaining==0) break;
            }
            var price=variant.getPrice()==null?product.getBasePrice():variant.getPrice();
            var item=new OrderItem(); item.setProductVariant(variant); item.setProductName(product.getName());
            item.setVariantSku(variant.getSku()); item.setSize(variant.getSize()); item.setColor(variant.getColor());
            item.setQuantity(line.getQuantity()); item.setUnitPrice(price); item.setSubtotal(price.multiply(BigDecimal.valueOf(line.getQuantity())));
            shopOrder.addItem(item); shopOrder.setSubtotal(shopOrder.getSubtotal().add(item.getSubtotal()));
            shopOrder.setTotalAmount(shopOrder.getSubtotal()); total=total.add(item.getSubtotal());
        }
        BigDecimal discount=BigDecimal.ZERO;
        if(voucher!=null) {
            // Cumulative rounding keeps shop discounts nonnegative and their sum exact.
            discount=vouchers.discount(total);
            var runningSubtotal=BigDecimal.ZERO;
            var assigned=BigDecimal.ZERO;
            for(var part:byShop.values()) {
                runningSubtotal=runningSubtotal.add(part.getSubtotal());
                var amount=vouchers.discount(runningSubtotal).subtract(assigned);
                part.setDiscountAmount(amount);
                part.setTotalAmount(part.getSubtotal().subtract(amount)); assigned=assigned.add(amount);
            }
            order.setVoucherCode(voucher.getCode()); order.setDiscountAmount(discount);
        }
        if(total.subtract(discount).compareTo(request.getExpectedTotal())!=0)
            throw rejected("Giá sản phẩm đã thay đổi. Vui lòng kiểm tra tổng tiền mới trước khi đặt hàng.");
        order.setTotalAmount(total.subtract(discount)); orders.saveAndFlush(order);
        if(voucher!=null) voucher.setUsedOrder(order);
        allocations.saveAll(reserved);
        for(var shopOrder:byShop.values()) {
            var event=new OrderStatusHistory(); event.setShopOrder(shopOrder); event.setStatus(OrderStatus.NEW);
            event.setNote("Khách đặt hàng COD — chưa thanh toán."); history.save(event);
        }
        carts.deleteAll(cart);
        return order.getId();
    }

    public record Line(String name,String size,String color,int quantity,BigDecimal price,BigDecimal subtotal) {}
    public record ShopView(String name,String status,BigDecimal total,List<Line> items) {
        public String statusLabel() { return CheckoutService.statusLabel(status); }
    }
    public record Receipt(Long id,String receiverName,String receiverPhone,String shippingAddress,
            BigDecimal total,String paymentMethod,String paymentStatus,List<ShopView> shops,String placedAt,
            String voucherCode,BigDecimal discount) {
        public Receipt(Long id,String receiverName,String receiverPhone,String shippingAddress,
                BigDecimal total,String paymentMethod,String paymentStatus,List<ShopView> shops,String placedAt) {
            this(id,receiverName,receiverPhone,shippingAddress,total,paymentMethod,paymentStatus,shops,placedAt,null,BigDecimal.ZERO);
        }
        public boolean canCancel() { return shops.stream().anyMatch(s->"NEW".equals(s.status())); }
        public String code() { return "H2T-"+String.format(java.util.Locale.ROOT,"%06d",id); }
        public String status() {
            var states=shops.stream().map(ShopView::status).distinct().toList();
            return states.size()==1?states.getFirst():"MIXED";
        }
        public String statusLabel() { return CheckoutService.statusLabel(status()); }
        public int itemCount() { return shops.stream().flatMap(s->s.items().stream()).mapToInt(Line::quantity).sum(); }
        public String paymentStatusLabel() {
            return switch(paymentStatus) {
                case "UNPAID" -> "Chưa thanh toán";
                case "PAID" -> "Đã thanh toán";
                case "CANCELLED" -> "Đã hủy — không thu tiền";
                default -> paymentStatus;
            };
        }
    }
    public static String statusLabel(String status) {
        return switch(status) {
            case "NEW" -> "Chờ xác nhận";
            case "CONFIRMED" -> "Đã xác nhận";
            case "PICKED_UP" -> "Đã lấy hàng";
            case "SHIPPING" -> "Đang giao hàng";
            case "DELIVERED" -> "Đã giao hàng";
            case "CANCELLED" -> "Đã hủy";
            case "RETURN_REQUESTED" -> "Yêu cầu trả hàng";
            case "RETURNED" -> "Đã trả hàng";
            case "REFUNDED" -> "Đã hoàn tiền";
            default -> "Theo từng shop";
        };
    }
    private Receipt receipt(OrderEntity order) {
        return new Receipt(order.getId(),order.getReceiverName(),order.getReceiverPhone(),order.getShippingAddress(),
                order.getTotalAmount(),order.getPaymentMethod(),order.getPaymentStatus(),order.getShopOrders().stream()
                .map(s->new ShopView(s.getShop().getName(),s.getStatus().name(),s.getTotalAmount(),s.getItems().stream()
                .map(i->new Line(i.getProductName(),i.getSize(),i.getColor(),i.getQuantity(),i.getUnitPrice(),i.getSubtotal())).toList())).toList(),
                order.getOrderDate().atOffset(java.time.ZoneOffset.UTC).atZoneSameInstant(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))
                        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")),order.getVoucherCode(),order.getDiscountAmount());
    }
    @Transactional(readOnly=true)
    public Receipt get(String username,Long id) {
        return receipt(orders.findByIdAndUserUsername(id,username).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }
    @Transactional(readOnly=true)
    public List<Receipt> list(String username) {
        return orders.findByUserUsernameOrderByIdDesc(username).stream().map(this::receipt).toList();
    }
}
