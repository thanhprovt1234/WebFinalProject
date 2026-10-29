package com.example.demo.service;

import com.example.demo.dto.ReviewRequest;
import com.example.demo.entity.ProductReview;
import com.example.demo.repository.OrderItemRepository;
import com.example.demo.repository.ProductReviewRepository;
import com.example.demo.repository.UserRepository;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReviewService {
    private final ProductReviewRepository reviews;
    private final UserRepository users;
    private final CatalogService catalog;
    private final OrderItemRepository orderItems;

    public ReviewService(ProductReviewRepository reviews, UserRepository users, CatalogService catalog,
            OrderItemRepository orderItems) {
        this.reviews = reviews;
        this.users = users;
        this.catalog = catalog;
        this.orderItems = orderItems;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> summary(String productId, String username, Long orderId) {
        var purchases = username != null && users.findByUsername(username).filter(u -> u.isEnabled()).isPresent()
                ? orderItems.findReviewableOrders(username, productId)
                : List.<com.example.demo.entity.OrderEntity>of();
        var entries = reviews.findByCatalogProductIdOrderByUpdatedAtDescIdDesc(productId);
        var dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        var comments = entries.stream().map(review -> Map.<String, Object>of(
                "author", review.getUser().getFullName(), "rating", review.getRating(),
                "comment", review.getComment(), "date", review.getUpdatedAt().atOffset(ZoneOffset.UTC)
                        .atZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh")).format(dateFormat))).toList();
        List<Map<String, Object>> distribution = new ArrayList<>();
        for (int star = 5; star >= 1; star--) {
            int rating = star;
            long count = entries.stream().filter(r -> r.getRating() == rating).count();
            distribution.add(Map.of("stars", star, "count", count,
                    "percent", entries.isEmpty() ? 0 : Math.round(count * 100.0 / entries.size())));
        }
        var form = new ReviewRequest();
        var selected = purchases.stream().filter(o -> Objects.equals(o.getId(), orderId)).findFirst();
        if (orderId == null) {
            selected = purchases.stream().filter(o -> entries.stream()
                    .noneMatch(r -> r.getOrder() != null && r.getOrder().getId().equals(o.getId()))).findFirst();
            if (selected.isEmpty()) selected = purchases.stream().findFirst();
        }
        selected.ifPresent(o -> {
            form.setOrderId(o.getId());
            entries.stream().filter(r -> r.getUser().getUsername().equals(username)
                    && r.getOrder() != null && r.getOrder().getId().equals(o.getId())).findFirst().ifPresent(r -> {
                form.setRating(r.getRating());
                form.setComment(r.getComment());
            });
        });
        var purchaseOptions = purchases.stream().map(o -> Map.<String, Object>of(
                "id", o.getId(), "reviewed", entries.stream().anyMatch(r -> r.getOrder() != null
                        && r.getOrder().getId().equals(o.getId())))).toList();
        double average = entries.stream().mapToInt(ProductReview::getRating).average().orElse(0);
        return Map.of("comments", comments, "total", entries.size(), "average", String.format(Locale.ROOT, "%.1f", average),
                "roundedAverage", Math.round(average), "distribution", distribution, "form", form,
                "canReview", selected.isPresent(), "purchases", purchaseOptions);
    }

    @Transactional
    public void save(String productId, String username, ReviewRequest form) {
        catalog.findProduct(productId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var user = users.findByUsername(username).filter(u -> u.isEnabled())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        var purchase = orderItems.findReviewableOrders(username, productId).stream()
                .filter(o -> o.getId().equals(form.getOrderId())).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Bạn chỉ có thể đánh giá sản phẩm trong đơn hàng của mình đã giao thành công."));
        var review = reviews.findByCatalogProductIdAndOrderId(productId, purchase.getId()).orElseGet(ProductReview::new);
        review.setOrder(purchase);
        review.setCatalogProductId(productId);
        review.setUser(user);
        review.setRating(form.getRating());
        review.setComment(form.getComment());
        reviews.saveAndFlush(review);
    }
}
