<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Trang chủ" />
<c:set var="pageDescription" value="Khám phá phong cách thời trang nam tinh giản cùng H2T Menswear." />
<c:set var="bodyClass" value="home-page" />
<%@ include file="fragments/header.jspf" %>
<section class="campaign" aria-labelledby="campaign-title">
  <img class="campaign-image" src="${pageContext.request.contextPath}/images/h2t-editorial-hero.png" alt="Hai người mẫu mặc sơ mi trắng kem và polo đen trong bộ sưu tập H2T" fetchpriority="high" width="1942" height="809">
  <div class="campaign-shade"></div>
  <div class="campaign-content">
    <p class="campaign-kicker"><span></span> THE EVERYDAY COLLECTION / 2026</p>
    <h1 id="campaign-title">ĐƠN GIẢN.<br>ĐÚNG <em>GU.</em></h1>
    <p class="campaign-description">Không cần cầu kỳ để khác biệt.<br>Chọn điều vừa vặn. Mặc chất riêng của bạn.</p>
    <a class="editorial-button" href="#collection">KHÁM PHÁ BỘ SƯU TẬP <span aria-hidden="true">↗</span></a>
  </div>
  <div class="campaign-bottom"><span>H2T® — MADE FOR EVERY DAY</span><a href="#collection">CUỘN ĐỂ KHÁM PHÁ <span aria-hidden="true">↓</span></a></div>
</section>
<div class="brand-strip" aria-label="Tinh thần H2T"><span>THIẾT KẾ TINH GIẢN</span><span aria-hidden="true">✳</span><span>CHẤT LIỆU CHỌN LỌC</span><span aria-hidden="true">✳</span><span>PHONG CÁCH MỖI NGÀY</span><span aria-hidden="true">✳</span><span>ĐẬM CHẤT RIÊNG</span></div>
<section id="collection" class="editorial-section">
  <div class="editorial-heading"><div><p class="overline">THE EDIT / 01</p><h2>Chọn món. Lên gu.</h2></div><p>Những thiết kế nền tảng.<br>Vô vàn cách thể hiện bạn.</p></div>
  <div class="category-grid">
    <article id="shirts" class="category">
      <div class="category-photo category-shirt"><img src="${pageContext.request.contextPath}/images/h2t-editorial-hero.png" alt="Sơ mi ngắn tay trắng kem, phom thoải mái" loading="lazy"><span class="photo-label">THE EVERYDAY SHIRT</span></div>
      <details><summary><span><small>01 / SHIRTS</small><strong>Áo sơ mi</strong></span><span class="category-arrow" aria-hidden="true">↗</span></summary><p>Sơ mi trắng kem, phom thoải mái và dễ phối. Kết hợp cùng quần tối màu cho ngày làm việc, hoặc khoác ngoài áo thun vào cuối tuần.</p></details>
    </article>
    <article id="polos" class="category">
      <div class="category-photo category-polo"><img src="${pageContext.request.contextPath}/images/h2t-editorial-hero.png" alt="Áo polo đen phối cổ trắng phong cách tối giản" loading="lazy"><span class="photo-label">EFFORTLESS ESSENTIALS</span></div>
      <details><summary><span><small>02 / POLOS</small><strong>Áo polo</strong></span><span class="category-arrow" aria-hidden="true">↗</span></summary><p>Polo đen với điểm nhấn cổ tương phản. Phối cùng quần xám hoặc jeans để có một diện mạo chỉn chu, thoải mái cho mọi cuộc hẹn.</p></details>
    </article>
    <article id="trousers" class="category">
      <div class="category-photo category-trouser"><img src="${pageContext.request.contextPath}/images/h2t-editorial-hero.png" alt="Quần dài xếp ly màu than chì kết hợp cùng sơ mi và polo" loading="lazy"><span class="photo-label">YOUR DAILY UNIFORM</span></div>
      <details><summary><span><small>03 / TROUSERS</small><strong>Quần dài</strong></span><span class="category-arrow" aria-hidden="true">↗</span></summary><p>Quần dài tông trung tính với đường ly gọn gàng. Sơ vin cùng sơ mi để thêm lịch lãm, hoặc phối áo polo cho phong cách thường ngày.</p></details>
    </article>
  </div>
</section>
<section id="about" class="brand-story">
  <p class="overline">LESS, BUT BETTER.</p><h2>Mặc đơn giản.<br>Sống có chất.</h2><div><p>H2T tin rằng phong cách bắt đầu từ sự thoải mái. Một chiếc áo vừa vặn, một bảng màu dễ phối, một thiết kế bạn muốn mặc lại mỗi ngày.</p><a class="text-link" href="#collection">Tìm phong cách của bạn <span aria-hidden="true">↗</span></a></div>
</section>
<c:if test="${empty pageContext.request.userPrincipal}">
  <section id="membership" class="membership">
    <div><p class="overline">THE H2T COMMUNITY</p><h2>Gu riêng của bạn.<br>Không gian của chúng ta.</h2><p>Trở thành một phần của H2T. Bắt đầu với tài khoản của bạn.</p></div>
    <div class="membership-actions"><a class="editorial-button" href="${pageContext.request.contextPath}/register">ĐĂNG KÝ THÀNH VIÊN <span aria-hidden="true">↗</span></a><p>Đã có tài khoản? <a href="${pageContext.request.contextPath}/login">Đăng nhập</a></p></div>
  </section>
</c:if>
<%@ include file="fragments/footer.jspf" %>
