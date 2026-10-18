<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Quản lý danh mục" />
<%@ include file="start.jspf" %>
<c:forEach var="c" items="${categories}"><form class="admin-panel admin-inline-form" method="post" action="${pageContext.request.contextPath}/admin/categories/${c.id}"><%@ include file="csrf.jspf" %><label>Tên danh mục<input name="name" maxlength="100" value="<c:out value='${c.name}' />" required></label><label class="admin-check"><input name="enabled" type="checkbox" value="true" ${c.enabled ? 'checked' : ''}> Hiển thị danh mục</label><button type="submit">Lưu</button></form></c:forEach>
<form class="admin-panel admin-inline-form" method="post" action="${pageContext.request.contextPath}/admin/categories"><%@ include file="csrf.jspf" %><h2>Thêm danh mục</h2><label>Tên danh mục<input name="name" maxlength="100" required></label><input name="enabled" type="hidden" value="true"><button class="admin-button" type="submit">Thêm danh mục</button></form>
<%@ include file="end.jspf" %>
