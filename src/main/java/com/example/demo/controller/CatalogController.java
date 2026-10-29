package com.example.demo.controller;

import com.example.demo.service.CatalogService;
import com.example.demo.service.ReviewService;
import com.example.demo.service.ProductActivityService;
import com.example.demo.dto.ReviewRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Controller
public class CatalogController {
    private final CatalogService catalogService;
    private final ReviewService reviewService;
    private final ProductActivityService activities;

    public CatalogController(CatalogService catalogService, ReviewService reviewService, ProductActivityService activities) {
        this.catalogService = catalogService;
        this.reviewService = reviewService;
        this.activities = activities;
    }

    @GetMapping("/products/{id}")
    public String detail(@PathVariable String id, @RequestParam(required = false) Long orderId, Model model, Principal principal) {
        var product = catalogService.findProduct(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm"));
        model.addAttribute("product", product);
        model.addAttribute("colors", catalogService.colorsFor(product));
        model.addAttribute("variants", catalogService.options(id));
        model.addAttribute("relatedProducts", catalogService.relatedTo(id));
        model.addAttribute("favorite", principal != null && activities.isFavorite(principal.getName(), id));
        var reviews = reviewService.summary(id, principal == null ? null : principal.getName(), orderId);
        model.addAttribute("reviews", reviews);
        if (!model.containsAttribute("reviewForm")) {
            model.addAttribute("reviewForm", reviews.get("form"));
        }
        return "catalog/product-detail";
    }

    @PostMapping("/products/{id}/reviews")
    public String review(@PathVariable String id, @Valid @ModelAttribute("reviewForm") ReviewRequest form,
            BindingResult errors, Principal principal, Model model, RedirectAttributes redirect) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        if (errors.hasErrors()) {
            model.addAttribute("reviewError", "Vui lòng chọn từ 1–5 sao và nhập nhận xét từ 1–2.000 ký tự.");
            return detail(id, form.getOrderId(), model, principal);
        }
        try {
            reviewService.save(id, principal.getName(), form);
        } catch (DataIntegrityViolationException ex) {
            model.addAttribute("reviewError", "Đánh giá vừa được gửi ở cửa sổ khác. Vui lòng tải lại trang và thử lại.");
            return detail(id, form.getOrderId(), model, principal);
        }
        redirect.addFlashAttribute("reviewSuccess", "Đánh giá của bạn đã được lưu. Cảm ơn bạn!");
        return "redirect:/products/" + id + "?orderId=" + form.getOrderId() + "#reviews-title";
    }

    @GetMapping("/collections/ao-thun-co-tron")
    public String crewNeck(@RequestParam(defaultValue = "featured") String sort,
            @RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page, Model model) {
        return collection(sort,q,catalogService.crewNeckCategory(),"",page,model);
    }

    @GetMapping("/collections")
    public String collection(@RequestParam(defaultValue = "featured") String sort,
            @RequestParam(defaultValue = "") String q, @RequestParam(required = false) Long category,
            @RequestParam(defaultValue = "") String shop, @RequestParam(defaultValue = "0") int page, Model model) {
        String selectedSort = switch (sort) {
            case "price-asc", "price-desc", "newest" -> sort;
            default -> "featured";
        };
        var result=catalogService.search(selectedSort,q,category,shop,page);
        model.addAttribute("products", result.getContent());
        model.addAttribute("catalogPage",result);
        model.addAttribute("selectedCategory",category);
        model.addAttribute("selectedShop",shop);
        model.addAttribute("shops",catalogService.shops());
        model.addAttribute("collectionTitle",catalogService.categories().stream().filter(c->c.get("id").equals(category))
                .map(c->c.get("name")).findFirst().orElse("Tất cả sản phẩm"));
        model.addAttribute("query", q.strip());
        model.addAttribute("sort", selectedSort);
        return "catalog/crew-neck";
    }
}
