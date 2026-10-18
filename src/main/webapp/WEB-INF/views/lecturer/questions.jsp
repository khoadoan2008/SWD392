<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<%-- Tự động chuyển tiếp sang rubric.jsp khi người dùng chọn xem hoặc sửa Rubric --%>
<c:if test="${param.view == 'rubric' && param.mode != 'list'}">
    <jsp:forward page="/WEB-INF/views/lecturer/rubric.jsp"/>
</c:if>

<c:set var="reviewMode" value="${param.status == 'PENDING_REVIEW'}"/>

<%
    if ("true".equals(pageContext.getAttribute("reviewMode").toString())) {
        request.setAttribute("pageTitle", "Duyệt câu hỏi AI");
        request.setAttribute("pageSection", "Quản lý ngân hàng câu hỏi & rubric");
        request.setAttribute("activeNav", "questions");
    } else {
        request.setAttribute("pageTitle", "Ngân hàng câu hỏi");
        request.setAttribute("pageSection", "Quản lý ngân hàng câu hỏi & rubric");
        request.setAttribute("activeNav", "questions");
    }
%>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<c:choose>
<%-- =========================================================================
     TRƯỜNG HỢP 1: DUYỆT BATCH CÂU HỎI AI (Figma Screen 3)
     ========================================================================= --%>
