<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="rubricView" value="${param.view == 'rubric'}"/>
<c:set var="reviewMode" value="${param.status == 'PENDING_REVIEW' && !rubricView}"/>
<%
    if ("true".equals(pageContext.getAttribute("rubricView").toString())) {
        request.setAttribute("pageTitle", "Quản lý rubric");
        request.setAttribute("activeNav", "rubrics");
    } else if ("true".equals(pageContext.getAttribute("reviewMode").toString())) {
        request.setAttribute("pageTitle", "Duyệt câu hỏi AI");
        request.setAttribute("activeNav", "questions");
    } else {
        request.setAttribute("pageTitle", "Ngân hàng câu hỏi");
        request.setAttribute("activeNav", "questions");
    }
    request.setAttribute("pageSection", "Quản lý ngân hàng câu hỏi & rubric");
%>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<c:if test="${!rubricView}">
<div class="stats">
    <a class="stat" href="${ctx}/lecturer/questions" style="text-decoration:none;color:inherit">
        <span class="stat-icon"><svg class="icon"><use href="${ico}#i-library"/></svg></span>
        <div><div class="label">Tổng câu hỏi</div><div class="value">${stats.TOTAL}</div>
            <div class="stat-hint muted small">Toàn bộ ngân hàng</div></div></a>
    <a class="stat success" href="${ctx}/lecturer/questions?status=APPROVED" style="text-decoration:none;color:inherit">
        <span class="stat-icon"><svg class="icon"><use href="${ico}#i-check-circle"/></svg></span>
        <div><div class="label">Đã phê duyệt</div><div class="value">${stats.APPROVED}</div>
            <div class="stat-hint muted small">Ngân hàng chính thức</div></div></a>
    <a class="stat warning" href="${ctx}/lecturer/questions?status=PENDING_REVIEW" style="text-decoration:none;color:inherit">
        <span class="stat-icon"><svg class="icon"><use href="${ico}#i-sparkles"/></svg></span>
        <div><div class="label">Chờ duyệt</div><div class="value">${stats.PENDING_REVIEW}</div>
            <div class="stat-hint muted small">Câu hỏi do AI sinh</div></div></a>
    <a class="stat info" href="${ctx}/lecturer/questions?status=DRAFT" style="text-decoration:none;color:inherit">
        <span class="stat-icon"><svg class="icon"><use href="${ico}#i-file"/></svg></span>
        <div><div class="label">Bản nháp</div><div class="value">${stats.DRAFT}</div>
            <div class="stat-hint muted small">Cần bổ sung rubric</div></div></a>
</div>
</c:if>

<c:if test="${stats.PENDING_REVIEW > 0 && param.status != 'PENDING_REVIEW' && !rubricView}">
    <div class="alert warning">
        <svg class="icon"><use href="${ico}#i-sparkles"/></svg>
        <span>Có <b>${stats.PENDING_REVIEW}</b> câu hỏi do AI sinh đang chờ bạn duyệt.
            <a href="${ctx}/lecturer/questions?status=PENDING_REVIEW">Duyệt ngay</a></span>
    </div>
</c:if>

