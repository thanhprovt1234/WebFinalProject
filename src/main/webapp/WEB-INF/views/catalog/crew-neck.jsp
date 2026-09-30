<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Áo Thun Cổ Tròn" />
<c:set var="pageDescription" value="Khám phá áo thun cổ tròn, nhiều màu sắc dễ phối cho phong cách mỗi ngày cùng H2T." />
<c:set var="bodyClass" value="catalog-page" />
<%@ include file="../fragments/header.jspf" %>
<section class="catalog-shell" aria-labelledby="catalog-title">
  <nav class="catalog-breadcrumb" aria-label="Đường dẫn">
    <a href="${pageContext.request.contextPath}/">Trang chủ</a><span aria-hidden="true">/</span><span>Áo</span><span aria-hidden="true">/</span><span aria-current="page">Áo Thun Cổ Tròn</span>
  </nav>
  <h1 id="catalog-title" class="visually-hidden">Áo Thun Cổ Tròn</h1>
  <div class="catalog-toolbar">
    <p><strong>${products.size()}</strong> sản phẩm</p>
    <form method="get" action="${pageContext.request.contextPath}/collections/ao-thun-co-tron" class="catalog-sort">
      <div class="catalog-search">
        <label for="catalog-search" class="visually-hidden">Tìm theo tên sản phẩm</label>
        <input id="catalog-search" type="search" name="q" value="<c:out value='${query}' />" placeholder="Tìm theo tên sản phẩm...">
      </div>
      <label for="catalog-sort">Sắp xếp theo</label>
      <select id="catalog-sort" name="sort">
        <option value="featured" ${sort == 'featured' ? 'selected' : ''}>Nổi bật</option>
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
          <img src="${pageContext.request.contextPath}/images/products/ao-thun-co-tron/${product.image}" alt="<c:out value='${product.name}' />" loading="${status.index < 4 ? 'eager' : 'lazy'}" decoding="async" width="600" height="800">
          <span class="product-shipping">Miễn phí ship*</span>
        </a>
        <h2 id="product-${status.index}"><a class="product-link" href="${pageContext.request.contextPath}/products/${product.id}"><c:out value="${product.name}" /></a></h2>
        <div class="product-prices">
          <c:if test="${product.originalPrice > product.price}">
            <del><span class="visually-hidden">Giá gốc: </span>${product.formattedOriginalPrice} VNĐ</del>
          </c:if>
          <span class="product-price"><span class="visually-hidden">Giá bán: </span>${product.formattedPrice} VNĐ</span>
        </div>
      </article>
    </c:forEach>
  </div>
  <p class="catalog-shipping-note">* Miễn phí vận chuyển cho đơn hàng từ 500.000 VNĐ.</p>
</section>
<%@ include file="../fragments/footer.jspf" %>
