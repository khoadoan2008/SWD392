<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%
    request.setAttribute("pageSection", "Lõi phỏng vấn AI");
    request.setAttribute("activeNav", "interview");
%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" scope="request" value="${empty turn ? 'Kết quả phỏng vấn' : 'Đang phỏng vấn'}"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<c:choose>
<%-- =========================== ĐANG THI =========================== --%>
<c:when test="${not empty turn}">
<div class="split">
    <div>
        <div class="card flush" style="overflow:hidden">
            <div class="stage">
                <div class="orb" id="orb"><svg class="icon"><use href="${ico}#i-bot"/></svg></div>
                <div class="state" id="stateLabel">${turn.followUp ? 'AI hỏi xoáy' : 'AI đang hỏi'}</div>

                <c:if test="${turn.followUp && not empty prevTurn}">
                    <div class="followup-note">
                        <svg class="icon"><use href="${ico}#i-corner"/></svg>
                        <div><b>Dựa trên câu trả lời trước của bạn:</b>
                            <div class="muted clamp-2">“<c:out value="${empty prevTurn.transcript ? '(không có câu trả lời)' : prevTurn.transcript}"/>”</div></div>
                    </div>
                </c:if>

                <div class="question" id="question" data-text="${fn:escapeXml(turn.questionText)}"><c:out value="${turn.questionText}"/></div>
                <div class="wave" id="wave"><i></i><i></i><i></i><i></i><i></i><i></i><i></i><i></i><i></i><i></i><i></i><i></i><i></i><i></i></div>
                <div class="controls">
                    <button class="btn" type="button" id="btnSpeak"><svg class="icon"><use href="${ico}#i-volume"/></svg>Nghe lại câu hỏi</button>
                    <button class="btn primary mic-btn" type="button" id="btnMic"><svg class="icon"><use href="${ico}#i-mic"/></svg><span>Bắt đầu trả lời</span></button>
                </div>
                <div class="hint" id="micStatus" style="margin-top:10px"></div>
            </div>

            <form id="answerForm" method="post" action="${ctx}/student/interview" style="padding:20px;border-top:1px solid var(--border)">
                <input type="hidden" name="action" value="answer">
                <input type="hidden" name="id" value="${se.id}">
                <input type="hidden" name="turnId" value="${turn.id}">
                <label class="mt-0" for="transcript">Câu trả lời của bạn <span class="muted" style="font-weight:400">(văn bản nhận dạng từ giọng nói — có thể chỉnh sửa)</span></label>
                <textarea id="transcript" name="transcript" style="min-height:130px" placeholder="Bấm “Bắt đầu trả lời” và nói, hoặc gõ câu trả lời tại đây..."></textarea>
                <div class="row between mt">
                    <span class="hint" style="margin:0">Hết giờ hệ thống sẽ tự động gửi câu trả lời.</span>
                    <button class="btn primary" type="submit"><svg class="icon"><use href="${ico}#i-send"/></svg>Gửi câu trả lời</button>
                </div>
            </form>
        </div>
    </div>

    <aside class="sticky">
        <div class="card">
            <div class="row between"><span class="muted small">Thời gian còn lại</span><span class="badge plain primary"><c:out value="${se.subjectCode}"/></span></div>
            <div class="timer" id="timer">--:--</div>
            <div class="progress" id="timeBar"><div style="width:100%"></div></div>
        </div>
        <div class="card">
            <h2>Tiến độ</h2>
            <ul class="steps mt">
                <c:forEach items="${se.turns}" var="t">
                    <li class="${t.followUp ? 'sub' : ''} ${not empty t.answeredAt ? 'done' : ''} ${t.id == turn.id ? 'current' : ''}">
                        <span class="dot">
                            <c:choose><c:when test="${not empty t.answeredAt}">✓</c:when><c:when test="${t.followUp}">↳</c:when><c:otherwise>${t.mainIndex}</c:otherwise></c:choose>
                        </span>
                        <c:choose><c:when test="${t.followUp}">Câu hỏi xoáy ${t.followupIndex}</c:when><c:otherwise>Câu hỏi ${t.mainIndex}</c:otherwise></c:choose>
                    </li>
                </c:forEach>
            </ul>
            <div class="hint">AI có thể hỏi xoáy tối đa ${se.maxFollowups} câu cho mỗi câu chính.</div>
            <div class="divider"></div>
            <form method="post" action="${ctx}/student/interview" onsubmit="return confirm('Bạn chắc chắn muốn kết thúc sớm? Các câu chưa trả lời sẽ không được tính.')">
                <input type="hidden" name="action" value="abort"><input type="hidden" name="id" value="${se.id}">
                <button class="btn danger block sm" type="submit">Kết thúc sớm</button>
            </form>
        </div>
    </aside>
</div>

