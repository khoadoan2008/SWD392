<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%
    request.setAttribute("pageTitle", "Tạo câu hỏi mới");
    request.setAttribute("pageSection", "Quản lý ngân hàng câu hỏi & rubric");
    request.setAttribute("activeNav", "questions");
%>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="row" style="margin-bottom:16px">
    <a class="btn sm ghost" href="${ctx}/lecturer/questions"><svg class="icon"><use href="${ico}#i-arrow-left"/></svg>Ngân hàng câu hỏi</a>
</div>
<p class="muted" style="margin:-8px 0 16px">Chọn cách thêm câu hỏi vào ngân hàng.</p>

<div class="method-cards">
    <a class="method-card" href="${ctx}/lecturer/questions?action=create">
        <span class="method-icon"><svg class="icon"><use href="${ico}#i-edit"/></svg></span>
        <b>Tạo thủ công</b>
        <span class="muted small">Nhập câu hỏi và rubric trực tiếp</span>
    </a>
    <span class="method-card disabled" title="Sắp có">
        <span class="method-icon"><svg class="icon"><use href="${ico}#i-file"/></svg></span>
        <b>Import tệp</b>
        <span class="muted small">Tải lên theo mẫu Excel/CSV</span>
    </span>
    <span class="method-card selected">
        <span class="method-icon ai"><svg class="icon"><use href="${ico}#i-sparkles"/></svg></span>
        <b>Sinh bằng AI</b>
        <span class="muted small">Grounding tài liệu + phân bổ Bloom</span>
        <span class="method-badge">Đang chọn</span>
    </span>
</div>

<div class="card">
    <div class="card-header">
        <div><h2>Phạm vi kiến thức</h2><p class="muted small">AI sinh câu hỏi bám chủ đề và tài liệu môn học.</p></div>
    </div>
    <form method="post" action="${ctx}/lecturer/questions" id="aiGenerateForm">
        <input type="hidden" name="action" value="generate">
        <div id="levelFields"></div>
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
                <label class="mt-0" for="topic">Chủ đề</label>
                <input type="text" id="topic" name="topic" placeholder="VD: Tầng giao vận - TCP / UDP" required>
            </div>
        </div>

        <span class="label">Tài liệu grounding</span>
        <div class="file-chip">
            <svg class="icon"><use href="${ico}#i-file"/></svg>
            <div>
                <div class="cell-main">Giáo trình môn học (demo)</div>
                <div class="cell-sub">Sẵn sàng · bản demo dùng mẫu câu hỏi theo Bloom</div>
            </div>
            <span class="badge success">Sẵn sàng</span>
        </div>
        <div class="row mt">
            <button type="button" class="btn sm" disabled>Chọn từ học liệu</button>
            <button type="button" class="btn sm" disabled>Tải tệp mới</button>
        </div>

        <span class="label">Số lượng &amp; phân bổ Bloom</span>
        <div class="row between" style="margin-bottom:10px">
            <span class="muted small">Tổng số câu sinh ra</span>
            <strong id="totalQuestions">12 câu</strong>
        </div>
        <div class="bloom-bars" id="bloomBars">
            <div class="bloom-bar" data-levels="REMEMBER,UNDERSTAND">
                <div class="bloom-bar-head"><span>Nhớ / Hiểu</span><span class="bloom-bar-val"><output>4</output> câu</span></div>
                <input type="range" min="0" max="12" value="4" aria-label="Số câu Nhớ / Hiểu">
            </div>
            <div class="bloom-bar" data-levels="APPLY">
                <div class="bloom-bar-head"><span>Vận dụng</span><span class="bloom-bar-val"><output>4</output> câu</span></div>
                <input type="range" min="0" max="12" value="4" aria-label="Số câu Vận dụng">
            </div>
            <div class="bloom-bar" data-levels="ANALYZE">
                <div class="bloom-bar-head"><span>Phân tích</span><span class="bloom-bar-val"><output>3</output> câu</span></div>
                <input type="range" min="0" max="12" value="3" aria-label="Số câu Phân tích">
            </div>
            <div class="bloom-bar" data-levels="EVALUATE">
                <div class="bloom-bar-head"><span>Đánh giá</span><span class="bloom-bar-val"><output>1</output> câu</span></div>
                <input type="range" min="0" max="12" value="1" aria-label="Số câu Đánh giá">
            </div>
        </div>
        <div class="alert info small mt">
            <svg class="icon"><use href="${ico}#i-info"/></svg>
            <span>Câu hỏi sinh ra kèm trích dẫn nguồn và ở trạng thái <b>Chờ duyệt</b> cho đến khi bạn phê duyệt.</span>
        </div>
        <button class="btn primary lg block mt" type="submit" id="aiSubmitBtn">
            <svg class="icon"><use href="${ico}#i-sparkles"/></svg>Sinh <span id="submitCount">12</span> câu hỏi bằng AI
        </button>
    </form>
</div>

<script>
(function () {
    var bars = document.querySelectorAll('.bloom-bar');
    var totalEl = document.getElementById('totalQuestions');
    var submitCount = document.getElementById('submitCount');
    var levelFields = document.getElementById('levelFields');
    var form = document.getElementById('aiGenerateForm');

    function sync() {
        var total = 0;
        bars.forEach(function (bar) {
            var range = bar.querySelector('input[type=range]');
            var n = parseInt(range.value, 10) || 0;
            total += n;
            bar.querySelector('output').textContent = n;
            var fill = Math.min(100, (n / 12) * 100);
            range.style.background = 'linear-gradient(90deg, var(--primary) ' + fill + '%, #eef0f5 ' + fill + '%)';
        });
        totalEl.textContent = total + ' câu';
        submitCount.textContent = total;
        document.getElementById('aiSubmitBtn').disabled = total === 0;
    }

    form.addEventListener('submit', function () {
        levelFields.innerHTML = '';
        bars.forEach(function (bar) {
            var range = bar.querySelector('input[type=range]');
            var n = parseInt(range.value, 10) || 0;
            var levels = bar.getAttribute('data-levels').split(',');
            for (var i = 0; i < n; i++) {
                var input = document.createElement('input');
                input.type = 'hidden';
                input.name = 'levels';
                input.value = levels[i % levels.length];
                levelFields.appendChild(input);
            }
        });
    });

    bars.forEach(function (bar) {
        bar.querySelector('input[type=range]').addEventListener('input', sync);
    });
    sync();
})();
</script>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
