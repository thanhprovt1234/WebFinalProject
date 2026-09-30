package com.example.demo.service;

import com.example.demo.entity.ProductActivity;
import com.example.demo.entity.User;
import com.example.demo.repository.ProductActivityRepository;
import com.example.demo.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductActivityService {
    private final ProductActivityRepository activities;
    private final UserRepository users;
    private final CatalogService catalog;

    public ProductActivityService(ProductActivityRepository activities, UserRepository users, CatalogService catalog) {
        this.activities = activities; this.users = users; this.catalog = catalog;
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(String username, String productId) {
        return activities.findByUserIdAndCatalogProductId(user(username, false).getId(), productId)
                .map(ProductActivity::isFavorite).orElse(false);
    }

    @Transactional
    public void favorite(String username, String productId, boolean desired) {
        var user = user(username, true);
        // Removing a favorite remains possible after a product leaves the catalog.
        if (desired) requireProduct(productId);
        var existing = activities.findByUserIdAndCatalogProductId(user.getId(), productId);
        if (existing.isEmpty() && !desired) return;
        var activity = existing.orElseGet(() -> newActivity(user, productId));
        if (desired && !activity.isFavorite()) activity.setFavoritedAt(LocalDateTime.now(ZoneOffset.UTC));
        activity.setFavorite(desired);
        if (!desired) activity.setFavoritedAt(null);
        if (!desired && activity.getViewedAt() == null) activities.delete(activity);
        else activities.save(activity);
    }

    @Transactional
    public void viewed(String username, String productId) {
        requireProduct(productId);
        var user = user(username, true);
        var activity = activities.findByUserIdAndCatalogProductId(user.getId(), productId)
                .orElseGet(() -> newActivity(user, productId));
        activity.setViewedAt(LocalDateTime.now(ZoneOffset.UTC));
        activities.save(activity);
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> list(String username, boolean favorites, int page) {
        var user = user(username, false);
        var pageable = PageRequest.of(Math.max(0, Math.min(page, 10000)), 20);
        var result = favorites
                ? activities.findByUserIdAndFavoriteTrueOrderByFavoritedAtDescIdDesc(user.getId(), pageable)
                : activities.findByUserIdAndViewedAtIsNotNullOrderByViewedAtDescIdDesc(user.getId(), pageable);
        if (result.isEmpty() && result.getTotalElements() > 0 && pageable.getPageNumber() > 0) {
            var last = PageRequest.of(result.getTotalPages() - 1, 20);
            result = favorites
                    ? activities.findByUserIdAndFavoriteTrueOrderByFavoritedAtDescIdDesc(user.getId(), last)
                    : activities.findByUserIdAndViewedAtIsNotNullOrderByViewedAtDescIdDesc(user.getId(), last);
        }
        return result.map(activity -> {
            var product = catalog.findProduct(activity.getCatalogProductId());
            Map<String, Object> row = new HashMap<>(product.orElse(Map.of("id", activity.getCatalogProductId(),
                    "name", "Sản phẩm không còn kinh doanh")));
            row.put("available", product.isPresent());
            row.put("favorite", activity.isFavorite());
            return row;
        });
    }

    private ProductActivity newActivity(User user, String productId) {
        var activity = new ProductActivity(); activity.setUser(user); activity.setCatalogProductId(productId);
        return activity;
    }
    private void requireProduct(String id) {
        catalog.findProduct(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    private User user(String username, boolean lock) {
        return (lock ? users.findForActivityUpdate(username) : users.findByUsername(username)).filter(User::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
    }
}
