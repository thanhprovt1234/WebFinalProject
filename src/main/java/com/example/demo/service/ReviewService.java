package com.example.demo.service;

import com.example.demo.dto.ReviewRequest;
import com.example.demo.entity.ProductReview;
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

    public ReviewService(ProductReviewRepository reviews, UserRepository users, CatalogService catalog) {
        this.reviews = reviews;
        this.users = users;
        this.catalog = catalog;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> summary(String productId, String username) {
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
        entries.stream().filter(r -> r.getUser().getUsername().equals(username)).findFirst().ifPresent(r -> {
            form.setRating(r.getRating());
            form.setComment(r.getComment());
        });
        double average = entries.stream().mapToInt(ProductReview::getRating).average().orElse(0);
        return Map.of("comments", comments, "total", entries.size(), "average", String.format(Locale.ROOT, "%.1f", average),
                "roundedAverage", Math.round(average), "distribution", distribution, "form", form);
    }

    @Transactional
    public void save(String productId, String username, ReviewRequest form) {
        catalog.findProduct(productId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var user = users.findByUsername(username).filter(u -> u.isEnabled())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        var review = reviews.findByCatalogProductIdAndUserId(productId, user.getId()).orElseGet(ProductReview::new);
        review.setCatalogProductId(productId);
        review.setUser(user);
        review.setRating(form.getRating());
        review.setComment(form.getComment());
        reviews.saveAndFlush(review);
    }
}
