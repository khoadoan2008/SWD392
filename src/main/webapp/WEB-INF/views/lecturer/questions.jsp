<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%
    request.setAttribute("pageTitle", "Ngân hàng câu hỏi");
    request.setAttribute("pageSection", "Quản lý ngân hàng câu hỏi & rubric");
    request.setAttribute("activeNav", "questions");
%>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="stats">
    <a class="stat" href="${ctx}/lecturer/questions" style="text-decoration:none;color:inherit">
        <span class="stat-icon"><svg class="icon"><use href="${ico}#i-library"/></svg></span>
        <div><div class="label">Tổng câu hỏi</div><div class="value">${stats.TOTAL}</div></div></a>
    <a class="stat success" href="${ctx}/lecturer/questions?status=APPROVED" style="text-decoration:none;color:inherit">
        <span class="stat-icon"><svg class="icon"><use href="${ico}#i-check-circle"/></svg></span>
        <div><div class="label">Đã duyệt (ngân hàng chính thức)</div><div class="value">${stats.APPROVED}</div></div></a>
    <a class="stat warning" href="${ctx}/lecturer/questions?status=PENDING_REVIEW" style="text-decoration:none;color:inherit">
        <span class="stat-icon"><svg class="icon"><use href="${ico}#i-sparkles"/></svg></span>
        <div><div class="label">Chờ duyệt</div><div class="value">${stats.PENDING_REVIEW}</div></div></a>
    <a class="stat info" href="${ctx}/lecturer/questions?status=DRAFT" style="text-decoration:none;color:inherit">
        <span class="stat-icon"><svg class="icon"><use href="${ico}#i-file"/></svg></span>
        <div><div class="label">Nháp</div><div class="value">${stats.DRAFT}</div></div></a>
</div>

<c:if test="${stats.PENDING_REVIEW > 0 && param.status != 'PENDING_REVIEW'}">
    <div class="alert warning">
        <svg class="icon"><use href="${ico}#i-sparkles"/></svg>
        <span>Có <b>${stats.PENDING_REVIEW}</b> câu hỏi do AI sinh đang chờ bạn duyệt.
            <a href="${ctx}/lecturer/questions?status=PENDING_REVIEW">Xem ngay</a></span>
    </div>
</c:if>

<div class="card flush">
    <div class="card-header">
        <div><h2>Danh sách câu hỏi</h2><p class="muted small">${fn:length(questions)} câu hỏi phù hợp bộ lọc</p></div>
        <div class="row">
            <a class="btn" href="${ctx}/lecturer/questions?action=generate"><svg class="icon"><use href="${ico}#i-sparkles"/></svg>AI sinh câu hỏi</a>
            <a class="btn primary" href="${ctx}/lecturer/questions?action=create"><svg class="icon"><use href="${ico}#i-plus"/></svg>Thêm câu hỏi</a>
        </div>
    </div>
    <form class="filters" method="get">
        <div class="search">
            <svg class="icon"><use href="${ico}#i-search"/></svg>
            <input type="text" name="q" value="${fn:escapeXml(param.q)}" placeholder="Tìm theo nội dung hoặc chủ đề...">
        </div>
        <select name="subjectId" onchange="this.form.submit()">
            <option value="">Tất cả môn</option>
            <c:forEach items="${subjects}" var="s">
                <option value="${s.id}" ${param.subjectId == s.id ? 'selected' : ''}><c:out value="${s.code}"/></option>
            </c:forEach>
        </select>
        <select name="status" onchange="this.form.submit()">
            <option value="">Mọi trạng thái</option>
            <c:forEach items="${statuses}" var="st">
                <option value="${st}" ${param.status == st ? 'selected' : ''}>${st.label}</option>
            </c:forEach>
        </select>
        <select name="bloom" onchange="this.form.submit()">
            <option value="">Mọi mức Bloom</option>
            <c:forEach items="${blooms}" var="b">
                <option value="${b}" ${param.bloom == b ? 'selected' : ''}>${b.label}</option>
            </c:forEach>
        </select>
        <button class="btn" type="submit">Lọc</button>
    </form>
    <div class="table-wrap">
        <table>
            <thead><tr><th style="width:45%">Câu hỏi</th><th>Bloom</th><th>Nguồn</th><th>Trạng thái</th><th>Cập nhật</th><th></th></tr></thead>
            <tbody>
            <c:forEach items="${questions}" var="q">
                <tr>
                    <td>
                        <a class="cell-main clamp-2" style="color:inherit" href="${ctx}/lecturer/questions?action=edit&id=${q.id}"><c:out value="${q.content}"/></a>
                        <div class="cell-sub"><c:out value="${q.subjectCode}"/><c:if test="${not empty q.topic}"> · <c:out value="${q.topic}"/></c:if></div>
                    </td>
                    <td><span class="bloom ${q.bloomLevel}">${q.bloomLevel.label}</span></td>
                    <td><span class="badge plain ${q.source == 'AI' ? 'AI' : ''}">${q.source.label}</span></td>
                    <td><span class="badge ${q.status}">${q.status.label}</span></td>
                    <td class="nowrap muted small"><fmt:formatDate value="${q.updatedAt}" pattern="dd/MM/yyyy"/></td>
                    <td class="actions">
                        <c:if test="${q.status == 'PENDING_REVIEW'}">
                            <form method="post" action="${ctx}/lecturer/questions">
                                <input type="hidden" name="action" value="status"><input type="hidden" name="id" value="${q.id}">
                                <input type="hidden" name="to" value="REJECTED"><input type="hidden" name="back" value="list">
                                <button class="btn sm ghost icon-btn danger" title="Loại bỏ" type="submit"><svg class="icon"><use href="${ico}#i-x"/></svg></button>
                            </form>
                        </c:if>
                        <a class="btn sm ghost icon-btn" title="Chỉnh sửa / duyệt" href="${ctx}/lecturer/questions?action=edit&id=${q.id}"><svg class="icon"><use href="${ico}#i-edit"/></svg></a>
                        <form method="post" action="${ctx}/lecturer/questions" onsubmit="return confirm('Xoá câu hỏi này cùng rubric của nó?')">
                            <input type="hidden" name="action" value="delete"><input type="hidden" name="id" value="${q.id}">
                            <button class="btn sm ghost icon-btn danger" title="Xoá" type="submit"><svg class="icon"><use href="${ico}#i-trash"/></svg></button>
                        </form>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty questions}">
                <tr><td colspan="6"><div class="empty">
                    <svg class="icon"><use href="${ico}#i-library"/></svg>
                    <div>Chưa có câu hỏi phù hợp.</div>
                    <div class="row" style="justify-content:center;margin-top:12px">
                        <a class="btn sm" href="${ctx}/lecturer/questions?action=generate">Thử để AI sinh câu hỏi</a>
                        <a class="btn sm primary" href="${ctx}/lecturer/questions?action=create">Thêm câu hỏi</a>
                    </div>
                </div></td></tr>
            </c:if>
            </tbody>
        </table>
    </div>
</div>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
