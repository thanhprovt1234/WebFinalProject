package com.example.demo.config;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.io.ClassPathResource;

/** H2-only fixtures. Never seeds production stock. */
@Component
@Profile("test")
public class CatalogTestData implements ApplicationRunner {
    private final UserRepository users; private final ShopRepository shops; private final CategoryRepository categories;
    private final ProductRepository products; private final ProductVariantRepository variants;
    private final StoreRepository stores; private final InventoryRepository inventory;
    public CatalogTestData(UserRepository users,ShopRepository shops,CategoryRepository categories,ProductRepository products,
            ProductVariantRepository variants,StoreRepository stores,InventoryRepository inventory) {
        this.users=users;this.shops=shops;this.categories=categories;this.products=products;this.variants=variants;this.stores=stores;this.inventory=inventory;
    }
    @Override @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if(products.findBySlug("ao-thun-01").isPresent()) return;
        var owner=new User(); owner.setUsername("catalog-fixture");owner.setEmail("catalog-fixture@example.com");
        owner.setPassword("test-only-no-login");owner.setFullName("Catalog fixture");users.save(owner);
        var shop=new Shop();shop.setOwner(owner);shop.setName("H2T");shop.setSlug("h2t");shop.setStatus(ShopStatus.ACTIVE);shops.save(shop);
        var category=new Category();category.setName("Áo Thun Cổ Tròn");categories.save(category);
        var store=new Store();store.setShop(shop);store.setCode("TEST-CATALOG");store.setName("Test branch");store.setAddress("Test only");stores.save(store);
        try(var input=new ClassPathResource("catalog/crew-neck.tsv").getInputStream()) {
            for(String line:new String(input.readAllBytes(),StandardCharsets.UTF_8).lines().skip(1).toList()) {
                var f=line.split("\t");var p=new Product();p.setName(f[0]);p.setSlug(f[1].replace(".jpg",""));
                p.setShop(shop);p.setCategory(category);p.setBasePrice(new BigDecimal(f[2]));p.setImageUrl("/images/products/ao-thun-co-tron/"+f[1]);products.save(p);
                for(String size: new String[]{"S","M","L","XL","XXL"}) {
                    var v=new ProductVariant();v.setProduct(p);v.setSize(size);v.setColor(f[0].split(" - ")[1]);v.setSku(p.getSlug()+"-"+size);variants.save(v);
                    var stock=new Inventory();stock.setStore(store);stock.setProductVariant(v);stock.setQuantity(200);inventory.save(stock);
                }
            }
        }
    }
}
