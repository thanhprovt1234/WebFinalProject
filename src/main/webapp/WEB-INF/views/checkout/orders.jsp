<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="Đơn hàng của tôi" /><c:set var="bodyClass" value="checkout-page" />
<%@ include file="../fragments/header.jspf" %>
<fmt:setLocale value="vi_VN" />
<section class="orders-shell" data-placed-order="${placedOrder}">
  <nav class="orders-breadcrumb" aria-label="Đường dẫn"><a href="${pageContext.request.contextPath}/">Trang chủ</a><span>/</span><span>Đơn hàng của tôi</span></nav>
  <div class="orders-layout">
    <aside class="orders-sidebar">
      <div class="orders-account"><span class="orders-avatar" aria-hidden="true"><i class="bi bi-person"></i></span><strong><c:out value="${account.fullName}" /></strong><span><c:out value="${account.email}" /></span></div>
      <nav aria-label="Tài khoản"><a href="${pageContext.request.contextPath}/profile"><i class="bi bi-person" aria-hidden="true"></i>Thông tin cá nhân</a><a href="${pageContext.request.contextPath}/orders" aria-current="page"><i class="bi bi-bag-check" aria-hidden="true"></i>Đơn hàng của tôi</a><a href="${pageContext.request.contextPath}/profile#addresses"><i class="bi bi-geo-alt" aria-hidden="true"></i>Địa chỉ giao hàng</a></nav>
    </aside>
    <div class="orders-content">
      <div class="orders-heading"><div><span class="orders-eyebrow">TÀI KHOẢN H2T</span><h1>Đơn hàng của tôi</h1><p>Theo dõi đơn hàng và xem lại thông tin mua sắm của bạn.</p></div><a class="orders-shop-link" href="${pageContext.request.contextPath}/collections">Tiếp tục mua sắm ↗</a></div>
      <c:if test="${not empty placedOrder}"><p class="orders-success" role="status"><i class="bi bi-check-circle" aria-hidden="true"></i> Đặt hàng thành công! Đơn hàng đang chờ xác nhận. Bạn thanh toán khi nhận hàng.</p></c:if>
      <c:if test="${not empty success}"><p class="orders-success" role="status"><c:out value="${success}" /></p></c:if>
      <c:if test="${not empty error}"><p class="alert alert-danger" role="alert"><c:out value="${error}" /></p></c:if>
      <section class="checkout-card" id="vouchers" aria-label="Voucher của tôi"><h2>Voucher của tôi</h2><p>Quà tặng từ H2T và voucher thưởng mua hàng. Mỗi mã dùng một lần cho đúng tài khoản được tặng.</p><c:if test="${empty vouchers}"><p>Bạn chưa có voucher.</p></c:if><%@ include file="voucher-wallet.jspf" %></section>
      <div class="orders-list-heading"><h2>Tất cả đơn hàng</h2><span>${orders.size()} đơn hàng</span></div>
      <c:if test="${empty orders}"><div class="orders-empty"><i class="bi bi-bag" aria-hidden="true"></i><h2>Bạn chưa có đơn hàng nào</h2><p>Khám phá các sản phẩm và chọn món đồ bạn yêu thích.</p><a class="checkout-button" href="${pageContext.request.contextPath}/collections">Khám phá sản phẩm</a></div></c:if>
      <c:forEach var="order" items="${orders}">
        <article class="order-card" id="order-${order.id}">
          <header class="order-card-header"><div><h3>Đơn hàng #<c:out value="${order.code}" /></h3><p>Ngày đặt: <c:out value="${order.placedAt}" /></p></div><span class="order-badge" data-status="${order.status}"><c:out value="${order.statusLabel}" /></span></header>
          <div class="order-card-products"><c:forEach var="shop" items="${order.shops}"><div class="order-card-shop"><span><i class="bi bi-shop" aria-hidden="true"></i> <c:out value="${shop.name}" /></span><c:if test="${order.status == 'MIXED'}"><span class="order-badge" data-status="${shop.status}"><c:out value="${shop.statusLabel}" /></span></c:if></div><c:forEach var="item" items="${shop.items}"><div class="order-card-line"><div><strong><c:out value="${item.name}" /></strong><p><c:out value="${item.color}" /> · Size <c:out value="${item.size}" /> · Số lượng: ${item.quantity}</p><%@ include file="review-link.jspf" %></div><span><fmt:formatNumber value="${item.subtotal}" />đ</span></div></c:forEach></c:forEach></div>
          <%@ include file="cancel.jspf" %>
          <footer class="order-card-footer"><div><span>${order.itemCount} sản phẩm · <c:out value="${order.paymentMethod}" /></span><p>Tổng tiền: <strong><fmt:formatNumber value="${order.total}" />đ</strong></p></div><a class="order-detail-link" href="${pageContext.request.contextPath}/orders/${order.id}" data-order-dialog="order-dialog-${order.id}">Xem chi tiết <i class="bi bi-arrow-right" aria-hidden="true"></i></a></footer>
        </article>
        <dialog class="order-dialog" id="order-dialog-${order.id}" aria-labelledby="order-dialog-title-${order.id}">
          <header class="order-dialog-header"><h2 id="order-dialog-title-${order.id}">Chi tiết đơn hàng</h2><span class="order-badge" data-status="${order.status}"><c:out value="${order.statusLabel}" /></span><button type="button" data-close-order aria-label="Đóng chi tiết đơn hàng">×</button></header>
          <div class="order-dialog-body"><%@ include file="details.jspf" %></div>
        </dialog>
      </c:forEach>
    </div>
  </div>
</section>
<script src="${pageContext.request.contextPath}/js/orders.js"></script>
<%@ include file="../fragments/footer.jspf" %>
