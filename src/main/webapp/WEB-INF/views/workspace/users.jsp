<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Khu vực làm việc" />
<c:set var="bodyClass" value="profile-page" />
<%@ include file="../fragments/header.jspf" %>
<main class="profile-shell">
<nav class="activity-nav"><a href="${pageContext.request.contextPath}/workspace">Khu vực làm việc</a><a href="${pageContext.request.contextPath}/profile">Tài khoản</a></nav>
<h1>Quản lý phân quyền</h1><form method="get" class="d-flex gap-2 mb-4"><label class="visually-hidden" for="q">Tìm tài khoản</label><input id="q" class="form-control" name="q" value="<c:out value='${query}'/>" placeholder="Tên đăng nhập hoặc họ tên"><button class="btn btn-dark">Tìm kiếm</button></form>
<div class="table-responsive"><table class="table"><thead><tr><th>Tài khoản</th><th>Họ tên</th><th>Vai trò</th><th>Trạng thái</th><th></th></tr></thead><tbody><c:forEach var="u" items="${usersPage.content}"><tr><td><c:out value="${u.username}"/></td><td><c:out value="${u.fullName}"/></td><td><c:out value="${u.roles}"/></td><td>${u.enabled ? 'Hoạt động' : 'Đã khóa'}</td><td><a href="${pageContext.request.contextPath}/admin/users/${u.id}">Phân quyền</a></td></tr></c:forEach></tbody></table></div>
<c:if test="${empty usersPage.content}"><p>Không tìm thấy tài khoản.</p></c:if>
<p>Trang ${usersPage.number + 1} / ${usersPage.totalPages == 0 ? 1 : usersPage.totalPages}</p>
<c:if test="${not usersPage.first}"><c:url var="prev" value="/admin/users"><c:param name="q" value="${query}"/><c:param name="page" value="${usersPage.number-1}"/></c:url><a href="<c:out value='${prev}'/>">← Trang trước</a></c:if>
<c:if test="${not usersPage.last}"><c:url var="next" value="/admin/users"><c:param name="q" value="${query}"/><c:param name="page" value="${usersPage.number+1}"/></c:url><a class="ms-3" href="<c:out value='${next}'/>">Trang sau →</a></c:if>
</main>
<%@ include file="../fragments/footer.jspf" %>
