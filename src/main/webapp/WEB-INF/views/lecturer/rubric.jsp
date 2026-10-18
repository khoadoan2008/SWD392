<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<%
    request.setAttribute("pageTitle", "Rubric: Phân tích lựa chọn giao thức");
    request.setAttribute("pageSection", "Trình chỉnh sửa rubric");
    request.setAttribute("activeNav", "rubrics");
%>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<!-- Top toolbar & breadcrumbs -->
<div class="rubric-top-nav">
    <div>
        <div class="row" style="gap:8px;margin-bottom:4px">
            <span class="badge plain primary">RB-TCP-04</span>
            <span class="muted small">Mạng máy tính (CNTT 301)</span>
            <span class="muted small">·</span>
            <a href="${ctx}/lecturer/questions?view=rubric&amp;mode=list" class="small muted" style="text-decoration:underline">Xem danh sách rubric</a>
        </div>
        <p class="muted small" style="margin:0">Thiết lập tiêu chí và mức đạt để AI chấm gợi ý nhất quán.</p>
    </div>
    <div class="row">
        <button type="button" class="btn" id="btnOpenPreview">
            <svg class="icon"><use href="${ico}#i-eye"/></svg>Xem trước
        </button>
        <button type="button" class="btn primary" id="btnSaveRubric">
            <svg class="icon"><use href="${ico}#i-save"/></svg>Lưu rubric
        </button>
    </div>
</div>

<!-- Alert toast placeholder -->
<div id="saveToast" class="alert success" style="display:none">
    <svg class="icon"><use href="${ico}#i-check-circle"/></svg>
    <span><b>Thành công!</b> Đã lưu cấu hình rubric và cập nhật thang điểm gợi ý cho AI.</span>
</div>

