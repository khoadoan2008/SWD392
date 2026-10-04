<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%
    request.setAttribute("pageTitle", "Phỏng vấn AI");
    request.setAttribute("pageSection", "Lõi phỏng vấn AI");
    request.setAttribute("activeNav", "interview");
%>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="hero">
    <span class="badge">Vấn đáp cùng AI</span>
    <h2 style="margin-top:12px">Sẵn sàng cho bài thi vấn đáp?</h2>
    <p>AI sẽ đọc từng câu hỏi bằng giọng nói. Bạn trả lời bằng micro, hệ thống chuyển giọng nói thành văn bản.
       Nếu câu trả lời chưa rõ, AI có thể hỏi xoáy thêm — hãy bình tĩnh và trình bày đầy đủ ý nhé!</p>
</div>

<form method="post" action="${ctx}/student/interview">
    <input type="hidden" name="action" value="start">
    <div class="split">
        <div class="card">
            <h2>1. Chọn môn thi</h2>
            <div class="chips mt">
                <c:forEach items="${subjects}" var="s" varStatus="st">
                    <label class="chip" style="margin:0"><input type="radio" name="subjectId" value="${s.id}" ${st.first ? 'checked' : ''} required>
                        <span><b><c:out value="${s.code}"/></b>&nbsp;<c:out value="${s.name}"/></span></label>
                </c:forEach>
                <c:if test="${empty subjects}"><p class="muted">Hiện chưa có môn nào mở thi.</p></c:if>
            </div>

            <h2 class="mt" style="margin-top:24px">2. Lưu ý trước khi bắt đầu</h2>
            <ul class="list-steps mt">
                <li><svg class="icon"><use href="${ico}#i-check-circle"/></svg><span>Ngồi ở nơi yên tĩnh, dùng Chrome hoặc Edge để nhận giọng nói tốt nhất.</span></li>
                <li><svg class="icon"><use href="${ico}#i-clock"/></svg><span>Mỗi câu có thời gian giới hạn, hết giờ hệ thống tự động nộp câu trả lời.</span></li>
                <li><svg class="icon"><use href="${ico}#i-edit"/></svg><span>Bạn có thể chỉnh sửa văn bản nhận dạng trước khi gửi nếu máy nghe nhầm.</span></li>
                <li><svg class="icon"><use href="${ico}#i-shield"/></svg><span>Toàn bộ câu hỏi và câu trả lời được lưu lại để giảng viên đánh giá.</span></li>
            </ul>
        </div>

        <aside class="sticky">
            <div class="card">
                <h2>3. Kiểm tra thiết bị</h2>
                <span class="label">Micro</span>
                <div class="meter"><div id="micLevel"></div></div>
                <div class="hint" id="micHint">Bấm "Kiểm tra micro" và nói thử vài câu.</div>
                <div class="row mt">
                    <button class="btn sm" type="button" id="btnMicTest"><svg class="icon"><use href="${ico}#i-mic"/></svg>Kiểm tra micro</button>
                    <button class="btn sm" type="button" id="btnSpeakerTest"><svg class="icon"><use href="${ico}#i-volume"/></svg>Kiểm tra loa</button>
                </div>
                <div class="divider"></div>
                <button class="btn primary lg block" type="submit" ${empty subjects ? 'disabled' : ''}>
                    <svg class="icon"><use href="${ico}#i-play"/></svg>Bắt đầu phỏng vấn</button>
                <p class="hint" style="text-align:center">Nếu đang có lượt thi dở, hệ thống sẽ tiếp tục lượt đó.</p>
            </div>
        </aside>
    </div>
</form>

<div class="card flush">
    <div class="card-header"><h2>Lịch sử phỏng vấn</h2></div>
    <div class="table-wrap" style="margin-top:12px">
        <table>
            <thead><tr><th>Lượt</th><th>Môn</th><th>Bắt đầu</th><th>Trạng thái</th><th></th></tr></thead>
            <tbody>
            <c:forEach items="${sessions}" var="x">
                <tr>
                    <td>#${x.id}</td>
                    <td><span class="badge plain primary"><c:out value="${x.subjectCode}"/></span></td>
                    <td><fmt:formatDate value="${x.startedAt}" pattern="HH:mm dd/MM/yyyy"/></td>
                    <td><span class="badge ${x.status}">${x.status == 'COMPLETED' ? 'Hoàn thành' : (x.status == 'IN_PROGRESS' ? 'Đang thi' : 'Kết thúc sớm')}</span></td>
                    <td class="actions"><a class="btn sm ${x.status == 'IN_PROGRESS' ? 'primary' : ''}" href="${ctx}/student/interview?id=${x.id}">${x.status == 'IN_PROGRESS' ? 'Tiếp tục' : 'Xem lại'}</a></td>
                </tr>
            </c:forEach>
            <c:if test="${empty sessions}">
                <tr><td colspan="5"><div class="empty"><svg class="icon"><use href="${ico}#i-mic"/></svg><div>Bạn chưa có lượt phỏng vấn nào.</div></div></td></tr>
            </c:if>
            </tbody>
        </table>
    </div>
</div>

<script>
(function () {
    var level = document.getElementById('micLevel'), hint = document.getElementById('micHint');
    document.getElementById('btnMicTest').onclick = function () {
        if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
            hint.textContent = 'Trình duyệt không hỗ trợ truy cập micro.'; return;
        }
        navigator.mediaDevices.getUserMedia({ audio: true }).then(function (stream) {
            var AC = window.AudioContext || window.webkitAudioContext, ac = new AC();
            var analyser = ac.createAnalyser(); analyser.fftSize = 256;
            ac.createMediaStreamSource(stream).connect(analyser);
            var data = new Uint8Array(analyser.frequencyBinCount), peak = 0, start = Date.now();
            hint.textContent = 'Đang nghe... hãy nói thử.';
            (function loop() {
                analyser.getByteFrequencyData(data);
                var sum = 0; for (var i = 0; i < data.length; i++) sum += data[i];
                var v = Math.min(100, sum / data.length * 2); peak = Math.max(peak, v);
                level.style.width = v + '%';
                if (Date.now() - start < 5000) { requestAnimationFrame(loop); return; }
                stream.getTracks().forEach(function (t) { t.stop(); }); ac.close(); level.style.width = '0';
                hint.textContent = peak > 15 ? '✅ Micro hoạt động tốt.' : '⚠️ Âm thanh khá nhỏ, hãy kiểm tra lại micro.';
            })();
        }).catch(function () { hint.textContent = 'Bạn chưa cho phép truy cập micro. Vẫn có thể gõ câu trả lời.'; });
    };
    document.getElementById('btnSpeakerTest').onclick = function () {
        if (!('speechSynthesis' in window)) return;
        var u = new SpeechSynthesisUtterance('Xin chào, bạn có nghe rõ tôi nói không?'); u.lang = 'vi-VN';
        speechSynthesis.cancel(); speechSynthesis.speak(u);
    };
})();
</script>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