<script>
(function () {
    var lang = '${speechLang}', limit = ${se.answerTimeLimitSec}, remaining = ${remainingSec};
    var form = document.getElementById('answerForm'), textarea = document.getElementById('transcript');
    var orb = document.getElementById('orb'), wave = document.getElementById('wave'), stateLabel = document.getElementById('stateLabel');
    var btnMic = document.getElementById('btnMic'), micStatus = document.getElementById('micStatus');
    var questionText = document.getElementById('question').getAttribute('data-text');
    var defaultState = stateLabel.textContent, submitted = false;

    function setState(mode) { // speaking | listening | idle
        orb.className = 'orb' + (mode === 'idle' ? '' : ' ' + mode);
        wave.className = 'wave' + (mode === 'idle' ? '' : ' on');
        stateLabel.textContent = mode === 'speaking' ? defaultState : (mode === 'listening' ? 'Đang nghe bạn trả lời' : 'Mời bạn trả lời');
    }

    // ---- Text-to-speech: đọc câu hỏi ----
    function speak() {
        if (!('speechSynthesis' in window)) { setState('idle'); return; }
        speechSynthesis.cancel();
        var u = new SpeechSynthesisUtterance(questionText);
        u.lang = lang;
        u.onstart = function () { setState('speaking'); };
        u.onend = u.onerror = function () { if (!listening) setState('idle'); };
        speechSynthesis.speak(u);
    }
    document.getElementById('btnSpeak').onclick = speak;

    // ---- Speech-to-text ----
    var Recognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    var recognition = null, listening = false, baseText = '';
    function stopListening() {
        listening = false;
        if (recognition) recognition.stop();
        btnMic.classList.remove('recording');
        btnMic.querySelector('span').textContent = 'Tiếp tục trả lời';
        micStatus.textContent = '';
        setState('idle');
    }
    if (!Recognition) {
        btnMic.disabled = true;
        micStatus.textContent = 'Trình duyệt chưa hỗ trợ nhận giọng nói — bạn vui lòng gõ câu trả lời.';
    } else {
        recognition = new Recognition();
        recognition.lang = lang; recognition.continuous = true; recognition.interimResults = true;
        recognition.onresult = function (e) {
            var finalText = '', interim = '';
            for (var i = 0; i < e.results.length; i++) {
                if (e.results[i].isFinal) finalText += e.results[i][0].transcript + ' ';
                else interim += e.results[i][0].transcript;
            }
            textarea.value = (baseText + ' ' + finalText + interim).trim();
        };
        recognition.onend = function () { if (listening) recognition.start(); };
        recognition.onerror = function (e) { if (e.error === 'not-allowed') { stopListening(); micStatus.textContent = 'Chưa có quyền dùng micro — bạn có thể gõ câu trả lời.'; } };
        btnMic.onclick = function () {
            if (listening) { stopListening(); return; }
            if ('speechSynthesis' in window) speechSynthesis.cancel();
            baseText = textarea.value; listening = true;
            recognition.start();
            btnMic.classList.add('recording');
            btnMic.querySelector('span').textContent = 'Dừng ghi';
            micStatus.textContent = 'Đang nghe... nói rõ ràng, tốc độ vừa phải.';
            setState('listening');
        };
    }

    // ---- Đếm ngược, hết giờ tự nộp ----
    var timerEl = document.getElementById('timer'), bar = document.getElementById('timeBar');
    form.onsubmit = function () { submitted = true; listening = false; if (recognition) recognition.stop(); };
    function tick() {
        var m = Math.floor(remaining / 60), s = remaining % 60;
        timerEl.textContent = m + ':' + (s < 10 ? '0' : '') + s;
        var low = remaining <= 15;
        timerEl.className = 'timer' + (low ? ' low' : '');
        bar.className = 'progress' + (low ? ' low' : '');
        bar.firstElementChild.style.width = Math.max(0, remaining / limit * 100) + '%';
        if (remaining <= 0) { if (!submitted) { form.onsubmit(); form.submit(); } return; }
        remaining--;
        setTimeout(tick, 1000);
    }
    tick();
    speak();
})();
</script>
</c:when>

<%-- =========================== ĐÃ KẾT THÚC =========================== --%>
<c:otherwise>
<div class="card" style="text-align:center;padding:40px 24px">
    <div class="success-mark"><svg class="icon"><use href="${ico}#i-check-circle"/></svg></div>
    <h2 style="font-size:22px">${se.status == 'COMPLETED' ? 'Bài thi đã được ghi nhận an toàn' : 'Lượt thi đã kết thúc sớm'}</h2>
    <p class="muted mt">Cảm ơn bạn đã hoàn thành phần vấn đáp môn <b><c:out value="${se.subjectCode}"/></b>.
        Giảng viên sẽ xem lại transcript và công bố điểm sau.</p>
    <div class="grid-3" style="max-width:560px;margin:24px auto 0">
        <div class="stat" style="display:block"><div class="label">Câu đã trả lời</div><div class="value">${se.answeredCount}/${fn:length(se.turns)}</div></div>
        <div class="stat" style="display:block"><div class="label">Câu hỏi xoáy</div><div class="value">${se.followUpCount}</div></div>
        <div class="stat" style="display:block"><div class="label">Thời gian trả lời</div><div class="value">${se.totalResponseSec}s</div></div>
    </div>
    <div class="row" style="justify-content:center;margin-top:24px">
        <a class="btn primary" href="${ctx}/student/interview"><svg class="icon"><use href="${ico}#i-arrow-left"/></svg>Về trang phỏng vấn</a>
    </div>
</div>
<div class="card">
    <div class="card-header"><h2>Nội dung buổi vấn đáp</h2>
        <span class="muted small"><fmt:formatDate value="${se.startedAt}" pattern="HH:mm dd/MM/yyyy"/></span></div>
    <%@ include file="/WEB-INF/views/common/transcript.jspf" %>
</div>
</c:otherwise>
</c:choose>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
