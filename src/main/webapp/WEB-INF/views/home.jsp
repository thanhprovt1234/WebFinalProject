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
  <div class="editorial-heading collection-heading">
    <h2>Đơn giản mà <span>có gu.</span></h2>
    <p>Từ những món đồ quen thuộc,<br>tạo nên phong cách chỉ riêng bạn.</p>
  </div>
  <div class="category-grid">
    <article id="shirts" class="category">
      <div class="category-photo category-shirt"><img src="${pageContext.request.contextPath}/images/aosomi.png" alt="Áo sơ mi trắng dài tay phối cùng quần kaki be" loading="lazy" width="600" height="600"><span class="photo-label">THE EVERYDAY SHIRT</span></div>
      <details><summary><span><small>01 / SHIRTS</small><strong>Áo sơ mi</strong></span><span class="category-arrow" aria-hidden="true">↗</span></summary><p>Sơ mi trắng thanh lịch và dễ phối. Kết hợp cùng quần kaki be cho ngày làm việc, hoặc khoác ngoài áo thun vào cuối tuần.</p></details>
    </article>
    <article id="polos" class="category">
      <div class="category-photo category-polo"><img src="${pageContext.request.contextPath}/images/aopolo.png" alt="Áo polo trắng viền cổ và tay tối màu phối cùng quần đen" loading="lazy" width="564" height="705"><span class="photo-label">EFFORTLESS ESSENTIALS</span></div>
      <details><summary><span><small>02 / POLOS</small><strong>Áo polo</strong></span><span class="category-arrow" aria-hidden="true">↗</span></summary><p>Polo trắng với điểm nhấn viền cổ và tay tương phản. Phối cùng quần đen hoặc jeans để có một diện mạo chỉn chu, thoải mái cho mọi cuộc hẹn.</p></details>
    </article>
    <article id="trousers" class="category">
      <div class="category-photo category-trouser"><img src="${pageContext.request.contextPath}/images/quandai.png" alt="Quần dài đen ống rộng phối áo trắng và giày thể thao trắng" loading="lazy" width="351" height="468"><span class="photo-label">YOUR DAILY UNIFORM</span></div>
      <details><summary><span><small>03 / TROUSERS</small><strong>Quần dài</strong></span><span class="category-arrow" aria-hidden="true">↗</span></summary><p>Quần dài tông trung tính với đường ly gọn gàng. Sơ vin cùng sơ mi để thêm lịch lãm, hoặc phối áo polo cho phong cách thường ngày.</p></details>
    </article>
  </div>
</section>
<section id="about" class="brand-story">
  <div class="story-visual">
    <img src="${pageContext.request.contextPath}/images/h2t-editorial-hero.png" alt="Phong cách thường ngày cùng sơ mi và polo H2T" loading="lazy" width="1942" height="809">
    <span class="story-edition">H2T JOURNAL <span>VOL. 01 / 2026</span></span>
    <div class="story-image-caption"><span>THE ART OF EVERYDAY</span><strong>Ít hơn.<br>Chất hơn.</strong></div>
    <span class="story-seal" aria-hidden="true">H2T<br><small>EVERYDAY CLUB</small></span>
  </div>
  <div class="story-copy">
    <p class="overline">TINH THẦN H2T / LESS, BUT BETTER.</p>
    <h2>Mặc đơn giản.<br>Sống <em>có chất.</em></h2>
    <p>Phong cách không nằm ở việc bạn có bao nhiêu món đồ. Mà ở cách bạn biến những điều đơn giản thành chất riêng.</p>
    <div class="story-principles">
      <div><span>01</span><div><h3>Vừa vặn với bạn</h3><p>Thoải mái trong từng chuyển động, tự tin trong từng khoảnh khắc.</p></div></div>
      <div><span>02</span><div><h3>Dễ phối, dễ yêu</h3><p>Bảng màu tinh giản. Một món đồ, nhiều cách thể hiện.</p></div></div>
      <div><span>03</span><div><h3>Luôn là chính mình</h3><p>Từ ngày làm việc đến cuối tuần — phong cách theo nhịp sống của bạn.</p></div></div>
    </div>
    <a class="text-link" href="#collection">Khám phá chất riêng <span aria-hidden="true">↗</span></a>
  </div>
</section>
<div class="signature-strip" aria-hidden="true"><span>YOUR STYLE.</span><span class="signature-star">✳</span><span>YOUR EVERYDAY.</span><span class="signature-star">✳</span><span>YOUR H2T.</span></div>
<c:if test="${empty pageContext.request.userPrincipal}">
  <section id="membership" class="membership">
    <div class="membership-copy"><p class="overline"><span class="club-dot" aria-hidden="true"></span> THE H2T COMMUNITY</p><h2>Cùng một tinh thần.<br><em>Muôn vàn chất riêng.</em></h2><p>Một không gian dành cho những người yêu sự tinh giản.<br>Gia nhập H2T và bắt đầu câu chuyện phong cách của bạn.</p><div class="membership-actions"><a class="editorial-button" href="${pageContext.request.contextPath}/register">GIA NHẬP H2T <span aria-hidden="true">↗</span></a><p>Đã có tài khoản? <a href="${pageContext.request.contextPath}/login">Đăng nhập</a></p></div></div>
    <div class="club-art" aria-hidden="true"><span class="club-orbit"></span><div class="club-card"><div class="club-card-top"><strong>H2T</strong><span>THE EVERYDAY<br>CLUB</span></div><span class="club-card-star">✳</span><div class="club-card-bottom"><span>MAKE IT YOURS.</span><span>MEMBER / H2T</span></div></div><span class="club-art-caption">BE YOURSELF. BELONG HERE.</span></div>
  </section>
</c:if>
<%@ include file="fragments/footer.jspf" %>
