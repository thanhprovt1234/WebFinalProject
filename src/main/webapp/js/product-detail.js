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
  document.querySelectorAll('[name="size"]').forEach(option => option.addEventListener('change', () => {
    const stock = Number(option.dataset.stock);
    document.querySelector('#variant-stock').textContent = stock > 0 ? `Còn ${stock} sản phẩm` : 'Hết hàng';
    document.querySelector('#variant-price').textContent = new Intl.NumberFormat('vi-VN').format(Number(option.dataset.price)) + ' VNĐ';
    quantity.max = Math.max(1, Math.min(99, stock));
    quantity.value = Math.min(Number(quantity.value), Number(quantity.max));
    const submit = document.querySelector('#product-options button[type="submit"]');
    submit.dataset.unavailable = String(stock === 0);
    submit.disabled = stock === 0;
  }));
  document.querySelectorAll("[data-step]").forEach(button => button.addEventListener("click", () => {
    quantity.value = Math.min(Number(quantity.max), Math.max(1, (Number(quantity.value) || 1) + Number(button.dataset.step)));
  }));
})();
