package com.example.demo.controller;

import com.example.demo.service.CatalogService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(assignableTypes={CatalogController.class, AuthController.class, ProfileController.class, ProductActivityController.class, WorkspaceController.class, AdminAccessController.class})
public class CatalogNavigation {
    private final CatalogService catalog;
    public CatalogNavigation(CatalogService catalog) { this.catalog=catalog; }
    @ModelAttribute("catalogCategories")
    public List<Map<String,Object>> categories() { return catalog.categories(); }
}
