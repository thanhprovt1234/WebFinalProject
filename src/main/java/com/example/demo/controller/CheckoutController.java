package com.example.demo.controller;

import com.example.demo.dto.CheckoutRequest;
import com.example.demo.service.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class CheckoutController {
    private final VoucherService vouchers;
    private final CartService carts;
    private final CheckoutService checkout;
    private final ProfileService profiles;
    private final CustomerOrderService customerOrders;
    public CheckoutController(CartService carts,CheckoutService checkout,ProfileService profiles,CustomerOrderService customerOrders,VoucherService vouchers) {
        this.vouchers=vouchers; this.carts=carts; this.checkout=checkout; this.profiles=profiles;this.customerOrders=customerOrders;
    }
    @GetMapping("/checkout")
    public String form(Principal principal,HttpSession session,Model model) {
        var request=new CheckoutRequest();
        request.setToken(UUID.randomUUID().toString()); session.setAttribute("checkoutToken",request.getToken());
        var account=profiles.account(principal.getName());
        request.setReceiverName(account.fullName()); request.setReceiverPhone(account.phone());
        profiles.addresses(principal.getName()).stream().filter(a->a.isDefault()).findFirst().ifPresent(a->{
            request.setReceiverName(a.getReceiverName()); request.setReceiverPhone(a.getPhone());
            request.setShippingAddress(String.join(", ",a.getAddressLine(),a.getWard(),a.getDistrict(),a.getCity()));
        });
        model.addAttribute("checkoutForm",request);
        return prepare(principal,request,model);
    }
    private String prepare(Principal principal,CheckoutRequest request,Model model) {
        var cart=carts.get(principal.getName()); model.addAttribute("cart",cart);
        java.math.BigDecimal discount=java.math.BigDecimal.ZERO;
        try { discount=vouchers.preview(principal.getName(),request.getVoucherCode(),cart.total()); }
        catch(ResponseStatusException ex) { model.addAttribute("voucherError",ex.getReason()); }
        request.setExpectedTotal(cart.total().subtract(discount));
        model.addAttribute("discount",discount);
        model.addAttribute("payableTotal",cart.total().subtract(discount));
        model.addAttribute("vouchers",vouchers.available(principal.getName()));
        model.addAttribute("canOrder",!cart.items().isEmpty() && cart.items().stream().allMatch(i->i.available() && i.quantity()<=i.stock()));
        return "checkout/index";
    }
    @PostMapping(value="/checkout",params="action=apply")
    public String apply(Principal principal,@ModelAttribute("checkoutForm") CheckoutRequest request,Model model) {
        return prepare(principal,request,model);
    }
    @PostMapping(value="/checkout",params="action!=apply")
    public String place(Principal principal,HttpSession session,@Valid @ModelAttribute("checkoutForm") CheckoutRequest request,
            BindingResult errors,Model model) {
        if(!java.util.Objects.equals(session.getAttribute("checkoutToken"),request.getToken()))
            errors.reject("token","Phiên đặt hàng đã thay đổi. Vui lòng mở lại trang thanh toán.");
        if(!errors.hasErrors()) {
            try { return "redirect:/orders?placed="+checkout.place(principal.getName(),request); }
            catch(ResponseStatusException ex) { errors.reject("checkout",ex.getReason()==null?"Chưa thể đặt hàng.":ex.getReason()); }
        }
        return prepare(principal,request,model);
    }
    @PostMapping("/orders/{id}/cancel")
    public String cancel(Principal principal,@PathVariable Long id,@RequestParam(defaultValue="") String reason,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        try {
            customerOrders.cancelPending(principal.getName(),id,reason);
            redirect.addFlashAttribute("success","Đã hủy phần đơn đang chờ duyệt và thông báo cho shop.");
        } catch(IllegalArgumentException ex) { redirect.addFlashAttribute("error",ex.getMessage()); }
        return "redirect:/orders";
    }
    @GetMapping("/orders/{id}")
    public String receipt(Principal principal,@PathVariable Long id,Model model) {
        model.addAttribute("order",checkout.get(principal.getName(),id)); return "checkout/receipt";
    }
    @GetMapping("/orders")
    public String orders(Principal principal,@RequestParam(required=false) Long placed,Model model) {
        if(placed!=null) model.addAttribute("placedOrder",checkout.get(principal.getName(),placed).id());
        model.addAttribute("account",profiles.account(principal.getName()));
        model.addAttribute("vouchers",vouchers.available(principal.getName()));
        model.addAttribute("orders",checkout.list(principal.getName())); return "checkout/orders";
    }
}
