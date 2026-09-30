package com.example.demo.controller;

import com.example.demo.dto.CartAddRequest;
import com.example.demo.dto.CartQuantityRequest;
import com.example.demo.service.CartService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartService carts;
    public CartController(CartService carts) { this.carts = carts; }

    @GetMapping
    public CartService.CartView get(Principal principal) { return carts.get(principal.getName()); }

    @PostMapping("/items")
    public CartService.CartView add(Principal principal, @Valid @ModelAttribute CartAddRequest request) {
        return carts.add(principal.getName(), request);
    }

    @PatchMapping("/items/{id}")
    public CartService.CartView update(Principal principal, @PathVariable Long id,
            @Valid @ModelAttribute CartQuantityRequest request) {
        return carts.update(principal.getName(), id, request.getQuantity());
    }

    @DeleteMapping("/items/{id}")
    public CartService.CartView remove(Principal principal, @PathVariable Long id) {
        return carts.remove(principal.getName(), id);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, String>> invalid(BindException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng chọn sản phẩm, size hợp lệ và số lượng từ 1 đến 99."));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> rejected(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message",
                ex.getReason() == null ? "Vui lòng đăng nhập lại." : ex.getReason()));
    }
}
