package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class RevenueService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final ShopRepository shops;
    private final ShopOrderRepository orders;
    private final StoreManagerRepository managers;
    private final AccessService access;

    public RevenueService(ShopRepository shops, ShopOrderRepository orders,
            StoreManagerRepository managers, AccessService access) {
        this.shops = shops; this.orders = orders; this.managers = managers; this.access = access;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> admin(LocalDate from, LocalDate to, Long shopId) {
        return report(shops.findAll(Sort.by("id")), from, to, shopId);
    }

    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    public Map<String, Object> seller(String actor, LocalDate from, LocalDate to, Long shopId) {
        var user = access.account(actor);
        return report(access.has(user, RoleName.ROLE_ADMIN) ? shops.findAll(Sort.by("id"))
                : shops.findByOwnerId(user.getId()), from, to, shopId);
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Map<String, Object> manager(String actor, LocalDate from, LocalDate to, Long shopId) {
        var user = access.account(actor);
        var visible = access.has(user, RoleName.ROLE_ADMIN) ? shops.findAll(Sort.by("id"))
                : managers.findByUserId(user.getId()).stream().map(m -> m.getStore().getShop()).distinct().toList();
        return report(visible, from, to, shopId);
    }

    private Map<String, Object> report(List<Shop> visible, LocalDate from, LocalDate to, Long shopId) {
        var today = LocalDate.now(ZONE);
        if (from == null) from = today.withDayOfMonth(1);
        if (to == null) to = today;
        if (from.isAfter(to) || from.getYear() < 1900 || to.getYear() > 9998)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khoảng ngày không hợp lệ.");
        if (shopId != null && visible.stream().noneMatch(s -> s.getId().equals(shopId)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem shop này.");
        var selected = visible.stream().filter(s -> shopId == null || s.getId().equals(shopId)).toList();
        var ids = selected.stream().map(Shop::getId).toList();
        var aggregates = ids.isEmpty() ? List.<Object[]>of() : orders.revenueSummary(ids,
                from.atStartOfDay(ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime(),
                to.plusDays(1).atStartOfDay(ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());
        var assignments = ids.isEmpty() ? List.<StoreManager>of() : managers.findForRevenue(ids);
        var rows = new ArrayList<Map<String, Object>>();
        long total = 0, sold = 0, cancelled = 0;
        BigDecimal revenue = BigDecimal.ZERO;
        for (var shop : selected) {
            long count = 0, delivered = 0, canceled = 0;
            BigDecimal amount = BigDecimal.ZERO;
            for (var a : aggregates) {
                if (!shop.getId().equals(a[0])) continue;
                long n = ((Number) a[2]).longValue(); count += n;
                if (a[1] == OrderStatus.DELIVERED) { delivered += n; amount = amount.add((BigDecimal) a[3]); }
                if (a[1] == OrderStatus.CANCELLED) canceled += n;
            }
            var names = assignments.stream().filter(m -> m.getStore().getShop().getId().equals(shop.getId()))
                    .map(m -> m.getUser().getUsername() + " — " + m.getStore().getName()).distinct().sorted().toList();
            rows.add(Map.of("id", shop.getId(), "name", shop.getName(), "owner", shop.getOwner().getUsername(),
                    "managers", names, "total", count, "sold", delivered, "pending", count - delivered - canceled,
                    "cancelled", canceled, "revenue", amount));
            total += count; sold += delivered; cancelled += canceled; revenue = revenue.add(amount);
        }
        return Map.of("rows", rows, "shops", visible.stream().map(s -> Map.of("id", s.getId(), "name", s.getName())).toList(),
                "from", from, "to", to, "selectedShop", shopId == null ? "" : shopId,
                "summary", Map.of("total", total, "sold", sold, "pending", total - sold - cancelled,
                        "cancelled", cancelled, "revenue", revenue));
    }
}