<c:choose>
<c:when test="${reviewMode && not empty questions}">
    <c:set var="currId" value="${empty param.curr ? questions[0].id : param.curr}"/>
    <div class="review-top">
        <div class="review-progress-wrap">
            <div class="row between small" style="margin-bottom:6px">
                <span><b>${fn:length(questions)}</b> câu chờ xử lý trong bộ lọc</span>
                <span class="muted">Chọn câu bên trái để xem trước</span>
            </div>
            <div class="progress" style="height:8px"><div style="width:35%"></div></div>
        </div>
    </div>
    <div class="review-layout">
        <aside class="card flush review-list">
            <div class="card-header" style="padding:14px 16px;margin:0;border-bottom:1px solid var(--border)"><h2>Danh sách câu</h2></div>
            <ul class="review-items">
                <c:forEach items="${questions}" var="qq" varStatus="st">
                    <li>
                        <a class="review-item ${qq.id == currId ? 'active' : ''} ${qq.status == 'APPROVED' ? 'done' : ''}"
                           href="${ctx}/lecturer/questions?status=PENDING_REVIEW&amp;curr=${qq.id}">
                            <span class="review-num">Câu ${st.index + 1}</span>
                            <span class="clamp-2 small"><c:out value="${qq.content}"/></span>
                            <span class="badge ${qq.status == 'APPROVED' ? 'APPROVED' : (qq.id == currId ? 'primary' : 'PENDING_REVIEW')}">
                                ${qq.status == 'APPROVED' ? 'Đã duyệt' : (qq.id == currId ? 'Đang xem' : 'Chờ duyệt')}</span>
                        </a>
                    </li>
                </c:forEach>
            </ul>
        </aside>
        <div class="review-detail">
            <c:forEach items="${questions}" var="qq">
                <c:if test="${qq.id == currId}">
                    <div class="card">
                        <div class="card-header">
                            <div><h2>Xem trước</h2><p class="muted small">NHCH-${qq.id} · <c:out value="${qq.topic}"/></p></div>
                            <span class="bloom ${qq.bloomLevel}">${qq.bloomLevel.label}</span>
                        </div>
                        <p class="cell-main" style="margin-bottom:12px"><c:out value="${qq.content}"/></p>
                        <p class="muted small">Nguồn: <span class="badge plain ${qq.source == 'AI' ? 'AI' : ''}">${qq.source.label}</span></p>
                        <div class="row" style="margin-top:16px">
                            <a class="btn primary" href="${ctx}/lecturer/questions?action=edit&amp;id=${qq.id}"><svg class="icon"><use href="${ico}#i-edit"/></svg>Mở biên tập &amp; duyệt</a>
                            <c:if test="${qq.status == 'PENDING_REVIEW'}">
                                <form method="post" action="${ctx}/lecturer/questions">
                                    <input type="hidden" name="action" value="status"><input type="hidden" name="id" value="${qq.id}">
                                    <input type="hidden" name="to" value="REJECTED"><input type="hidden" name="back" value="list">
                                    <button class="btn danger" type="submit">Loại bỏ</button>
                                </form>
                            </c:if>
                        </div>
                    </div>
                </c:if>
            </c:forEach>
        </div>
    </div>