<div class="split wide-side">
    <!-- Cột trái: Form & Danh sách tiêu chí -->
    <div>
        <div class="card">
            <!-- Thông tin chung của Rubric -->
            <div class="rubric-header-grid">
                <div>
                    <label class="mt-0" for="rubricTitleInput">Tên rubric</label>
                    <input type="text" id="rubricTitleInput" value="Phân tích lựa chọn giao thức truyền tải" placeholder="Nhập tên rubric...">
                </div>
                <div>
                    <label class="mt-0" for="rubricSubjectSelect">Môn học</label>
                    <select id="rubricSubjectSelect">
                        <option value="NET301" selected>Mạng máy tính</option>
                        <c:forEach items="${subjects}" var="s">
                            <option value="${s.id}"><c:out value="${s.code} · ${s.name}"/></option>
                        </c:forEach>
                    </select>
                </div>
                <div>
                    <label class="mt-0" for="rubricTotalSelect">Tổng điểm</label>
                    <select id="rubricTotalSelect">
                        <option value="10" selected>10 điểm</option>
                        <option value="5">5 điểm</option>
                        <option value="20">20 điểm</option>
                        <option value="100">100 điểm</option>
                    </select>
                </div>
            </div>

            <!-- Danh sách tiêu chí đánh giá (Figma Screen 4) -->
            <div id="criteriaContainer">
                <!-- Tiêu chí 1: Lập luận kỹ thuật -->
                <div class="rubric-criterion" data-criterion-id="1">
                    <div class="rubric-criterion-head">
                        <div class="rubric-criterion-title">
                            <span class="c-index">1.</span>
                            <input type="text" class="c-title-input" value="Lập luận kỹ thuật" aria-label="Tên tiêu chí 1">
                        </div>
                        <div class="row" style="gap:8px">
                            <span class="rubric-badge-score"><span class="c-score-val">4</span> điểm</span>
                            <button type="button" class="btn sm ghost icon-btn danger btn-del-criterion" title="Xoá tiêu chí">
                                <svg class="icon"><use href="${ico}#i-trash"/></svg>
                            </button>
                        </div>
                    </div>
                    <div class="rubric-levels-grid">
                        <div class="rubric-level-card">
                            <span class="rubric-level-pill excellent">Xuất sắc - 100%</span>
                            <textarea class="rubric-level-desc" aria-label="Mô tả mức xuất sắc">Phân tích đầy đủ độ trễ, tin cậy và overhead.</textarea>
                        </div>
                        <div class="rubric-level-card">
                            <span class="rubric-level-pill good">Đạt - 70%</span>
                            <textarea class="rubric-level-desc" aria-label="Mô tả mức đạt">Hiểu đúng ít nhất hai yếu tố đơn vị.</textarea>
                        </div>
                        <div class="rubric-level-card">
                            <span class="rubric-level-pill improve">Cần cải thiện - 30%</span>
                            <textarea class="rubric-level-desc" aria-label="Mô tả mức cần cải thiện">Lập luận chung, thiếu cơ sở kỹ thuật.</textarea>
                        </div>
                    </div>
                </div>

                <!-- Tiêu chí 2: Liên hệ ngữ cảnh -->
                <div class="rubric-criterion" data-criterion-id="2">
                    <div class="rubric-criterion-head">
                        <div class="rubric-criterion-title">
                            <span class="c-index">2.</span>
                            <input type="text" class="c-title-input" value="Liên hệ ngữ cảnh" aria-label="Tên tiêu chí 2">
                        </div>
                        <div class="row" style="gap:8px">
                            <span class="rubric-badge-score"><span class="c-score-val">3</span> điểm</span>
                            <button type="button" class="btn sm ghost icon-btn danger btn-del-criterion" title="Xoá tiêu chí">
                                <svg class="icon"><use href="${ico}#i-trash"/></svg>
                            </button>
                        </div>
                    </div>
                    <div class="rubric-levels-grid">
                        <div class="rubric-level-card">
                            <span class="rubric-level-pill excellent">Xuất sắc - 100%</span>
                            <textarea class="rubric-level-desc" aria-label="Mô tả mức xuất sắc">Ví dụ phù hợp với giả thiết đưa ra.</textarea>
                        </div>
                        <div class="rubric-level-card">
                            <span class="rubric-level-pill good">Đạt - 70%</span>
                            <textarea class="rubric-level-desc" aria-label="Mô tả mức đạt">Ví dụ phù hợp nhưng giải thích còn ngắn.</textarea>
                        </div>
                        <div class="rubric-level-card">
                            <span class="rubric-level-pill improve">Cần cải thiện - 30%</span>
                            <textarea class="rubric-level-desc" aria-label="Mô tả mức cần cải thiện">Ví dụ chưa khớp với giao thức đã chọn.</textarea>
                        </div>
                    </div>
                </div>

                <!-- Tiêu chí 3: Tính nhất quán -->
                <div class="rubric-criterion" data-criterion-id="3">
                    <div class="rubric-criterion-head">
                        <div class="rubric-criterion-title">
                            <span class="c-index">3.</span>
                            <input type="text" class="c-title-input" value="Tính nhất quán" aria-label="Tên tiêu chí 3">
                        </div>
                        <div class="row" style="gap:8px">
                            <span class="rubric-badge-score"><span class="c-score-val">3</span> điểm</span>
                            <button type="button" class="btn sm ghost icon-btn danger btn-del-criterion" title="Xoá tiêu chí">
                                <svg class="icon"><use href="${ico}#i-trash"/></svg>
                            </button>
                        </div>
                    </div>
                    <div class="rubric-levels-grid">
                        <div class="rubric-level-card">
                            <span class="rubric-level-pill excellent">Xuất sắc - 100%</span>
                            <textarea class="rubric-level-desc" aria-label="Mô tả mức xuất sắc">Kết luận nhất quán với toàn bộ dữ kiện.</textarea>
                        </div>
                        <div class="rubric-level-card">
                            <span class="rubric-level-pill good">Đạt - 70%</span>
                            <textarea class="rubric-level-desc" aria-label="Mô tả mức đạt">Kết luận đúng, còn một điểm chưa rõ.</textarea>
                        </div>
                        <div class="rubric-level-card">
                            <span class="rubric-level-pill improve">Cần cải thiện - 30%</span>
                            <textarea class="rubric-level-desc" aria-label="Mô tả mức cần cải thiện">Kết luận mâu thuẫn với phần phân tích.</textarea>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Nút thêm tiêu chí -->
            <button type="button" class="rubric-add-btn" id="btnAddCriterion">
                <svg class="icon"><use href="${ico}#i-plus"/></svg>Thêm tiêu chí
            </button>
        </div>
    </div>

    <!-- Cột phải: Hướng dẫn AI & Transcript mẫu -->
    <aside class="sticky">
        <div class="ai-rubric-card">
            <div class="ai-header">
                <div class="ai-icon"><svg class="icon"><use href="${ico}#i-sparkles"/></svg></div>
                <div>
                    <h2>Cách AI chấm gợi ý</h2>
                    <span class="muted small">3 tiêu chí · Đối chiếu thời gian thực</span>
                </div>
            </div>
            <p class="small text-2" style="margin-bottom:12px;line-height:1.5">
                AI đối chiếu transcript với từng tiêu chí, trích bằng chứng rồi đề xuất mức đạt. Giảng viên luôn là người quyết định cuối cùng.
            </p>

            <div class="divider"></div>

            <div class="row between" style="margin-bottom:6px">
                <span class="small muted">Transcript mẫu</span>
                <span class="small badge plain">Học viên: Trần An</span>
            </div>
            <div class="transcript-quote-box" id="sampleQuoteText">
                “Em chọn <mark style="background:#fef08a;padding:0 2px">UDP</mark> cho cuộc gọi video để giảm thời gian bắt tay. Mất một vài gói âm thanh thường ít ảnh hưởng hơn việc chờ truyền lại…”
            </div>

            <div class="ai-eval-result" id="aiResultBox">
                <div class="ai-eval-result-head">
                    <strong id="aiCriterionName">Lập luận kỹ thuật</strong>
                    <span class="rubric-level-pill good" id="aiScoreBadge">Đạt - 2.8/4</span>
                </div>
                <div class="ai-evidence-text" id="aiEvidenceText">
                    Bằng chứng: <b>“giảm thời gian bắt tay”</b>, <b>“ít ảnh hưởng hơn việc chờ truyền lại…”</b>
                </div>
            </div>

            <div class="row between">
                <button type="button" class="btn-retry-ai" id="btnSimulateAi">
                    <svg class="icon"><use href="${ico}#i-sparkles"/></svg>Thử lại điểm gợi ý
                </button>
                <span class="small muted" id="simStatus">Đã sẵn sàng</span>
            </div>
        </div>

        <!-- Card liên kết câu hỏi -->
        <div class="card">
            <div class="card-header" style="margin-bottom:8px">
                <h2>Câu hỏi áp dụng</h2>
                <span class="badge plain AI">AI sinh</span>
            </div>
            <p class="small cell-main" style="margin-bottom:8px">Phân tích khác biệt giữa TCP và UDP trong truyền thời gian thực.</p>
            <div class="row between small">
                <span class="muted">Mã: NHCH-1082</span>
                <a href="${ctx}/lecturer/questions?action=edit&amp;id=1082" class="small">Chỉnh sửa câu hỏi &rarr;</a>
            </div>
        </div>
    </aside>
