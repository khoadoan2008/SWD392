<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%
    request.setAttribute("pageTitle", "Lượt phỏng vấn AI");
    request.setAttribute("pageSection", "Lõi phỏng vấn AI");
    request.setAttribute("activeNav", "sessions");
%>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="stats">
    <div class="stat"><span class="stat-icon"><svg class="icon"><use href="${ico}#i-message"/></svg></span>
        <div><div class="label">Tổng lượt thi</div><div class="value">${stats.TOTAL}</div></div></div>
    <div class="stat success"><span class="stat-icon"><svg class="icon"><use href="${ico}#i-check-circle"/></svg></span>
        <div><div class="label">Hoàn thành</div><div class="value">${empty stats.COMPLETED ? 0 : stats.COMPLETED}</div></div></div>
    <div class="stat warning"><span class="stat-icon"><svg class="icon"><use href="${ico}#i-mic"/></svg></span>
        <div><div class="label">Đang diễn ra</div><div class="value">${empty stats.IN_PROGRESS ? 0 : stats.IN_PROGRESS}</div></div></div>
    <div class="stat danger"><span class="stat-icon"><svg class="icon"><use href="${ico}#i-x"/></svg></span>
        <div><div class="label">Kết thúc sớm</div><div class="value">${empty stats.ABORTED ? 0 : stats.ABORTED}</div></div></div>
</div>

<div class="card flush">
    <div class="card-header">
        <div><h2>Danh sách lượt phỏng vấn</h2><p class="muted small">Xem transcript đầy đủ để đối chiếu và chấm điểm.</p></div>
    </div>
    <form class="filters" method="get">
        <select name="subjectId" onchange="this.form.submit()">
            <option value="">Tất cả môn</option>
            <c:forEach items="${subjects}" var="s">
                <option value="${s.id}" ${param.subjectId == s.id ? 'selected' : ''}><c:out value="${s.code}"/></option>
            </c:forEach>
        </select>
    </form>
    <div class="table-wrap">
        <table>
            <thead><tr><th>Sinh viên</th><th>Môn</th><th>Bắt đầu</th><th>Kết thúc</th><th>Trạng thái</th><th></th></tr></thead>
            <tbody>
            <c:forEach items="${sessions}" var="x">
                <tr>
                    <td><div class="cell-main"><c:out value="${x.studentName}"/></div><div class="cell-sub"><c:out value="${x.studentCode}"/> · Lượt #${x.id}</div></td>
                    <td><span class="badge plain primary"><c:out value="${x.subjectCode}"/></span></td>
                    <td class="nowrap"><fmt:formatDate value="${x.startedAt}" pattern="HH:mm dd/MM/yyyy"/></td>
                    <td class="nowrap"><c:choose><c:when test="${empty x.endedAt}"><span class="muted">—</span></c:when>
                        <c:otherwise><fmt:formatDate value="${x.endedAt}" pattern="HH:mm dd/MM/yyyy"/></c:otherwise></c:choose></td>
                    <td><span class="badge ${x.status}">${x.status == 'COMPLETED' ? 'Hoàn thành' : (x.status == 'IN_PROGRESS' ? 'Đang thi' : 'Kết thúc sớm')}</span></td>
                    <td class="actions">
                        <a class="btn sm" href="${ctx}/lecturer/sessions?action=view&id=${x.id}"><svg class="icon"><use href="${ico}#i-eye"/></svg>Transcript</a>
                        <form method="post" action="${ctx}/lecturer/sessions" onsubmit="return confirm('Xoá lượt phỏng vấn này và toàn bộ transcript?')">
                            <input type="hidden" name="action" value="delete"><input type="hidden" name="id" value="${x.id}">
                            <button class="btn sm ghost icon-btn danger" title="Xoá" type="submit"><svg class="icon"><use href="${ico}#i-trash"/></svg></button>
                        </form>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty sessions}">
                <tr><td colspan="6"><div class="empty"><svg class="icon"><use href="${ico}#i-message"/></svg><div>Chưa có lượt phỏng vấn nào.</div></div></td></tr>
            </c:if>
            </tbody>
        </table>
    </div>
</div>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
