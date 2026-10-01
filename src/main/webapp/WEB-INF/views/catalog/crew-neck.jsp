<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="${collectionTitle}" />
<c:set var="pageDescription" value="Khám phá áo thun cổ tròn, nhiều màu sắc dễ phối cho phong cách mỗi ngày cùng H2T." />
<c:set var="bodyClass" value="catalog-page" />
<%@ include file="../fragments/header.jspf" %>
<section class="catalog-shell" aria-labelledby="catalog-title">
  <nav class="catalog-breadcrumb" aria-label="Đường dẫn">
    <a href="${pageContext.request.contextPath}/">Trang chủ</a><span aria-hidden="true">/</span><span aria-current="page"><c:out value="${collectionTitle}" /></span>
  </nav>
  <h1 id="catalog-title"><c:out value="${collectionTitle}" /></h1>
  <div class="catalog-toolbar">
    <p><strong>${catalogPage.totalElements}</strong> sản phẩm</p>
    <form method="get" action="${pageContext.request.contextPath}/collections" class="catalog-sort">
      <label for="category-filter" class="visually-hidden">Danh mục</label>
      <select id="category-filter" name="category"><option value="">Tất cả danh mục</option><c:forEach var="category" items="${catalogCategories}"><option value="${category.id}" ${selectedCategory == category.id ? 'selected' : ''}><c:out value="${category.name}" /></option></c:forEach></select>
      <label for="shop-filter" class="visually-hidden">Shop</label>
      <select id="shop-filter" name="shop"><option value="">Tất cả shop</option><c:forEach var="shop" items="${shops}"><option value="<c:out value='${shop.slug}' />" ${selectedShop == shop.slug ? 'selected' : ''}><c:out value="${shop.name}" /></option></c:forEach></select>
      <div class="catalog-search">
        <label for="catalog-search" class="visually-hidden">Tìm theo tên sản phẩm</label>
        <input id="catalog-search" type="search" name="q" value="<c:out value='${query}' />" placeholder="Tìm theo tên sản phẩm...">
      </div>
      <label for="catalog-sort">Sắp xếp theo</label>
      <select id="catalog-sort" name="sort">
        <option value="featured" ${sort == 'featured' ? 'selected' : ''}>Nổi bật</option>
        <option value="newest" ${sort == 'newest' ? 'selected' : ''}>Mới nhất</option>
        <option value="price-asc" ${sort == 'price-asc' ? 'selected' : ''}>Giá: Thấp đến cao</option>
        <option value="price-desc" ${sort == 'price-desc' ? 'selected' : ''}>Giá: Cao đến thấp</option>
      </select>
      <button type="submit">Áp dụng</button>
    </form>
  </div>
  <c:if test="${empty products}">
    <div class="catalog-empty" role="status">
      <p>Không tìm thấy sản phẩm phù hợp với “<c:out value="${query}" />”.</p>
      <a href="${pageContext.request.contextPath}/collections/ao-thun-co-tron">Xem tất cả sản phẩm</a>
    </div>
  </c:if>
  <div class="product-grid">
    <c:forEach var="product" items="${products}" varStatus="status">
      <article class="catalog-product" aria-labelledby="product-${status.index}">
        <a class="product-photo product-link" href="${pageContext.request.contextPath}/products/${product.id}" aria-label="<c:out value='${product.name}' />">
          <img src="<c:out value="${product.imageUrl.startsWith('/') ? pageContext.request.contextPath.concat(product.imageUrl) : product.imageUrl}" />" alt="<c:out value='${product.name}' />" loading="${status.index < 4 ? 'eager' : 'lazy'}" decoding="async" width="600" height="800">
        </a>
        <h2 id="product-${status.index}"><a class="product-link" href="${pageContext.request.contextPath}/products/${product.id}"><c:out value="${product.name}" /></a></h2>
        <p class="catalog-shop"><c:out value="${product.shopName}" /></p>
        <div class="product-prices">
          <c:if test="${product.originalPrice > product.price}">
            <del><span class="visually-hidden">Giá gốc: </span>${product.formattedOriginalPrice} VNĐ</del>
          </c:if>
          <span class="product-price"><span class="visually-hidden">Giá bán: </span>${product.formattedPrice} VNĐ</span>
        </div>
      </article>
    </c:forEach>
  </div>
  <nav class="activity-pagination" aria-label="Phân trang sản phẩm">
    <c:if test="${catalogPage.number > 0}"><c:url var="previousPage" value="/collections"><c:param name="category" value="${selectedCategory}"/><c:param name="shop" value="${selectedShop}"/><c:param name="q" value="${query}"/><c:param name="sort" value="${sort}"/><c:param name="page" value="${catalogPage.number - 1}"/></c:url><a href="<c:out value='${previousPage}' />">← Trang trước</a></c:if>
    <span>Trang ${catalogPage.number + 1} / ${catalogPage.totalPages > 0 ? catalogPage.totalPages : 1}</span>
    <c:if test="${catalogPage.hasNext()}"><c:url var="nextPage" value="/collections"><c:param name="category" value="${selectedCategory}"/><c:param name="shop" value="${selectedShop}"/><c:param name="q" value="${query}"/><c:param name="sort" value="${sort}"/><c:param name="page" value="${catalogPage.number + 1}"/></c:url><a href="<c:out value='${nextPage}' />">Trang sau →</a></c:if>
  </nav>
</section>
<%@ include file="../fragments/footer.jspf" %>