<c:when test="${reviewMode && not empty questions}">
    <c:set var="currId" value="${empty param.curr ? questions[0].id : param.curr}"/>
    
    <div class="review-top row between" style="flex-wrap:wrap">
        <div class="review-progress-wrap" style="flex:1">
            <div class="row between small" style="margin-bottom:6px">
                <span><b>Tiến độ duyệt AI</b>: 7 / 12 đã duyệt</span>
                <span class="muted">${fn:length(questions)} câu trong bộ lọc</span>
            </div>
            <div class="progress" style="height:8px"><div style="width:58%"></div></div>
        </div>
        <a class="btn sm" href="${ctx}/lecturer/questions"><svg class="icon"><use href="${ico}#i-arrow-left"/></svg>Lưu &amp; thoát</a>
    </div>

    <div class="review-layout">
        <!-- Danh sách câu hỏi bên trái -->
        <aside class="card flush review-list">
            <div class="card-header" style="padding:14px 16px;margin:0;border-bottom:1px solid var(--border)">
                <h2>Câu hỏi AI <small class="muted">(${fn:length(questions)} còn lại)</small></h2>
            </div>
            <ul class="review-items">
                <c:forEach items="${questions}" var="qq" varStatus="st">
                    <li>
                        <a class="review-item ${qq.id == currId ? 'active' : ''} ${qq.status == 'APPROVED' ? 'done' : ''}"
                           href="${ctx}/lecturer/questions?status=PENDING_REVIEW&amp;curr=${qq.id}">
                            <div class="row between">
                                <span class="review-num">Câu ${st.index + 1}</span>
                                <span class="badge ${qq.status == 'APPROVED' ? 'APPROVED' : (qq.id == currId ? 'primary' : 'PENDING_REVIEW')}">
                                    ${qq.status == 'APPROVED' ? 'Đã duyệt' : (qq.id == currId ? 'Đang xem' : 'Chờ duyệt')}
                                </span>
                            </div>
                            <span class="clamp-2 small" style="color:var(--text)"><c:out value="${qq.content}"/></span>
                        </a>
                    </li>
                </c:forEach>
            </ul>
        </aside>

        <!-- Chi tiết chỉnh sửa câu hỏi & rubric bên phải -->
        <div class="review-detail">
            <c:forEach items="${questions}" var="qq">
                <c:if test="${qq.id == currId}">
                    <div class="card">
                        <div class="card-header">
                            <div>
                                <h2>Câu ${qq.id} - Chi tiết chỉnh sửa</h2>
                                <p class="muted small">NHCH-${qq.id} · <c:out value="${qq.topic}"/></p>
                            </div>
                            <div class="row">
                                <span class="bloom ${qq.bloomLevel}">${qq.bloomLevel.label}</span>
                                <span class="badge plain AI">AI sinh - RAG</span>
                            </div>
                        </div>

                        <!-- Form chỉnh sửa câu hỏi -->
                        <form method="post" action="${ctx}/lecturer/questions" id="reviewForm${qq.id}">
                            <input type="hidden" name="action" value="save">
                            <input type="hidden" name="id" value="${qq.id}">
                            <input type="hidden" name="subjectId" value="${qq.subjectId}">

                            <label for="cContent${qq.id}">Nội dung câu hỏi</label>
                            <textarea id="cContent${qq.id}" name="content" rows="3" required><c:out value="${qq.content}"/></textarea>

                            <label for="cAns${qq.id}">Đáp án kỳ vọng</label>
                            <textarea id="cAns${qq.id}" name="referenceAnswer" rows="3" placeholder="Ý chính sinh viên cần trình bày..."><c:out value="${qq.referenceAnswer}"/></textarea>

                            <div class="grid-2">
                                <div>
                                    <label for="cTopic${qq.id}">Nguồn trích dẫn / Chủ đề</label>
                                    <input type="text" id="cTopic${qq.id}" name="topic" value="${fn:escapeXml(qq.topic)}" placeholder="Giáo trình trang...">
                                </div>
                                <div>
                                    <label for="cBloom${qq.id}">Mức Bloom</label>
                                    <select id="cBloom${qq.id}" name="bloomLevel">
                                        <c:forEach items="${blooms}" var="b">
                                            <option value="${b}" ${qq.bloomLevel == b ? 'selected' : ''}>${b.label}</option>
                                        </c:forEach>
                                    </select>
                                </div>
                            </div>
                        </form>

                        <!-- Box Rubric gợi ý (Figma Screen 3) -->
                        <div class="review-rubric-card">
                            <div class="review-rubric-card-head">
                                <div>
                                    <strong style="font-size:14px">Rubric gợi ý</strong>
                                    <span class="muted small">· Tổng 10 điểm</span>
                                </div>
                                <a class="small btn sm ghost" href="${ctx}/lecturer/questions?view=rubric&amp;id=${qq.id}">
                                    <svg class="icon"><use href="${ico}#i-external"/></svg>Mở trình chỉnh sửa
                                </a>
                            </div>

                            <table class="rubric-preview">
                                <thead>
                                    <tr>
                                        <th style="width:35%">TIÊU CHÍ</th>
                                        <th style="width:50%">MÔ TẢ ĐẠT YÊU CẦU</th>
                                        <th style="width:15%">ĐIỂM</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:choose>
                                        <c:when test="${not empty qq.rubrics}">
                                            <c:forEach items="${qq.rubrics}" var="r">
                                                <tr>
                                                    <td><b><c:out value="${r.criterion}"/></b></td>
                                                    <td class="muted small"><c:out value="${empty r.keywords ? 'Phân tích đúng các khía cạnh kỹ thuật theo yêu cầu.' : r.keywords}"/></td>
                                                    <td><span class="badge plain info"><c:out value="${r.maxScore}"/> điểm</span></td>
                                                </tr>
                                            </c:forEach>
                                        </c:when>
                                        <c:otherwise>
                                            <tr>
                                                <td><b>Lập luận kỹ thuật</b></td>
                                                <td class="muted small">Phân tích đúng đánh đổi độ trễ và độ tin cậy.</td>
                                                <td><span class="badge plain info">4 điểm</span></td>
                                            </tr>
                                            <tr>
                                                <td><b>Ví dụ thực tế</b></td>
                                                <td class="muted small">Liên hệ VoIP, game trực tuyến hoặc livestream.</td>
                                                <td><span class="badge plain info">3 điểm</span></td>
                                            </tr>
                                            <tr>
                                                <td><b>Tính nhất quán</b></td>
                                                <td class="muted small">Kết luận phù hợp với ngữ cảnh đã chọn.</td>
                                                <td><span class="badge plain info">3 điểm</span></td>
                                            </tr>
                                        </c:otherwise>
                                    </c:choose>
                                </tbody>
                            </table>
                        </div>

                        <!-- Các hành động duyệt -->
                        <div class="row between" style="margin-top:20px;padding-top:16px;border-top:1px solid var(--border)">
                            <form method="post" action="${ctx}/lecturer/questions">
                                <input type="hidden" name="action" value="status">
                                <input type="hidden" name="id" value="${qq.id}">
                                <input type="hidden" name="to" value="REJECTED">
                                <input type="hidden" name="back" value="list">
                                <button class="btn danger" type="submit" onclick="return confirm('Loại bỏ câu hỏi này?')">
                                    <svg class="icon"><use href="${ico}#i-trash"/></svg>Loại bỏ
                                </button>
                            </form>
                            <div class="row">
                                <button class="btn" type="submit" form="reviewForm${qq.id}">
                                    <svg class="icon"><use href="${ico}#i-save"/></svg>Lưu chỉnh sửa
                                </button>
                                <form method="post" action="${ctx}/lecturer/questions">
                                    <input type="hidden" name="action" value="status">
                                    <input type="hidden" name="id" value="${qq.id}">
                                    <input type="hidden" name="to" value="APPROVED">
                                    <button class="btn success" type="submit">
                                        <svg class="icon"><use href="${ico}#i-check-circle"/></svg>Phê duyệt vào ngân hàng
                                    </button>
                                </form>
                            </div>
                        </div>
                    </div>
                </c:if>
            </c:forEach>
        </div>
    </div>
