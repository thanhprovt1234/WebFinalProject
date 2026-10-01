package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class CatalogService {
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final InventoryRepository inventory;
    private final CategoryRepository categories;
    private final ShopRepository shops;
    public CatalogService(ProductRepository products, ProductVariantRepository variants, InventoryRepository inventory,
            CategoryRepository categories, ShopRepository shops) {
        this.products=products; this.variants=variants; this.inventory=inventory; this.categories=categories; this.shops=shops;
    }

    public Page<Map<String,Object>> search(String sort, String query, Long category, String shop, int page) {
        Specification<Product> spec = (root, cq, cb) -> {
            var conditions = new ArrayList<jakarta.persistence.criteria.Predicate>();
            conditions.add(cb.isTrue(root.get("enabled")));
            conditions.add(cb.isTrue(root.get("category").get("enabled")));
            conditions.add(cb.isTrue(root.get("shop").get("enabled")));
            conditions.add(cb.equal(root.get("shop").get("status"), ShopStatus.ACTIVE));
            if (category != null) conditions.add(cb.equal(root.get("category").get("id"),category));
            if (shop != null && !shop.isBlank()) conditions.add(cb.equal(root.get("shop").get("slug"),shop));
            String term=Product.normalize(query).replace("\\","\\\\").replace("%","\\%").replace("_","\\_");
            if (!term.isBlank()) conditions.add(cb.like(root.get("searchName"),"%"+term+"%",'\\'));
            return cb.and(conditions.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        Sort order=switch(sort) {
            case "price-asc" -> Sort.by("basePrice").ascending().and(Sort.by("id"));
            case "price-desc" -> Sort.by("basePrice").descending().and(Sort.by("id"));
            case "newest" -> Sort.by("createdAt").descending().and(Sort.by("id").descending());
            default -> Sort.by("id");
        };
        return products.findAll(spec, PageRequest.of(Math.max(0,Math.min(page,10000)),20,order)).map(this::display);
    }
    public Optional<Map<String,Object>> findProduct(String slug) {
        return products.findBySlug(slug).filter(this::visible).map(this::display);
    }
    private boolean visible(Product p) {
        return p.isEnabled() && p.getCategory().isEnabled() && p.getShop().isEnabled() && p.getShop().getStatus()==ShopStatus.ACTIVE;
    }
    public Long crewNeckCategory() {
        return categories.findByName("Áo Thun Cổ Tròn").stream().filter(Category::isEnabled).map(Category::getId).findFirst().orElse(-1L);
    }
    public List<Map<String,Object>> getCrewNeckProducts(String sort) { return getCrewNeckProducts(sort,""); }
    public List<Map<String,Object>> getCrewNeckProducts(String sort,String query) { return search(sort,query,crewNeckCategory(),"",0).getContent(); }
    public List<Map<String,Object>> categories() {
        return categories.findAll(Sort.by("name")).stream().filter(Category::isEnabled)
                .map(c->Map.<String,Object>of("id",c.getId(),"name",c.getName())).toList();
    }
    public List<Map<String,Object>> shops() {
        return shops.findAll(Sort.by("name")).stream().filter(s->s.isEnabled() && s.getStatus()==ShopStatus.ACTIVE)
                .map(s->Map.<String,Object>of("slug",s.getSlug(),"name",s.getName())).toList();
    }
    public List<Map<String,Object>> colorsFor(Map<String,Object> product) { return List.of(product); }
    public List<Map<String,Object>> relatedTo(String slug) {
        return products.findBySlug(slug).filter(this::visible).map(p->search("featured","",p.getCategory().getId(),"",0)
                .getContent().stream().filter(row->!row.get("id").equals(slug)).limit(4).toList()).orElse(List.of());
    }
    public List<Map<String,Object>> options(String slug) {
        var p=products.findBySlug(slug).filter(this::visible).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        return variants.findByProductIdAndEnabledTrueOrderByIdAsc(p.getId()).stream().map(v->Map.<String,Object>of(
                "id",v.getId(),"size",v.getSize(),"color",v.getColor(),"price",v.getPrice()==null?p.getBasePrice():v.getPrice(),
                "stock",inventory.availableQuantity(v.getId()))).toList();
    }
    public Map<String,Object> option(String slug,String size,String color) {
        var matches=options(slug).stream().filter(v->v.get("size").equals(size))
                .filter(v->color==null || color.isBlank() || v.get("color").equals(color)).toList();
        if(matches.size()!=1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Vui lòng chọn đúng màu và size của sản phẩm.");
        return matches.getFirst();
    }
    private Map<String,Object> display(Product p) {
        var format=NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        format.setMaximumFractionDigits(2);
        Map<String,Object> result=new HashMap<>();
        result.put("id",p.getSlug()); result.put("databaseId",p.getId()); result.put("name",p.getName());
        result.put("price",p.getBasePrice()); result.put("originalPrice",p.getBasePrice()); result.put("discount",0);
        result.put("formattedPrice",format.format(p.getBasePrice())); result.put("formattedOriginalPrice",format.format(p.getBasePrice()));
        String image=p.getImageUrl();
        // Only local image paths or HTTPS URLs may be rendered as product images.
        if(image==null || !(image.startsWith("/images/") || image.startsWith("https://"))) image="/images/aosomi.png";
        result.put("imageUrl",image); result.put("image",image.substring(image.lastIndexOf('/')+1));
        result.put("shopName",p.getShop().getName()); result.put("shopSlug",p.getShop().getSlug());
        result.put("categoryName",p.getCategory().getName()); result.put("categoryId",p.getCategory().getId());
        result.put("description",p.getDescription()==null?"":p.getDescription());
        String[] name=p.getName().split(" - ",2);
        result.put("family",name[0]); result.put("color",name.length>1?name[1]:"Chọn màu bên dưới");
        return result;
    }
}
