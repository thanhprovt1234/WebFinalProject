(() => {
  "use strict";

  const navbar = document.querySelector(".site-navbar");

  const updateNavbar = () => {
    if (navbar) {
      navbar.classList.toggle("is-scrolled", window.scrollY > 8);
    }
  };

  updateNavbar();
  window.addEventListener("scroll", updateNavbar, { passive: true });

  document.querySelectorAll("[data-password-toggle]").forEach((button) => {
    const input = document.querySelector(button.dataset.passwordToggle);

    if (!input) {
      return;
    }

    button.addEventListener("click", () => {
      const shouldShow = input.type === "password";
      input.type = shouldShow ? "text" : "password";
      button.setAttribute("aria-label", shouldShow ? "Ẩn mật khẩu" : "Hiện mật khẩu");
      button.setAttribute("aria-pressed", String(shouldShow));

      const icon = button.querySelector("i");
      if (icon) {
        icon.className = shouldShow ? "bi bi-eye-slash" : "bi bi-eye";
      }
    });
  });

  document.querySelectorAll("[data-submit-form]").forEach((form) => {
    form.addEventListener("submit", () => {
      const button = form.querySelector('button[type="submit"]');

      if (!button || button.disabled) {
        return;
      }

      const loadingText = button.dataset.loadingText || "Đang xử lý...";
      button.disabled = true;
      button.setAttribute("aria-busy", "true");
      button.innerHTML = `<span class="spinner-border spinner-border-sm" aria-hidden="true"></span><span>${loadingText}</span>`;
    });
  });
})();
