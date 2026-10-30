<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Doanh thu theo shop" />
<%@ include file="start.jspf" %>
<p>Thống kê theo ngày đặt đơn (giờ Việt Nam). Doanh thu chỉ tính đơn đã giao, sau giảm giá và gồm phí giao hàng; số tiền này chưa phản ánh đối soát COD.</p>
<c:if test="${revenueScope == 'manager'}"><p>Bạn đang xem doanh thu toàn shop có chi nhánh được phân công, không phải doanh thu riêng từng chi nhánh.</p></c:if>
<form method="get" class="admin-inline-form admin-panel">
    <label>Từ ngày<input type="date" name="from" value="${from}" required></label>
    <label>Đến ngày<input type="date" name="to" value="${to}" required></label>
    <label>Shop<select name="shopId"><option value="">Tất cả shop được phép xem</option><c:forEach var="s" items="${shops}"><option value="${s.id}" ${selectedShop == s.id ? 'selected' : ''}><c:out value="${s.name}"/></option></c:forEach></select></label>
    <button type="submit">Xem thống kê</button>
</form>
<div class="admin-stats">
    <div><strong>${summary.total}</strong><span>Tổng đơn theo shop</span></div>
    <div><strong>${summary.sold}</strong><span>Đơn đã giao</span></div>
    <div><strong>${summary.cancelled}</strong><span>Đơn đã hủy</span></div>
    <div><strong><fmt:formatNumber value="${summary.revenue}" maxFractionDigits="0"/> ₫</strong><span>Doanh thu đã giao</span></div>
</div>
<div class="admin-table"><table>
<thead><tr><th>Shop / Chủ shop</th><th>Manager / Chi nhánh</th><th>Tổng đơn</th><th>Đã giao</th><th>Đang xử lý</th><th>Đã hủy</th><th>Doanh thu (VNĐ)</th></tr></thead>
<tbody><c:forEach var="r" items="${rows}"><tr>
<td><c:out value="${r.name}"/><small><c:out value="${r.owner}"/></small></td>
<td><c:forEach var="m" items="${r.managers}"><div><c:out value="${m}"/></div></c:forEach><c:if test="${empty r.managers}">Chưa phân công</c:if></td>
<td>${r.total}</td><td>${r.sold}</td><td>${r.pending}</td><td>${r.cancelled}</td><td><fmt:formatNumber value="${r.revenue}" maxFractionDigits="0"/> ₫</td>
</tr></c:forEach><c:if test="${empty rows}"><tr><td colspan="7">Chưa có shop được phân công.</td></tr></c:if></tbody>
</table></div>
<%@ include file="end.jspf" %>
