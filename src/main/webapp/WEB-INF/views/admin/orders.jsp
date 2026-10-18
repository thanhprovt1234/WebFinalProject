<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Duyệt & xử lý đơn hàng" />
<%@ include file="start.jspf" %>
<%@ include file="cancellations.jspf" %>
<p>Mỗi dòng là phần đơn của một shop. <c:choose><c:when test="${sellerOrders}">Bạn chỉ xem và xử lý đơn của shop mình sở hữu.</c:when><c:otherwise>Admin xử lý được đơn của tất cả shop.</c:otherwise></c:choose></p>
<div class="admin-table"><table><thead><tr><th>Đơn shop / Đơn khách</th><th>Shop</th><th>Khách hàng</th><th>Trạng thái</th><th>Tổng tiền</th><th>Thao tác</th></tr></thead><tbody><c:forEach var="o" items="${ordersPage.content}"><tr><td>#${o.id} / #${o.orderId}</td><td><c:out value="${o.shop}" /></td><td><c:out value="${o.customer}" /></td><td><c:out value="${o.statusLabel}" /></td><td><fmt:formatNumber value="${o.total}" />đ</td><td><a href="${pageContext.request.contextPath}${ordersBase}/${o.id}">Xem / Xác nhận</a></td></tr></c:forEach></tbody></table></div>
<c:if test="${empty ordersPage.content}"><p>Chưa có đơn hàng.</p></c:if>
<nav class="admin-pagination"><c:if test="${ordersPage.hasPrevious()}"><a href="${pageContext.request.contextPath}${ordersBase}?page=${ordersPage.number-1}">← Trước</a></c:if><span>Trang ${ordersPage.number+1} / ${ordersPage.totalPages == 0 ? 1 : ordersPage.totalPages}</span><c:if test="${ordersPage.hasNext()}"><a href="${pageContext.request.contextPath}${ordersBase}?page=${ordersPage.number+1}">Sau →</a></c:if></nav>
<%@ include file="end.jspf" %>
