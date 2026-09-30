package com.example.demo.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.text.NumberFormat;
import java.text.Normalizer;
import java.util.Locale;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/** Local display catalog. Does not represent database stock or checkout prices. */
@Service
public class CatalogService {
    private final List<Map<String, Object>> products;

    public CatalogService() throws IOException {
        var currencyFormat = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"));
        try (var reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("catalog/crew-neck.tsv").getInputStream(), StandardCharsets.UTF_8))) {
            products = reader.lines().skip(1).filter(line -> !line.isBlank()).map(line -> {
                String[] fields = line.split("\t");
                Map<String, Object> product = new HashMap<>(Map.<String, Object>of("name", fields[0], "image", fields[1],
                        "price", Integer.parseInt(fields[2]), "originalPrice", Integer.parseInt(fields[3]),
                        "formattedPrice", currencyFormat.format(Integer.parseInt(fields[2])),
                        "formattedOriginalPrice", currencyFormat.format(Integer.parseInt(fields[3]))));
                product.put("id", fields[1].replace(".jpg", ""));
                String[] name = fields[0].split(" - ", 2);
                product.put("family", name[0]);
                product.put("color", name.length > 1 ? name[1] : "Mặc định");
                product.put("discount", Math.round(100f * (Integer.parseInt(fields[3]) - Integer.parseInt(fields[2])) / Integer.parseInt(fields[3])));
                return Map.copyOf(product);
            }).toList();
        }
    }

    public List<Map<String, Object>> getCrewNeckProducts(String sort) {
        return getCrewNeckProducts(sort, "");
    }

    public List<Map<String, Object>> getCrewNeckProducts(String sort, String query) {
        String search = normalize(query);
        var matches = products.stream()
                .filter(p -> normalize((String) p.get("name")).contains(search));
        Comparator<Map<String, Object>> byPrice = Comparator.comparingInt(p -> (Integer) p.get("price"));
        return switch (sort) {
            case "price-asc" -> matches.sorted(byPrice).toList();
            case "price-desc" -> matches.sorted(byPrice.reversed()).toList();
            default -> matches.toList();
        };
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value.strip().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd').replaceAll("\\s+", " ");
    }

    public Optional<Map<String, Object>> findProduct(String id) {
        return products.stream().filter(p -> p.get("id").equals(id)).findFirst();
    }

    public List<Map<String, Object>> colorsFor(Map<String, Object> product) {
        return products.stream().filter(p -> p.get("family").equals(product.get("family"))).toList();
    }

    public List<Map<String, Object>> relatedTo(String id) {
        return products.stream().filter(p -> !p.get("id").equals(id)).limit(4).toList();
    }
}
