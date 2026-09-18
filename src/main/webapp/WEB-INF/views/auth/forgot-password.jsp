<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Quên mật khẩu" />
<c:set var="pageDescription" value="Khôi phục mật khẩu tài khoản H2T bằng email." />
<c:set var="bodyClass" value="auth-page" />
<%@ include file="../fragments/header.jspf" %>

<section class="auth-shell section-space">
  <div class="container app-container">
    <div class="row align-items-center justify-content-center g-5">
      <div class="col-lg-5 d-none d-lg-block">
        <div class="auth-intro">
          <span class="auth-intro-icon"><i class="bi bi-envelope-check" aria-hidden="true"></i></span>
          <h2>Lấy lại quyền truy cập thật dễ dàng.</h2>
          <p>H2T sẽ gửi mã xác minh một lần đến email đã đăng ký để bảo vệ tài khoản của bạn.</p>
          <div class="auth-trust-list">
            <span><i class="bi bi-check-circle-fill" aria-hidden="true"></i> Mã OTP chỉ có hiệu lực trong thời gian ngắn</span>
            <span><i class="bi bi-check-circle-fill" aria-hidden="true"></i> Không tiết lộ email có tồn tại trong hệ thống</span>
          </div>
        </div>
      </div>

      <div class="col-md-9 col-lg-6 col-xl-5">
        <div class="auth-card">
          <div class="auth-card-header">
            <span class="eyebrow">Khôi phục tài khoản</span>
            <h1>Quên mật khẩu?</h1>
            <p>Nhập email đã đăng ký để nhận mã OTP gồm 6 chữ số.</p>
          </div>

          <c:if test="${param.sent != null}">
            <div class="alert alert-success" role="status">
              <i class="bi bi-check-circle-fill" aria-hidden="true"></i>
              <span>Nếu email tồn tại trong hệ thống, mã OTP đã được gửi.</span>
            </div>
          </c:if>

          <form:form
            method="post"
            action="${pageContext.request.contextPath}/forgot-password"
            modelAttribute="forgotPasswordRequest"
            cssClass="form-stack"
            data-submit-form="true"
          >
            <form:errors path="*" element="div" cssClass="alert alert-danger" />

            <div>
              <form:label path="email" cssClass="form-label">Email</form:label>
              <form:input path="email" type="email" cssClass="form-control" autocomplete="email" placeholder="ban@example.com" autofocus="true" />
            </div>

            <button class="btn btn-primary w-100" type="submit" data-loading-text="Đang gửi mã...">
              Gửi mã OTP
              <i class="bi bi-send" aria-hidden="true"></i>
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
