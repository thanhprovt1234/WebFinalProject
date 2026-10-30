package com.example.demo.controller;

import com.example.demo.service.RevenueService;
import java.security.Principal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class RevenueController {
    private final RevenueService revenue;
    public RevenueController(RevenueService revenue) { this.revenue = revenue; }

    @GetMapping({"/admin/revenue", "/seller/revenue", "/manager/revenue"})
    public String report(jakarta.servlet.http.HttpServletRequest request, Principal principal,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required=false) Long shopId, Model model) {
        String scope = request.getServletPath().split("/")[1];
        model.addAllAttributes(switch (scope) {
            case "admin" -> revenue.admin(from, to, shopId);
            case "seller" -> revenue.seller(principal.getName(), from, to, shopId);
            default -> revenue.manager(principal.getName(), from, to, shopId);
        });
        model.addAttribute("revenueScope", scope);
        model.addAttribute("sellerOrders", !scope.equals("admin"));
        return "admin/revenue";
    }
}
