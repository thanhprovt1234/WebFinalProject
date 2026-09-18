<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Xác minh OTP" />
<c:set var="pageDescription" value="Xác minh mã OTP để khôi phục tài khoản H2T." />
<c:set var="bodyClass" value="auth-page" />
<%@ include file="../fragments/header.jspf" %>

<section class="auth-shell section-space">
  <div class="container app-container">
    <div class="row align-items-center justify-content-center g-5">
      <div class="col-lg-5 d-none d-lg-block">
        <div class="auth-intro">
          <span class="auth-intro-icon"><i class="bi bi-shield-lock" aria-hidden="true"></i></span>
          <h2>Một bước xác minh để bảo vệ bạn.</h2>
          <p>Nhập chính xác mã được gửi đến email. Đừng chia sẻ mã này với bất kỳ ai.</p>
          <div class="auth-trust-list">
            <span><i class="bi bi-check-circle-fill" aria-hidden="true"></i> Sáu chữ số, xác minh một lần</span>
            <span><i class="bi bi-check-circle-fill" aria-hidden="true"></i> Giới hạn số lần nhập để tăng bảo mật</span>
          </div>
        </div>
      </div>

      <div class="col-md-9 col-lg-6 col-xl-5">
        <div class="auth-card">
          <div class="auth-card-header">
            <span class="eyebrow">Bước 2 / 3</span>
            <h1>Xác minh OTP</h1>
            <p>Nhập mã gồm 6 chữ số được gửi đến email của bạn.</p>
          </div>

          <c:if test="${param.verified != null}">
            <div class="alert alert-success" role="status">
              <i class="bi bi-check-circle-fill" aria-hidden="true"></i>
              <span>Mã OTP đã được xác minh thành công.</span>
            </div>
          </c:if>
          <c:if test="${param.sent != null}">
            <div class="alert alert-info" role="status">
              <i class="bi bi-envelope-check-fill" aria-hidden="true"></i>
              <span>Nếu email tồn tại trong hệ thống, mã OTP đã được gửi.</span>
            </div>
          </c:if>

          <form:form
            method="post"
            action="${pageContext.request.contextPath}/verify-otp"
            modelAttribute="verifyOtpRequest"
            cssClass="form-stack"
            data-submit-form="true"
          >
            <form:errors path="*" element="div" cssClass="alert alert-danger" />

            <div>
              <form:label path="otp" cssClass="form-label">Mã OTP</form:label>
              <form:input
                path="otp"
                cssClass="form-control otp-input"
                inputmode="numeric"
                autocomplete="one-time-code"
                maxlength="6"
                pattern="[0-9]{6}"
                placeholder="••••••"
                autofocus="true"
              />
              <div class="form-hint text-center">Mã gồm đúng 6 chữ số.</div>
            </div>

            <button class="btn btn-primary w-100" type="submit" data-loading-text="Đang xác minh...">
              Xác minh
              <i class="bi bi-shield-check" aria-hidden="true"></i>
            </button>
          </form:form>

          <p class="auth-footer-text">
            Chưa nhận được mã?
            <a href="${pageContext.request.contextPath}/forgot-password">Gửi lại mã OTP</a>
          </p>
        </div>
      </div>
    </div>
  </div>
</section>

<%@ include file="../fragments/footer.jspf" %>
