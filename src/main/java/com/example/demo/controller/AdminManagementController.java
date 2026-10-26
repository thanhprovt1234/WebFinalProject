package com.example.demo.controller;

import com.example.demo.dto.AdminForms.*;
import com.example.demo.entity.ShopStatus;
import com.example.demo.service.AdminManagementService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequestMapping("/admin")
public class AdminManagementController {
    private final AdminManagementService admin;
    private final com.example.demo.service.ShipperService shipping;
    private final com.example.demo.service.CodSettlementService cod;
    public AdminManagementController(AdminManagementService admin,com.example.demo.service.ShipperService shipping,
            com.example.demo.service.CodSettlementService cod) { this.admin=admin;this.shipping=shipping;this.cod=cod; }
    @GetMapping public String home(Model model) { model.addAttribute("totals",admin.totals());model.addAttribute("cancellationNotices",admin.cancellationNotices());return "admin/index"; }
    @GetMapping("/shops") public String shops(Model model) { model.addAttribute("shops",admin.shops());return "admin/shops"; }
    @GetMapping({"/shops/new","/shops/{id}/edit"}) public String shop(@PathVariable(required=false) Long id,Model model) {
        if(!model.containsAttribute("form"))model.addAttribute("form",admin.shopForm(id));
        model.addAttribute("entityId",id);model.addAttribute("owners",admin.owners());model.addAttribute("statuses",ShopStatus.values());
        if(id!=null)model.addAttribute("branches",admin.branches(id));
        return "admin/shop-form";
    }
    private String save(BindingResult errors,Runnable action,String target,RedirectAttributes redirect) {
        if(errors!=null&&errors.hasErrors()) {
            redirect.addFlashAttribute("error","Thông tin chưa hợp lệ. Vui lòng kiểm tra các trường bắt buộc, độ dài và giá/số lượng.");
            return "redirect:"+target;
        }
        try { action.run();redirect.addFlashAttribute("success","Đã lưu thay đổi."); }
        catch(IllegalArgumentException ex) { redirect.addFlashAttribute("error",ex.getMessage()); }
        catch(DataIntegrityViolationException ex) { redirect.addFlashAttribute("error","Mã hoặc dữ liệu đã tồn tại, hoặc bản ghi đang được sử dụng. Vui lòng kiểm tra lại."); }
        return "redirect:"+target;
    }
    @PostMapping({"/shops","/shops/{id}"}) public String shopSave(@PathVariable(required=false) Long id,@Valid @ModelAttribute ShopForm form,BindingResult errors,RedirectAttributes redirect) {
        final Long[] saved={id};var target=id==null?"/admin/shops/new":"/admin/shops/"+id+"/edit";
        var result=save(errors,()->saved[0]=admin.saveShop(id,form),target,redirect);
        if(redirect.getFlashAttributes().containsKey("error")){redirect.addFlashAttribute("form",form);return result;}
        return "redirect:/admin/shops/"+saved[0]+"/edit";
    }
    @PostMapping("/shops/{id}/delete") public String shopDelete(@PathVariable Long id,RedirectAttributes redirect) { return save(null,()->admin.archiveShop(id),"/admin/shops",redirect); }
    @GetMapping("/products") public String products(@RequestParam(defaultValue="") String q,@RequestParam(required=false) Long shopId,@RequestParam(defaultValue="0") int page,Model model) {
        model.addAttribute("productsPage",admin.products(q,shopId,page));model.addAttribute("query",q);model.addAttribute("selectedShop",shopId);model.addAttribute("shops",admin.shops());return "admin/products";
    }
    @GetMapping({"/products/new","/products/{id}/edit"}) public String product(@PathVariable(required=false) Long id,Model model) {
        if(!model.containsAttribute("form"))model.addAttribute("form",admin.productForm(id));
        model.addAttribute("entityId",id);model.addAttribute("shops",admin.shops());model.addAttribute("categories",admin.categories());
        if(id!=null)model.addAllAttributes(admin.productOptions(id));return "admin/product-form";
    }
    @PostMapping({"/products","/products/{id}"}) public String productSave(@PathVariable(required=false) Long id,@Valid @ModelAttribute ProductForm form,BindingResult errors,RedirectAttributes redirect) {
        final Long[] saved={id};var target=id==null?"/admin/products/new":"/admin/products/"+id+"/edit";
        var result=save(errors,()->saved[0]=admin.saveProduct(id,form),target,redirect);
        if(redirect.getFlashAttributes().containsKey("error")){redirect.addFlashAttribute("form",form);return result;}
        return "redirect:/admin/products/"+saved[0]+"/edit";
    }
    @PostMapping("/products/{id}/delete") public String productDelete(@PathVariable Long id,RedirectAttributes redirect) { return save(null,()->admin.archiveProduct(id),"/admin/products",redirect); }
    @PostMapping({"/products/{productId}/variants","/products/{productId}/variants/{id}"})
    public String variant(@PathVariable Long productId,@PathVariable(required=false) Long id,@Valid @ModelAttribute VariantForm form,BindingResult errors,RedirectAttributes redirect) {
        return save(errors,()->admin.saveVariant(productId,id,form),"/admin/products/"+productId+"/edit#variants",redirect);
    }
    @PostMapping("/products/{id}/stock") public String stock(@PathVariable Long id,@Valid @ModelAttribute StockForm form,BindingResult errors,RedirectAttributes redirect) {
        return save(errors,()->admin.saveStock(id,form),"/admin/products/"+id+"/edit#stock",redirect);
    }
    @PostMapping({"/shops/{shopId}/stores","/shops/{shopId}/stores/{id}"})
    public String store(@PathVariable Long shopId,@PathVariable(required=false) Long id,@Valid @ModelAttribute StoreForm form,BindingResult errors,RedirectAttributes redirect) {
        return save(errors,()->admin.saveStore(shopId,id,form),"/admin/shops/"+shopId+"/edit",redirect);
    }
    @GetMapping("/categories") public String categories(Model model) { model.addAttribute("categories",admin.categories());return "admin/categories"; }
    @PostMapping({"/categories","/categories/{id}"}) public String category(@PathVariable(required=false) Long id,@Valid @ModelAttribute CategoryForm form,BindingResult errors,RedirectAttributes redirect) {
        return save(errors,()->admin.saveCategory(id,form),"/admin/categories",redirect);
    }
    @GetMapping("/orders") public String orders(@RequestParam(defaultValue="0") int page,Model model) { model.addAttribute("ordersPage",admin.orders(page));model.addAttribute("cancellationNotices",admin.cancellationNotices());return "admin/orders"; }
    @GetMapping("/orders/{id}") public String order(@PathVariable Long id,Model model) { model.addAllAttributes(admin.order(id));model.addAllAttributes(shipping.assignment(id));return "admin/order"; }
    @PostMapping("/orders/{id}/assign") public String assign(Principal principal,@PathVariable Long id,@RequestParam Long shipperId,RedirectAttributes redirect) {
        return save(null,()->shipping.assign(principal.getName(),id,shipperId),"/admin/orders/"+id,redirect);
    }
    @PostMapping("/orders/{id}/cod/confirm") public String confirmCod(Principal principal,@PathVariable Long id,
            @RequestParam java.math.BigDecimal amount,@RequestParam(defaultValue="") String note,
            @RequestParam(defaultValue="false") boolean confirmed,RedirectAttributes redirect) {
        return save(null,()->cod.confirm(principal.getName(),id,amount,note,confirmed),"/admin/orders/"+id,redirect);
    }
    @PostMapping("/orders/{id}/status") public String status(Principal principal,@PathVariable Long id,@Valid @ModelAttribute StatusForm form,BindingResult errors,RedirectAttributes redirect) {
        return save(errors,()->admin.changeStatus(principal.getName(),id,form),"/admin/orders/"+id,redirect);
    }
}
