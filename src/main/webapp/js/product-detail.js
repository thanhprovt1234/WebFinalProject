(() => {
  "use strict";
  const root = document.querySelector("[data-product-id]");
  if (!root) return;
  if (root.dataset.signedIn === "true") {
    const headers = {};
    headers[document.querySelector('meta[name="csrf-header"]').content] = document.querySelector('meta[name="csrf-token"]').content;
    const recordView = () => fetch(root.dataset.viewUrl, {
      method: "POST", headers, credentials: "same-origin", signal: AbortSignal.timeout(15000)
    }).then(response => {
      if (!response.ok) throw new Error("view failed");
    }).catch(() => {
      document.querySelector("#view-history-status").textContent = "Chưa lưu được lịch sử xem. Bạn có thể tải lại trang để thử lại.";
    });
    recordView();
    window.addEventListener("pageshow", event => { if (event.persisted) recordView(); });
  }
  document.querySelectorAll("[data-zoom]").forEach(button => {
    button.addEventListener("click", () => {
      document.querySelector(".gallery-stage").classList.toggle("is-zoomed", button.dataset.zoom === "true");
      document.querySelectorAll("[data-zoom]").forEach(item => item.setAttribute("aria-pressed", String(item === button)));
    });
  });
  const quantity = document.querySelector("#quantity");
  document.querySelectorAll("[data-step]").forEach(button => button.addEventListener("click", () => {
    quantity.value = Math.min(99, Math.max(1, (Number(quantity.value) || 1) + Number(button.dataset.step)));
  }));
})();
