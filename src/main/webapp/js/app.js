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

  const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)");
  if (!reducedMotion.matches && "IntersectionObserver" in window) {
    const revealItems = [...document.querySelectorAll(
      ".campaign-content > *, .editorial-heading, .category-grid > .category, .brand-story > *, .membership > *, .footer-main"
    )];
    const observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add("is-visible");
          observer.unobserve(entry.target);
        }
      });
    }, { threshold: 0.08 });
    revealItems.forEach((item) => {
      const siblings = [...item.parentElement.children];
      const delay = item.matches(".category, .campaign-content > *")
        ? Math.min(siblings.indexOf(item), 3) * 90 : 0;
      item.style.setProperty("--reveal-delay", `${delay}ms`);
      item.classList.add("reveal-pending");
      observer.observe(item);
    });
    reducedMotion.addEventListener("change", (event) => {
      if (event.matches) {
        observer.disconnect();
        revealItems.forEach((item) => item.classList.add("is-visible"));
      }
    });
  }

  const categoryMenus = [...document.querySelectorAll(".category-dropdown")];
  const desktopHover = window.matchMedia("(min-width: 992px) and (hover: hover)");
  const setCategoryOpen = (menu, open) => {
    menu.querySelector(".category-toggle").setAttribute("aria-expanded", String(open));
    menu.querySelector(".category-submenu").hidden = !open;
  };
  const closeCategories = () => categoryMenus.forEach((menu) => setCategoryOpen(menu, false));

  categoryMenus.forEach((menu) => {
    const button = menu.querySelector(".category-toggle");
    button.addEventListener("click", () => {
      const open = button.getAttribute("aria-expanded") !== "true";
      closeCategories();
      setCategoryOpen(menu, open);
    });
    menu.addEventListener("pointerenter", () => {
      if (desktopHover.matches) {
        closeCategories();
        setCategoryOpen(menu, true);
      }
    });
    menu.addEventListener("pointerleave", () => {
      if (desktopHover.matches) setCategoryOpen(menu, false);
    });
    menu.addEventListener("focusout", (event) => {
      if (!menu.contains(event.relatedTarget)) setCategoryOpen(menu, false);
    });
    menu.addEventListener("keydown", (event) => {
      if (event.key === "Escape") {
        closeCategories();
        button.focus();
      }
    });
  });
  document.addEventListener("click", (event) => {
    if (!event.target.closest(".category-dropdown")) closeCategories();
  });
  desktopHover.addEventListener("change", closeCategories);
  document.querySelector("#mainNavigation")?.addEventListener("hide.bs.collapse", closeCategories);

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
