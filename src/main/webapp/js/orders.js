(() => {
  "use strict";
  const root = document.querySelector(".orders-shell");
  if (!root || typeof HTMLDialogElement === "undefined") return;
  document.querySelectorAll("[data-order-dialog]").forEach(link => {
    link.addEventListener("click", event => {
      const dialog = document.getElementById(link.dataset.orderDialog);
      if (!dialog) return;
      event.preventDefault();
      dialog.showModal();
    });
  });
  document.querySelectorAll(".order-dialog").forEach(dialog => {
    dialog.querySelector("[data-close-order]").addEventListener("click", () => dialog.close());
    dialog.addEventListener("click", event => {
      const bounds = dialog.getBoundingClientRect();
      if (event.target === dialog && (event.clientX < bounds.left || event.clientX > bounds.right || event.clientY < bounds.top || event.clientY > bounds.bottom)) dialog.close();
    });
  });
  const placed = root.dataset.placedOrder;
  if (placed) document.getElementById(`order-dialog-${placed}`)?.showModal();
})();
