<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%
    request.setAttribute("pageTitle", "Cấu hình hệ thống");
    request.setAttribute("pageSection", "Quản trị hệ thống");
    request.setAttribute("activeNav", "configs");
%>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<form method="post" action="${ctx}/admin/configs">
    <div class="split">
        <div>
            <div class="card">
                <div class="card-header">
                    <div class="row"><span style="width:34px;height:34px;border-radius:9px;display:grid;place-items:center;background:var(--primary-soft);color:var(--primary)"><svg class="icon"><use href="${ico}#i-globe"/></svg></span>
                        <div><h2>Giọng nói (STT / TTS)</h2><p class="muted small">Ngôn ngữ AI đọc câu hỏi và nhận dạng câu trả lời.</p></div></div>
                </div>
                <div class="chips">
                    <label class="chip"><input type="radio" name="speech.language" value="vi-VN" ${cfg['speech.language'] == 'vi-VN' ? 'checked' : ''}><span>Tiếng Việt (vi-VN)</span></label>
                    <label class="chip"><input type="radio" name="speech.language" value="en-US" ${cfg['speech.language'] == 'en-US' ? 'checked' : ''}><span>English (en-US)</span></label>
                </div>
                <button class="btn sm mt" type="button" onclick="previewVoice()"><svg class="icon"><use href="${ico}#i-volume"/></svg>Nghe thử giọng đọc</button>
            </div>

            <div class="card">
                <div class="card-header">
                    <div class="row"><span style="width:34px;height:34px;border-radius:9px;display:grid;place-items:center;background:var(--primary-soft);color:var(--primary)"><svg class="icon"><use href="${ico}#i-bot"/></svg></span>
                        <div><h2>Phỏng vấn AI</h2><p class="muted small">Áp dụng cho các lượt thi bắt đầu sau khi lưu.</p></div></div>
                </div>
                <div class="grid-3">
                    <div>
                        <label class="mt-0" for="mainQ">Số câu hỏi chính</label>
                        <input type="number" id="mainQ" name="interview.main_questions" min="1" max="20" value="${cfg['interview.main_questions']}" required>
                        <div class="hint">1 – 20 câu / lượt thi</div>
                    </div>
                    <div>
                        <label class="mt-0" for="answerTime">Thời gian trả lời (giây)</label>
                        <input type="number" id="answerTime" name="interview.answer_time" min="15" max="900" value="${cfg['interview.answer_time']}" required>
                        <div class="hint">15 – 900 giây / câu, hết giờ tự nộp</div>
                    </div>
                    <div>
                        <label class="mt-0" for="maxFollow">Câu hỏi xoáy tối đa</label>
                        <input type="number" id="maxFollow" name="interview.max_followups" min="0" max="5" value="${cfg['interview.max_followups']}" required>
                        <div class="hint">0 – 5 câu cho mỗi câu chính</div>
                    </div>
                </div>
            </div>
        </div>

        <aside class="sticky">
            <div class="card">
                <h2>Tóm tắt một lượt thi</h2>
                <dl class="kv mt">
                    <dt>Câu hỏi chính</dt><dd id="sumMain"></dd>
                    <dt>Tối đa câu hỏi</dt><dd id="sumTotal"></dd>
                    <dt>Thời gian tối đa</dt><dd id="sumTime"></dd>
                </dl>
                <div class="alert info mt small"><svg class="icon"><use href="${ico}#i-shield"/></svg>
                    <span>AI chỉ hỗ trợ đặt câu hỏi. Giảng viên luôn là người chốt điểm cuối cùng.</span></div>
                <button class="btn primary block" type="submit"><svg class="icon"><use href="${ico}#i-check"/></svg>Lưu cấu hình</button>
            </div>
        </aside>
    </div>
</form>
<script>
    function v(id) { return parseInt(document.getElementById(id).value || '0', 10); }
    function summarize() {
        var main = v('mainQ'), follow = v('maxFollow'), t = v('answerTime');
        var total = main * (1 + follow);
        document.getElementById('sumMain').textContent = main + ' câu';
        document.getElementById('sumTotal').textContent = total + ' câu (gồm hỏi xoáy)';
        document.getElementById('sumTime').textContent = Math.ceil(total * t / 60) + ' phút';
    }
    ['mainQ', 'maxFollow', 'answerTime'].forEach(function (id) { document.getElementById(id).addEventListener('input', summarize); });
    summarize();
    function previewVoice() {
        if (!('speechSynthesis' in window)) { alert('Trình duyệt chưa hỗ trợ đọc văn bản.'); return; }
        var lang = document.querySelector('input[name="speech.language"]:checked').value;
        var u = new SpeechSynthesisUtterance(lang === 'vi-VN'
            ? 'Xin chào, tôi là trợ lý vấn đáp. Hãy trình bày vòng đời của một Servlet.'
            : 'Hello, I am your oral exam assistant. Please describe the servlet lifecycle.');
        u.lang = lang;
        speechSynthesis.cancel(); speechSynthesis.speak(u);
    }
</script>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
