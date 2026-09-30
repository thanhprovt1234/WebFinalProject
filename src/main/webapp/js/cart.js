(() => {
  "use strict";
  const dialog = document.querySelector("#cart-dialog");
  if (!dialog) return;
  const form = document.querySelector("#product-options");
  const root = document.querySelector("[data-product-id]");
  const list = document.querySelector("#cart-items");
  const login = document.querySelector("#cart-login");
  const retry = document.querySelector("#cart-retry");
  const csrfToken = document.querySelector('meta[name="csrf-token"]').content;
  const csrfHeader = document.querySelector('meta[name="csrf-header"]').content;
  let authenticated = dialog.dataset.authenticated === "true";
  let busy = false;
  let cart = { items: [], count: 0, total: 0 };
  const money = value => new Intl.NumberFormat("vi-VN").format(value) + " VNĐ";
  const status = message => {
    document.querySelectorAll("#cart-status, #shared-cart-status").forEach(node => { node.textContent = message; });
  };
  const setBusy = value => {
    busy = value;
    dialog.setAttribute("aria-busy", String(value));
    document.querySelectorAll('#cart-items button, #cart-items input, #product-options button[type="submit"], #cart-retry')
      .forEach(node => { node.disabled = value || node.dataset.unavailable === "true"; });
  };
  const render = () => {
    document.querySelectorAll("[data-cart-count], #cart-count").forEach(node => { node.textContent = cart.count; });
    document.querySelector(".nav-cart")?.setAttribute("aria-label", `Giỏ hàng, ${cart.count} sản phẩm`);
    login.hidden = authenticated;
    list.replaceChildren();
    if (authenticated && !cart.items.length) list.textContent = "Giỏ hàng đang trống.";
    cart.items.forEach(item => {
      const row = document.createElement("div"); row.className = "cart-item";
      if (item.image) {
        const image = document.createElement("img");
        image.src = dialog.dataset.imageBase + item.image; image.alt = item.name;
        row.append(image);
      }
      const info = document.createElement("div"); info.className = "cart-item-info";
      const name = document.createElement("p"); name.textContent = item.name;
      const details = document.createElement("p");
      details.textContent = `Size ${item.size} · ${item.available ? money(item.price) + " / sản phẩm" : "Không còn kinh doanh"}`;
      const subtotal = document.createElement("p"); subtotal.textContent = "Thành tiền: " + money(item.subtotal);
      const controls = document.createElement("div"); controls.className = "cart-item-controls";
      const label = document.createElement("label"); label.textContent = "Số lượng ";
      const input = document.createElement("input");
      input.type = "number"; input.min = "1"; input.max = "99"; input.step = "1"; input.required = true;
      input.value = item.quantity; input.dataset.unavailable = String(!item.available);
      input.setAttribute("aria-label", `Số lượng ${item.name}, size ${item.size}`);
      input.addEventListener("change", () => {
        if (!input.reportValidity()) { input.value = item.quantity; return; }
        mutate(`/items/${item.id}`, "PATCH", { quantity: Number(input.value) }, "Đã cập nhật số lượng.");
      });
      label.append(input);
      const remove = document.createElement("button"); remove.type = "button"; remove.textContent = "Xóa";
      remove.setAttribute("aria-label", `Xóa ${item.name}, size ${item.size}`);
      remove.addEventListener("click", () => mutate(`/items/${item.id}`, "DELETE", null, "Đã xóa sản phẩm khỏi giỏ hàng."));
      controls.append(label, remove); info.append(name, details, subtotal, controls); row.append(info); list.append(row);
    });
    document.querySelector("#cart-total").textContent = money(cart.total);
    setBusy(busy);
  };
  const request = async (path = "", method = "GET", data = null) => {
    const headers = { Accept: "application/json" };
    if (method !== "GET") headers[csrfHeader] = csrfToken;
    const response = await fetch(dialog.dataset.api + path, {
      method, headers, credentials: "same-origin", cache: "no-store",
      body: data ? new URLSearchParams(data) : null,
      signal: AbortSignal.timeout(20000)
    });
    if (response.status === 401 || response.redirected) {
      authenticated = false;
      cart = { items: [], count: 0, total: 0 };
      throw new Error("Vui lòng đăng nhập để sử dụng giỏ hàng.");
    }
    if (response.status === 403) throw new Error("Phiên làm việc đã thay đổi. Vui lòng tải lại trang rồi thử lại.");
    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      throw new Error(error.message || "Chưa thể cập nhật giỏ hàng. Vui lòng tải lại giỏ và thử lại.");
    }
    return response.json();
  };
  const mutate = async (path, method, data, message) => {
    if (busy) return;
    setBusy(true); status("Đang cập nhật giỏ hàng…"); retry.hidden = true;
    try { cart = await request(path, method, data); status(message); }
    catch (error) {
      status(error instanceof TypeError || error.name === "TimeoutError"
        ? "Chưa xác nhận được thay đổi. Hãy tải lại giỏ hàng để kiểm tra." : error.message);
      retry.hidden = !authenticated;
    } finally { setBusy(false); render(); }
  };
  const load = async () => {
    if (busy || !authenticated) return;
    setBusy(true); status("Đang tải giỏ hàng…"); retry.hidden = true;
    try { cart = await request(); status(""); }
    catch (error) { status("Không tải được giỏ hàng. " + error.message); retry.hidden = !authenticated; }
    finally { setBusy(false); render(); }
  };
  form?.addEventListener("submit", event => {
    event.preventDefault();
    if (!authenticated) { render(); status("Vui lòng đăng nhập để thêm sản phẩm."); dialog.showModal(); return; }
    if (!form.reportValidity()) return;
    const values = new FormData(form);
    mutate("/items", "POST", { productId: root.dataset.productId, size: values.get("size"), quantity: values.get("quantity") },
      "Đã thêm sản phẩm vào giỏ hàng.");
  });
  document.querySelectorAll("[data-open-cart], #open-cart").forEach(button => button.addEventListener("click", () => {
    render(); dialog.showModal(); load();
  }));
  document.querySelector("#close-cart").addEventListener("click", () => dialog.close());
  retry.addEventListener("click", load);
  window.addEventListener("pageshow", event => { if (event.persisted) load(); });
  render(); load();
})();
