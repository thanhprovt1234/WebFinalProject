<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Khu vực làm việc" />
<c:set var="bodyClass" value="profile-page" />
<%@ include file="../fragments/header.jspf" %>
<main class="profile-shell">
<nav class="activity-nav"><a href="${pageContext.request.contextPath}/workspace">Khu vực làm việc</a><a href="${pageContext.request.contextPath}/profile">Tài khoản</a></nav>
<h1>Khu vực làm việc</h1><p>Các chức năng được mở theo vai trò của tài khoản.</p>
<div class="profile-layout">
<section class="profile-card"><h2>Khách hàng</h2><p>Mua sắm, quản lý thông tin và địa chỉ nhận hàng.</p><a href="${pageContext.request.contextPath}/collections">Xem sản phẩm</a></section>
<c:if test="${roles.contains('ROLE_ADMIN')}"><section class="profile-card"><h2>Admin — Quản trị toàn hệ thống</h2><p>Quản lý tất cả shop, sản phẩm, tồn kho, đơn hàng và phân quyền tài khoản.</p><a href="${pageContext.request.contextPath}/admin">Mở trang quản trị</a> · <a href="${pageContext.request.contextPath}/admin/users">Phân quyền</a></section></c:if>
<c:if test="${roles.contains('ROLE_VENDOR') || roles.contains('ROLE_ADMIN')}"><section class="profile-card"><h2>Seller — Chủ shop</h2><a href="${pageContext.request.contextPath}/seller">Xem shop và sản phẩm</a> · <a href="${pageContext.request.contextPath}/seller/orders">Quản lý đơn hàng shop</a></section></c:if>
<c:if test="${roles.contains('ROLE_MANAGER') || roles.contains('ROLE_ADMIN')}"><section class="profile-card"><h2>Manager — Quản lý cửa hàng</h2><a href="${pageContext.request.contextPath}/manager">Xem chi nhánh được phân công</a></section></c:if>
<c:if test="${roles.contains('ROLE_SHIPPER') || roles.contains('ROLE_ADMIN')}"><section class="profile-card"><h2>Shipper — Giao hàng</h2><a href="${pageContext.request.contextPath}/shipper">Xem đơn được phân công</a></section></c:if>
</div>
</main>
<%@ include file="../fragments/footer.jspf" %>

