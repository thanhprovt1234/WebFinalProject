<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!DOCTYPE html>
<html lang="vi">
  <head>
    <meta charset="UTF-8" />
    <title>Quên mật khẩu</title>
  </head>
  <body>
    <h1>Quên mật khẩu</h1>

    <p>Nhập email đã đăng ký để nhận mã OTP.</p>

    <c:if test="${param.sent != null}">
      <p style="color: green;">
        Nếu email tồn tại trong hệ thống, mã OTP đã được gửi.
      </p>
    </c:if>

    <form:form
      method="post"
      action="${pageContext.request.contextPath}/forgot-password"
      modelAttribute="forgotPasswordRequest"
    >
      <form:errors path="*" cssStyle="color: red;" element="div" />

      <div>
        <form:label path="email">Email</form:label>
        <form:input path="email" type="email" autocomplete="email" />
      </div>

      <button type="submit">Gửi mã OTP</button>
    </form:form>

    <p>
      <a href="${pageContext.request.contextPath}/login">
        Quay lại đăng nhập
      </a>
    </p>
  </body>
</html>
