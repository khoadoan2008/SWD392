<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="ico" value="${ctx}/assets/icons.svg"/>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đăng nhập · AIVES</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${ctx}/assets/style.css">
</head>
<body>
<div class="auth">
    <section class="auth-side">
        <div class="row"><span class="avatar" style="background:linear-gradient(135deg,#7c3aed,#4f46e5);border-radius:10px">AI</span><b>AIVES</b></div>
        <div>
            <span class="badge" style="background:rgba(255,255,255,.12);color:#e0e7ff">AI Adaptive Oral Examination</span>
            <h1>Vấn đáp thông minh,<br>giảng viên luôn là người chốt điểm.</h1>
            <p>Hệ thống hỗ trợ tổ chức thi vấn đáp: AI đặt câu hỏi bằng giọng nói, hỏi xoáy khi câu trả lời chưa rõ,
               và lưu transcript minh bạch để giảng viên đánh giá.</p>
            <ul>
                <li><svg class="icon"><use href="${ico}#i-library"/></svg>Ngân hàng câu hỏi theo thang Bloom &amp; rubric</li>
                <li><svg class="icon"><use href="${ico}#i-mic"/></svg>Phỏng vấn bằng giọng nói, hỏi xoáy thích ứng</li>
                <li><svg class="icon"><use href="${ico}#i-shield"/></svg>Phân quyền rõ ràng, lưu vết đầy đủ</li>
            </ul>
        </div>
        <small style="color:#94a3b8">© AIVES · Đồ án môn học</small>
    </section>

    <section class="auth-form">
        <div class="box">
            <h2>Đăng nhập</h2>
            <p class="muted">Chào mừng bạn quay lại! Vui lòng nhập thông tin tài khoản.</p>

            <%-- Lời nhắc từ hệ thống: cần đăng nhập, không đủ quyền, đã đăng xuất... --%>
            <c:if test="${not empty sessionScope.flashMessage}">
                <div class="alert ${sessionScope.flashType}" style="margin-top:16px">
                    <svg class="icon"><use href="${ico}#${sessionScope.flashType == 'success' ? 'i-check-circle' : 'i-info'}"/></svg>
                    <span><c:out value="${sessionScope.flashMessage}"/></span>
                </div>
                <c:remove var="flashMessage" scope="session"/>
                <c:remove var="flashType" scope="session"/>
            </c:if>
            <c:if test="${not empty error}">
                <div class="alert error" style="margin-top:16px"><svg class="icon"><use href="${ico}#i-alert"/></svg><span><c:out value="${error}"/></span></div>
            </c:if>

            <c:if test="${not empty sessionScope.user}">
                <div class="alert info">
                    <svg class="icon"><use href="${ico}#i-info"/></svg>
                    <span>Bạn đang đăng nhập với tài khoản <strong><c:out value="${sessionScope.user.username}"/></strong>
                    (<c:out value="${sessionScope.user.role.label}"/>). <a href="${ctx}/home">Quay lại trang của tôi</a></span>
                </div>
            </c:if>

            <form method="post" action="${ctx}/login">
                <label for="username">Tên đăng nhập</label>
                <input type="text" id="username" name="username" value="${fn:escapeXml(username)}"
                       placeholder="VD: student1" autocomplete="username" required autofocus>
                <label for="password">Mật khẩu</label>
                <input type="password" id="password" name="password" placeholder="••••••••" autocomplete="current-password" required>
                <button class="btn primary lg block mt" type="submit">Đăng nhập</button>
            </form>
            <p class="muted small mt">Quên mật khẩu? Vui lòng liên hệ quản trị viên để được cấp lại.</p>
        </div>
    </section>
</div>
</body>
</html>