</div>

<!-- Modal Xem trước Rubric -->
<div class="modal-overlay" id="previewModal">
    <div class="modal-window">
        <div class="modal-head">
            <div>
                <h2>Bảng ma trận Rubric</h2>
                <p class="muted small" id="modalSubhead">Phân tích lựa chọn giao thức truyền tải · 10 điểm</p>
            </div>
            <button type="button" class="btn sm ghost icon-btn" id="btnClosePreview"><svg class="icon"><use href="${ico}#i-x"/></svg></button>
        </div>
        <div class="modal-body">
            <table class="matrix-table" id="matrixTable">
                <thead>
                    <tr>
                        <th style="width:25%">Tiêu chí</th>
                        <th style="width:10%">Điểm</th>
                        <th style="width:22%;color:#15803d">Xuất sắc (100%)</th>
                        <th style="width:22%;color:#1d4ed8">Đạt (70%)</th>
                        <th style="width:21%;color:#b45309">Cần cải thiện (30%)</th>
                    </tr>
                </thead>
                <tbody id="matrixBody"></tbody>
            </table>
        </div>
        <div class="modal-foot">
            <button type="button" class="btn" id="btnClosePreviewFoot">Đóng</button>
        </div>
    </div>
</div>

<!-- Client-side script cho Rubric Editor -->
<script>
(function () {
    var container = document.getElementById('criteriaContainer');
    var btnAdd = document.getElementById('btnAddCriterion');
    var btnSave = document.getElementById('btnSaveRubric');
    var toast = document.getElementById('saveToast');
    var btnPreview = document.getElementById('btnOpenPreview');
    var modal = document.getElementById('previewModal');
    var btnClose = document.getElementById('btnClosePreview');
    var btnCloseFoot = document.getElementById('btnClosePreviewFoot');
    var matrixBody = document.getElementById('matrixBody');
    var btnSimulate = document.getElementById('btnSimulateAi');
    var aiResultBox = document.getElementById('aiResultBox');
    var simStatus = document.getElementById('simStatus');

    function reindex() {
        var items = container.querySelectorAll('.rubric-criterion');
        items.forEach(function (el, idx) {
            var num = idx + 1;
            el.setAttribute('data-criterion-id', num);
            var numSpan = el.querySelector('.c-index');
            if (numSpan) numSpan.textContent = num + '.';
        });
    }

    // Xoá tiêu chí
    container.addEventListener('click', function (e) {
        var delBtn = e.target.closest('.btn-del-criterion');
        if (delBtn) {
            var item = delBtn.closest('.rubric-criterion');
            if (container.querySelectorAll('.rubric-criterion').length <= 1) {
                alert('Rubric phải có ít nhất 1 tiêu chí.');
                return;
            }
            if (confirm('Bạn có chắc muốn xoá tiêu chí này?')) {
                item.remove();
                reindex();
            }
        }
    });

    // Thêm tiêu chí mới
    btnAdd.addEventListener('click', function () {
        var count = container.querySelectorAll('.rubric-criterion').length + 1;
        var div = document.createElement('div');
        div.className = 'rubric-criterion';
        div.setAttribute('data-criterion-id', count);
        div.innerHTML =
            '<div class="rubric-criterion-head">' +
                '<div class="rubric-criterion-title">' +
                    '<span class="c-index">' + count + '.</span>' +
                    '<input type="text" class="c-title-input" value="Tiêu chí mới ' + count + '" aria-label="Tên tiêu chí ' + count + '">' +
                '</div>' +
                '<div class="row" style="gap:8px">' +
                    '<span class="rubric-badge-score"><span class="c-score-val">3</span> điểm</span>' +
                    '<button type="button" class="btn sm ghost icon-btn danger btn-del-criterion" title="Xoá tiêu chí">' +
                        '<svg class="icon"><use href="${ico}#i-trash"/></svg>' +
                    '</button>' +
                '</div>' +
            '</div>' +
            '<div class="rubric-levels-grid">' +
                '<div class="rubric-level-card">' +
                    '<span class="rubric-level-pill excellent">Xuất sắc - 100%</span>' +
                    '<textarea class="rubric-level-desc">Trình bày chính xác, sâu sắc và đầy đủ luận cứ.</textarea>' +
                '</div>' +
                '<div class="rubric-level-card">' +
                    '<span class="rubric-level-pill good">Đạt - 70%</span>' +
                    '<textarea class="rubric-level-desc">Nắm được trọng tâm, trả lời đúng phần lớn yêu cầu.</textarea>' +
                '</div>' +
                '<div class="rubric-level-card">' +
                    '<span class="rubric-level-pill improve">Cần cải thiện - 30%</span>' +
                    '<textarea class="rubric-level-desc">Trả lời sơ sài, thiếu dẫn chứng hoặc chưa chính xác.</textarea>' +
                '</div>' +
            '</div>';
        container.appendChild(div);
        div.scrollIntoView({ behavior: 'smooth', block: 'center' });
    });

    // Lưu Rubric
    btnSave.addEventListener('click', function () {
        btnSave.disabled = true;
        btnSave.innerHTML = '<svg class="icon"><use href="${ico}#i-sparkles"/></svg>Đang lưu...';
        setTimeout(function () {
            btnSave.disabled = false;
            btnSave.innerHTML = '<svg class="icon"><use href="${ico}#i-save"/></svg>Lưu rubric';
            toast.style.display = 'flex';
            toast.scrollIntoView({ behavior: 'smooth' });
            setTimeout(function () {
                toast.style.display = 'none';
            }, 4000);
        }, 600);
    });

    // Xem trước modal
    function openPreview() {
        var rowsHtml = '';
        var items = container.querySelectorAll('.rubric-criterion');
        items.forEach(function (el) {
            var title = el.querySelector('.c-title-input').value;
            var score = el.querySelector('.c-score-val').textContent;
            var descs = el.querySelectorAll('.rubric-level-desc');
            var ex = descs[0] ? descs[0].value : '';
            var gd = descs[1] ? descs[1].value : '';
            var im = descs[2] ? descs[2].value : '';

            rowsHtml += '<tr>' +
                '<td><b>' + title + '</b></td>' +
                '<td><span class="badge plain info">' + score + ' điểm</span></td>' +
                '<td>' + ex + '</td>' +
                '<td>' + gd + '</td>' +
                '<td>' + im + '</td>' +
            '</tr>';
        });
        matrixBody.innerHTML = rowsHtml;
        modal.classList.add('open');
    }

    function closePreview() {
        modal.classList.remove('open');
    }

    btnPreview.addEventListener('click', openPreview);
    btnClose.addEventListener('click', closePreview);
    btnCloseFoot.addEventListener('click', closePreview);
    modal.addEventListener('click', function (e) {
        if (e.target === modal) closePreview();
    });

    // Thử lại AI gợi ý
    var simSamples = [
        {
            criterion: "Lập luận kỹ thuật",
            score: "Đạt - 2.8/4",
            evidence: "Bằng chứng: <b>“giảm thời gian bắt tay”</b>, <b>“ít ảnh hưởng hơn việc chờ truyền lại…”</b>"
        },
        {
            criterion: "Liên hệ ngữ cảnh",
            score: "Xuất sắc - 3.0/3",
            evidence: "Bằng chứng: <b>“cuộc gọi video”</b>, <b>“giao thức streaming thời gian thực”</b>"
        },
        {
            criterion: "Tính nhất quán",
            score: "Đạt - 2.5/3",
            evidence: "Bằng chứng: kết luận lựa chọn <b>UDP</b> thống nhất xuyên suốt câu trả lời."
        }
    ];
    var simIdx = 0;

    btnSimulate.addEventListener('click', function () {
        simStatus.textContent = 'AI đang phân tích...';
        btnSimulate.disabled = true;
        aiResultBox.style.opacity = '0.4';

        setTimeout(function () {
            simIdx = (simIdx + 1) % simSamples.length;
            var s = simSamples[simIdx];
            document.getElementById('aiCriterionName').textContent = s.criterion;
            document.getElementById('aiScoreBadge').textContent = s.score;
            document.getElementById('aiEvidenceText').innerHTML = s.evidence;
            aiResultBox.style.opacity = '1';
            btnSimulate.disabled = false;
            simStatus.textContent = 'Hoàn tất';
        }, 500);
    });
})();
</script>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
