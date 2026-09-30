document.querySelectorAll('[data-delete-address]').forEach(form => {
  form.addEventListener('submit', event => {
    if (!window.confirm('Bạn muốn xóa địa chỉ nhận hàng này?')) event.preventDefault();
  });
});
