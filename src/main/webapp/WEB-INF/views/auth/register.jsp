<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %> <%@
taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<!DOCTYPE html>
<html lang="vi">
  <head>
    <meta charset="UTF-8" />
    <title>Đăng ký</title>
  </head>
  <body>
    <h1>Đăng ký tài khoản</h1>

    <form:form
      method="post"
      action="${pageContext.request.contextPath}/register"
      modelAttribute="registerRequest"
    >
      <form:errors path="*" cssStyle="color: red;" element="div" />

      <div>
        <form:label path="username">Tên đăng nhập</form:label>
        <form:input path="username" autocomplete="username" />
      </div>

      <div>
        <form:label path="email">Email</form:label>
        <form:input path="email" type="email" autocomplete="email" />
      </div>

      <div>
        <form:label path="fullName">Họ và tên</form:label>
        <form:input path="fullName" autocomplete="name" />
      </div>

      <div>
        <form:label path="password">Mật khẩu</form:label>
        <form:password path="password" autocomplete="new-password" />
      </div>

      <div>
        <form:label path="confirmPassword"> Nhập lại mật khẩu </form:label>
        <form:password path="confirmPassword" autocomplete="new-password" />
      </div>

      <button type="submit">Đăng ký</button>
    </form:form>

    <p>
      Đã có tài khoản?
      <a href="${pageContext.request.contextPath}/login"> Đăng nhập </a>
    </p>
  </body>
</html>
