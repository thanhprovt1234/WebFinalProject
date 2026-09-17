<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
  <%@ taglib prefix="c" uri="jakarta.tags.core" %>
    <!DOCTYPE html>
    <html lang="vi">

    <head>
      <meta charset="UTF-8" />
      <title>Trang chủ</title>
    </head>

    <body>
      <h1>Trang chủ H2T</h1>

      <c:choose>
        <c:when test="${not empty pageContext.request.userPrincipal}">
          <p>
            Xin chào,
            <c:out value="${pageContext.request.userPrincipal.name}" />
          </p>

          <form action="${pageContext.request.contextPath}/logout" method="post">

            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">

            <button type="submit">Đăng xuất</button>
          </form>
        </c:when>

        <c:otherwise>
          <a href="${pageContext.request.contextPath}/login">
            Đăng nhập
          </a>

          <a href="${pageContext.request.contextPath}/register">
            Đăng ký
          </a>
        </c:otherwise>
      </c:choose>
    </body>

    </html>