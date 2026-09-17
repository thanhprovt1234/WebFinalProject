<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
  <%@ taglib prefix="c" uri="jakarta.tags.core" %>
    <!DOCTYPE html>
    <html>

    <head>
      <meta charset="UTF-8" />
      <title>Đăng nhập</title>
    </head>

    <body>
      <h1>Đăng nhập</h1>
      <c:if test="${param.error != null}">
        <p style="color: red;">
          Tên đăng nhập hoặc mật khẩu không đúng.
        </p>
      </c:if>

      <c:if test="${param.logout != null}">
        <p style="color: green;">
          Bạn đã đăng xuất thành công.
        </p>
      </c:if>

      <c:if test="${param.registered != null}">
        <p style="color: green;">
          Đăng ký thành công. Bạn có thể đăng nhập.
        </p>
      </c:if>

      <form action="${pageContext.request.contextPath}/login" method="post">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />

        <div>
          <label>Tên đăng nhập</label>
          <input type="text" name="username" />
        </div>

        <div>
          <label>Mật khẩu</label>
          <input type="password" name="password" />
        </div>

        <button type="submit">Đăng nhập</button>
      </form>
    </body>

    </html>