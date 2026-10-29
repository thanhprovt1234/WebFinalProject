package com.example.demo.controller;

import com.example.demo.dto.VoucherCampaignForm;
import com.example.demo.service.AdminVoucherService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/vouchers")
public class AdminVoucherController {
    private final AdminVoucherService service;
    public AdminVoucherController(AdminVoucherService service){this.service=service;}
    @GetMapping
    public String index(Principal p,@RequestParam(defaultValue="0") int page,Model model) {
        model.addAttribute("campaigns",service.campaigns(p.getName(),page));
        if(!model.containsAttribute("campaignForm")) model.addAttribute("campaignForm",new VoucherCampaignForm());
        return "admin/vouchers";
    }
    @PostMapping
    public String create(Principal p,@Valid @ModelAttribute("campaignForm") VoucherCampaignForm form,BindingResult errors,Model model,RedirectAttributes redirect) {
        if(errors.hasErrors())return index(p,0,model);
        try {
            Long id=service.create(p.getName(),form);redirect.addFlashAttribute("success","Đã tạo chương trình. Hãy chọn người nhận quà.");
            return "redirect:/admin/vouchers/"+id;
        }catch(IllegalArgumentException ex){model.addAttribute("error",ex.getMessage());return index(p,0,model);}
    }
    @GetMapping("/{id}")
    public String detail(Principal p,@PathVariable Long id,@RequestParam(defaultValue="") String q,Model model) {
        model.addAttribute("campaign",service.campaign(p.getName(),id));
        model.addAttribute("recipients",service.candidates(p.getName(),id,q));
        model.addAttribute("query",q);model.addAttribute("issued",service.issued(p.getName(),id));
        return "admin/voucher-campaign";
    }
    @PostMapping("/{id}/random")
    public String random(Principal p,@PathVariable Long id,@RequestParam int count,Model model) {
        try{model.addAttribute("selectedRecipients",service.random(p.getName(),id,count));}
        catch(IllegalArgumentException ex){model.addAttribute("error",ex.getMessage());}
        return detail(p,id,"",model);
    }
    @PostMapping("/{id}/issue")
    public String issue(Principal p,@PathVariable Long id,@RequestParam(defaultValue="") List<Long> recipientIds,RedirectAttributes redirect) {
        try{int count=service.issue(p.getName(),id,recipientIds);redirect.addFlashAttribute("success","Đã tặng "+count+" voucher. Mỗi người nhận một mã riêng; người đã nhận không được phát trùng.");}
        catch(IllegalArgumentException ex){redirect.addFlashAttribute("error",ex.getMessage());}
        return "redirect:/admin/vouchers/"+id;
    }
    @PostMapping("/{id}/revoke/{voucherId}")
    public String revoke(Principal p,@PathVariable Long id,@PathVariable Long voucherId,RedirectAttributes redirect) {
        try{service.revoke(p.getName(),id,voucherId);redirect.addFlashAttribute("success","Đã thu hồi voucher.");}
        catch(IllegalArgumentException ex){redirect.addFlashAttribute("error",ex.getMessage());}
        return "redirect:/admin/vouchers/"+id;
    }
}
