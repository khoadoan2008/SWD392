<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%
    request.setAttribute("pageTitle", "Tạo câu hỏi bằng AI");
    request.setAttribute("pageSection", "Quản lý ngân hàng câu hỏi & rubric");
    request.setAttribute("activeNav", "generate");
%>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="split">
    <div class="card">
        <div class="card-header">
            <div class="row"><span class="avatar" style="border-radius:10px"><svg class="icon"><use href="${ico}#i-sparkles"/></svg></span>
                <div><h2>Sinh câu hỏi tự động</h2><p class="muted small">Mỗi mức Bloom được chọn sẽ sinh 1 câu hỏi ở trạng thái <b>Chờ duyệt</b>.</p></div></div>
        </div>
        <form method="post" action="${ctx}/lecturer/questions">
            <input type="hidden" name="action" value="generate">
            <div class="grid-2">
                <div>
                    <label class="mt-0" for="subjectId">Môn học</label>
                    <select id="subjectId" name="subjectId" required>
                        <c:forEach items="${subjects}" var="s">
                            <option value="${s.id}"><c:out value="${s.code} · ${s.name}"/></option>
                        </c:forEach>
                    </select>
                </div>
                <div>
                    <label class="mt-0" for="topic">Chủ đề / chương</label>
                    <input type="text" id="topic" name="topic" placeholder="VD: vòng đời Servlet" required>
                </div>
            </div>

            <span class="label">Phân bố mức độ Bloom</span>
            <div class="chips">
                <c:forEach items="${blooms}" var="b" varStatus="st">
                    <label class="chip" style="margin:0"><input type="checkbox" name="levels" value="${b}" ${st.index < 4 ? 'checked' : ''}><span>${b.label}</span></label>
                </c:forEach>
            </div>

            <label for="material">Tài liệu môn học (RAG)</label>
            <textarea id="material" disabled placeholder="Sắp có: AI sẽ đọc giáo trình / slide bài giảng của môn để sinh câu hỏi bám sát nội dung."></textarea>
            <div class="hint">Bản demo hiện sinh câu hỏi theo mẫu cho từng mức Bloom.</div>

            <div class="form-actions">
                <a class="btn" href="${ctx}/lecturer/questions?action=create"><svg class="icon"><use href="${ico}#i-edit"/></svg>Nhập tay thay vào đó</a>
                <button class="btn primary" type="submit"><svg class="icon"><use href="${ico}#i-sparkles"/></svg>Sinh câu hỏi</button>
            </div>
        </form>
    </div>

    <aside class="sticky">
        <div class="card">
            <h2>Quy trình kiểm duyệt</h2>
            <ul class="steps mt">
                <li class="done"><span class="dot">1</span>AI sinh câu hỏi theo chủ đề</li>
                <li class="current"><span class="dot">2</span>Câu hỏi vào hàng chờ duyệt</li>
                <li><span class="dot">3</span>Giảng viên chỉnh sửa, thêm rubric</li>
                <li><span class="dot">4</span>Duyệt vào ngân hàng chính thức</li>
            </ul>
            <div class="alert info small mt"><svg class="icon"><use href="${ico}#i-shield"/></svg>
                <span>Câu hỏi do AI sinh không bao giờ được dùng để thi khi chưa được giảng viên duyệt.</span></div>
        </div>
    </aside>
</div>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
