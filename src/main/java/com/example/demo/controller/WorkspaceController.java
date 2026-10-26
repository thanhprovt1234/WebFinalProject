package com.example.demo.controller;

import com.example.demo.service.AccessService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class WorkspaceController {
    private final AccessService access;
    public WorkspaceController(AccessService access){this.access=access;}
    @GetMapping("/workspace")
    public String workspace(Principal principal,Model model){model.addAttribute("roles",access.roleNames(principal.getName()));return "workspace/index";}
    @GetMapping("/seller")
    public String seller(Principal principal,Model model){model.addAttribute("shops",access.sellerShops(principal.getName()));model.addAttribute("section","seller");return "workspace/dashboard";}
    @GetMapping("/seller/shops/{id}")
    public String shop(Principal principal,@PathVariable Long id,Model model){model.addAllAttributes(access.sellerShop(principal.getName(),id));model.addAttribute("section","shop");return "workspace/dashboard";}
    @GetMapping("/manager")
    public String manager(Principal principal,Model model){model.addAttribute("stores",access.managerStores(principal.getName()));model.addAttribute("section","manager");return "workspace/dashboard";}
    @GetMapping("/manager/stores/{id}")
    public String store(Principal principal,@PathVariable Long id,Model model){model.addAllAttributes(access.managerStore(principal.getName(),id));model.addAttribute("section","store");return "workspace/dashboard";}
}
