package com.example.demo.controller;

import com.example.demo.entity.ShipmentStatus;
import com.example.demo.service.ShipperService;
import java.security.Principal;
import java.time.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequestMapping("/shipper")
public class ShipperController {
    private final ShipperService shipping;
    private final com.example.demo.service.CodSettlementService cod;
    public ShipperController(ShipperService shipping,com.example.demo.service.CodSettlementService cod) {this.shipping=shipping;this.cod=cod;}
    @GetMapping({"","/statistics"})
    public String list(Principal principal,@RequestParam(defaultValue="") String q,
            @RequestParam(required=false) ShipmentStatus status,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue="0") int page,
            jakarta.servlet.http.HttpServletRequest request,Model model) {
        boolean stats=request.getRequestURI().endsWith("/statistics");
        if(stats&&from==null&&to==null){from=LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).withDayOfMonth(1);to=LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));}
        if(from!=null&&to!=null&&from.isAfter(to)){model.addAttribute("error","Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");from=null;to=null;}
        model.addAllAttributes(shipping.dashboard(principal.getName(),q,status,from,to,page));
        model.addAttribute("query",q);model.addAttribute("selectedStatus",status);model.addAttribute("from",from);model.addAttribute("to",to);
        model.addAttribute("statuses",ShipmentStatus.values());model.addAttribute("statistics",stats);
        var today=LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));model.addAttribute("today",today);model.addAttribute("monthStart",today.withDayOfMonth(1));
        return "shipper/list";
    }
    @GetMapping("/shipments/{id}") public String detail(Principal principal,@PathVariable Long id,Model model) {
        model.addAllAttributes(shipping.detail(principal.getName(),id));return "shipper/detail";
    }
    @PostMapping("/shipments/{id}/cod/remit") public String remit(Principal principal,@PathVariable Long id,
            @RequestParam java.math.BigDecimal amount,@RequestParam(defaultValue="") String note,
            @RequestParam(defaultValue="false") boolean confirmed,RedirectAttributes redirect) {
        try{cod.remit(principal.getName(),id,amount,note,confirmed);redirect.addFlashAttribute("success","Đã ghi nhận bàn giao COD. Đang chờ shop xác nhận nhận tiền.");}
        catch(IllegalArgumentException ex){redirect.addFlashAttribute("error",ex.getMessage());}
        return "redirect:/shipper/shipments/"+id;
    }
    @PostMapping("/shipments/{id}/status") public String update(Principal principal,@PathVariable Long id,
            @RequestParam ShipmentStatus status,@RequestParam(defaultValue="") String note,
            @RequestParam(defaultValue="false") boolean codCollected,RedirectAttributes redirect) {
        try{shipping.update(principal.getName(),id,status,note,codCollected);redirect.addFlashAttribute("success","Đã cập nhật tiến trình giao hàng.");}
        catch(IllegalArgumentException ex){redirect.addFlashAttribute("error",ex.getMessage());}
        return "redirect:/shipper/shipments/"+id;
    }
}
