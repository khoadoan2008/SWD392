<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%
    request.setAttribute("pageTitle", "Transcript phỏng vấn");
    request.setAttribute("pageSection", "Lõi phỏng vấn AI");
    request.setAttribute("activeNav", "sessions");
%>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="row" style="margin-bottom:16px">
    <a class="btn sm ghost" href="${ctx}/lecturer/sessions"><svg class="icon"><use href="${ico}#i-arrow-left"/></svg>Danh sách lượt phỏng vấn</a>
</div>

<div class="split">
    <div class="card">
        <div class="card-header">
            <div><h2>Hội thoại</h2><p class="muted small">Câu hỏi của AI và câu trả lời (văn bản nhận dạng giọng nói) của sinh viên.</p></div>
        </div>
        <%@ include file="/WEB-INF/views/common/transcript.jspf" %>
    </div>

    <aside class="sticky">
        <div class="card">
            <div class="row" style="flex-wrap:nowrap">
                <span class="avatar lg"><c:out value="${fn:substring(se.studentName, 0, 1)}"/></span>
                <div><h2><c:out value="${se.studentName}"/></h2><div class="muted small"><c:out value="${se.studentCode}"/></div></div>
            </div>
            <div class="divider"></div>
            <dl class="kv">
                <dt>Lượt thi</dt><dd>#${se.id}</dd>
                <dt>Môn</dt><dd><c:out value="${se.subjectCode}"/></dd>
                <dt>Trạng thái</dt><dd><span class="badge ${se.status}">${se.status == 'COMPLETED' ? 'Hoàn thành' : (se.status == 'IN_PROGRESS' ? 'Đang thi' : 'Kết thúc sớm')}</span></dd>
                <dt>Bắt đầu</dt><dd><fmt:formatDate value="${se.startedAt}" pattern="HH:mm dd/MM/yyyy"/></dd>
                <dt>Kết thúc</dt><dd><c:choose><c:when test="${empty se.endedAt}">—</c:when><c:otherwise><fmt:formatDate value="${se.endedAt}" pattern="HH:mm dd/MM/yyyy"/></c:otherwise></c:choose></dd>
            </dl>
        </div>
        <div class="card">
            <h2>Thống kê</h2>
            <div class="grid-2 mt" style="gap:12px">
                <div><div class="muted small">Câu chính</div><div style="font-size:20px;font-weight:700">${se.mainCount}</div></div>
                <div><div class="muted small">Câu hỏi xoáy</div><div style="font-size:20px;font-weight:700">${se.followUpCount}</div></div>
                <div><div class="muted small">Đã trả lời</div><div style="font-size:20px;font-weight:700">${se.answeredCount}/${fn:length(se.turns)}</div></div>
                <div><div class="muted small">Tổng thời gian nói</div><div style="font-size:20px;font-weight:700">${se.totalResponseSec}s</div></div>
            </div>
            <div class="divider"></div>
            <p class="small muted">Giới hạn: ${se.answerTimeLimitSec}s mỗi câu · tối đa ${se.maxFollowups} câu hỏi xoáy.</p>
            <div class="alert info small"><svg class="icon"><use href="${ico}#i-shield"/></svg>
                <span>AI chỉ hỗ trợ. Giảng viên là người quyết định điểm cuối cùng.</span></div>
        </div>
    </aside>
</div>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
