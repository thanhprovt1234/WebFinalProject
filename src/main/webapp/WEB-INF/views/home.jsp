<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Trang chủ" />
<c:set var="pageDescription" value="H2T Menswear - thời trang nam hiện đại, tinh gọn và dễ mặc mỗi ngày." />
<c:set var="bodyClass" value="home-page" />
<%@ include file="fragments/header.jspf" %>

<section class="home-hero">
  <div class="container app-container">
    <div class="hero-glass">
      <div class="row align-items-center g-0">
        <div class="col-lg-6">
          <div class="hero-copy">
            <span class="eyebrow">
              <i class="bi bi-stars" aria-hidden="true"></i>
              Bộ sưu tập mới · 2026
            </span>
            <h1 class="hero-title">
              Mặc đẹp,<br>
              <span class="text-accent">nhẹ tênh.</span>
            </h1>
            <p class="hero-lead">
              Những thiết kế nam tối giản, dễ phối và vừa vặn với nhịp sống hiện đại.
              Chỉn chu từ chất liệu đến từng đường may.
            </p>

            <div class="hero-actions">
              <a class="btn btn-primary btn-lg" href="#collection">
                Khám phá bộ sưu tập
                <i class="bi bi-arrow-down-right" aria-hidden="true"></i>
              </a>
              <c:if test="${empty pageContext.request.userPrincipal}">
                <a class="btn btn-glass btn-lg" href="${pageContext.request.contextPath}/register">
                  Tạo tài khoản
                </a>
              </c:if>
            </div>

            <div class="hero-details" aria-label="Danh mục nổi bật">
              <span>Áo sơ mi</span>
              <span>Áo polo</span>
              <span>Quần dài</span>
            </div>
          </div>
        </div>

        <div class="col-lg-6">
          <div class="hero-fashion">
            <img
              src="${pageContext.request.contextPath}/images/h2t-fashion-hero.png"
              alt="Người mẫu nam mặc áo khoác sơ mi xanh navy của H2T"
            >
            <div class="fashion-tag fashion-tag-top">
              <span class="tag-kicker">New drop</span>
              <strong>Navy Edit</strong>
            </div>
            <div class="fashion-tag fashion-tag-bottom">
              <span class="tag-icon"><i class="bi bi-droplet-half" aria-hidden="true"></i></span>
              <span><strong>Chất liệu thoáng</strong><small>Êm nhẹ cả ngày</small></span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</section>

<section id="collection" class="section-space collection-section">
  <div class="container app-container">
    <div class="section-heading section-heading-row">
      <div>
        <span class="eyebrow">Chọn theo phong cách</span>
        <h2>Tủ đồ gọn hơn.<br>Phối đồ dễ hơn.</h2>
      </div>
      <p>
        Bắt đầu với những món đồ nền tảng có thể đồng hành cùng bạn từ ngày làm việc
        đến buổi hẹn cuối tuần.
      </p>
    </div>

    <div class="row g-4">
      <div class="col-md-6 col-lg-4">
        <article class="collection-card collection-card-shirt">
          <div class="collection-icon"><i class="bi bi-person-standing" aria-hidden="true"></i></div>
          <div class="collection-content">
            <span class="collection-index">01</span>
            <h3>Sơ mi tinh gọn</h3>
            <p>Phom đứng vừa phải, bảng màu dễ mặc và chất vải mát nhẹ.</p>
            <a href="#about" aria-label="Xem thêm về sơ mi tinh gọn">Khám phá <i class="bi bi-arrow-right" aria-hidden="true"></i></a>
          </div>
        </article>
      </div>

      <div class="col-md-6 col-lg-4">
        <article class="collection-card collection-card-polo">
          <div class="collection-icon"><i class="bi bi-wind" aria-hidden="true"></i></div>
          <div class="collection-content">
            <span class="collection-index">02</span>
            <h3>Polo linh hoạt</h3>
            <p>Thoải mái như áo thun, chỉn chu vừa đủ cho mọi cuộc hẹn.</p>
            <a href="#about" aria-label="Xem thêm về áo polo linh hoạt">Khám phá <i class="bi bi-arrow-right" aria-hidden="true"></i></a>
          </div>
        </article>
      </div>

      <div class="col-md-12 col-lg-4">
        <article class="collection-card collection-card-trouser">
          <div class="collection-icon"><i class="bi bi-bag" aria-hidden="true"></i></div>
          <div class="collection-content">
            <span class="collection-index">03</span>
            <h3>Quần may đo cảm hứng</h3>
            <p>Đường cắt gọn, chuyển động thoải mái và luôn giữ vẻ lịch lãm.</p>
            <a href="#about" aria-label="Xem thêm về quần may đo cảm hứng">Khám phá <i class="bi bi-arrow-right" aria-hidden="true"></i></a>
          </div>
        </article>
      </div>
    </div>
  </div>
</section>

<section id="about" class="section-space pt-0">
  <div class="container app-container">
    <div class="promise-glass">
      <div class="row align-items-center g-4">
        <div class="col-lg-5">
          <span class="eyebrow">
            <i class="bi bi-gem" aria-hidden="true"></i>
            Chất H2T
          </span>
          <h2>Đẹp từ cảm giác đầu tiên.</h2>
          <p>
            Chúng tôi chọn thiết kế tinh giản để chất liệu, phom dáng và sự thoải mái
            được lên tiếng.
          </p>
        </div>
        <div class="col-lg-7">
          <div class="promise-grid">
            <div class="promise-item">
              <span class="promise-icon"><i class="bi bi-truck" aria-hidden="true"></i></span>
              <strong>Giao hàng nhanh</strong>
              <small>Theo dõi đơn dễ dàng</small>
            </div>
            <div class="promise-item">
              <span class="promise-icon"><i class="bi bi-arrow-repeat" aria-hidden="true"></i></span>
              <strong>Đổi size 7 ngày</strong>
              <small>Mua sắm an tâm hơn</small>
            </div>
            <div class="promise-item">
              <span class="promise-icon"><i class="bi bi-chat-heart" aria-hidden="true"></i></span>
              <strong>Tư vấn tận tâm</strong>
              <small>Chọn đúng phom, đúng gu</small>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</section>

<c:if test="${empty pageContext.request.userPrincipal}">
  <section class="pb-5">
    <div class="container app-container">
      <div class="cta-card">
        <div>
          <span class="eyebrow mb-2">H2T Membership</span>
          <h2>Phong cách riêng, bắt đầu từ đây.</h2>
          <p>Tạo tài khoản để trải nghiệm hành trình mua sắm liền mạch cùng H2T.</p>
        </div>
        <div class="d-flex flex-wrap gap-2">
          <a class="btn btn-glass" href="${pageContext.request.contextPath}/login">Đăng nhập</a>
          <a class="btn btn-primary" href="${pageContext.request.contextPath}/register">
            Tạo tài khoản
            <i class="bi bi-arrow-right" aria-hidden="true"></i>
          </a>
        </div>
      </div>
    </div>
  </section>
</c:if>

<%@ include file="fragments/footer.jspf" %>
