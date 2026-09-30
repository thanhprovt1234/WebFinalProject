package com.example.demo.controller;

import com.example.demo.dto.ProfileRequest;
import com.example.demo.dto.AddressRequest;
import com.example.demo.service.ProfileService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileController {
    private final ProfileService profiles;
    public ProfileController(ProfileService profiles) { this.profiles = profiles; }

    @GetMapping
    public String profile(Principal principal, Model model) {
        var account = profiles.account(principal.getName());
        model.addAttribute("account", account);
        if (!model.containsAttribute("profileForm")) {
            var form = new ProfileRequest();
            form.setFullName(account.fullName()); form.setPhone(account.phone());
            model.addAttribute("profileForm", form);
        }
        model.addAttribute("addresses", profiles.addresses(principal.getName()));
        return "profile/index";
    }

    @PostMapping
    public String update(Principal principal, @Valid @ModelAttribute("profileForm") ProfileRequest form,
            BindingResult errors, Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) return profile(principal, model);
        profiles.update(principal.getName(), form);
        redirect.addFlashAttribute("success", "Đã cập nhật thông tin cá nhân.");
        return "redirect:/profile";
    }

    @GetMapping("/addresses/new")
    public String newAddress(Model model) {
        model.addAttribute("addressForm", new AddressRequest());
        return "profile/address";
    }

    @GetMapping("/addresses/{id}/edit")
    public String edit(Principal principal, @PathVariable Long id, Model model) {
        model.addAttribute("addressForm", profiles.address(principal.getName(), id));
        model.addAttribute("addressId", id);
        return "profile/address";
    }

    @PostMapping("/addresses")
    public String create(Principal principal, @Valid @ModelAttribute("addressForm") AddressRequest form,
            BindingResult errors, RedirectAttributes redirect) {
        if (errors.hasErrors()) return "profile/address";
        profiles.saveAddress(principal.getName(), null, form);
        return saved(redirect, "Đã thêm địa chỉ nhận hàng.");
    }

    @PostMapping("/addresses/{id}")
    public String save(Principal principal, @PathVariable Long id,
            @Valid @ModelAttribute("addressForm") AddressRequest form, BindingResult errors,
            Model model, RedirectAttributes redirect) {
        profiles.address(principal.getName(), id);
        if (errors.hasErrors()) { model.addAttribute("addressId", id); return "profile/address"; }
        profiles.saveAddress(principal.getName(), id, form);
        return saved(redirect, "Đã cập nhật địa chỉ.");
    }

    @PostMapping("/addresses/{id}/default")
    public String makeDefault(Principal principal, @PathVariable Long id, RedirectAttributes redirect) {
        profiles.makeDefault(principal.getName(), id);
        return saved(redirect, "Đã chọn địa chỉ mặc định.");
    }

    @PostMapping("/addresses/{id}/delete")
    public String delete(Principal principal, @PathVariable Long id, RedirectAttributes redirect) {
        profiles.deleteAddress(principal.getName(), id);
        return saved(redirect, "Đã xóa địa chỉ.");
    }

    private String saved(RedirectAttributes redirect, String message) {
        redirect.addFlashAttribute("success", message);
        return "redirect:/profile#addresses";
    }
}
