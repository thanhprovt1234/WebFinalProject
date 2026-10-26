package com.example.demo.controller;

import com.example.demo.dto.AdminForms.StatusForm;
import com.example.demo.service.AdminManagementService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequestMapping("/seller/orders")
public class SellerOrderController {
    private final AdminManagementService orders;
    private final com.example.demo.service.CodSettlementService cod;
    public SellerOrderController(AdminManagementService orders,com.example.demo.service.CodSettlementService cod) { this.orders=orders;this.cod=cod; }
    @ModelAttribute public void navigation(Model model) {
        model.addAttribute("sellerOrders",true);
        model.addAttribute("ordersBase","/seller/orders");
    }
    @GetMapping public String list(Principal principal,@RequestParam(defaultValue="0") int page,Model model) {
        model.addAttribute("ordersPage",orders.sellerOrders(principal.getName(),page));
        model.addAttribute("cancellationNotices",orders.sellerCancellationNotices(principal.getName()));
        return "admin/orders";
    }
    @GetMapping("/{id}") public String detail(Principal principal,@PathVariable Long id,Model model) {
        model.addAllAttributes(orders.sellerOrder(principal.getName(),id));
        return "admin/order";
    }
    @PostMapping("/{id}/status") public String status(Principal principal,@PathVariable Long id,
            @Valid @ModelAttribute StatusForm form,BindingResult errors,RedirectAttributes redirect) {
        if(errors.hasErrors()) redirect.addFlashAttribute("error","Thông tin trạng thái chưa hợp lệ.");
        else try {
            orders.sellerChangeStatus(principal.getName(),id,form);
            redirect.addFlashAttribute("success","Đã cập nhật trạng thái đơn hàng.");
        } catch(IllegalArgumentException ex) { redirect.addFlashAttribute("error",ex.getMessage()); }
        return "redirect:/seller/orders/"+id;
    }
    @PostMapping("/{id}/cod/confirm") public String confirmCod(Principal principal,@PathVariable Long id,
            @RequestParam java.math.BigDecimal amount,@RequestParam(defaultValue="") String note,
            @RequestParam(defaultValue="false") boolean confirmed,RedirectAttributes redirect) {
        try{cod.confirm(principal.getName(),id,amount,note,confirmed);redirect.addFlashAttribute("success","Đã xác nhận shop nhận đủ tiền COD.");}
        catch(IllegalArgumentException ex){redirect.addFlashAttribute("error",ex.getMessage());}
        return "redirect:/seller/orders/"+id;
    }
}
