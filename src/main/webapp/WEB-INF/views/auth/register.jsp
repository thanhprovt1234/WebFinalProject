<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Đăng ký" />
<c:set var="pageDescription" value="Tạo tài khoản H2T Menswear." />
<c:set var="bodyClass" value="auth-page" />
<%@ include file="../fragments/header.jspf" %>

<section class="auth-shell section-space">
  <div class="container app-container">
    <div class="auth-stage auth-stage-register">
      <div class="row g-0">
        <div class="col-12">
          <div class="auth-panel auth-panel-register">
            <div class="auth-card">
              <div class="auth-card-header">
                <span class="auth-mini-mark" aria-hidden="true">H2T</span>
                <h1>Tạo tài khoản</h1>
                <p>Điền thông tin để bắt đầu hành trình phong cách của bạn.</p>
              </div>

              <form:form
                method="post"
                action="${pageContext.request.contextPath}/register"
                modelAttribute="registerRequest"
                cssClass="form-stack"
                data-submit-form="true"
              >
                <form:errors path="*" element="div" cssClass="alert alert-danger" />

                <div class="row g-3">
                  <div class="col-12">
                    <form:label path="username" cssClass="form-label">Tên đăng nhập</form:label>
                    <form:input path="username" cssClass="form-control" autocomplete="username" placeholder="Ví dụ: minhnguyen" />
                  </div>
                  <div class="col-12">
                    <form:label path="fullName" cssClass="form-label">Họ và tên</form:label>
                    <form:input path="fullName" cssClass="form-control" autocomplete="name" placeholder="Nguyễn Văn Minh" />
                  </div>
                </div>

                <div>
                  <form:label path="email" cssClass="form-label">Email</form:label>
                  <div class="input-group input-group-glass">
                    <span class="input-group-text"><i class="bi bi-envelope" aria-hidden="true"></i></span>
                    <form:input path="email" type="email" cssClass="form-control" autocomplete="email" placeholder="ban@example.com" />
                  </div>
                </div>

                <div>
                  <form:label path="password" cssClass="form-label">Mật khẩu</form:label>
                  <div class="input-group input-group-glass">
                    <span class="input-group-text"><i class="bi bi-lock" aria-hidden="true"></i></span>
                    <form:password path="password" id="registerPassword" cssClass="form-control" autocomplete="new-password" placeholder="Tạo mật khẩu" />
                    <button class="btn-password" type="button" data-password-toggle="#registerPassword" aria-label="Hiện mật khẩu" aria-pressed="false">
                      <i class="bi bi-eye" aria-hidden="true"></i>
                    </button>
                  </div>
                  <div class="form-hint">Dùng mật khẩu đủ mạnh và không chia sẻ với người khác.</div>
                </div>

                <div>
                  <form:label path="confirmPassword" cssClass="form-label">Nhập lại mật khẩu</form:label>
                  <div class="input-group input-group-glass">
                    <span class="input-group-text"><i class="bi bi-shield-lock" aria-hidden="true"></i></span>
                    <form:password path="confirmPassword" id="confirmRegisterPassword" cssClass="form-control" autocomplete="new-password" placeholder="Nhập lại mật khẩu" />
                    <button class="btn-password" type="button" data-password-toggle="#confirmRegisterPassword" aria-label="Hiện mật khẩu" aria-pressed="false">
                      <i class="bi bi-eye" aria-hidden="true"></i>
                    </button>
                  </div>
                </div>

                <button class="btn btn-primary btn-auth w-100" type="submit" data-loading-text="Đang tạo tài khoản...">
                  Tạo tài khoản
                  <i class="bi bi-arrow-right" aria-hidden="true"></i>
                </button>
              </form:form>

              <p class="auth-footer-text">
                Đã có tài khoản?
                <a href="${pageContext.request.contextPath}/login">Đăng nhập</a>
              </p>

            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</section>

<%@ include file="../fragments/footer.jspf" %>
