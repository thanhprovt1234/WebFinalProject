package com.example.demo.controller;

import com.example.demo.entity.RoleName;
import com.example.demo.service.AdminAccessService;
import java.security.Principal;
import java.util.Set;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequestMapping("/admin")
public class AdminAccessController {
    private final AdminAccessService access;
    public AdminAccessController(AdminAccessService access){this.access=access;}
    @GetMapping("/users")
    public String users(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page,Model model){
        model.addAttribute("usersPage",access.users(q,page));model.addAttribute("query",q);return "workspace/users";
    }
    @GetMapping("/users/{id}")
    public String user(@PathVariable Long id,Model model){
        model.addAllAttributes(access.user(id));model.addAttribute("roleOptions",RoleName.values());model.addAttribute("stores",access.stores());return "workspace/user-access";
    }
    @PostMapping("/users/{id}/roles")
    public String roles(Principal principal,@PathVariable Long id,@RequestParam(defaultValue="ROLE_USER") Set<RoleName> roles,RedirectAttributes redirect){
        try{access.updateRoles(principal.getName(),id,roles);redirect.addFlashAttribute("success","Đã cập nhật quyền. Thay đổi áp dụng ở yêu cầu tiếp theo.");}
        catch(IllegalArgumentException ex){redirect.addFlashAttribute("error",ex.getMessage());}
        return "redirect:/admin/users/"+id;
    }
    @PostMapping("/users/{id}/stores")
    public String stores(Principal principal,@PathVariable Long id,@RequestParam Long storeId,@RequestParam boolean assigned,RedirectAttributes redirect){
        try{access.assignStore(principal.getName(),id,storeId,assigned);redirect.addFlashAttribute("success","Đã cập nhật phân công chi nhánh.");}
        catch(IllegalArgumentException ex){redirect.addFlashAttribute("error",ex.getMessage());}
        return "redirect:/admin/users/"+id;
    }
}
