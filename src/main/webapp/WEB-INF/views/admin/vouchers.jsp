<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<c:set var="pageTitle" value="Voucher & quà tặng" />
<%@ include file="start.jspf" %>
<p>Tạo quà tặng và gửi mã riêng cho những khách hàng bạn chọn. Mỗi mã chỉ dùng một lần cho đúng người nhận.</p>
<section class="admin-panel">
  <h2>Tạo chương trình quà tặng</h2>
  <form:form modelAttribute="campaignForm" method="post" action="${pageContext.request.contextPath}/admin/vouchers" cssClass="admin-form" htmlEscape="true">
    <div class="voucher-form-errors" role="alert"><form:errors path="*" element="p" /></div>
    <label>Tên chương trình<form:input path="name" maxlength="150" required="required" placeholder="Ví dụ: Quà may mắn tháng 10" /></label>
    <label>Loại giảm giá<form:select path="discountType"><form:option value="FIXED">Giảm số tiền (đ)</form:option><form:option value="PERCENT">Giảm phần trăm (%)</form:option></form:select></label>
    <label>Mức giảm<form:input path="discountValue" type="number" min="0.01" step="0.01" max="999999999999" required="required" /></label>
    <label>Giá trị đơn tối thiểu (đ)<form:input path="minimumTotal" type="number" min="0" step="0.01" max="999999999999" required="required" /></label>
    <label>Giảm tối đa (đ, chỉ áp dụng cho giảm %)<form:input path="maximumDiscount" type="number" min="0.01" step="0.01" max="999999999999" placeholder="Để trống nếu không giới hạn" /></label>
    <label>Ngày hết hạn<form:input path="expiryDate" type="date" required="required" /><small>Có hiệu lực đến hết ngày này, theo giờ Việt Nam.</small></label>
    <button type="submit">Tạo và chọn người nhận →</button>
  </form:form>
</section>
<h2>Các chương trình đã tạo</h2>
<div class="admin-table"><table><thead><tr><th>Chương trình</th><th>Ưu đãi</th><th>Đơn tối thiểu</th><th>Hạn dùng</th><th></th></tr></thead><tbody>
<c:forEach var="campaign" items="${campaigns.content}"><tr>
  <td><c:out value="${campaign.name}" /></td>
  <td><fmt:formatNumber value="${campaign.discountValue}" />${campaign.discountType == 'PERCENT' ? '%' : 'đ'}<c:if test="${not empty campaign.maximumDiscount}"><small>Tối đa <fmt:formatNumber value="${campaign.maximumDiscount}" />đ</small></c:if></td>
  <td><fmt:formatNumber value="${campaign.minimumTotal}" />đ</td><td>${campaign.expiryLabel}</td>
  <td><a class="admin-button" href="${pageContext.request.contextPath}/admin/vouchers/${campaign.id}">Chọn người nhận &amp; quản lý</a></td>
</tr></c:forEach>
<c:if test="${empty campaigns.content}"><tr><td colspan="5">Chưa có chương trình quà tặng.</td></tr></c:if>
</tbody></table></div>
<nav class="admin-pagination" aria-label="Trang chương trình"><c:if test="${campaigns.hasPrevious()}"><a href="?page=${campaigns.number - 1}">← Trước</a></c:if><span>Trang ${campaigns.number + 1}</span><c:if test="${campaigns.hasNext()}"><a href="?page=${campaigns.number + 1}">Sau →</a></c:if></nav>
<%@ include file="end.jspf" %>
