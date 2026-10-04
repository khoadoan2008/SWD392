<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%
    request.setAttribute("pageTitle", "Môn học");
    request.setAttribute("pageSection", "Quản trị hệ thống");
    request.setAttribute("activeNav", "subjects");
%>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="split wide-side">
    <div class="card flush">
        <div class="card-header">
            <div><h2>Danh sách môn học</h2><p class="muted small">${fn:length(subjects)} môn</p></div>
            <a class="btn primary" href="${ctx}/admin/subjects?action=create"><svg class="icon"><use href="${ico}#i-plus"/></svg>Thêm môn học</a>
        </div>
        <div class="table-wrap" style="margin-top:16px">
            <table>
                <thead><tr><th>Mã môn</th><th>Tên môn</th><th>Trạng thái</th><th></th></tr></thead>
                <tbody>
                <c:forEach items="${subjects}" var="x">
                    <tr class="${not empty s && s.id == x.id && s.id != 0 ? 'selected' : ''}">
                        <td><span class="badge plain primary"><c:out value="${x.code}"/></span></td>
                        <td><div class="cell-main"><c:out value="${x.name}"/></div>
                            <div class="cell-sub clamp-2"><c:out value="${x.description}"/></div></td>
                        <td><span class="badge ${x.active ? 'active' : 'locked'}">${x.active ? 'Đang mở' : 'Ngừng'}</span></td>
                        <td class="actions">
                            <a class="btn sm ghost icon-btn" title="Chỉnh sửa" href="${ctx}/admin/subjects?action=edit&id=${x.id}"><svg class="icon"><use href="${ico}#i-edit"/></svg></a>
                            <form method="post" action="${ctx}/admin/subjects" onsubmit="return confirm('Xoá môn học này?')">
                                <input type="hidden" name="action" value="delete">
                                <input type="hidden" name="id" value="${x.id}">
                                <button class="btn sm ghost icon-btn danger" title="Xoá" type="submit"><svg class="icon"><use href="${ico}#i-trash"/></svg></button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty subjects}">
                    <tr><td colspan="4"><div class="empty"><svg class="icon"><use href="${ico}#i-book"/></svg><div>Chưa có môn học nào.</div></div></td></tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </div>

    <aside class="sticky">
        <c:choose>
            <c:when test="${not empty s}">
                <div class="card">
                    <div class="card-header">
                        <h2>${s.id == 0 ? 'Thêm môn học' : 'Chỉnh sửa môn học'}</h2>
                        <a class="btn sm ghost icon-btn" href="${ctx}/admin/subjects" title="Đóng"><svg class="icon"><use href="${ico}#i-x"/></svg></a>
                    </div>
                    <form method="post" action="${ctx}/admin/subjects">
                        <input type="hidden" name="action" value="save">
                        <input type="hidden" name="id" value="${s.id}">
                        <label class="mt-0" for="code">Mã môn</label>
                        <input type="text" id="code" name="code" value="${fn:escapeXml(s.code)}" maxlength="20" placeholder="VD: PRJ301" required>
                        <label for="name">Tên môn</label>
                        <input type="text" id="name" name="name" value="${fn:escapeXml(s.name)}" required>
                        <label for="description">Mô tả</label>
                        <textarea id="description" name="description" placeholder="Nội dung chính, giáo trình tham khảo..."><c:out value="${s.description}"/></textarea>
                        <div class="divider"></div>
                        <label class="switch"><input type="checkbox" name="active" ${s.active ? 'checked' : ''}> Đang mở cho thi vấn đáp</label>
                        <div class="form-actions">
                            <a class="btn" href="${ctx}/admin/subjects">Huỷ</a>
                            <button class="btn primary" type="submit"><svg class="icon"><use href="${ico}#i-check"/></svg>Lưu</button>
                        </div>
                    </form>
                </div>
            </c:when>
            <c:otherwise>
                <div class="card">
                    <h2>Gợi ý</h2>
                    <p class="muted small mt">Môn đã có câu hỏi hoặc lượt thi không thể xoá để giữ toàn vẹn dữ liệu —
                        hãy tắt <b>Đang mở</b> để ngừng sử dụng.</p>
                    <p class="muted small">Phân công giảng viên cho môn ở trang <a href="${ctx}/admin/users?filterRole=LECTURER">Tài khoản</a>.</p>
                </div>
            </c:otherwise>
        </c:choose>
    </aside>
</div>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
