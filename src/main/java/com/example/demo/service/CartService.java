package com.example.demo.service;

import com.example.demo.entity.CartItem;
import com.example.demo.entity.User;
import com.example.demo.dto.CartAddRequest;
import com.example.demo.repository.CartItemRepository;
import com.example.demo.repository.UserRepository;
import java.util.List;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CartService {
    public record Line(Long id, String productId, String name, String image, String size, String color,
            int quantity, BigDecimal price, BigDecimal subtotal, boolean available, long stock) {}
    public record CartView(List<Line> items, int count, BigDecimal total) {}

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
        var option = catalog.option(request.getProductId(), request.getSize(), request.getColor());
        String color = (String) option.get("color");
        var item = items.findByUserIdAndCatalogProductIdAndSizeAndColor(user.getId(), request.getProductId(), request.getSize(), color)
                .orElseGet(CartItem::new);
        int quantity = item.getQuantity() + request.getQuantity();
        if (quantity > 99) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Mỗi sản phẩm và size được thêm tối đa 99 sản phẩm.");
        checkStock(option, quantity);
        item.setColor(color);
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
        checkStock(catalog.option(item.getCatalogProductId(), item.getSize(), item.getColor()), quantity);
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

    @Transactional
    public boolean mergeGuest(String username, List<Line> guestItems) {
        var user=user(username,true);
        boolean limited=false;
        for(var line:guestItems) {
            var item=items.findByUserIdAndCatalogProductIdAndSizeAndColor(user.getId(),line.productId(),line.size(),line.color())
                    .orElseGet(CartItem::new);
            int quantity=item.getQuantity()+line.quantity();
            limited |= quantity>99;
            item.setUser(user); item.setCatalogProductId(line.productId()); item.setSize(line.size()); item.setColor(line.color());
            item.setQuantity(Math.min(99,quantity));
            items.save(item);
        }
        items.flush();
        return limited;
    }

    @Transactional(readOnly = true)
    public Line guestLine(Long id,String productId,String size,String color,int quantity,boolean validate) {
        if(validate) checkStock(catalog.option(productId,size,color),quantity);
        return line(id,productId,size,color,quantity);
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

    private void checkStock(Map<String,Object> option, int quantity) {
        if (quantity > (Long) option.get("stock")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "S\u1ed1 l\u01b0\u1ee3ng v\u01b0\u1ee3t qu\u00e1 t\u1ed3n kho hi\u1ec7n t\u1ea1i.");
    }

    private CartView view(Long userId) {
        var lines = items.findByUserIdOrderByIdAsc(userId).stream()
                .map(item->line(item.getId(),item.getCatalogProductId(),item.getSize(),item.getColor(),item.getQuantity())).toList();
        return new CartView(lines, lines.stream().mapToInt(Line::quantity).sum(),
                lines.stream().map(Line::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private Line line(Long id,String productId,String size,String color,int quantity) {
            var product = catalog.findProduct(productId);
            Map<String,Object> option = Map.of();
            if (product.isPresent()) {
                option = catalog.options(productId).stream()
                        .filter(v -> v.get("size").equals(size) && v.get("color").equals(color))
                        .findFirst().orElse(Map.of());
            }
            BigDecimal price = (BigDecimal) option.getOrDefault("price", BigDecimal.ZERO);
            long stock = (Long) option.getOrDefault("stock", 0L);
            return new Line(id, productId,
                    product.map(p -> (String) p.get("name")).orElse("Sản phẩm không còn kinh doanh"),
                    product.map(p -> (String) p.get("imageUrl")).orElse(""), size, color, quantity,
                    price, price.multiply(BigDecimal.valueOf(quantity)), !option.isEmpty(), stock);
    }
}
