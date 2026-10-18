<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Tổng quan quản trị" />
<%@ include file="start.jspf" %>
<%@ include file="cancellations.jspf" %>
<p>Quản lý toàn bộ shop và dữ liệu bán hàng từ một tài khoản Admin.</p>
<div class="admin-stats"><a href="${pageContext.request.contextPath}/admin/shops"><strong>${totals.shops}</strong><span>Shop</span></a><a href="${pageContext.request.contextPath}/admin/products"><strong>${totals.products}</strong><span>Sản phẩm</span></a><a href="${pageContext.request.contextPath}/admin/orders"><strong>${totals.orders}</strong><span>Đơn theo shop</span></a><a href="${pageContext.request.contextPath}/admin/users"><strong>${totals.users}</strong><span>Tài khoản</span></a></div>
<div class="admin-panel"><h2>Quy trình xử lý đơn COD</h2><p>Chờ xác nhận → Admin xác nhận → Đã giao hàng &amp; đã thanh toán.</p><p>Admin có thể từ chối/hủy đơn chưa xác nhận, kèm lý do. Tồn kho được hoàn một lần khi hủy. Với đơn nhiều shop, tổng đơn ghi nhận đã thanh toán khi tất cả phần đơn đã được xác nhận hoặc hủy và có phần đã giao.</p><a class="admin-button" href="${pageContext.request.contextPath}/admin/orders">Duyệt đơn hàng</a></div>
<%@ include file="end.jspf" %>
