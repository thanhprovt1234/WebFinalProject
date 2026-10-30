<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Khu vực làm việc" />
<c:set var="bodyClass" value="profile-page" />
<%@ include file="../fragments/header.jspf" %>
<main class="profile-shell">
<nav class="activity-nav"><a href="${pageContext.request.contextPath}/workspace">Khu vực làm việc</a><a href="${pageContext.request.contextPath}/profile">Tài khoản</a></nav>
<c:if test="${section == 'seller' || section == 'shop'}"><p><a href="${pageContext.request.contextPath}/seller/revenue">Xem doanh thu và số đơn theo shop</a></p></c:if>
<c:if test="${section == 'manager' || section == 'store'}"><p><a href="${pageContext.request.contextPath}/manager/revenue">Xem doanh thu và số đơn shop phụ trách</a></p></c:if>
<c:choose>
<c:when test="${section == 'seller'}"><h1>Shop của bạn</h1><p><a href="${pageContext.request.contextPath}/seller/orders">Quản lý đơn hàng shop</a></p><c:if test="${empty shops}"><p>Chưa có shop được liên kết với tài khoản.</p></c:if><c:forEach var="s" items="${shops}"><article class="profile-card mb-3"><h2><c:out value="${s.name}"/></h2><p><c:out value="${s.status}"/></p><a href="${pageContext.request.contextPath}/seller/shops/${s.id}">Xem sản phẩm của shop</a></article></c:forEach></c:when>
<c:when test="${section == 'shop'}"><h1><c:out value="${shop.name}"/></h1><p>Danh sách sản phẩm thuộc shop.</p><c:if test="${empty products}"><p>Shop chưa có sản phẩm.</p></c:if><div class="table-responsive"><table class="table"><thead><tr><th>Sản phẩm</th><th>Giá (VNĐ)</th><th>Hiển thị</th></tr></thead><tbody><c:forEach var="p" items="${products}"><tr><td><c:out value="${p.name}"/></td><td><c:out value="${p.price}"/></td><td>${p.enabled ? 'Đang bật' : 'Đã tắt'}</td></tr></c:forEach></tbody></table></div></c:when>
<c:when test="${section == 'manager'}"><h1>Chi nhánh được phân công</h1><c:if test="${empty stores}"><p>Bạn chưa được phân công chi nhánh. Admin có thể phân công trong trang tài khoản.</p></c:if><c:forEach var="s" items="${stores}"><article class="profile-card mb-3"><h2><c:out value="${s.name}"/></h2><p><c:out value="${s.shop}"/></p><a href="${pageContext.request.contextPath}/manager/stores/${s.id}">Xem tồn kho chi nhánh</a></article></c:forEach></c:when>
<c:when test="${section == 'store'}"><h1><c:out value="${store.name}"/></h1><p>Shop: <c:out value="${store.shop}"/></p><div class="table-responsive"><table class="table"><thead><tr><th>Sản phẩm</th><th>Size</th><th>Màu</th><th>Tồn kho</th></tr></thead><tbody><c:forEach var="i" items="${inventory}"><tr><td><c:out value="${i.name}"/></td><td><c:out value="${i.size}"/></td><td><c:out value="${i.color}"/></td><td>${i.quantity}</td></tr></c:forEach></tbody></table></div><c:if test="${empty inventory}"><p>Chưa có tồn kho tại chi nhánh.</p></c:if></c:when>
<c:when test="${section == 'shipper'}"><h1>Đơn giao được phân công</h1><c:if test="${empty shipments}"><p>Chưa có đơn được phân công.</p></c:if><c:forEach var="s" items="${shipments}"><article class="profile-card mb-3"><h2>Đơn #${s.orderId}</h2><p><c:out value="${s.shop}"/> — <c:out value="${s.status}"/></p><a href="${pageContext.request.contextPath}/shipper/shipments/${s.id}">Xem đơn giao</a></article></c:forEach></c:when>
<c:when test="${section == 'shipment'}"><h1>Đơn giao #${shipment.id}</h1><p>Đơn shop #${shipment.orderId}</p><p><c:out value="${shipment.shop}"/></p><p>Trạng thái: <c:out value="${shipment.status}"/></p></c:when>
</c:choose>
</main>
<%@ include file="../fragments/footer.jspf" %>

