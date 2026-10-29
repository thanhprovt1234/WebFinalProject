package com.example.demo.controller;

import com.example.demo.dto.CartAddRequest;
import com.example.demo.dto.CartQuantityRequest;
import com.example.demo.service.CartService;
import com.example.demo.service.GuestCartService;
import jakarta.servlet.http.HttpSession;
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
    private final GuestCartService guests;
    public CartController(CartService carts,GuestCartService guests) { this.carts = carts; this.guests=guests; }

    @GetMapping
    public CartService.CartView get(Principal principal,HttpSession session) {
        return principal==null?guests.get(session):carts.get(principal.getName());
    }

    @PostMapping("/items")
    public CartService.CartView add(Principal principal,HttpSession session, @Valid @ModelAttribute CartAddRequest request) {
        return principal==null?guests.add(session,request):carts.add(principal.getName(), request);
    }

    @PatchMapping("/items/{id}")
    public CartService.CartView update(Principal principal,HttpSession session, @PathVariable Long id,
            @Valid @ModelAttribute CartQuantityRequest request) {
        return principal==null?guests.update(session,id,request.getQuantity()):carts.update(principal.getName(), id, request.getQuantity());
    }

    @DeleteMapping("/items/{id}")
    public CartService.CartView remove(Principal principal,HttpSession session, @PathVariable Long id) {
        return principal==null?guests.remove(session,id):carts.remove(principal.getName(), id);
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
