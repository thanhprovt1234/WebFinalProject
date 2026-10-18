<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Quản lý tất cả shop" />
<%@ include file="start.jspf" %>
<p><a class="admin-button" href="${pageContext.request.contextPath}/admin/shops/new">+ Thêm shop</a></p>
<p>Xóa shop sẽ tắt hoạt động và ẩn sản phẩm của shop; dữ liệu đơn hàng vẫn được giữ lại.</p>
<div class="admin-table"><table><thead><tr><th>Shop</th><th>Chủ sở hữu</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody><c:forEach var="s" items="${shops}"><tr><td><strong><c:out value="${s.name}" /></strong><small><c:out value="${s.slug}" /></small></td><td><c:out value="${s.owner}" /></td><td><c:out value="${s.status}" /> · ${s.enabled ? 'Đang bật' : 'Đã tắt'}</td><td><div class="admin-actions"><a href="${pageContext.request.contextPath}/admin/shops/${s.id}/edit">Sửa / Chi nhánh</a><a href="${pageContext.request.contextPath}/admin/products?shopId=${s.id}">Sản phẩm</a><form method="post" action="${pageContext.request.contextPath}/admin/shops/${s.id}/delete" data-confirm="Xóa shop khỏi hoạt động bán hàng? Có thể bật lại trong trang sửa."><%@ include file="csrf.jspf" %><button class="admin-danger" type="submit">Xóa / Ngừng hoạt động</button></form></div></td></tr></c:forEach></tbody></table></div>
<%@ include file="end.jspf" %>
