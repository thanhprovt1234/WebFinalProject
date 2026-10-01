<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Khu vực làm việc" />
<c:set var="bodyClass" value="profile-page" />
<%@ include file="../fragments/header.jspf" %>
<main class="profile-shell">
<nav class="activity-nav"><a href="${pageContext.request.contextPath}/workspace">Khu vực làm việc</a><a href="${pageContext.request.contextPath}/profile">Tài khoản</a></nav>
<a href="${pageContext.request.contextPath}/admin/users">← Danh sách tài khoản</a><h1>Phân quyền: <c:out value="${user.username}"/></h1>
<c:if test="${not empty success}"><p class="alert alert-success" role="status"><c:out value="${success}"/></p></c:if><c:if test="${not empty error}"><p class="alert alert-danger" role="alert"><c:out value="${error}"/></p></c:if>
<div class="profile-layout"><section class="profile-card"><h2>Vai trò</h2><p>User: khách hàng · Vendor: chủ shop · Manager: quản lý cửa hàng · Admin: quản trị · Shipper: giao hàng.</p>
<form method="post" action="${pageContext.request.contextPath}/admin/users/${user.id}/roles">
<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
<c:forEach var="role" items="${roleOptions}"><label class="d-block mb-3"><input type="checkbox" name="roles" value="${role}" ${user.roles.contains(role.name()) ? 'checked' : ''}> <c:out value="${role}"/></label></c:forEach>
<p>Quyền khách hàng luôn được giữ. Seller cần có shop được liên kết với tài khoản.</p><button class="btn btn-dark">Lưu vai trò</button></form></section>
<section class="profile-card"><h2>Phân công Manager</h2><c:forEach var="s" items="${assignments}"><form class="mb-3" method="post" action="${pageContext.request.contextPath}/admin/users/${user.id}/stores"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"><input type="hidden" name="storeId" value="${s.id}"><input type="hidden" name="assigned" value="false"><p><c:out value="${s.shop}"/> / <c:out value="${s.name}"/></p><button class="btn btn-outline-danger btn-sm">Gỡ phân công</button></form></c:forEach>
<c:if test="${empty assignments}"><p>Chưa được phân công chi nhánh.</p></c:if>
<c:if test="${user.roles.contains('ROLE_MANAGER') && not empty stores}"><form method="post" action="${pageContext.request.contextPath}/admin/users/${user.id}/stores"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"><input type="hidden" name="assigned" value="true"><label for="storeId">Chọn chi nhánh</label><select class="form-select mb-3" name="storeId" id="storeId"><c:forEach var="s" items="${stores}"><option value="${s.id}"><c:out value="${s.shop}"/> / <c:out value="${s.name}"/></option></c:forEach></select><button class="btn btn-dark">Phân công</button></form></c:if>
</section></div>
</main>
<%@ include file="../fragments/footer.jspf" %>

