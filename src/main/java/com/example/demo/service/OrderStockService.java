package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service @Transactional
public class OrderStockService {
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    private final InventoryRepository inventory;
    private final OrderStockAllocationRepository allocations;
    public OrderStockService(InventoryRepository inventory,OrderStockAllocationRepository allocations) {
        this.inventory=inventory;this.allocations=allocations;
    }
    private <T> T found(Optional<T> value) { return value.orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND)); }
    public void restore(ShopOrder s) {
        var rows=allocations.findByShopOrderIdOrderByInventoryIdAsc(s.getId());
        if(rows.isEmpty()) {
            // Legacy checkout deducted from the only matching inventory row, when unambiguous.
            rows=new ArrayList<>();
            for(var item:s.getItems()) {
                var candidates=inventory.findByProductVariantIdOrderByIdAsc(item.getProductVariant().getId()).stream()
                        .filter(i->i.getStore().getShop().getId().equals(s.getShop().getId())).toList();
                if(candidates.size()!=1)throw new IllegalArgumentException("Đơn cũ chưa lưu chi nhánh xuất kho và có nhiều hoặc không có kho phù hợp. Cần đối soát tồn kho trước khi hủy.");
                var allocation=new OrderStockAllocation();allocation.setShopOrder(s);allocation.setInventory(candidates.getFirst());allocation.setQuantity(item.getQuantity());rows.add(allocation);
            }
            rows=allocations.saveAll(rows);
        }
        rows.sort(Comparator.comparing(r->r.getInventory().getId()));
        for(var allocation:rows) {
            if(allocation.isRestored())continue;
            var row=found(inventory.lockById(allocation.getInventory().getId()));
            entityManager.refresh(row,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            row.setQuantity(Math.addExact(row.getQuantity(),allocation.getQuantity()));allocation.setRestored(true);
        }
    }
}
