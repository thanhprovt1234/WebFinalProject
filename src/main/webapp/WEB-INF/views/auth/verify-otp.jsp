<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
        <%@ taglib prefix="c" uri="jakarta.tags.core" %>

            <!DOCTYPE html>
            <html lang="vi">

            <head>
                <meta charset="UTF-8" />
                <title>Xác minh OTP</title>
            </head>

            <body>
                <h1>Xác minh OTP</h1>

                <c:if test="${param.verified != null}">
                    <p style="color: green;">
                        Mã OTP đã được xác minh thành công.
                    </p>
                </c:if>

                <c:if test="${param.sent != null}">
                    <p style="color: green;">
                        Nếu email tồn tại trong hệ thống, mã OTP đã được gửi.
                    </p>
                </c:if>

                <p>Nhập mã OTP gồm 6 chữ số được gửi đến email của bạn.</p>

                <form:form method="post" action="${pageContext.request.contextPath}/verify-otp"
                    modelAttribute="verifyOtpRequest">

                    <form:errors path="*" cssStyle="color: red;" element="div" />

                    <div>
                        <form:label path="otp">Mã OTP</form:label>
                        <form:input path="otp" inputmode="numeric" autocomplete="one-time-code" maxlength="6"
                            pattern="[0-9]{6}" />
                    </div>

                    <button type="submit">Xác minh</button>
                </form:form>

                <p>
                    <a href="${pageContext.request.contextPath}/forgot-password">
                        Gửi lại mã OTP
                    </a>
                </p>
            </body>

            </html>