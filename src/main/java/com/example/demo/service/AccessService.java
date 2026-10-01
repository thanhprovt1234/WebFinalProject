package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service @Transactional(readOnly=true)
public class AccessService {
    private final UserRepository users; private final ShopRepository shops; private final StoreRepository stores;
    private final StoreManagerRepository managers; private final ShipmentRepository shipments;
    private final ProductRepository products; private final InventoryRepository inventory;
    public AccessService(UserRepository users,ShopRepository shops,StoreRepository stores,StoreManagerRepository managers,
            ShipmentRepository shipments,ProductRepository products,InventoryRepository inventory) {
        this.users=users;this.shops=shops;this.stores=stores;this.managers=managers;this.shipments=shipments;this.products=products;this.inventory=inventory;
    }
    public User account(String name) {
        return users.findByUsername(name).filter(User::isEnabled).orElseThrow(()->new AccessDeniedException("Tài khoản không được phép truy cập."));
    }
    public boolean has(User user,RoleName role){return user.getRoles().stream().anyMatch(r->r.getName()==role);}
    public Set<String> roleNames(String name) {
        var result=new HashSet<String>();account(name).getRoles().forEach(r->result.add(r.getName().name()));return result;
    }
    private User require(String name,RoleName role) {
        var user=account(name);if(!has(user,RoleName.ROLE_ADMIN)&&!has(user,role))throw new AccessDeniedException("Bạn không có quyền truy cập khu vực này.");return user;
    }
    private void deny(){throw new AccessDeniedException("Bạn chưa được phân quyền với dữ liệu này.");}
    private Map<String,Object> shopRow(Shop s){return Map.of("id",s.getId(),"name",s.getName(),"slug",s.getSlug(),"status",s.getStatus().name());}
    private Map<String,Object> storeRow(Store s){return Map.of("id",s.getId(),"name",s.getName(),"shop",s.getShop().getName(),"code",s.getCode());}

    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    public List<Map<String,Object>> sellerShops(String name) {
        var user=require(name,RoleName.ROLE_VENDOR);
        return (has(user,RoleName.ROLE_ADMIN)?shops.findAll():shops.findByOwnerId(user.getId())).stream().map(this::shopRow).toList();
    }
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    public Map<String,Object> sellerShop(String name,Long id) {
        var user=require(name,RoleName.ROLE_VENDOR);
        var shop=shops.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(!has(user,RoleName.ROLE_ADMIN)&&!shop.getOwner().getId().equals(user.getId()))deny();
        var rows=products.findByShopId(id).stream().map(p->Map.<String,Object>of("name",p.getName(),"slug",p.getSlug(),"price",p.getBasePrice(),"enabled",p.isEnabled())).toList();
        return Map.of("shop",shopRow(shop),"products",rows);
    }
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public List<Map<String,Object>> managerStores(String name) {
        var user=require(name,RoleName.ROLE_MANAGER);
        return (has(user,RoleName.ROLE_ADMIN)?stores.findAll():managers.findByUserId(user.getId()).stream().map(StoreManager::getStore).toList())
                .stream().map(this::storeRow).toList();
    }
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Map<String,Object> managerStore(String name,Long id) {
        var user=require(name,RoleName.ROLE_MANAGER);
        if(!has(user,RoleName.ROLE_ADMIN)&&!managers.existsByUserIdAndStoreId(user.getId(),id))deny();
        var store=stores.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        var rows=inventory.findByStoreId(id).stream().filter(i->i.getProductVariant().getProduct().getShop().getId().equals(store.getShop().getId()))
                .map(i->Map.<String,Object>of("name",i.getProductVariant().getProduct().getName(),"size",i.getProductVariant().getSize(),"color",i.getProductVariant().getColor(),"quantity",i.getQuantity())).toList();
        return Map.of("store",storeRow(store),"inventory",rows);
    }
    private Map<String,Object> shipmentRow(Shipment s){return Map.of("id",s.getId(),"orderId",s.getShopOrder().getId(),"shop",s.getShopOrder().getShop().getName(),"status",s.getStatus().name());}
    @PreAuthorize("hasAnyRole('SHIPPER','ADMIN')")
    public List<Map<String,Object>> shipperShipments(String name) {
        var user=require(name,RoleName.ROLE_SHIPPER);
        return (has(user,RoleName.ROLE_ADMIN)?shipments.findAll():shipments.findByShipperId(user.getId())).stream().map(this::shipmentRow).toList();
    }
    @PreAuthorize("hasAnyRole('SHIPPER','ADMIN')")
    public Map<String,Object> shipperShipment(String name,Long id) {
        var user=require(name,RoleName.ROLE_SHIPPER);
        var shipment=shipments.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(!has(user,RoleName.ROLE_ADMIN)&&!shipment.getShipper().getId().equals(user.getId()))deny();
        return shipmentRow(shipment);
    }
}
