package com.example.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.service.CatalogService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CatalogControllerTests {
    @Value("${local.server.port}")
    private int port;

    @Autowired
    private CatalogService catalogService;

    @Test void homeRendersCollectionsAndHonestEmptyStates() throws Exception {
        try (var client=HttpClient.newHttpClient()) {
            var page=client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/")).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(page.statusCode()).isEqualTo(200);
            assertThat(page.body()).contains("ĐƯỢC YÊU THÍCH NHẤT", "Bán chạy", "Deal hôm nay",
                    "id=\"bestsellers-panel\"", "id=\"deals-panel\"", "Hiện chưa có chương trình Flash Sale.",
                    "/js/home-collections.js", "/css/home-collections.css");
            assertThat(page.body().indexOf("data-home-picks")).isLessThan(page.body().indexOf("id=\"home-products-title\""));
        }
    }

    @Test
    void anonymousVisitorsCanOpenCatalogAndAllLocalImages() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var page = client.send(HttpRequest.newBuilder(URI.create(
                    "http://localhost:" + port + "/collections/ao-thun-co-tron")).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(page.statusCode()).isEqualTo(200);
            assertThat(page.body()).contains("Áo Thun Cổ Tròn", "99.000 VNĐ", "199.000 VNĐ", "aria-current=\"page\"");
            assertThat(catalogService.getCrewNeckProducts("featured")).hasSize(8);
            for (var product : catalogService.getCrewNeckProducts("featured")) {
                var image = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port
                        + "/images/products/ao-thun-co-tron/" + product.get("image"))).build(),
                        HttpResponse.BodyHandlers.ofByteArray());
                assertThat(image.statusCode()).isEqualTo(200);
                assertThat(image.headers().firstValue("Content-Type").orElse("")).startsWith("image/");
                assertThat(image.body().length).isGreaterThan(1000);
            }
        }
    }

    @Test
    void eachProductHasPublicDetailAndUnknownProductsReturn404() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            for (var product : catalogService.getCrewNeckProducts("featured")) {
                var page = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port
                        + "/products/" + product.get("id"))).build(), HttpResponse.BodyHandlers.ofString());
                assertThat(page.statusCode()).isEqualTo(200);
                assertThat(page.body()).contains(product.get("name").toString(), "THÊM VÀO GIỎ HÀNG",
                        "GỢI Ý SẢN PHẨM", "Đánh giá sản phẩm", "id=\"quantity\"");
                assertThat(catalogService.colorsFor(product)).allSatisfy(color ->
                        assertThat(color.get("family")).isEqualTo(product.get("family")));
                assertThat(catalogService.relatedTo(product.get("id").toString()))
                        .hasSize(4).doesNotContain(product);
            }
            var missing = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port
                    + "/products/does-not-exist")).build(), HttpResponse.BodyHandlers.ofString());
            assertThat(missing.statusCode()).isEqualTo(404);
        }
    }

    @Test
    void searchesNamesWithoutAccentsAndCombinesWithSorting() {
        var matches = catalogService.getCrewNeckProducts("price-desc", "  AO THUN  ");
        assertThat(matches).hasSize(8);
        assertThat(matches.getFirst().get("price")).isEqualTo(new java.math.BigDecimal("199000.00"));
        assertThat(catalogService.getCrewNeckProducts("featured", "ghi dam"))
                .singleElement().satisfies(p -> assertThat(p.get("name").toString()).contains("GHI ĐẬM"));
        assertThat(catalogService.getCrewNeckProducts("featured", "khong-co-san-pham")).isEmpty();
    }

    @Test
    void searchFormRetainsQueryAndShowsFilteredResults() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var page = client.send(HttpRequest.newBuilder(URI.create(
                    "http://localhost:" + port + "/collections/ao-thun-co-tron?q=Defoxx&sort=price-desc")).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(page.statusCode()).isEqualTo(200);
            assertThat(page.body()).contains("value=\"Defoxx\"", "<strong>2</strong>", "199.000 VNĐ")
                    .doesNotContain("Simwood", "H2T / EVERYDAY ESSENTIALS", "Thoải mái mỗi ngày.");
            var empty = client.send(HttpRequest.newBuilder(URI.create(
                    "http://localhost:" + port + "/collections/ao-thun-co-tron?q=%3Cscript%3E")).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(empty.statusCode()).isEqualTo(200);
            assertThat(empty.body()).contains("Không tìm thấy sản phẩm", "&lt;script&gt;")
                    .doesNotContain("value=\"<script>\"");
        }
    }

    @Test
    void sortsByNumericPriceAndPreservesAllProducts() {
        var ascending = catalogService.getCrewNeckProducts("price-asc");
        var descending = catalogService.getCrewNeckProducts("price-desc");
        assertThat(ascending).containsExactlyInAnyOrderElementsOf(descending);
        assertThat(ascending.stream().map(p -> (java.math.BigDecimal) p.get("price")).toList()).isSorted();
        assertThat(descending.getFirst().get("price")).isEqualTo(new java.math.BigDecimal("199000.00"));
        assertThat(descending.getLast().get("price")).isEqualTo(new java.math.BigDecimal("99000.00"));
    }
}
