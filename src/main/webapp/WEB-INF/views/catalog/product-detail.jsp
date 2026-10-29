<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="${product.name}" />
<c:set var="pageDescription" value="Khám phá chi tiết áo thun cổ tròn H2T, chọn màu và kích thước yêu thích." />
<c:set var="bodyClass" value="product-page" />
<%@ include file="../fragments/header.jspf" %>
<div class="detail-shell" data-product-id="${product.id}" data-view-url="${pageContext.request.contextPath}/api/products/${product.id}/viewed" data-signed-in="${not empty pageContext.request.userPrincipal}">
  <nav class="catalog-breadcrumb" aria-label="Đường dẫn">
    <a href="${pageContext.request.contextPath}/">Trang chủ</a><span>/</span>
    <a href="${pageContext.request.contextPath}/collections?category=${product.categoryId}"><c:out value="${product.categoryName}" /></a><span>/</span>
    <span aria-current="page"><c:out value="${product.name}" /></span>
  </nav>
  <section class="detail-layout" aria-labelledby="detail-title">
    <div class="detail-gallery">
      <div class="gallery-thumbs" aria-label="Chế độ xem ảnh">
        <button type="button" class="gallery-thumb" data-zoom="false" aria-pressed="true" aria-label="Xem toàn bộ áo"><img src="<c:out value="${product.imageUrl.startsWith('/') ? pageContext.request.contextPath.concat(product.imageUrl) : product.imageUrl}" />" alt="Toàn bộ áo"></button>
        <button type="button" class="gallery-thumb thumb-zoom" data-zoom="true" aria-pressed="false" aria-label="Phóng to ảnh áo"><img src="<c:out value="${product.imageUrl.startsWith('/') ? pageContext.request.contextPath.concat(product.imageUrl) : product.imageUrl}" />" alt="Phóng to ảnh"></button>
      </div>
      <div class="gallery-stage"><img id="detail-image" src="<c:out value="${product.imageUrl.startsWith('/') ? pageContext.request.contextPath.concat(product.imageUrl) : product.imageUrl}" />" alt="<c:out value='${product.name}' />" width="600" height="800" fetchpriority="high"></div>
    </div>
    <div class="detail-info">
      <h1 id="detail-title"><c:out value="${product.name}" /></h1>
      <p class="detail-sku">Mã sản phẩm: <span>${product.id}</span></p>
      <c:if test="${not empty activitySuccess}"><p class="activity-status" role="status"><c:out value="${activitySuccess}" /></p></c:if>
      <c:choose>
        <c:when test="${not empty pageContext.request.userPrincipal}">
          <form class="favorite-form" method="post" action="${pageContext.request.contextPath}/products/${product.id}/favorite">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
            <input type="hidden" name="saved" value="${not favorite}">
            <button class="favorite-button" type="submit" aria-pressed="${favorite}"><i class="bi ${favorite ? 'bi-heart-fill' : 'bi-heart'}" aria-hidden="true"></i> ${favorite ? 'Đã yêu thích · Bỏ lưu' : 'Thêm vào yêu thích'}</button>
          </form>
          <p id="view-history-status" class="activity-status" role="status"></p>
          <nav class="activity-nav" aria-label="Danh sách sản phẩm của bạn"><a href="${pageContext.request.contextPath}/favorites">Yêu thích của tôi</a><a href="${pageContext.request.contextPath}/recently-viewed">Đã xem gần đây</a></nav>
        </c:when>
        <c:otherwise><p class="activity-login"><a href="${pageContext.request.contextPath}/login"><i class="bi bi-heart" aria-hidden="true"></i> Đăng nhập để lưu yêu thích và lịch sử xem</a></p></c:otherwise>
      </c:choose>
      <p class="option-heading">Màu: <strong><c:out value="${product.color}" /></strong></p>
      <div class="color-options">
        <c:forEach var="color" items="${colors}">
          <a href="${pageContext.request.contextPath}/products/${color.id}" class="color-option" title="<c:out value='${color.color}' />" aria-label="<c:out value='${color.color}' />" <c:if test="${color.id == product.id}">aria-current="true"</c:if>><img src="<c:out value="${color.imageUrl.startsWith('/') ? pageContext.request.contextPath.concat(color.imageUrl) : color.imageUrl}" />" alt="<c:out value='${color.color}' />"></a>
        </c:forEach>
      </div>
      <p>Shop: <c:out value="${product.shopName}" /> · <c:out value="${product.categoryName}" /></p>
      <div class="detail-pricing"><div><strong id="variant-price">${product.formattedPrice} VNĐ</strong></div></div>
      <p class="shipping-line"><i class="bi bi-truck" aria-hidden="true"></i> Miễn phí vận chuyển cho đơn từ 500.000 VNĐ</p>
      <form id="product-options" data-name="<c:out value='${product.name}' />" data-price="${product.price}" data-image="<c:out value="${product.imageUrl.startsWith('/') ? pageContext.request.contextPath.concat(product.imageUrl) : product.imageUrl}" />">
        <fieldset class="size-options"><legend>Chọn màu và size</legend><div>
          <c:forEach var="variant" items="${variants}"><label><input type="radio" name="size" value="<c:out value='${variant.size}' />" data-color="<c:out value='${variant.color}' />" data-price="${variant.price}" data-stock="${variant.stock}" required><span><c:out value="${variant.color}" /> · <c:out value="${variant.size}" /></span></label></c:forEach>
        </div></fieldset>
        <p id="variant-stock" role="status">${empty variants ? 'Sản phẩm chưa có phiên bản để bán.' : 'Chọn màu và size để xem số lượng còn hàng.'}</p>
        <details class="size-guide"><summary>Hướng dẫn chọn size</summary><p>Chọn size bạn thường mặc. Nếu thích phom rộng, cân nhắc tăng một size. Liên hệ cửa hàng để xác nhận số đo trước khi đặt hàng.</p></details>
        <div class="quantity-row"><label for="quantity">Số lượng:</label><div class="quantity-control"><button type="button" data-step="-1" aria-label="Giảm số lượng">−</button><input id="quantity" name="quantity" type="number" min="1" max="99" value="1" required><button type="button" data-step="1" aria-label="Tăng số lượng">+</button></div></div>
        <button type="submit" class="detail-primary" data-unavailable="true" disabled>THÊM VÀO GIỎ HÀNG</button>
        <p id="cart-status" role="status"></p>
      </form>
      <button type="button" id="open-cart" class="detail-text-button">Xem giỏ hàng (<span id="cart-count">0</span>)</button>
      <p class="detail-local-note">Đăng nhập để lưu giỏ hàng và đặt hàng thanh toán khi nhận hàng (COD).</p>
      <div class="detail-benefits"><p><i class="bi bi-truck" aria-hidden="true"></i> Miễn phí giao hàng cho đơn từ 500K</p><p><i class="bi bi-palette" aria-hidden="true"></i> Nhiều màu sắc, dễ phối mỗi ngày</p><p><i class="bi bi-heart" aria-hidden="true"></i> Phong cách đơn giản cùng H2T</p></div>
      <details class="product-description"><summary>Thông tin sản phẩm</summary><p><c:out value="${product.description}" default="" /></p></details>
    </div>
  </section>
  <section class="detail-related" aria-labelledby="related-title"><h2 id="related-title">GỢI Ý SẢN PHẨM</h2><div class="product-grid">
    <c:forEach var="related" items="${relatedProducts}"><article class="catalog-product"><a class="product-photo product-link" href="${pageContext.request.contextPath}/products/${related.id}"><img src="<c:out value="${related.imageUrl.startsWith('/') ? pageContext.request.contextPath.concat(related.imageUrl) : related.imageUrl}" />" alt="<c:out value='${related.name}' />" width="600" height="800" loading="lazy"></a><h3><a class="product-link" href="${pageContext.request.contextPath}/products/${related.id}"><c:out value="${related.name}" /></a></h3><div class="product-prices"><span class="product-price">${related.formattedPrice} VNĐ</span></div></article></c:forEach>
  </div></section>
  <section class="detail-reviews" aria-labelledby="reviews-title">
    <h2 id="reviews-title">Đánh giá sản phẩm</h2>
    <div class="review-layout">
      <div>
        <div class="review-score"><strong>${reviews.average}</strong><span>
          <span class="review-stars" aria-label="${reviews.average} trên 5 sao"><c:forEach begin="1" end="5" var="star">${star <= reviews.roundedAverage ? '★' : '☆'}</c:forEach></span>
          <small>Dựa trên ${reviews.total} đánh giá từ khách hàng</small>
        </span></div>
        <h3>Tỷ lệ đánh giá</h3>
        <c:forEach var="row" items="${reviews.distribution}">
          <div class="rating-row" aria-label="${row.stars} sao: ${row.count} đánh giá, ${row.percent}%"><span>${row.stars} <b>★</b></span><span class="rating-bar"><span style="width: ${row.percent}%"></span></span><span>${row.percent}%</span></div>
        </c:forEach>
      </div>
      <div class="review-comments">
        <h3>Nhận xét của khách hàng</h3>
        <c:if test="${not empty reviewSuccess}"><p class="review-success" role="status"><c:out value="${reviewSuccess}" /></p></c:if>
        <c:if test="${not empty reviewError}"><p class="review-error" role="alert"><c:out value="${reviewError}" /></p></c:if>
        <c:choose>
          <c:when test="${reviews.canReview}">
            <form class="review-form" id="review-form" tabindex="-1" method="post" action="${pageContext.request.contextPath}/products/${product.id}/reviews#reviews-title">
              <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
              <h4>Đánh giá của bạn — đơn hàng #${reviews.form.orderId}</h4>
              <input type="hidden" name="orderId" value="${reviewForm.orderId}">
              <p>Chọn lần mua để đánh giá:</p>
              <c:forEach var="purchase" items="${reviews.purchases}">
                <c:url var="purchaseReviewUrl" value="/products/${product.id}"><c:param name="orderId" value="${purchase.id}" /></c:url>
                <a href="<c:out value='${purchaseReviewUrl}' />#review-form">Đơn #${purchase.id} (${purchase.reviewed ? 'Đã đánh giá' : 'Chưa đánh giá'})</a>
              </c:forEach>
              <fieldset class="review-rating"><legend>Chọn số sao</legend><div class="review-star-options">
                <c:forEach begin="1" end="5" var="star">
                  <label><input type="radio" name="rating" value="${star}" required ${reviewForm.rating == star ? 'checked' : ''}><span>★ <small>${star}</small></span><span class="visually-hidden">${star} sao</span></label>
                </c:forEach>
              </div></fieldset>
              <label for="review-comment">Nhận xét của bạn</label>
              <textarea id="review-comment" name="comment" rows="4" maxlength="2000" required aria-describedby="review-hint" placeholder="Chia sẻ cảm nhận của bạn về sản phẩm..."><c:out value="${reviewForm.comment}" /></textarea>
              <p id="review-hint">Tối đa 2.000 ký tự. Mỗi sản phẩm trong mỗi đơn hàng có một đánh giá riêng. Mua lại ở đơn khác, bạn được đánh giá thêm lần nữa.</p>
              <button type="submit" class="detail-primary">${empty reviews.form.rating ? 'Gửi đánh giá' : 'Cập nhật đánh giá'}</button>
            </form>
          </c:when>
          <c:when test="${not empty pageContext.request.userPrincipal}">
            <p class="review-login">Bạn chỉ có thể đánh giá sản phẩm đã mua khi đơn hàng đã giao thành công.</p>
          </c:when>
          <c:otherwise><p class="review-login"><a href="${pageContext.request.contextPath}/login">Đăng nhập</a> để viết nhận xét và đánh giá sản phẩm.</p></c:otherwise>
        </c:choose>
        <c:if test="${empty reviews.comments}"><p>Hiện tại sản phẩm chưa có đánh giá.</p></c:if>
        <div class="review-list">
          <c:forEach var="review" items="${reviews.comments}">
            <article class="review-entry">
              <div class="review-entry-heading"><strong><c:out value="${review.author}" /></strong><span>${review.date}</span></div>
              <div class="review-stars" aria-label="${review.rating} trên 5 sao"><c:forEach begin="1" end="5" var="star">${star <= review.rating ? '★' : '☆'}</c:forEach></div>
              <p class="review-content"><c:out value="${review.comment}" /></p>
            </article>
          </c:forEach>
        </div>
      </div>
    </div>
  </section>
</div>

<script src="${pageContext.request.contextPath}/js/product-detail.js" defer></script>
<%@ include file="../fragments/footer.jspf" %>
