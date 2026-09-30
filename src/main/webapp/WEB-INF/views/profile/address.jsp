<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="${empty addressId ? 'Thêm địa chỉ' : 'Chỉnh sửa địa chỉ'}" />
<c:set var="pageDescription" value="Quản lý địa chỉ nhận hàng của bạn." />
<c:set var="bodyClass" value="profile-page" />
<%@ include file="../fragments/header.jspf" %>
<section class="profile-shell profile-address-shell">
  <a href="${pageContext.request.contextPath}/profile#addresses">← Quay lại tài khoản</a>
  <h1><c:out value="${pageTitle}" /></h1>
  <div class="profile-card">
    <c:set var="addressAction" value="${pageContext.request.contextPath}/profile/addresses" />
    <c:if test="${not empty addressId}"><c:set var="addressAction" value="${addressAction}/${addressId}" /></c:if>
    <form:form method="post" action="${addressAction}" modelAttribute="addressForm" htmlEscape="true" cssClass="profile-form">
      <div role="alert"><form:errors path="*" element="p" cssClass="text-danger" htmlEscape="true" /></div>
      <div class="address-form-grid">
        <div><form:label path="receiverName">Họ tên người nhận</form:label><form:input path="receiverName" required="required" maxlength="150" autocomplete="shipping name" /></div>
        <div><form:label path="phone">Số điện thoại</form:label><form:input path="phone" type="tel" required="required" maxlength="20" autocomplete="shipping tel" /></div>
        <div class="address-full-width"><form:label path="addressLine">Số nhà, tên đường</form:label><form:input path="addressLine" required="required" maxlength="500" autocomplete="shipping address-line1" /></div>
        <div><form:label path="ward">Phường / xã</form:label><form:input path="ward" required="required" maxlength="100" /></div>
        <div><form:label path="district">Quận / huyện / khu vực</form:label><form:input path="district" required="required" maxlength="100" /></div>
        <div class="address-full-width"><form:label path="city">Tỉnh / thành phố</form:label><form:input path="city" required="required" maxlength="100" autocomplete="shipping address-level1" /></div>
      </div>
      <label class="address-default-choice"><form:checkbox path="defaultAddress" /> Đặt làm địa chỉ mặc định</label>
      <p class="profile-hint">Địa chỉ đầu tiên tự động là mặc định. Để đổi địa chỉ mặc định, hãy chọn một địa chỉ khác trong danh sách.</p>
      <div class="address-actions"><button class="btn btn-dark" type="submit">Lưu địa chỉ</button><a href="${pageContext.request.contextPath}/profile#addresses">Hủy</a></div>
    </form:form>
  </div>
</section>
<%@ include file="../fragments/footer.jspf" %>
