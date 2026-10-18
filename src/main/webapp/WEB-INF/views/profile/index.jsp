<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Tài khoản của tôi" />
<c:set var="pageDescription" value="Quản lý thông tin cá nhân và địa chỉ nhận hàng của bạn tại H2T." />
<c:set var="bodyClass" value="profile-page" />
<%@ include file="../fragments/header.jspf" %>
<section class="profile-shell">
  <nav aria-label="Đường dẫn"><a href="${pageContext.request.contextPath}/">Trang chủ</a> / Tài khoản</nav>
  <h1>Tài khoản của tôi</h1>
  <p><a href="${pageContext.request.contextPath}/orders">Đơn hàng của tôi →</a></p>
  <p><a href="${pageContext.request.contextPath}/workspace">Khu vực làm việc &amp; phân quyền →</a></p>
  <p class="profile-intro">Thông tin của bạn, sẵn sàng cho những lần mua sắm tiếp theo.</p>
  <nav class="activity-nav" aria-label="Sản phẩm của bạn"><a href="${pageContext.request.contextPath}/favorites"><i class="bi bi-heart" aria-hidden="true"></i> Sản phẩm yêu thích</a><a href="${pageContext.request.contextPath}/recently-viewed"><i class="bi bi-clock-history" aria-hidden="true"></i> Sản phẩm đã xem</a></nav>
  <c:if test="${not empty success}"><p class="alert alert-success" role="status"><c:out value="${success}" /></p></c:if>
  <div class="profile-layout">
    <section class="profile-card" aria-labelledby="profile-info-title">
      <h2 id="profile-info-title">Thông tin cá nhân</h2>
      <dl class="profile-account"><dt>Tên đăng nhập</dt><dd><c:out value="${account.username}" /></dd><dt>Email</dt><dd><c:out value="${account.email}" /></dd></dl>
      <form:form method="post" action="${pageContext.request.contextPath}/profile" modelAttribute="profileForm" htmlEscape="true" cssClass="profile-form">
        <div role="alert"><form:errors path="*" element="p" cssClass="text-danger" htmlEscape="true" /></div>
        <div><form:label path="fullName">Họ và tên</form:label><form:input path="fullName" required="required" maxlength="150" autocomplete="name" /></div>
        <div><form:label path="phone">Số điện thoại <small>(không bắt buộc)</small></form:label><form:input path="phone" type="tel" maxlength="20" autocomplete="tel" /></div>
        <button class="btn btn-dark" type="submit">Lưu thông tin</button>
      </form:form>
    </section>
    <section id="addresses" class="profile-card" aria-labelledby="address-list-title">
      <div class="profile-card-heading"><h2 id="address-list-title">Địa chỉ nhận hàng</h2><a class="btn btn-outline-dark btn-sm" href="${pageContext.request.contextPath}/profile/addresses/new">+ Thêm địa chỉ</a></div>
      <c:if test="${empty addresses}"><div class="profile-empty"><i class="bi bi-geo-alt" aria-hidden="true"></i><p>Bạn chưa có địa chỉ nhận hàng.</p><p>Thêm địa chỉ nhà riêng hoặc nơi làm việc để dùng khi đặt hàng.</p></div></c:if>
      <c:forEach var="address" items="${addresses}">
        <article class="address-card" data-address-id="${address.id}">
          <div class="address-name"><strong><c:out value="${address.receiverName}" /></strong><c:if test="${address['default']}"><span class="address-badge">Mặc định</span></c:if></div>
          <p><c:out value="${address.phone}" /></p>
          <p><c:out value="${address.addressLine}" />, <c:out value="${address.ward}" />, <c:out value="${address.district}" />, <c:out value="${address.city}" /></p>
          <div class="address-actions">
            <a href="${pageContext.request.contextPath}/profile/addresses/${address.id}/edit">Chỉnh sửa</a>
            <c:if test="${not address['default']}"><form method="post" action="${pageContext.request.contextPath}/profile/addresses/${address.id}/default"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"><button type="submit">Đặt mặc định</button></form></c:if>
            <form method="post" action="${pageContext.request.contextPath}/profile/addresses/${address.id}/delete" data-delete-address><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"><button type="submit" class="address-delete">Xóa</button></form>
          </div>
        </article>
      </c:forEach>
    </section>
  </div>
</section>
<script src="${pageContext.request.contextPath}/js/profile.js" defer></script>
<%@ include file="../fragments/footer.jspf" %>
