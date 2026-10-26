package com.example.demo.service;

import com.example.demo.dto.AdminForms.*;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service @Transactional(readOnly=true) @PreAuthorize("hasRole('ADMIN')")
public class AdminManagementService {
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    private final ShopRepository shops; private final ProductRepository products; private final CategoryRepository categories;
    private final UserRepository users; private final StoreRepository stores; private final ProductVariantRepository variants;
    private final InventoryRepository inventory; private final ShopOrderRepository shopOrders; private final OrderEntityRepository orders;
    private final OrderStatusHistoryRepository history; private final OrderStockAllocationRepository allocations;
    private final CheckoutService checkout;
    private final OrderStockService orderStock;
    private final AccessService access;
    private final PurchaseConfirmationService confirmations;
    private final ShipmentRepository shipments;
    public AdminManagementService(ShopRepository shops,ProductRepository products,CategoryRepository categories,UserRepository users,
            StoreRepository stores,ProductVariantRepository variants,InventoryRepository inventory,ShopOrderRepository shopOrders,
            OrderEntityRepository orders,OrderStatusHistoryRepository history,OrderStockAllocationRepository allocations,CheckoutService checkout,AccessService access,OrderStockService orderStock,PurchaseConfirmationService confirmations,ShipmentRepository shipments) {
        this.shops=shops;this.products=products;this.categories=categories;this.users=users;this.stores=stores;
        this.variants=variants;this.inventory=inventory;this.shopOrders=shopOrders;this.orders=orders;this.history=history;
        this.allocations=allocations;this.checkout=checkout;this.access=access;this.orderStock=orderStock;
        this.confirmations=confirmations;this.shipments=shipments;
    }
    private <T> T found(Optional<T> value) { return value.orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND)); }
    private String text(String s) { return s==null?"":s; }
    private Map<String,Object> shopRow(Shop s) { return Map.of("id",s.getId(),"name",s.getName(),"slug",s.getSlug(),"owner",s.getOwner().getUsername(),"status",s.getStatus().name(),"enabled",s.isEnabled()); }
    public List<Map<String,Object>> shops() { return shops.findAll(Sort.by("id")).stream().map(this::shopRow).toList(); }
    public List<Map<String,Object>> branches(Long shopId) { return stores.findByShopId(shopId).stream().map(s->Map.<String,Object>of("id",s.getId(),"code",s.getCode(),"name",s.getName(),"address",s.getAddress(),"enabled",s.isEnabled())).toList(); }
    public Map<String,Long> totals() { return Map.of("shops",shops.count(),"products",products.count(),"orders",shopOrders.count(),"users",users.count()); }
    public List<Map<String,Object>> owners() { return users.findAll(Sort.by("username")).stream().filter(User::isEnabled).map(u->Map.<String,Object>of("id",u.getId(),"name",u.getUsername()+" — "+u.getFullName())).toList(); }
    public List<Map<String,Object>> categories() { return categories.findAll(Sort.by("name")).stream().map(c->Map.<String,Object>of("id",c.getId(),"name",c.getName(),"enabled",c.isEnabled())).toList(); }
    public ShopForm shopForm(Long id) {
        if(id==null) return new ShopForm("","",null,"","","",ShopStatus.ACTIVE,true);
        var s=found(shops.findById(id)); return new ShopForm(s.getName(),s.getSlug(),s.getOwner().getId(),s.getDescription(),s.getPhone(),s.getEmail(),s.getStatus(),s.isEnabled());
    }
    @Transactional public Long saveShop(Long id,ShopForm f) {
        var s=id==null?new Shop():found(shops.findById(id));
        if(id!=null && !s.getSlug().equals(f.slug())) throw new IllegalArgumentException("Không đổi đường dẫn shop đã tạo.");
        if(shops.findBySlug(f.slug()).filter(existing->!Objects.equals(existing.getId(),id)).isPresent()) throw new IllegalArgumentException("Đường dẫn shop đã tồn tại.");
        s.setOwner(found(users.findById(f.ownerId()).filter(User::isEnabled))); s.setName(f.name());s.setSlug(f.slug());
        s.setDescription(f.description());s.setPhone(f.phone());s.setEmail(f.email());s.setStatus(f.status());s.setEnabled(f.enabled());
        return shops.saveAndFlush(s).getId();
    }
    @Transactional public void archiveShop(Long id) { var s=found(shops.findById(id));s.setEnabled(false);s.setStatus(ShopStatus.SUSPENDED); }
    public ProductForm productForm(Long id) {
        if(id==null) return new ProductForm(null,null,"","",BigDecimal.ZERO,"/images/aosomi.png","",true);
        var p=found(products.findById(id));return new ProductForm(p.getShop().getId(),p.getCategory().getId(),p.getName(),p.getSlug(),p.getBasePrice(),p.getImageUrl(),p.getDescription(),p.isEnabled());
    }
    public Page<Map<String,Object>> products(String q,Long shopId,int page) {
        Specification<Product> spec=(root,cq,cb)->{
            var predicates=new ArrayList<jakarta.persistence.criteria.Predicate>();
            if(shopId!=null) predicates.add(cb.equal(root.get("shop").get("id"),shopId));
            var term=Product.normalize(q).replace("\\","\\\\").replace("%","\\%").replace("_","\\_");
            if(!term.isBlank())predicates.add(cb.like(root.get("searchName"),"%"+term+"%",'\\'));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return products.findAll(spec,PageRequest.of(Math.max(0,Math.min(page,10000)),20,Sort.by("id").descending())).map(p->Map.<String,Object>of(
                "id",p.getId(),"name",p.getName(),"slug",p.getSlug(),"price",p.getBasePrice(),"shop",p.getShop().getName(),"category",p.getCategory().getName(),"enabled",p.isEnabled()));
    }
    @Transactional public Long saveProduct(Long id,ProductForm f) {
        var p=id==null?new Product():found(products.findById(id));
        if(id!=null && (!p.getSlug().equals(f.slug()) || !p.getShop().getId().equals(f.shopId()))) throw new IllegalArgumentException("Không chuyển shop hoặc đổi đường dẫn sản phẩm đã tạo.");
        if(products.findBySlug(f.slug()).filter(existing->!Objects.equals(existing.getId(),id)).isPresent()) throw new IllegalArgumentException("Đường dẫn sản phẩm đã tồn tại.");
        if(f.image().contains("..") || f.image().contains("\\"))throw new IllegalArgumentException("Đường dẫn ảnh không hợp lệ.");
        p.setShop(found(shops.findById(f.shopId())));p.setCategory(found(categories.findById(f.categoryId())));p.setName(f.name());p.setSlug(f.slug());
        p.setBasePrice(f.price());p.setImageUrl(f.image());p.setDescription(f.description());p.setEnabled(f.enabled());return products.saveAndFlush(p).getId();
    }
    @Transactional public void archiveProduct(Long id) { found(products.findById(id)).setEnabled(false); }
    public Map<String,Object> productOptions(Long id) {
        var p=found(products.findById(id));
        var options=variants.findAll().stream().filter(v->v.getProduct().getId().equals(id)).map(v->Map.<String,Object>of("id",v.getId(),"sku",v.getSku(),"size",v.getSize(),"color",v.getColor(),"price",v.getPrice()==null?"":v.getPrice(),"enabled",v.isEnabled())).toList();
        var branches=stores.findByShopId(p.getShop().getId()).stream().map(s->Map.<String,Object>of("id",s.getId(),"code",s.getCode(),"name",s.getName(),"address",s.getAddress(),"enabled",s.isEnabled())).toList();
        var stock=inventory.findAll().stream().filter(i->i.getProductVariant().getProduct().getId().equals(id) && i.getStore().getShop().getId().equals(p.getShop().getId()))
                .map(i->Map.<String,Object>of("variantId",i.getProductVariant().getId(),"storeId",i.getStore().getId(),"sku",i.getProductVariant().getSku(),"store",i.getStore().getName(),"quantity",i.getQuantity())).toList();
        return Map.of("options",options,"branches",branches,"stock",stock,"productName",p.getName(),"shopId",p.getShop().getId());
    }
    @Transactional public void saveVariant(Long productId,Long id,VariantForm f) {
        var p=found(products.findById(productId));var v=id==null?new ProductVariant():found(variants.lockById(id));
        if(id!=null && !v.getProduct().getId().equals(productId))throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if(id!=null && (!v.getSize().equals(f.size()) || !v.getColor().equals(f.color()) || !v.getSku().equals(f.sku())))
            throw new IllegalArgumentException("SKU, size và màu đã tạo được giữ cố định để bảo toàn giỏ hàng. Hãy tắt phiên bản cũ và tạo phiên bản mới.");
        if(variants.findBySku(f.sku()).filter(existing->!Objects.equals(existing.getId(),id)).isPresent())throw new IllegalArgumentException("SKU đã tồn tại.");
        if(p.getVariants().stream().anyMatch(existing->!Objects.equals(existing.getId(),id)&&existing.getSize().equals(f.size())&&existing.getColor().equals(f.color())))throw new IllegalArgumentException("Size/màu này đã tồn tại.");
        v.setProduct(p);v.setSku(f.sku());v.setSize(f.size());v.setColor(f.color());v.setPrice(f.price());v.setEnabled(f.enabled());variants.saveAndFlush(v);
    }
    @Transactional public void saveStore(Long shopId,Long id,StoreForm f) {
        var s=id==null?new Store():found(stores.findById(id));
        if(id!=null&&!s.getShop().getId().equals(shopId))throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if(stores.findByCode(f.code()).filter(existing->!Objects.equals(existing.getId(),id)).isPresent())throw new IllegalArgumentException("Mã chi nhánh đã tồn tại.");
        s.setShop(found(shops.findById(shopId)));s.setCode(f.code());s.setName(f.name());s.setAddress(f.address());s.setEnabled(f.enabled());stores.saveAndFlush(s);
    }
    @Transactional public void saveStock(Long productId,StockForm f) {
        var v=found(variants.lockById(f.variantId()));var s=found(stores.findById(f.storeId()));
        if(!v.getProduct().getId().equals(productId)||!s.getShop().getId().equals(v.getProduct().getShop().getId()))throw new IllegalArgumentException("Phiên bản và chi nhánh phải thuộc cùng shop của sản phẩm.");
        var existing=inventory.findByStoreIdAndProductVariantId(s.getId(),v.getId());
        var i=existing.isPresent()?found(inventory.lockById(existing.get().getId())):new Inventory();
        i.setProductVariant(v);i.setStore(s);i.setQuantity(f.quantity());inventory.saveAndFlush(i);
    }
    @Transactional public void saveCategory(Long id,CategoryForm f) {
        if(categories.findByName(f.name()).stream().anyMatch(c->!Objects.equals(c.getId(),id)))throw new IllegalArgumentException("Tên danh mục đã tồn tại.");
        var c=id==null?new Category():found(categories.findById(id));c.setName(f.name());c.setEnabled(f.enabled());categories.saveAndFlush(c);
    }
    public record CancellationNotice(Long id,Long shopOrderId,String shop,String note,String time) {}
    private List<CancellationNotice> notices(Long ownerId) {
        return history.customerCancellations(ownerId,PageRequest.of(0,20)).stream()
                .map(h->new CancellationNotice(h.getId(),h.getShopOrder().getId(),h.getShopOrder().getShop().getName(),h.getNote(),
                    h.getChangedAt().atOffset(java.time.ZoneOffset.UTC).atZoneSameInstant(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))
                        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")))).toList();
    }
    public List<CancellationNotice> cancellationNotices() { return notices(null); }
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    public List<CancellationNotice> sellerCancellationNotices(String actor) {
        var user=access.account(actor);
        if(!access.has(user,RoleName.ROLE_VENDOR) && !access.has(user,RoleName.ROLE_ADMIN))
            throw new org.springframework.security.access.AccessDeniedException("Bạn chưa có quyền quản lý shop.");
        return notices(user.getId());
    }
    public record OrderRow(Long id,Long orderId,String shop,String customer,String status,String statusLabel,BigDecimal total,String codSettlementLabel) {}
    private OrderRow orderRow(ShopOrder s) { return new OrderRow(s.getId(),s.getOrder().getId(),s.getShop().getName(),s.getOrder().getUser().getUsername(),s.getStatus().name(),CheckoutService.statusLabel(s.getStatus().name()),s.getTotalAmount(),
            (String)CodSettlementService.describe(shipments.findByShopOrderId(s.getId()).orElse(null)).get("label")); }
    public Page<OrderRow> orders(int page) { return shopOrders.findAll(PageRequest.of(Math.max(0,Math.min(page,10000)),20,Sort.by("id").descending())).map(this::orderRow); }
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    public Page<OrderRow> sellerOrders(String actor,int page) {
        var user=access.account(actor);
        if(!access.has(user,RoleName.ROLE_VENDOR) && !access.has(user,RoleName.ROLE_ADMIN))
            throw new org.springframework.security.access.AccessDeniedException("Bạn chưa có quyền quản lý shop.");
        return shopOrders.findByShopOwnerId(user.getId(),PageRequest.of(Math.max(0,Math.min(page,10000)),20,Sort.by("id").descending())).map(this::orderRow);
    }
    private ShopOrder ownedOrder(String actor,Long id) {
        var user=access.account(actor);
        if(!access.has(user,RoleName.ROLE_VENDOR) && !access.has(user,RoleName.ROLE_ADMIN))
            throw new org.springframework.security.access.AccessDeniedException("Bạn chưa có quyền quản lý shop.");
        var s=found(shopOrders.findById(id));
        if(!s.getShop().getOwner().getId().equals(user.getId()))
            throw new org.springframework.security.access.AccessDeniedException("Đơn hàng không thuộc shop của bạn.");
        return s;
    }
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    public Map<String,Object> sellerOrder(String actor,Long id) {
        var s=ownedOrder(actor,id);
        var parent=s.getOrder();
        var view=new CheckoutService.ShopView(s.getShop().getName(),s.getStatus().name(),s.getTotalAmount(),s.getItems().stream()
                .map(i->new CheckoutService.Line(i.getProductName(),i.getSize(),i.getColor(),i.getQuantity(),i.getUnitPrice(),i.getSubtotal())).toList());
        var payment=s.getStatus()==OrderStatus.CANCELLED?"CANCELLED":s.getStatus()==OrderStatus.DELIVERED?"PAID":"UNPAID";
        var receipt=new CheckoutService.Receipt(parent.getId(),parent.getReceiverName(),parent.getReceiverPhone(),parent.getShippingAddress(),
                s.getTotalAmount(),parent.getPaymentMethod(),"COD".equals(parent.getPaymentMethod())?payment:parent.getPaymentStatus(),List.of(view),
                parent.getOrderDate().atOffset(java.time.ZoneOffset.UTC).atZoneSameInstant(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))
                      .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")),parent.getVoucherCode(),s.getDiscountAmount());
        return orderDetails(s,receipt);
    }
    public Map<String,Object> order(Long id) {
        var s=found(shopOrders.findById(id));
        var result=new LinkedHashMap<>(orderDetails(s,checkout.get(s.getOrder().getUser().getUsername(),s.getOrder().getId())));
        if(shipments.findByShopOrderId(id).filter(sh->sh.getStatus()==ShipmentStatus.FAILED).isPresent()) {
            result.put("nextStatuses",List.of(OrderStatus.CANCELLED));result.put("returnToStock",true);
        }
        return result;
    }
    private Map<String,Object> orderDetails(ShopOrder s,CheckoutService.Receipt receipt) {
        var id=s.getId();
        var events=history.findByShopOrderIdOrderByChangedAtAscIdAsc(id).stream().sorted(Comparator.comparing(OrderStatusHistory::getId))
                .map(h->Map.<String,Object>of("status",CheckoutService.statusLabel(h.getStatus().name()),"note",text(h.getNote()),"time",h.getChangedAt().atOffset(java.time.ZoneOffset.UTC).atZoneSameInstant(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")))).toList();
        return Map.of("shopOrder",orderRow(s),"order",receipt,"nextStatuses",next(s.getStatus()),"events",events,
                "codSettlement",CodSettlementService.describe(shipments.findByShopOrderId(id).orElse(null)));
    }
    private List<OrderStatus> next(OrderStatus status) {
        return switch(status) {
            case NEW -> List.of(OrderStatus.CONFIRMED,OrderStatus.CANCELLED);
            case CONFIRMED -> List.of(OrderStatus.CANCELLED);
            default -> List.of();
        };
    }
    @Transactional @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    public void sellerChangeStatus(String actor,Long id,StatusForm form) {
        ownedOrder(actor,id);
        updateStatus(actor,id,form);
    }
    @Transactional public void changeStatus(String actor,Long id,StatusForm form) {
        updateStatus(actor,id,form);
    }
    private void updateStatus(String actor,Long id,StatusForm form) {
        var parent=found(orders.lockById(found(shopOrders.parentOrderId(id))));
        var s=found(shopOrders.findById(id));
        // Seller ownership checks can load the entity before the parent lock is acquired.
        entityManager.refresh(s);
        boolean alreadyCompleted = confirmations.completed(parent);
        var target=form.status();
        if(s.getStatus()==target)return;
        var shipment=shipments.findByShopOrderId(id);
        boolean returning=target==OrderStatus.CANCELLED && s.getStatus()==OrderStatus.SHIPPING
                && shipment.filter(sh->sh.getStatus()==ShipmentStatus.FAILED).isPresent()
                && access.has(access.account(actor),RoleName.ROLE_ADMIN);
        if(!returning&&!next(s.getStatus()).contains(target))throw new IllegalArgumentException("Không thể chuyển trạng thái đơn theo cách này.");
        if(target==OrderStatus.CANCELLED) {
            if(form.note()==null||form.note().isBlank())throw new IllegalArgumentException("Vui lòng ghi lý do hủy hoặc từ chối đơn.");
            if(shipment.isPresent() && shipment.get().getStatus()!=ShipmentStatus.ASSIGNED && !returning)
                throw new IllegalArgumentException("Hàng đã được lấy; chỉ Admin xác nhận nhận lại hàng sau giao thất bại mới được hoàn kho.");
            orderStock.restore(s);
            shipment.ifPresent(sh->sh.setStatus(ShipmentStatus.CANCELLED));
            parent.setTotalAmount(parent.getTotalAmount().subtract(s.getTotalAmount()));
            s.setTotalAmount(BigDecimal.ZERO);
        }
        s.setStatus(target);
        var event=new OrderStatusHistory();event.setShopOrder(s);event.setStatus(target);event.setNote("Shop/Admin "+actor+": "+(returning?"Đã nhận lại hàng và hoàn tồn kho. ":"")+text(form.note()));
        if(event.getNote().length()>2000)event.setNote(event.getNote().substring(0,2000));history.save(event);
        var all=parent.getShopOrders();
        if(all.stream().allMatch(o->o.getStatus()==OrderStatus.CANCELLED))parent.setPaymentStatus("CANCELLED");
        else if("COD".equals(parent.getPaymentMethod()) && all.stream().allMatch(o->o.getStatus()==OrderStatus.DELIVERED||o.getStatus()==OrderStatus.CANCELLED))parent.setPaymentStatus("PAID");
        confirmations.notifyIfCompleted(parent, alreadyCompleted);
    }
}
