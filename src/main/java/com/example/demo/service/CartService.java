package com.example.demo.service;

import com.example.demo.entity.CartItem;
import com.example.demo.entity.User;
import com.example.demo.dto.CartAddRequest;
import com.example.demo.repository.CartItemRepository;
import com.example.demo.repository.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CartService {
    public record Line(Long id, String productId, String name, String image, String size,
            int quantity, int price, long subtotal, boolean available) {}
    public record CartView(List<Line> items, int count, long total) {}

    private final CartItemRepository items;
    private final UserRepository users;
    private final CatalogService catalog;

    public CartService(CartItemRepository items, UserRepository users, CatalogService catalog) {
        this.items = items;
        this.users = users;
        this.catalog = catalog;
    }

    @Transactional(readOnly = true)
    public CartView get(String username) { return view(user(username, false).getId()); }

    @Transactional
    public CartView add(String username, CartAddRequest request) {
        var user = user(username, true);
        requireProduct(request.getProductId());
        var item = items.findByUserIdAndCatalogProductIdAndSize(user.getId(), request.getProductId(), request.getSize())
                .orElseGet(CartItem::new);
        int quantity = item.getQuantity() + request.getQuantity();
        if (quantity > 99) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Mỗi sản phẩm và size được thêm tối đa 99 sản phẩm.");
        item.setUser(user);
        item.setCatalogProductId(request.getProductId());
        item.setSize(request.getSize());
        item.setQuantity(quantity);
        items.saveAndFlush(item);
        return view(user.getId());
    }

    @Transactional
    public CartView update(String username, Long id, int quantity) {
        var user = user(username, true);
        var item = ownedItem(id, user.getId());
        requireProduct(item.getCatalogProductId());
        item.setQuantity(quantity);
        items.flush();
        return view(user.getId());
    }

    @Transactional
    public CartView remove(String username, Long id) {
        var user = user(username, true);
        items.delete(ownedItem(id, user.getId()));
        items.flush();
        return view(user.getId());
    }

    private User user(String username, boolean lock) {
        // Serialize writes per account, including the first insert, across requests/devices.
        return (lock ? users.findForCartUpdate(username) : users.findByUsername(username))
                .filter(User::isEnabled).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private CartItem ownedItem(Long id, Long userId) {
        return items.findByIdAndUserId(id, userId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Sản phẩm không còn trong giỏ hàng. Vui lòng tải lại giỏ."));
    }

    private void requireProduct(String id) {
        catalog.findProduct(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Sản phẩm không còn trong danh mục."));
    }

    private CartView view(Long userId) {
        var lines = items.findByUserIdOrderByIdAsc(userId).stream().map(item -> {
            var product = catalog.findProduct(item.getCatalogProductId());
            int price = product.map(p -> (Integer) p.get("price")).orElse(0);
            return new Line(item.getId(), item.getCatalogProductId(),
                    product.map(p -> (String) p.get("name")).orElse("Sản phẩm không còn kinh doanh"),
                    product.map(p -> (String) p.get("image")).orElse(""), item.getSize(), item.getQuantity(),
                    price, (long) price * item.getQuantity(), product.isPresent());
        }).toList();
        return new CartView(lines, lines.stream().mapToInt(Line::quantity).sum(),
                lines.stream().mapToLong(Line::subtotal).sum());
    }
}
