<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Xử lý đơn hàng" />
<%@ include file="start.jspf" %>
<div class="admin-panel"><h2>Đơn shop #${shopOrder.id} — <c:out value="${shopOrder.shop}" /></h2><p>Trạng thái: <strong><c:out value="${shopOrder.statusLabel}" /></strong></p><p>Thay đổi dưới đây áp dụng cho phần đơn của shop này.</p>
<c:if test="${not empty nextStatuses}"><form method="post" action="${pageContext.request.contextPath}${ordersBase}/${shopOrder.id}/status" class="admin-form"><%@ include file="csrf.jspf" %><label>Chuyển trạng thái<select name="status" required><c:forEach var="s" items="${nextStatuses}"><option value="${s}"><c:choose><c:when test="${s == 'CONFIRMED'}">Xác nhận đơn</c:when><c:when test="${s == 'CANCELLED'}">Từ chối / Hủy đơn &amp; hoàn tồn kho</c:when><c:when test="${s == 'SHIPPING'}">Bắt đầu giao hàng</c:when><c:when test="${s == 'DELIVERED'}">Xác nhận đã giao &amp; đã thanh toán</c:when><c:otherwise>${s}</c:otherwise></c:choose></option></c:forEach></select></label><label>Ghi chú (bắt buộc khi hủy đơn)<textarea name="note" maxlength="1800" rows="3"></textarea></label><button class="admin-button" type="submit">Lưu trạng thái</button></form></c:if></div>
<div class="admin-panel"><h2>Thông tin đơn của khách</h2><%@ include file="../checkout/details.jspf" %></div>
<div class="admin-panel"><h2>Lịch sử xử lý phần đơn</h2><c:forEach var="e" items="${events}"><p><strong><c:out value="${e.status}" /></strong> · <c:out value="${e.time}" /><br><c:out value="${e.note}" /></p></c:forEach></div>
<%@ include file="end.jspf" %>
