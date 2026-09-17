<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

        <!DOCTYPE html>
        <html lang="vi">

        <head>
            <meta charset="UTF-8" />
            <title>Đặt lại mật khẩu</title>
        </head>

        <body>
            <h1>Đặt lại mật khẩu</h1>

            <form:form method="post" action="${pageContext.request.contextPath}/reset-password"
                modelAttribute="resetPasswordRequest">

                <form:errors path="*" cssStyle="color: red;" element="div" />

                <div>
                    <form:label path="newPassword">
                        Mật khẩu mới
                    </form:label>

                    <form:password path="newPassword" autocomplete="new-password" />
                </div>

                <div>
                    <form:label path="confirmPassword">
                        Nhập lại mật khẩu
                    </form:label>

                    <form:password path="confirmPassword" autocomplete="new-password" />
                </div>

                <button type="submit">
                    Đổi mật khẩu
                </button>
            </form:form>

            <p>
                <a href="${pageContext.request.contextPath}/login">
                    Quay lại đăng nhập
                </a>
            </p>
        </body>

        </html>