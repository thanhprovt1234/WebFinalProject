<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Đăng nhập" />
<c:set var="pageDescription" value="Đăng nhập tài khoản H2T Menswear." />
<c:set var="bodyClass" value="auth-page" />
<%@ include file="../fragments/header.jspf" %>

<section class="auth-shell section-space">
  <div class="container app-container">
    <div class="auth-stage">
      <div class="row g-0">
        <div class="col-12">
          <div class="auth-panel">
            <div class="auth-card">
              <div class="auth-card-header">
                <span class="auth-mini-mark" aria-hidden="true">H2T</span>
                <h1>Chào mừng trở lại</h1>
                <p>Nhập thông tin của bạn để tiếp tục mua sắm.</p>
              </div>

              <c:if test="${param.error != null}">
                <div class="alert alert-danger" role="alert">
                  <i class="bi bi-exclamation-circle-fill" aria-hidden="true"></i>
                  <span>Tên đăng nhập hoặc mật khẩu không đúng.</span>
                </div>
              </c:if>
              <c:if test="${param.logout != null}">
                <div class="alert alert-success" role="status">
                  <i class="bi bi-check-circle-fill" aria-hidden="true"></i>
                  <span>Bạn đã đăng xuất thành công.</span>
                </div>
              </c:if>
              <c:if test="${param.registered != null}">
                <div class="alert alert-success" role="status">
                  <i class="bi bi-check-circle-fill" aria-hidden="true"></i>
                  <span>Đăng ký thành công. Bạn có thể đăng nhập ngay.</span>
                </div>
              </c:if>
              <c:if test="${param.resetSuccess != null}">
                <div class="alert alert-success" role="status">
                  <i class="bi bi-check-circle-fill" aria-hidden="true"></i>
                  <span>Đổi mật khẩu thành công. Hãy đăng nhập bằng mật khẩu mới.</span>
                </div>
              </c:if>

              <form action="${pageContext.request.contextPath}/login" method="post" class="form-stack" data-submit-form>
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">

                <div>
                  <label class="form-label" for="username">Tên đăng nhập</label>
                  <div class="input-group input-group-glass">
                    <span class="input-group-text"><i class="bi bi-person" aria-hidden="true"></i></span>
                    <input
                      class="form-control"
                      id="username"
                      type="text"
                      name="username"
                      autocomplete="username"
                      placeholder="Nhập tên đăng nhập"
                      required
                      autofocus
                    >
                  </div>
                </div>

                <div>
                  <label class="form-label" for="loginPassword">Mật khẩu</label>
                  <div class="input-group input-group-glass">
                    <span class="input-group-text"><i class="bi bi-lock" aria-hidden="true"></i></span>
                    <input
                      class="form-control"
                      id="loginPassword"
                      type="password"
                      name="password"
                      autocomplete="current-password"
                      placeholder="Nhập mật khẩu"
                      required
                    >
                    <button
                      class="btn-password"
                      type="button"
                      data-password-toggle="#loginPassword"
                      aria-label="Hiện mật khẩu"
                      aria-pressed="false"
                    ><i class="bi bi-eye" aria-hidden="true"></i></button>
                  </div>
                </div>

                <div class="auth-meta justify-content-end">
                  <a href="${pageContext.request.contextPath}/forgot-password">Quên mật khẩu?</a>
                </div>

                <button class="btn btn-primary btn-auth w-100" type="submit" data-loading-text="Đang đăng nhập...">
                  Đăng nhập
                  <i class="bi bi-arrow-right" aria-hidden="true"></i>
                </button>
              </form>

              <p class="auth-footer-text">
                Chưa có tài khoản?
                <a href="${pageContext.request.contextPath}/register">Đăng ký ngay</a>
              </p>

            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</section>

<%@ include file="../fragments/footer.jspf" %>
