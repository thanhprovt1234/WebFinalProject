<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<c:set var="pageTitle" value="Đặt hàng COD" />
<c:set var="bodyClass" value="checkout-page" />
<%@ include file="../fragments/header.jspf" %>
<fmt:setLocale value="vi_VN" />
<section class="checkout-shell">
  <nav><a href="${pageContext.request.contextPath}/collections">Sản phẩm</a> / Thanh toán</nav>
  <h1>Đặt hàng</h1>
  <p>Kiểm tra thông tin trước khi xác nhận. Bạn thanh toán cho người giao hàng khi nhận hàng.</p>
  <div class="checkout-layout">
    <section class="checkout-card">
      <h2>Thông tin nhận hàng</h2>
      <form:form modelAttribute="checkoutForm" method="post" action="${pageContext.request.contextPath}/checkout" htmlEscape="true" cssClass="checkout-form">
        <div role="alert"><form:errors path="*" element="p" cssClass="text-danger" htmlEscape="true" /></div>
        <form:hidden path="token" /><form:hidden path="expectedTotal" />
        <label for="receiverName">Họ tên người nhận</label><form:input path="receiverName" id="receiverName" maxlength="150" required="required" autocomplete="name" />
        <label for="receiverPhone">Số điện thoại</label><form:input path="receiverPhone" id="receiverPhone" type="tel" maxlength="20" required="required" autocomplete="tel" />
        <label for="shippingAddress">Địa chỉ đầy đủ (số nhà, đường, phường/xã, tỉnh/thành)</label><form:textarea path="shippingAddress" id="shippingAddress" maxlength="500" required="required" autocomplete="street-address" rows="3" />
        <label for="voucherCode">Mã giảm giá</label>
        <form:input path="voucherCode" id="voucherCode" maxlength="40" placeholder="Nhập mã voucher" autocomplete="off" aria-describedby="voucher-help" />
        <button type="submit" name="action" value="apply" formnovalidate="formnovalidate" class="checkout-button">Áp dụng mã</button>
        <p id="voucher-help">Mỗi mã dùng một lần cho tài khoản được tặng. Xem điều kiện và hạn dùng của từng mã bên dưới.</p>
        <c:if test="${not empty voucherError}"><p class="text-danger" role="alert"><c:out value="${voucherError}" /></p></c:if>
        <c:if test="${discount > 0}"><p role="status">Đã áp dụng mã, giảm <fmt:formatNumber value="${discount}" />đ.</p></c:if>
        <%@ include file="voucher-wallet.jspf" %>
        <fieldset><legend>Phương thức thanh toán</legend><label><input type="radio" checked disabled /> Thanh toán khi nhận hàng (COD)</label><p>Chưa thu tiền khi đặt hàng.</p></fieldset>
        <c:choose><c:when test="${canOrder}"><button type="submit" class="checkout-button">Xác nhận đặt hàng COD</button></c:when><c:otherwise><p role="status">Giỏ hàng trống hoặc có sản phẩm không đủ tồn kho. Hãy cập nhật giỏ hàng trước khi đặt.</p><button type="button" data-open-cart class="checkout-button">Mở giỏ hàng</button></c:otherwise></c:choose>
      </form:form>
    </section>
    <section class="checkout-card"><h2>Đơn hàng của bạn</h2>
      <c:forEach var="item" items="${cart.items}"><article class="checkout-line"><img src="<c:out value="${item.image.startsWith('/') ? pageContext.request.contextPath.concat(item.image) : item.image}" />" alt="<c:out value='${item.name}' />" /><div><strong><c:out value="${item.name}" /></strong><p><c:out value="${item.color}" /> · Size <c:out value="${item.size}" /> · SL ${item.quantity}</p><p><fmt:formatNumber value="${item.subtotal}" />đ</p></div></article></c:forEach>
      <p>Phí giao hàng: <strong>Miễn phí</strong></p>
      <p>Tạm tính: <strong><fmt:formatNumber value="${cart.total}" />đ</strong></p>
      <p>Giảm giá: <strong>−<fmt:formatNumber value="${discount}" />đ</strong></p>
      <p>Đơn thực trả trên 500.000đ được tặng mã giảm 10% sau khi giao hàng và thanh toán thành công.</p>
      <p class="checkout-total">Tổng thanh toán: <strong><fmt:formatNumber value="${payableTotal}" />đ</strong></p>
    </section>
  </div>
</section>
<script>document.querySelector('.checkout-form')?.addEventListener('submit', function (event) { if(event.submitter?.value==='apply') return; const button=event.submitter; if(button){button.disabled=true;button.textContent='Đang đặt hàng…';} });</script>
<%@ include file="../fragments/footer.jspf" %>
