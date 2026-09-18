<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Đặt lại mật khẩu" />
<c:set var="pageDescription" value="Đặt mật khẩu mới cho tài khoản H2T." />
<c:set var="bodyClass" value="auth-page" />
<%@ include file="../fragments/header.jspf" %>

<section class="auth-shell section-space">
  <div class="container app-container">
    <div class="row align-items-center justify-content-center g-5">
      <div class="col-lg-5 d-none d-lg-block">
        <div class="auth-intro">
          <span class="auth-intro-icon"><i class="bi bi-key" aria-hidden="true"></i></span>
          <h2>Tạo mật khẩu mới an toàn hơn.</h2>
          <p>Chọn mật khẩu khó đoán, khác với mật khẩu cũ và chỉ mình bạn biết.</p>
          <div class="auth-trust-list">
            <span><i class="bi bi-check-circle-fill" aria-hidden="true"></i> Không dùng lại mật khẩu ở dịch vụ khác</span>
            <span><i class="bi bi-check-circle-fill" aria-hidden="true"></i> Kết hợp chữ, số và ký tự đặc biệt</span>
          </div>
        </div>
      </div>

      <div class="col-md-9 col-lg-6 col-xl-5">
        <div class="auth-card">
          <div class="auth-card-header">
            <span class="eyebrow">Bước 3 / 3</span>
            <h1>Đặt lại mật khẩu</h1>
            <p>Nhập mật khẩu mới cho tài khoản của bạn.</p>
          </div>

          <form:form
            method="post"
            action="${pageContext.request.contextPath}/reset-password"
            modelAttribute="resetPasswordRequest"
            cssClass="form-stack"
            data-submit-form="true"
          >
            <form:errors path="*" element="div" cssClass="alert alert-danger" />

            <div>
              <form:label path="newPassword" cssClass="form-label">Mật khẩu mới</form:label>
              <div class="input-group">
                <form:password path="newPassword" id="newPassword" cssClass="form-control" autocomplete="new-password" placeholder="Nhập mật khẩu mới" autofocus="true" />
                <button class="btn-password" type="button" data-password-toggle="#newPassword" aria-label="Hiện mật khẩu" aria-pressed="false">
                  <i class="bi bi-eye" aria-hidden="true"></i>
                </button>
              </div>
            </div>

            <div>
              <form:label path="confirmPassword" cssClass="form-label">Nhập lại mật khẩu</form:label>
              <div class="input-group">
                <form:password path="confirmPassword" id="confirmNewPassword" cssClass="form-control" autocomplete="new-password" placeholder="Xác nhận mật khẩu mới" />
                <button class="btn-password" type="button" data-password-toggle="#confirmNewPassword" aria-label="Hiện mật khẩu" aria-pressed="false">
                  <i class="bi bi-eye" aria-hidden="true"></i>
                </button>
              </div>
            </div>

            <button class="btn btn-primary w-100" type="submit" data-loading-text="Đang cập nhật...">
              Đổi mật khẩu
              <i class="bi bi-check2" aria-hidden="true"></i>
            </button>
          </form:form>

          <p class="auth-footer-text">
            <a href="${pageContext.request.contextPath}/login">
              <i class="bi bi-arrow-left" aria-hidden="true"></i>
              Quay lại đăng nhập
            </a>
          </p>
        </div>
      </div>
    </div>
  </div>
</section>

<%@ include file="../fragments/footer.jspf" %>