</c:when>

<%-- =========================================================================
     TRƯỜNG HỢP 2: DANH SÁCH NGÂN HÀNG CÂU HỎI (Figma Screen 1)
     ========================================================================= --%>
<c:otherwise>
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

    <c:if test="${stats.PENDING_REVIEW > 0 && param.status != 'PENDING_REVIEW'}">
        <div class="alert warning">
            <svg class="icon"><use href="${ico}#i-sparkles"/></svg>
            <span>Có <b>${stats.PENDING_REVIEW}</b> câu hỏi do AI sinh đang chờ bạn duyệt.
                <a href="${ctx}/lecturer/questions?status=PENDING_REVIEW">Duyệt ngay</a></span>
        </div>
    </c:if>

    <div class="card flush">
        <div class="card-header">
            <div>
                <h2>Danh sách câu hỏi</h2>
                <p class="muted small">${fn:length(questions)} câu hỏi phù hợp bộ lọc</p>
            </div>
            <div class="row">
                <a class="btn" href="${ctx}/lecturer/questions?action=generate"><svg class="icon"><use href="${ico}#i-sparkles"/></svg>AI sinh câu hỏi</a>
                <a class="btn primary" href="${ctx}/lecturer/questions?action=generate"><svg class="icon"><use href="${ico}#i-plus"/></svg>Tạo câu hỏi</a>
            </div>
        </div>

        <form class="filters" method="get" id="filterForm">
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
                <thead>
                    <tr>
                        <th style="width:38%">CÂU HỎI</th>
                        <th>MỨC BLOOM</th>
                        <th>RUBRIC</th>
                        <th>NGUỒN</th>
                        <th>TRẠNG THÁI</th>
                        <th></th>
                    </tr>
                </thead>
                <tbody>
                <c:forEach items="${questions}" var="q" varStatus="st">
                    <tr>
                        <td>
                            <a class="cell-main clamp-2" style="color:inherit" href="${ctx}/lecturer/questions?action=edit&amp;id=${q.id}">
                                <c:out value="${q.content}"/>
                            </a>
                            <div class="cell-sub">NHCH-${q.id} · <c:out value="${q.subjectCode}"/><c:if test="${not empty q.topic}"> · <c:out value="${q.topic}"/></c:if></div>
                        </td>
                        <td><span class="bloom ${q.bloomLevel}">${q.bloomLevel.label}</span></td>
                        <td>
                            <c:choose>
                                <c:when test="${q.status == 'DRAFT'}">
                                    <span class="muted small">Chưa gắn</span>
                                </c:when>
                                <c:otherwise>
                                    <a class="badge plain primary" title="Mở trình chỉnh sửa rubric" href="${ctx}/lecturer/questions?view=rubric&amp;id=${q.id}">
                                        RB-<c:choose><c:when test="${st.index == 0}">OSI-02</c:when><c:when test="${st.index == 1}">TCP-04</c:when><c:when test="${st.index == 2}">IP-03</c:when><c:otherwise>DNS-${q.id}</c:otherwise></c:choose>
                                    </a>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <span class="badge plain ${q.source == 'AI' ? 'AI' : ''}">
                                <c:choose>
                                    <c:when test="${not empty q.topic && fn:contains(q.topic, 'Giáo trình')}"><c:out value="${q.topic}"/></c:when>
                                    <c:otherwise>${q.source.label}</c:otherwise>
                                </c:choose>
                            </span>
                        </td>
                        <td><span class="badge ${q.status}">${q.status.label}</span></td>
                        <td class="actions">
                            <a class="btn sm ghost icon-btn" title="Chỉnh sửa câu hỏi" href="${ctx}/lecturer/questions?action=edit&amp;id=${q.id}"><svg class="icon"><use href="${ico}#i-edit"/></svg></a>
                            <a class="btn sm ghost icon-btn" title="Trình chỉnh sửa rubric" href="${ctx}/lecturer/questions?view=rubric&amp;id=${q.id}"><svg class="icon"><use href="${ico}#i-target"/></svg></a>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty questions}">
                    <tr><td colspan="6"><div class="empty">
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
                    <span class="small muted">1 / 10</span>
                    <button class="btn sm" type="button">Sau</button>
                </div>
            </div>
        </c:if>
    </div>
</c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
