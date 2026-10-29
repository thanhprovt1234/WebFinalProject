package com.example.demo.service;

import com.example.demo.dto.CartAddRequest;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Guest carts stay on the server and migrate with the session at login. */
@Service
public class GuestCartService {
    private static final String KEY="guestCart";
    private final CartService carts;
    private final CatalogService catalog;
    public GuestCartService(CartService carts,CatalogService catalog) { this.carts=carts; this.catalog=catalog; }
    private static class State {
        final List<CartService.Line> lines=new ArrayList<>();
        long nextId=-1;
    }
    private State state(HttpSession session) {
        var state=(State)session.getAttribute(KEY);
        if(state==null) { state=new State(); session.setAttribute(KEY,state); }
        return state;
    }
    public CartService.CartView get(HttpSession session) {
        synchronized(session) { return view(state(session)); }
    }
    public CartService.CartView add(HttpSession session,CartAddRequest request) {
        synchronized(session) {
            var state=state(session);
            String color=(String)catalog.option(request.getProductId(),request.getSize(),request.getColor()).get("color");
            var old=state.lines.stream().filter(l->l.productId().equals(request.getProductId())
                    && l.size().equals(request.getSize()) && l.color().equals(color)).findFirst().orElse(null);
            int quantity=request.getQuantity()+(old==null?0:old.quantity());
            if(quantity>99) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Mỗi lựa chọn được thêm tối đa 99 sản phẩm.");
            if(old==null && state.lines.size()>=100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Giỏ hàng đã có tối đa 100 lựa chọn.");
            var line=carts.guestLine(old==null?state.nextId:old.id(),request.getProductId(),request.getSize(),color,quantity,true);
            if(old==null) state.nextId--; else state.lines.remove(old);
            state.lines.add(line);
            return view(state);
        }
    }
    public CartService.CartView update(HttpSession session,Long id,int quantity) {
        synchronized(session) {
            var state=state(session); var old=owned(state,id);
            var line=carts.guestLine(id,old.productId(),old.size(),old.color(),quantity,true);
            state.lines.set(state.lines.indexOf(old),line);
            return view(state);
        }
    }
    public CartService.CartView remove(HttpSession session,Long id) {
        synchronized(session) {
            var state=state(session); state.lines.remove(owned(state,id)); return view(state);
        }
    }
    public void merge(HttpSession session,String username) {
        synchronized(session) {
            var state=(State)session.getAttribute(KEY);
            if(state==null || state.lines.isEmpty()) return;
            boolean limited=carts.mergeGuest(username,List.copyOf(state.lines));
            // Clear only after the account transaction has committed successfully.
            session.removeAttribute(KEY);
            if(limited) session.setAttribute("cartMergeNotice","Đã gộp giỏ hàng. Lựa chọn trùng được giới hạn tối đa 99 sản phẩm; vui lòng kiểm tra số lượng trước khi đặt.");
        }
    }
    private CartService.Line owned(State state,Long id) {
        return state.lines.stream().filter(l->l.id().equals(id)).findFirst()
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Sản phẩm không còn trong giỏ hàng."));
    }
    private CartService.CartView view(State state) {
        var lines=state.lines.stream().map(l->carts.guestLine(l.id(),l.productId(),l.size(),l.color(),l.quantity(),false)).toList();
        return new CartService.CartView(lines,lines.stream().mapToInt(CartService.Line::quantity).sum(),
                lines.stream().map(CartService.Line::subtotal).reduce(BigDecimal.ZERO,BigDecimal::add));
    }
}
