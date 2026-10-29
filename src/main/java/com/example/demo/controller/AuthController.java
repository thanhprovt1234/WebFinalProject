package com.example.demo.controller;

import com.example.demo.dto.RegisterRequest;
import com.example.demo.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final AuthService authService;
    private final com.example.demo.service.CatalogService catalog;

    public AuthController(AuthService authService, com.example.demo.service.CatalogService catalog) {
        this.authService = authService;
        this.catalog = catalog;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registerRequest") RegisterRequest request,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            authService.register(request);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject(
                    "registration.error",
                    exception.getMessage());
            return "auth/register";
        }

        return "redirect:/login?registered";
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("homeProducts",catalog.search("newest","",null,"",0).getContent());
        model.addAttribute("bestsellers",catalog.bestsellers());
        return "home";
    }

}
