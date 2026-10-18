<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="Chi tiết đơn hàng" /><c:set var="bodyClass" value="checkout-page" />
<%@ include file="../fragments/header.jspf" %>
<fmt:setLocale value="vi_VN" />
<section class="checkout-shell order-receipt">
  <p><a href="${pageContext.request.contextPath}/orders">← Đơn hàng của tôi</a></p>
  <header class="order-dialog-header"><h1>Chi tiết đơn hàng</h1><span class="order-badge" data-status="${order.status}"><c:out value="${order.statusLabel}" /></span></header>
  <%@ include file="details.jspf" %>
  <%@ include file="cancel.jspf" %>
  <p><a href="${pageContext.request.contextPath}/collections">Tiếp tục mua sắm →</a></p>
</section>
<%@ include file="../fragments/footer.jspf" %>
