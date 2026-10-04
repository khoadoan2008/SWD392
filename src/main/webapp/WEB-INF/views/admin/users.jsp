<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%
    request.setAttribute("pageTitle", "Tài khoản & phân quyền");
    request.setAttribute("pageSection", "Quản trị hệ thống");
    request.setAttribute("activeNav", "users");
%>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="stats">
    <div class="stat"><span class="stat-icon"><svg class="icon"><use href="${ico}#i-users"/></svg></span>
        <div><div class="label">Tổng tài khoản</div><div class="value">${stats.TOTAL}</div></div></div>
    <div class="stat info"><span class="stat-icon"><svg class="icon"><use href="${ico}#i-book"/></svg></span>
        <div><div class="label">Giảng viên</div><div class="value">${stats.LECTURER}</div></div></div>
    <div class="stat success"><span class="stat-icon"><svg class="icon"><use href="${ico}#i-mic"/></svg></span>
        <div><div class="label">Sinh viên</div><div class="value">${stats.STUDENT}</div></div></div>
    <div class="stat danger"><span class="stat-icon"><svg class="icon"><use href="${ico}#i-lock"/></svg></span>
        <div><div class="label">Đang tạm khoá</div><div class="value">${stats.LOCKED}</div></div></div>
</div>

<div class="split wide-side">
    <%-- ===== Danh sách ===== --%>
    <div class="card flush">
        <div class="card-header">
            <div><h2>Danh sách tài khoản</h2><p class="muted small">${fn:length(users)} kết quả</p></div>
            <a class="btn primary" href="${ctx}/admin/users?action=create"><svg class="icon"><use href="${ico}#i-plus"/></svg>Thêm tài khoản</a>
        </div>
        <form class="filters" method="get">
            <div class="search">
                <svg class="icon"><use href="${ico}#i-search"/></svg>
                <input type="text" name="q" value="${fn:escapeXml(param.q)}" placeholder="Tìm theo username, họ tên, MSSV...">
            </div>
            <select name="filterRole" onchange="this.form.submit()">
                <option value="">Tất cả vai trò</option>
                <c:forEach items="${roles}" var="r">
                    <option value="${r}" ${param.filterRole == r ? 'selected' : ''}>${r.label}</option>
                </c:forEach>
            </select>
            <button class="btn" type="submit">Lọc</button>
        </form>
        <div class="table-wrap">
            <table>
                <thead><tr><th>Người dùng</th><th>Vai trò</th><th>MSSV</th><th>Trạng thái</th><th></th></tr></thead>
                <tbody>
                <c:forEach items="${users}" var="x">
                    <tr class="${u.id == x.id && u.id != 0 ? 'selected' : ''}">
                        <td>
                            <div class="row" style="flex-wrap:nowrap">
                                <span class="avatar"><c:out value="${x.initials}"/></span>
                                <div><div class="cell-main"><c:out value="${x.fullName}"/></div>
                                    <div class="cell-sub">@<c:out value="${x.username}"/><c:if test="${not empty x.email}"> · <c:out value="${x.email}"/></c:if></div></div>
                            </div>
                        </td>
                        <td><span class="badge plain ${x.role}">${x.role.label}</span></td>
                        <td><c:out value="${empty x.studentCode ? '—' : x.studentCode}"/></td>
                        <td><span class="badge ${x.active ? 'active' : 'locked'}">${x.active ? 'Hoạt động' : 'Tạm khoá'}</span></td>
                        <td class="actions">
                            <a class="btn sm ghost icon-btn" title="Chỉnh sửa" href="${ctx}/admin/users?action=edit&id=${x.id}"><svg class="icon"><use href="${ico}#i-edit"/></svg></a>
                            <form method="post" action="${ctx}/admin/users"
                                  onsubmit="return confirm('Bạn chắc chắn muốn xoá tài khoản ${fn:escapeXml(x.username)}?')">
                                <input type="hidden" name="action" value="delete">
                                <input type="hidden" name="id" value="${x.id}">
                                <button class="btn sm ghost icon-btn danger" title="Xoá" type="submit"><svg class="icon"><use href="${ico}#i-trash"/></svg></button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty users}">
                    <tr><td colspan="5"><div class="empty"><svg class="icon"><use href="${ico}#i-search"/></svg><div>Không tìm thấy tài khoản phù hợp.</div></div></td></tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </div>

    <%-- ===== Panel bên phải: thêm / sửa ===== --%>
    <aside class="sticky">
        <c:choose>
            <c:when test="${not empty u}">
                <c:set var="isNew" value="${u.id == 0}"/>
                <div class="card">
                    <div class="card-header">
                        <h2>${isNew ? 'Thêm tài khoản' : 'Chỉnh sửa tài khoản'}</h2>
                        <a class="btn sm ghost icon-btn" href="${ctx}/admin/users" title="Đóng"><svg class="icon"><use href="${ico}#i-x"/></svg></a>
                    </div>
                    <form method="post" action="${ctx}/admin/users">
                        <input type="hidden" name="action" value="save">
                        <input type="hidden" name="id" value="${u.id}">

                        <label class="mt-0" for="username">Tên đăng nhập</label>
                        <input type="text" id="username" name="username" value="${fn:escapeXml(u.username)}" ${isNew ? 'required' : 'disabled'}>
                        <label for="fullName">Họ tên</label>
                        <input type="text" id="fullName" name="fullName" value="${fn:escapeXml(u.fullName)}" required>
                        <label for="email">Email</label>
                        <input type="email" id="email" name="email" value="${fn:escapeXml(u.email)}" placeholder="ten@fpt.edu.vn">
                        <label for="password">${isNew ? 'Mật khẩu' : 'Đặt lại mật khẩu'}</label>
                        <input type="password" id="password" name="password" minlength="6" ${isNew ? 'required' : ''}
                               placeholder="${isNew ? 'Tối thiểu 6 ký tự' : 'Để trống nếu giữ nguyên'}">

                        <span class="label">Vai trò</span>
                        <div class="chips">
                            <c:forEach items="${roles}" var="r">
                                <label class="chip" style="margin:0"><input type="radio" name="role" value="${r}" ${u.role == r ? 'checked' : ''}
                                       onchange="toggleRoleFields()"><span>${r.label}</span></label>
                            </c:forEach>
                        </div>

                        <div id="studentCodeBox">
                            <label for="studentCode">MSSV</label>
                            <input type="text" id="studentCode" name="studentCode" value="${fn:escapeXml(u.studentCode)}" placeholder="VD: SE170001">
                        </div>
                        <div id="subjectsBox">
                            <span class="label">Môn học được phân công</span>
                            <div class="chips">
                                <c:forEach items="${subjects}" var="s">
                                    <label class="chip" style="margin:0"><input type="checkbox" name="subjectIds" value="${s.id}"
                                        ${assignedSubjectIds.contains(s.id) ? 'checked' : ''}><span><c:out value="${s.code}"/></span></label>
                                </c:forEach>
                            </div>
                            <div class="hint">Giảng viên chỉ quản lý câu hỏi và xem lượt thi của các môn được chọn.</div>
                        </div>

                        <div class="divider"></div>
                        <label class="switch"><input type="checkbox" name="active" ${u.active ? 'checked' : ''}> Cho phép đăng nhập</label>

                        <div class="form-actions">
                            <a class="btn" href="${ctx}/admin/users">Huỷ</a>
                            <button class="btn primary" type="submit"><svg class="icon"><use href="${ico}#i-check"/></svg>Lưu</button>
                        </div>
                    </form>
                </div>
                <script>
                    function toggleRoleFields() {
                        var checked = document.querySelector('input[name=role]:checked');
                        var role = checked ? checked.value : '';
                        document.getElementById('studentCodeBox').style.display = role === 'STUDENT' ? '' : 'none';
                        document.getElementById('subjectsBox').style.display = role === 'LECTURER' ? '' : 'none';
                    }
                    toggleRoleFields();
                </script>
            </c:when>
            <c:otherwise>
                <div class="card">
                    <h2>Phân quyền theo vai trò</h2>
                    <p class="muted small">Chọn một tài khoản trong danh sách để chỉnh sửa.</p>
                    <div class="divider"></div>
                    <div class="stack">
                        <div><span class="badge plain ADMIN">Quản trị viên</span>
                            <p class="small muted mt-0" style="margin-top:6px">Quản lý tài khoản, môn học, cấu hình STT/TTS và toàn bộ ngân hàng câu hỏi.</p></div>
                        <div><span class="badge plain LECTURER">Giảng viên</span>
                            <p class="small muted" style="margin-top:6px">Soạn &amp; duyệt câu hỏi, rubric; xem transcript lượt thi của môn được phân công.</p></div>
                        <div><span class="badge plain STUDENT">Sinh viên</span>
                            <p class="small muted" style="margin-top:6px">Tham gia phỏng vấn AI và xem lại lượt thi của chính mình.</p></div>
                    </div>
                </div>
            </c:otherwise>
        </c:choose>
    </aside>
</div>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