</c:when>
<c:otherwise>
<c:if test="${rubricView}"><div class="split wide-side"></c:if>
<div class="card flush" style="margin-bottom:${rubricView ? '0' : '20px'}">
    <div class="card-header">
        <div>
            <h2><c:choose><c:when test="${rubricView}">Rubric theo câu hỏi</c:when><c:otherwise>Danh sách câu hỏi</c:otherwise></c:choose></h2>
            <p class="muted small">${fn:length(questions)} câu hỏi phù hợp bộ lọc</p>
        </div>
        <c:if test="${!rubricView}">
        <div class="row">
            <a class="btn" href="${ctx}/lecturer/questions?action=generate"><svg class="icon"><use href="${ico}#i-sparkles"/></svg>AI sinh câu hỏi</a>
            <a class="btn primary" href="${ctx}/lecturer/questions?action=generate"><svg class="icon"><use href="${ico}#i-plus"/></svg>Tạo câu hỏi</a>
        </div>
        </c:if>
    </div>
    <form class="filters" method="get" id="filterForm">
        <c:if test="${rubricView}"><input type="hidden" name="view" value="rubric"></c:if>
        <div class="search">
            <svg class="icon"><use href="${ico}#i-search"/></svg>
            <input type="text" name="q" value="${fn:escapeXml(param.q)}" placeholder="Tìm theo nội dung, mã câu hỏi...">
        </div>
        <select name="subjectId" onchange="this.form.submit()">
            <option value="">Tất cả chủ đề / môn</option>
            <c:forEach items="${subjects}" var="s">
                <option value="${s.id}" ${param.subjectId == s.id ? 'selected' : ''}><c:out value="${s.code}"/></option>
            </c:forEach>
        </select>
        <select name="bloom" onchange="this.form.submit()">
            <option value="">Mức Bloom</option>
            <c:forEach items="${blooms}" var="b">
                <option value="${b}" ${param.bloom == b ? 'selected' : ''}>${b.label}</option>
            </c:forEach>
        </select>
        <select name="status" onchange="this.form.submit()">
            <option value="">Trạng thái</option>
            <c:forEach items="${statuses}" var="st">
                <option value="${st}" ${param.status == st ? 'selected' : ''}>${st.label}</option>
            </c:forEach>
        </select>
        <button class="btn" type="submit">Lọc</button>
    </form>
    <div class="table-wrap">
        <table>
            <thead><tr>
                <th style="width:40%">Câu hỏi</th>
                <th>Mức Bloom</th>
                <c:if test="${rubricView}"><th>Rubric</th></c:if>
                <th>Nguồn</th>
                <th>Trạng thái</th>
                <th>Cập nhật</th>
                <th></th>
            </tr></thead>
            <tbody>
            <c:forEach items="${questions}" var="q">
                <tr>
                    <td>
                        <a class="cell-main clamp-2" style="color:inherit" href="${ctx}/lecturer/questions?action=edit&amp;id=${q.id}${rubricView ? '#rubrics' : ''}"><c:out value="${q.content}"/></a>
                        <div class="cell-sub">NHCH-${q.id} · <c:out value="${q.subjectCode}"/><c:if test="${not empty q.topic}"> · <c:out value="${q.topic}"/></c:if></div>
                    </td>
                    <td><span class="bloom ${q.bloomLevel}">${q.bloomLevel.label}</span></td>
                    <c:if test="${rubricView}">
                        <td><a class="small" href="${ctx}/lecturer/questions?action=edit&amp;id=${q.id}#rubrics">RB-${q.id}</a></td>
                    </c:if>
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
                        <a class="btn sm ghost icon-btn" title="Chỉnh sửa" href="${ctx}/lecturer/questions?action=edit&amp;id=${q.id}${rubricView ? '#rubrics' : ''}"><svg class="icon"><use href="${ico}#i-edit"/></svg></a>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty questions}">
                <tr><td colspan="${rubricView ? 7 : 6}"><div class="empty">
                    <svg class="icon"><use href="${ico}#i-library"/></svg>
                    <div>Chưa có câu hỏi phù hợp.</div>
                    <div class="row" style="justify-content:center;margin-top:12px">
                        <a class="btn sm" href="${ctx}/lecturer/questions?action=generate">AI sinh câu hỏi</a>
                        <a class="btn sm primary" href="${ctx}/lecturer/questions?action=create">Thêm câu hỏi</a>
                    </div>
                </div></td></tr>
            </c:if>
            </tbody>
        </table>
    </div>
    <c:if test="${not empty questions}">
    <div class="table-footer">
        <span class="muted small">Hiển thị 1–${fn:length(questions)} trong ${stats.TOTAL} câu hỏi</span>
        <div class="row">
            <button class="btn sm" type="button" disabled>Trước</button>
            <span class="small muted">1 / 1</span>
            <button class="btn sm" type="button" disabled>Sau</button>
        </div>
    </div>
    </c:if>
</div>

<c:if test="${rubricView}">
<aside class="sticky">
    <div class="card ai-panel">
        <div class="row" style="align-items:flex-start">
            <span class="avatar" style="border-radius:10px;width:40px;height:40px"><svg class="icon"><use href="${ico}#i-bot"/></svg></span>
            <div>
                <h2>Cách AI chấm gợi ý</h2>
                <p class="muted small">AI đối chiếu transcript với từng tiêu chí rubric và gợi ý điểm cho giảng viên.</p>
            </div>
        </div>
        <div class="divider"></div>
        <p class="small muted">Transcript mẫu</p>
        <div class="transcript-sample">
            <p>“…Em chọn <mark>UDP</mark> vì không cần bắt tay ba bước, phù hợp streaming latency thấp…”</p>
        </div>
        <div class="alert info small">
            <svg class="icon"><use href="${ico}#i-sparkles"/></svg>
            <span><b>Lập luận kỹ thuật</b> · gợi ý <b>2.8/4</b> (Đạt)</span>
        </div>
    </div>
</aside>
</div>
</c:if>
</c:otherwise>
</c:choose>

<c:if test="${rubricView && param.status != 'APPROVED'}">
<script>
(function () {
    var form = document.getElementById('filterForm');
    if (form && !form.querySelector('[name=status]').value) {
        form.querySelector('[name=status]').value = 'APPROVED';
        form.submit();
    }
})();
</script>
</c:if>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
