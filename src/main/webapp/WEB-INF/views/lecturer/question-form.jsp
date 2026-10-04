<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%
    request.setAttribute("pageSection", "Quản lý ngân hàng câu hỏi & rubric");
    request.setAttribute("activeNav", "questions");
%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="isNew" value="${q.id == 0}"/>
<c:set var="pageTitle" scope="request" value="${isNew ? 'Thêm câu hỏi mới' : 'Biên tập & duyệt câu hỏi'}"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="row" style="margin-bottom:16px">
    <a class="btn sm ghost" href="${ctx}/lecturer/questions"><svg class="icon"><use href="${ico}#i-arrow-left"/></svg>Ngân hàng câu hỏi</a>
</div>

<div class="split">
    <div>
        <%-- ===== Nội dung câu hỏi ===== --%>
        <div class="card">
            <div class="card-header">
                <div><h2>Nội dung câu hỏi</h2><p class="muted small">Câu hỏi được AI đọc bằng giọng nói khi phỏng vấn.</p></div>
            </div>
            <form method="post" action="${ctx}/lecturer/questions" id="questionForm">
                <input type="hidden" name="action" value="save">
                <input type="hidden" name="id" value="${q.id}">
                <div class="grid-2">
                    <div>
                        <label class="mt-0" for="subjectId">Môn học</label>
                        <select id="subjectId" name="subjectId" required>
                            <c:forEach items="${subjects}" var="s">
                                <option value="${s.id}" ${q.subjectId == s.id ? 'selected' : ''}><c:out value="${s.code} · ${s.name}"/></option>
                            </c:forEach>
                        </select>
                    </div>
                    <div>
                        <label class="mt-0" for="topic">Chương / chủ đề</label>
                        <input type="text" id="topic" name="topic" value="${fn:escapeXml(q.topic)}" placeholder="VD: Chương 2 - Servlet">
                    </div>
                </div>

                <span class="label">Mức độ nhận thức (Bloom)</span>
                <div class="chips">
                    <c:forEach items="${blooms}" var="b">
                        <label class="chip" style="margin:0"><input type="radio" name="bloomLevel" value="${b}" ${q.bloomLevel == b ? 'checked' : ''}><span>${b.label}</span></label>
                    </c:forEach>
                </div>

                <label for="content">Câu hỏi</label>
                <textarea id="content" name="content" required placeholder="Nhập nội dung câu hỏi vấn đáp..."><c:out value="${q.content}"/></textarea>
                <label for="referenceAnswer">Đáp án tham khảo</label>
                <textarea id="referenceAnswer" name="referenceAnswer" placeholder="Các ý chính mà sinh viên cần trình bày (chỉ giảng viên thấy)"><c:out value="${q.referenceAnswer}"/></textarea>

                <div class="form-actions">
                    <a class="btn" href="${ctx}/lecturer/questions">Huỷ</a>
                    <button class="btn primary" type="submit"><svg class="icon"><use href="${ico}#i-check"/></svg>${isNew ? 'Tạo câu hỏi' : 'Lưu thay đổi'}</button>
                </div>
            </form>
        </div>

        <%-- ===== Rubric ===== --%>
        <div class="card" id="rubrics">
            <div class="card-header">
                <div><h2>Rubric chấm điểm</h2>
                    <p class="muted small">Tiêu chí + thang điểm là căn cứ để AI gợi ý và giảng viên chấm. Từ khoá giúp AI phát hiện thiếu ý để hỏi xoáy.</p></div>
            </div>
            <c:choose>
                <c:when test="${isNew}">
                    <div class="empty"><svg class="icon"><use href="${ico}#i-target"/></svg>
                        <div>Hãy lưu câu hỏi trước, sau đó thêm tiêu chí rubric.</div></div>
                </c:when>
                <c:otherwise>
                    <div class="rubric-row head"><span>Thứ tự</span><span>Tiêu chí</span><span>Từ khoá (phân tách dấu phẩy)</span><span>Điểm</span><span></span></div>
                    <c:set var="total" value="0"/>
                    <c:forEach items="${q.rubrics}" var="r">
                        <c:set var="total" value="${total + r.maxScore}"/>
                        <div class="rubric-row">
                            <input form="r${r.id}" type="number" name="sortOrder" value="${r.sortOrder}" aria-label="Thứ tự">
                            <input form="r${r.id}" type="text" name="criterion" value="${fn:escapeXml(r.criterion)}" aria-label="Tiêu chí" required>
                            <input form="r${r.id}" type="text" name="keywords" value="${fn:escapeXml(r.keywords)}" aria-label="Từ khoá">
                            <input form="r${r.id}" type="number" name="maxScore" value="${r.maxScore}" step="0.25" min="0.25" aria-label="Điểm tối đa" required>
                            <form id="r${r.id}" method="post" action="${ctx}/lecturer/rubrics" class="row" style="flex-wrap:nowrap;gap:4px">
                                <input type="hidden" name="questionId" value="${q.id}">
                                <input type="hidden" name="id" value="${r.id}">
                                <button class="btn sm ghost icon-btn" title="Lưu" type="submit" name="action" value="save"><svg class="icon"><use href="${ico}#i-check"/></svg></button>
                                <button class="btn sm ghost icon-btn danger" title="Xoá" type="submit" name="action" value="delete"
                                        onclick="return confirm('Xoá tiêu chí này?')"><svg class="icon"><use href="${ico}#i-trash"/></svg></button>
                            </form>
                        </div>
                    </c:forEach>
                    <c:if test="${empty q.rubrics}">
                        <p class="muted small" style="padding:12px 0">Chưa có tiêu chí nào. Cần ít nhất 1 tiêu chí để duyệt câu hỏi.</p>
                    </c:if>

                    <div class="rubric-row new">
                        <input form="rNew" type="number" name="sortOrder" value="${fn:length(q.rubrics) + 1}" aria-label="Thứ tự">
                        <input form="rNew" type="text" name="criterion" placeholder="VD: Nêu đúng 3 phương thức vòng đời" aria-label="Tiêu chí mới" required>
                        <input form="rNew" type="text" name="keywords" placeholder="init, service, destroy" aria-label="Từ khoá">
                        <input form="rNew" type="number" name="maxScore" value="1" step="0.25" min="0.25" aria-label="Điểm tối đa" required>
                        <form id="rNew" method="post" action="${ctx}/lecturer/rubrics">
                            <input type="hidden" name="questionId" value="${q.id}">
                            <input type="hidden" name="id" value="0">
                            <button class="btn sm primary" type="submit" name="action" value="save"><svg class="icon"><use href="${ico}#i-plus"/></svg>Thêm</button>
                        </form>
                    </div>
                    <div class="score-total"><span>Tổng điểm rubric</span><span>${total} điểm</span></div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>

    <%-- ===== Cột phải: trạng thái & duyệt ===== --%>
    <aside class="sticky">
        <c:if test="${!isNew}">
            <div class="card">
                <div class="card-header"><h2>Trạng thái duyệt</h2><span class="badge ${q.status}">${q.status.label}</span></div>
                <dl class="kv">
                    <dt>Mã câu hỏi</dt><dd>#${q.id}</dd>
                    <dt>Nguồn</dt><dd><span class="badge plain ${q.source == 'AI' ? 'AI' : ''}">${q.source.label}</span></dd>
                    <dt>Người tạo</dt><dd><c:out value="${q.createdByName}"/></dd>
                    <dt>Cập nhật</dt><dd><fmt:formatDate value="${q.updatedAt}" pattern="HH:mm dd/MM/yyyy"/></dd>
                </dl>
                <div class="divider"></div>
                <c:if test="${q.status == 'PENDING_REVIEW'}">
                    <div class="alert warning small"><svg class="icon"><use href="${ico}#i-sparkles"/></svg>
                        <span>Câu hỏi do AI sinh. Hãy kiểm tra nội dung và bổ sung rubric trước khi duyệt.</span></div>
                </c:if>
                <div class="stack">
                    <c:if test="${q.status != 'APPROVED'}">
                        <form method="post" action="${ctx}/lecturer/questions">
                            <input type="hidden" name="action" value="status"><input type="hidden" name="id" value="${q.id}"><input type="hidden" name="to" value="APPROVED">
                            <button class="btn success block" type="submit"><svg class="icon"><use href="${ico}#i-check-circle"/></svg>Duyệt vào ngân hàng chính thức</button>
                        </form>
                    </c:if>
                    <div class="row" style="flex-wrap:nowrap">
                        <c:if test="${q.status != 'DRAFT'}">
                            <form method="post" action="${ctx}/lecturer/questions" style="flex:1">
                                <input type="hidden" name="action" value="status"><input type="hidden" name="id" value="${q.id}"><input type="hidden" name="to" value="DRAFT">
                                <button class="btn block" type="submit">Chuyển về nháp</button>
                            </form>
                        </c:if>
                        <c:if test="${q.status != 'REJECTED'}">
                            <form method="post" action="${ctx}/lecturer/questions" style="flex:1">
                                <input type="hidden" name="action" value="status"><input type="hidden" name="id" value="${q.id}"><input type="hidden" name="to" value="REJECTED">
                                <button class="btn danger block" type="submit">Loại bỏ</button>
                            </form>
                        </c:if>
                    </div>
                </div>
            </div>
        </c:if>

        <div class="card">
            <h2>Thang nhận thức Bloom</h2>
            <p class="muted small">Chọn mức phù hợp với mục tiêu đánh giá.</p>
            <dl class="kv mt" style="grid-template-columns:96px 1fr">
                <dt><span class="bloom REMEMBER">Nhớ</span></dt><dd class="small muted" style="font-weight:400">Nêu, liệt kê, định nghĩa</dd>
                <dt><span class="bloom UNDERSTAND">Hiểu</span></dt><dd class="small muted" style="font-weight:400">Giải thích, so sánh, diễn giải</dd>
                <dt><span class="bloom APPLY">Vận dụng</span></dt><dd class="small muted" style="font-weight:400">Áp dụng vào tình huống cụ thể</dd>
                <dt><span class="bloom ANALYZE">Phân tích</span></dt><dd class="small muted" style="font-weight:400">Tách thành phần, tìm quan hệ</dd>
                <dt><span class="bloom EVALUATE">Đánh giá</span></dt><dd class="small muted" style="font-weight:400">Phán đoán, lập luận, bảo vệ</dd>
                <dt><span class="bloom CREATE">Sáng tạo</span></dt><dd class="small muted" style="font-weight:400">Thiết kế, đề xuất giải pháp mới</dd>
            </dl>
        </div>
    </aside>
</div>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
