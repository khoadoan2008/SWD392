<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="code" value="${requestScope['javax.servlet.error.status_code']}"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Thông báo · AIVES</title>
    <link rel="stylesheet" href="${ctx}/assets/style.css">
</head>
<body>
<div class="auth-form" style="min-height:100vh">
    <div class="box card" style="text-align:center;max-width:440px">
        <div class="success-mark" style="background:var(--warning-soft);color:var(--warning)">
            <svg class="icon"><use href="${ctx}/assets/icons.svg#i-info"/></svg>
        </div>
        <c:choose>
            <c:when test="${code == 404}">
                <h2>Không tìm thấy nội dung</h2>
                <p class="muted mt">Trang hoặc dữ liệu bạn tìm có thể đã bị xoá hoặc không còn tồn tại.</p>
            </c:when>
            <c:otherwise>
                <h2>Hệ thống đang gặp sự cố</h2>
                <p class="muted mt">Rất tiếc vì sự bất tiện này. Bạn vui lòng thử lại sau ít phút
                    hoặc liên hệ quản trị viên nếu lỗi vẫn tiếp diễn.</p>
            </c:otherwise>
        </c:choose>
        <a class="btn primary mt" href="${ctx}/home">Về trang chủ</a>
    </div>
</div>
</body>
</html>
