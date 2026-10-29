<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Tặng voucher cho khách hàng" />
<%@ include file="start.jspf" %>
<a href="${pageContext.request.contextPath}/admin/vouchers">← Các chương trình</a>
<section class="admin-panel">
  <h2><c:out value="${campaign.name}" /></h2>
  <p>Giảm <strong><fmt:formatNumber value="${campaign.discountValue}" />${campaign.discountType == 'PERCENT' ? '%' : 'đ'}</strong> · Đơn từ <fmt:formatNumber value="${campaign.minimumTotal}" />đ<c:if test="${not empty campaign.maximumDiscount}"> · Giảm tối đa <fmt:formatNumber value="${campaign.maximumDiscount}" />đ</c:if></p>
  <p>Hạn dùng: ${campaign.expiryLabel} (giờ Việt Nam). Mỗi khách nhận tối đa một mã trong chương trình này.</p>
</section>
<c:if test="${not campaign.active}"><p class="alert alert-info" role="status">Chương trình đã hết hạn. Bạn vẫn có thể xem lịch sử và thu hồi mã chưa dùng.</p></c:if>
<c:if test="${campaign.active}">
<section class="admin-panel">
  <h2>Bốc ngẫu nhiên khách hàng may mắn</h2>
  <p>Chọn trong các khách hàng đang hoạt động và chưa nhận quà của chương trình. Bạn sẽ xem danh sách trước khi phát.</p>
  <form class="admin-inline-form" method="post" action="${pageContext.request.contextPath}/admin/vouchers/${campaign.id}/random">
    <%@ include file="csrf.jspf" %><label>Số người nhận<input name="count" type="number" min="1" max="100" value="1" required></label><button type="submit">Chọn ngẫu nhiên</button>
  </form>
  <c:if test="${not empty selectedRecipients}">
    <h3>Danh sách may mắn — chưa phát voucher</h3>
    <form method="post" action="${pageContext.request.contextPath}/admin/vouchers/${campaign.id}/issue">
      <%@ include file="csrf.jspf" %>
      <ul><c:forEach var="recipient" items="${selectedRecipients}"><li><input type="hidden" name="recipientIds" value="${recipient.id}"><c:out value="${recipient.fullName}" /> · <c:out value="${recipient.username}" /> · <c:out value="${recipient.email}" /></li></c:forEach></ul>
      <button type="submit">Tặng voucher cho ${selectedRecipients.size()} người này</button>
    </form>
  </c:if>
</section>
<section class="admin-panel">
  <h2>Chọn người nhận trực tiếp</h2>
  <form class="admin-search" method="get"><input name="q" value="<c:out value='${query}' />" placeholder="Tìm tên, tài khoản hoặc email" aria-label="Tìm khách hàng"><button type="submit">Tìm kiếm</button></form>
  <form method="post" action="${pageContext.request.contextPath}/admin/vouchers/${campaign.id}/issue">
    <%@ include file="csrf.jspf" %>
    <div class="voucher-recipient-list"><c:forEach var="recipient" items="${recipients}"><label class="voucher-recipient"><input type="checkbox" name="recipientIds" value="${recipient.id}"><span><strong><c:out value="${recipient.fullName}" /></strong><small><c:out value="${recipient.username}" /> · <c:out value="${recipient.email}" /></small></span></label></c:forEach></div>
    <c:choose><c:when test="${empty recipients}"><p>Không có khách hàng phù hợp chưa nhận quà.</p></c:when><c:otherwise><p>Chọn tối đa 100 người mỗi lần. Mã sẽ xuất hiện trong “Voucher của tôi” của người nhận.</p><button type="submit">Tặng voucher cho người đã chọn</button></c:otherwise></c:choose>
  </form>
</section>
</c:if>
<h2>Voucher đã phát (${issued.size()})</h2>
<div class="admin-table"><table><thead><tr><th>Người nhận</th><th>Mã voucher</th><th>Ngày tặng / admin</th><th>Trạng thái</th><th></th></tr></thead><tbody>
<c:forEach var="voucher" items="${issued}"><tr><td><c:out value="${voucher.username}" /><small><c:out value="${voucher.email}" /></small></td><td><c:out value="${voucher.code}" /></td><td>${voucher.issuedAt}<small><c:out value="${voucher.issuedBy}" /></small></td><td><c:out value="${voucher.status}" /></td><td><c:if test="${voucher.canRevoke}"><form method="post" action="${pageContext.request.contextPath}/admin/vouchers/${campaign.id}/revoke/${voucher.id}"><%@ include file="csrf.jspf" %><button class="admin-danger" type="submit">Thu hồi</button></form></c:if></td></tr></c:forEach>
<c:if test="${empty issued}"><tr><td colspan="5">Chưa phát voucher nào.</td></tr></c:if>
</tbody></table></div>
<%@ include file="end.jspf" %>
