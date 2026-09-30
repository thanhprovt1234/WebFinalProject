<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="${favoritesPage ? 'Sản phẩm yêu thích' : 'Sản phẩm đã xem'}" />
<c:set var="pageDescription" value="Các sản phẩm bạn quan tâm tại H2T." />
<c:set var="bodyClass" value="catalog-page" />
<c:set var="listPath" value="${favoritesPage ? '/favorites' : '/recently-viewed'}" />
<%@ include file="../fragments/header.jspf" %>
<section class="catalog-shell activity-shell">
  <nav class="catalog-breadcrumb" aria-label="Đường dẫn"><a href="${pageContext.request.contextPath}/profile">Tài khoản</a><span>/</span><span><c:out value="${pageTitle}" /></span></nav>
  <h1><c:out value="${pageTitle}" /></h1>
  <nav class="activity-nav" aria-label="Sản phẩm của bạn"><a href="${pageContext.request.contextPath}/favorites" ${favoritesPage ? 'aria-current="page"' : ''}>Yêu thích</a><a href="${pageContext.request.contextPath}/recently-viewed" ${not favoritesPage ? 'aria-current="page"' : ''}>Đã xem gần đây</a></nav>
  <c:if test="${not empty activitySuccess}"><p class="activity-status" role="status"><c:out value="${activitySuccess}" /></p></c:if>
  <p class="activity-summary">${activityPage.totalElements} sản phẩm · ${favoritesPage ? 'Mới lưu nhất ở đầu' : 'Xem gần nhất ở đầu'}</p>
  <c:if test="${empty products}"><div class="catalog-empty"><p>${favoritesPage ? 'Chưa có sản phẩm yêu thích ở trang này.' : 'Chưa có sản phẩm đã xem ở trang này.'}</p><a href="${pageContext.request.contextPath}/collections/ao-thun-co-tron">Khám phá sản phẩm</a></div></c:if>
  <div class="product-grid">
    <c:forEach var="product" items="${products}">
      <article class="catalog-product" data-activity-product="${product.id}">
        <c:choose>
          <c:when test="${product.available}">
            <a class="product-photo product-link" href="${pageContext.request.contextPath}/products/${product.id}"><img src="${pageContext.request.contextPath}/images/products/ao-thun-co-tron/${product.image}" alt="<c:out value='${product.name}' />" width="600" height="800" loading="lazy"></a>
            <h2><a class="product-link" href="${pageContext.request.contextPath}/products/${product.id}"><c:out value="${product.name}" /></a></h2>
            <p class="product-price">${product.formattedPrice} VNĐ</p>
          </c:when>
          <c:otherwise><div class="activity-unavailable"><i class="bi bi-bag-x" aria-hidden="true"></i><p>Sản phẩm không còn kinh doanh</p></div></c:otherwise>
        </c:choose>
        <c:if test="${product.available || product.favorite}">
          <form class="favorite-form" method="post" action="${pageContext.request.contextPath}/products/${product.id}/favorite">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
            <input type="hidden" name="saved" value="${not product.favorite}">
            <input type="hidden" name="source" value="${favoritesPage ? 'favorites' : 'history'}">
            <input type="hidden" name="page" value="${activityPage.number}">
            <button class="favorite-button" type="submit" aria-pressed="${product.favorite}"><i class="bi ${product.favorite ? 'bi-heart-fill' : 'bi-heart'}" aria-hidden="true"></i> ${product.favorite ? 'Bỏ yêu thích' : 'Yêu thích'}</button>
          </form>
        </c:if>
      </article>
    </c:forEach>
  </div>
  <c:if test="${activityPage.totalPages > 1 || activityPage.number > 0}">
    <nav class="activity-pagination" aria-label="Phân trang">
      <c:if test="${activityPage.number > 0}"><a href="${pageContext.request.contextPath}${listPath}?page=${activityPage.number - 1}">← Trang trước</a></c:if>
      <span>Trang ${activityPage.number + 1} / ${activityPage.totalPages > 0 ? activityPage.totalPages : 1}</span>
      <c:if test="${activityPage.hasNext()}"><a href="${pageContext.request.contextPath}${listPath}?page=${activityPage.number + 1}">Trang sau →</a></c:if>
    </nav>
  </c:if>
</section>
<%@ include file="../fragments/footer.jspf" %>
