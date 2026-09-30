package com.example.demo.controller;

import com.example.demo.service.ProductActivityService;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProductActivityController {
    private final ProductActivityService activities;
    public ProductActivityController(ProductActivityService activities) { this.activities = activities; }

    @GetMapping("/favorites")
    public String favorites(Principal principal, @RequestParam(defaultValue = "0") int page, Model model) {
        return list(principal, true, page, model);
    }
    @GetMapping("/recently-viewed")
    public String recentlyViewed(Principal principal, @RequestParam(defaultValue = "0") int page, Model model) {
        return list(principal, false, page, model);
    }
    private String list(Principal principal, boolean favorites, int page, Model model) {
        var result = activities.list(principal.getName(), favorites, page);
        model.addAttribute("activityPage", result);
        model.addAttribute("products", result.getContent());
        model.addAttribute("favoritesPage", favorites);
        return "catalog/activity";
    }

    @PostMapping("/products/{id}/favorite")
    public String favorite(Principal principal, @PathVariable String id, @RequestParam boolean saved,
            @RequestParam(defaultValue = "detail") String source, @RequestParam(defaultValue = "0") int page,
            RedirectAttributes redirect) {
        activities.favorite(principal.getName(), id, saved);
        redirect.addFlashAttribute("activitySuccess", saved ? "Đã thêm vào sản phẩm yêu thích." : "Đã bỏ yêu thích sản phẩm.");
        return switch (source) {
            case "favorites" -> "redirect:/favorites?page=" + Math.max(0, Math.min(page, 10000));
            case "history" -> "redirect:/recently-viewed?page=" + Math.max(0, Math.min(page, 10000));
            default -> "redirect:/products/" + id;
        };
    }

    @PostMapping("/api/products/{id}/viewed")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void viewed(Principal principal, @PathVariable String id) {
        activities.viewed(principal.getName(), id);
    }
}
