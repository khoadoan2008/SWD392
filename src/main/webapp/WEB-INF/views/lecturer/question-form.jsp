<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%
    request.setAttribute("pageSection", "Quản lý ngân hàng câu hỏi & rubric");
    request.setAttribute("activeNav", "questions");
%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="isNew" value="${q.id == 0}"/>
<c:set var="pageTitle" scope="request" value="${isNew ? 'Thêm câu hỏi mới' : (q.status == 'PENDING_REVIEW' ? 'Duyệt câu hỏi AI' : 'Biên tập & duyệt câu hỏi')}"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="row between" style="margin-bottom:16px;flex-wrap:wrap">
    <a class="btn sm ghost" href="${ctx}/lecturer/questions"><svg class="icon"><use href="${ico}#i-arrow-left"/></svg>Ngân hàng câu hỏi</a>
    <c:if test="${q.status == 'PENDING_REVIEW'}">
        <a class="btn sm" href="${ctx}/lecturer/questions?status=PENDING_REVIEW"><svg class="icon"><use href="${ico}#i-layers"/></svg>Chế độ duyệt batch</a>
    </c:if>
</div>

<div class="split wide-side">
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
                <label for="referenceAnswer">Đáp án kỳ vọng</label>
                <textarea id="referenceAnswer" name="referenceAnswer" placeholder="Các ý chính mà sinh viên cần trình bày (chỉ giảng viên thấy)"><c:out value="${q.referenceAnswer}"/></textarea>

                <div class="form-actions">
                    <a class="btn" href="${ctx}/lecturer/questions">Huỷ</a>
                    <button class="btn primary" type="submit"><svg class="icon"><use href="${ico}#i-check"/></svg>${isNew ? 'Tạo câu hỏi' : 'Lưu thay đổi'}</button>
                </div>
            </form>
        </div>

        <%-- ===== Rubric ===== --%>
        <div class="card" id="rubrics">
            <c:choose>
                <c:when test="${isNew}">
                    <div class="empty"><svg class="icon"><use href="${ico}#i-target"/></svg>
                        <div>Hãy lưu câu hỏi trước, sau đó thiết lập rubric.</div></div>
                </c:when>
                <c:otherwise>
                    <c:set var="total" value="0"/>
                    <div class="card-header row between" style="margin-top:-8px;padding-top:0">
                        <div>
                            <h2>Rubric gợi ý</h2>
                            <p class="muted small">Tiêu chí, thang điểm và từ khoá để AI chấm thi nhất quán.</p>
                        </div>
                        <a class="btn sm primary" href="${ctx}/lecturer/questions?view=rubric&amp;id=${q.id}">
                            <svg class="icon"><use href="${ico}#i-target"/></svg>Mở trình chỉnh sửa Rubric
                        </a>
                    </div>

                    <!-- Hiển thị các tiêu chí dạng Card 3 mức độ đạt -->
                    <c:if test="${not empty q.rubrics}">
                        <c:forEach items="${q.rubrics}" var="r" varStatus="rst">
                            <c:set var="total" value="${total + r.maxScore}"/>
                            <div class="rubric-criterion">
                                <div class="rubric-criterion-head">
                                    <div class="rubric-criterion-title">
                                        <span>${rst.index + 1}.</span>
                                        <span><c:out value="${r.criterion}"/></span>
                                    </div>
                                    <div class="row" style="gap:8px">
                                        <span class="rubric-badge-score"><c:out value="${r.maxScore}"/> điểm</span>
                                        <form method="post" action="${ctx}/lecturer/rubrics" style="display:inline">
                                            <input type="hidden" name="questionId" value="${q.id}">
                                            <input type="hidden" name="id" value="${r.id}">
                                            <button class="btn sm ghost icon-btn danger" title="Xoá tiêu chí" type="submit" name="action" value="delete"
                                                    onclick="return confirm('Xoá tiêu chí này?')">
                                                <svg class="icon"><use href="${ico}#i-trash"/></svg>
                                            </button>
                                        </form>
                                    </div>
                                </div>
                                <div class="rubric-levels-grid">
                                    <div class="rubric-level-card">
                                        <span class="rubric-level-pill excellent">Xuất sắc - 100%</span>
                                        <p class="small text-2" style="margin:0"><c:out value="${not empty r.keywords ? r.keywords : 'Phân tích đầy đủ và chính xác tất cả khía cạnh kỹ thuật.'}"/></p>
                                    </div>
                                    <div class="rubric-level-card">
                                        <span class="rubric-level-pill good">Đạt - 70%</span>
                                        <p class="small text-2" style="margin:0">Hiểu đúng bản chất, trả lời đúng phần lớn yêu cầu trọng tâm.</p>
                                    </div>
                                    <div class="rubric-level-card">
                                        <span class="rubric-level-pill improve">Cần cải thiện - 30%</span>
                                        <p class="small text-2" style="margin:0">Lập luận còn chung chung, thiếu cơ sở kỹ thuật hoặc ví dụ cụ thể.</p>
                                    </div>
                                </div>
                            </div>
                        </c:forEach>
                    </c:if>

                    <c:if test="${empty q.rubrics}">
                        <div class="empty" style="padding:24px 0">
                            <svg class="icon"><use href="${ico}#i-target"/></svg>
                            <p class="muted small">Chưa có tiêu chí nào. Hãy bổ sung tiêu chí để AI hỗ trợ chấm thi.</p>
                        </div>
                    </c:if>

                    <!-- Form thêm tiêu chí mới nhanh -->
                    <div class="card" style="background:var(--surface-2);border:1px dashed #cbd5e1;padding:14px;margin-top:12px">
                        <strong class="small" style="display:block;margin-bottom:8px">Thêm nhanh tiêu chí mới:</strong>
                        <form method="post" action="${ctx}/lecturer/rubrics">
                            <input type="hidden" name="questionId" value="${q.id}">
                            <input type="hidden" name="id" value="0">
                            <input type="hidden" name="sortOrder" value="${fn:length(q.rubrics) + 1}">
                            <div class="grid-3" style="margin-bottom:8px">
                                <div style="grid-column: span 2">
                                    <input type="text" name="criterion" placeholder="Tên tiêu chí (VD: Lập luận kỹ thuật)" required aria-label="Tên tiêu chí">
                                </div>
                                <div>
                                    <input type="number" name="maxScore" value="3" step="0.5" min="0.5" placeholder="Điểm tối đa" required aria-label="Điểm">
                                </div>
                            </div>
                            <input type="text" name="keywords" placeholder="Từ khoá / mô tả mức xuất sắc (VD: độ trễ, tin cậy, overhead)" style="margin-bottom:10px" aria-label="Mô tả">
                            <button class="btn sm primary" type="submit" name="action" value="save">
                                <svg class="icon"><use href="${ico}#i-plus"/></svg>Thêm tiêu chí
                            </button>
                        </form>
                    </div>

                    <div class="score-total" style="margin-top:16px">
                        <span>Tổng điểm rubric hiện tại</span>
                        <span id="rubricTotalValue">${total} điểm</span>
                    </div>
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
                            <button class="btn success block" type="submit"><svg class="icon"><use href="${ico}#i-check-circle"/></svg>Phê duyệt vào ngân hàng</button>
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

        <c:choose>
            <c:when test="${!isNew && not empty q.rubrics}">
                <div class="card ai-panel">
                    <div class="row" style="align-items:flex-start">
                        <span class="avatar" style="border-radius:10px;width:40px;height:40px"><svg class="icon"><use href="${ico}#i-bot"/></svg></span>
                        <div>
                            <h2>Cách AI chấm gợi ý</h2>
                            <p class="muted small">AI phân tích transcript theo rubric và gợi ý điểm từng tiêu chí.</p>
                        </div>
                    </div>
                    <div class="divider"></div>
                    <p class="small muted">Transcript mẫu</p>
                    <div class="transcript-sample">
                        <p>“…<mark>UDP</mark> không cần handshake nên phù hợp ứng dụng real-time…”</p>
                    </div>
                    <div class="alert info small">
                        <svg class="icon"><use href="${ico}#i-sparkles"/></svg>
                        <span><b><c:out value="${q.rubrics[0].criterion}"/></b> · gợi ý điểm theo rubric</span>
                    </div>
                </div>
            </c:when>
            <c:otherwise>
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
            </c:otherwise>
        </c:choose>
    </aside>
</div>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
