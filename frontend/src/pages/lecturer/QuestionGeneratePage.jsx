import React, { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { questionApi } from '../../api/questionApi';
import { 
  BrainCircuit, 
  Sparkles, 
  ArrowLeft, 
  CheckCircle2, 
  AlertCircle, 
  Loader2, 
  ArrowRight
} from 'lucide-react';

const BLOOM_OPTIONS = [
  { value: 'REMEMBER', label: '1. Nhớ (Remember)', desc: 'Nêu định nghĩa, liệt kê thành phần cốt lõi' },
  { value: 'UNDERSTAND', label: '2. Hiểu (Understand)', desc: 'Giải thích nguyên lý và cơ chế hoạt động' },
  { value: 'APPLY', label: '3. Vận dụng (Apply)', desc: 'Áp dụng vào tình huống dự án thực tế' },
  { value: 'ANALYZE', label: '4. Phân tích (Analyze)', desc: 'So sánh ưu nhược điểm, đánh đổi kỹ thuật' },
  { value: 'EVALUATE', label: '5. Đánh giá (Evaluate)', desc: 'Phản biện khi nào không nên dùng và lập luận' },
  { value: 'CREATE', label: '6. Sáng tạo (Create)', desc: 'Đề xuất kiến trúc hoặc giải pháp cải tiến mới' },
];

export default function QuestionGeneratePage() {
  const submitting = useRef(false);
  const [subjects, setSubjects] = useState([]);
  const [subjectId, setSubjectId] = useState('');
  const [topic, setTopic] = useState('');
  const [selectedLevels, setSelectedLevels] = useState(['UNDERSTAND', 'APPLY', 'ANALYZE']);
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [generatedResult, setGeneratedResult] = useState(null);
  const [materialFile, setMaterialFile] = useState(null);
  const [allowAiTransfer, setAllowAiTransfer] = useState(false);

  useEffect(() => {
    let current = true;
    questionApi.getGenerationSubjects().then((res) => {
      if (!current) return;
      if (res.success && res.data.length > 0) {
        setSubjects(res.data);
        setSubjectId(res.data[0].id);
      }
    }).catch((err) => {
      if (current) setErrorMsg(err.response?.data?.message || 'Không tải được môn học được phân công. Kiểm tra backend.');
    });
    return () => { current = false; };
  }, []);

  const toggleLevel = (val) => {
    setSelectedLevels((prev) =>
      prev.includes(val) ? prev.filter((v) => v !== val) : [...prev, val]
    );
  };

  const handleGenerate = async (e) => {
    e.preventDefault();
    if (submitting.current) return;
    setErrorMsg('');
    setGeneratedResult(null);

    if (!subjectId) {
      setErrorMsg('Vui lòng chọn môn học.');
      return;
    }
    if (!topic.trim()) {
      setErrorMsg('Vui lòng nhập chủ đề / nội dung cần sinh câu hỏi.');
      return;
    }
    if (selectedLevels.length === 0) {
      setErrorMsg('Vui lòng chọn ít nhất một mức nhận thức Bloom.');
      return;
    }
    if (!materialFile || materialFile.size > 10 * 1024 * 1024 || !/\.(pdf|docx|txt)$/i.test(materialFile.name)) {
      setErrorMsg('Chọn tệp PDF/DOCX/TXT tối đa 10 MB.');
      return;
    }
    if (!allowAiTransfer) {
      setErrorMsg('Cần đồng ý gửi văn bản học liệu đến Google Gemini.');
      return;
    }

    submitting.current = true;
    setLoading(true);
    try {
      const payload = new FormData();
      payload.append('subjectId', subjectId);
      payload.append('topic', topic.trim());
      selectedLevels.forEach((level) => payload.append('levels', level));
      payload.append('materialFile', materialFile);
      payload.append('allowAiTransfer', 'true');
      const res = await questionApi.generateFromMaterial(payload, crypto.randomUUID());
      if (res.success && res.data) {
        setGeneratedResult(res.data);
      }
    } catch (err) {
      setErrorMsg(err.code === 'ECONNABORTED'
        ? 'Hết thời gian chờ. Kiểm tra danh sách câu hỏi trước khi tạo lại: backend có thể vẫn đang xử lý. Không bấm liên tục.'
        : err.response?.data?.message || err.message || 'Lỗi khi gọi Gemini, chưa nhận được kết quả.');
    } finally {
      submitting.current = false;
      setLoading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      
      {/* Back button */}
      <div>
        <Link
          to="/lecturer/questions"
          className="inline-flex items-center gap-2 text-sm text-slate-400 hover:text-white transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          Quay lại Ngân hàng câu hỏi
        </Link>
      </div>

      {/* Header */}
      <div>
        <div className="flex items-center gap-2 text-amber-400 text-xs font-semibold uppercase tracking-wider mb-1">
          <BrainCircuit className="w-4 h-4" />
          AI Generator · Google Gemini · Học liệu
        </div>
        <h1 className="text-2xl font-bold text-white tracking-tight">Sinh câu hỏi tự động bằng AI</h1>
        <p className="text-sm text-slate-400">
          Tải học liệu, chọn chủ đề và mức Bloom. Gemini sinh câu hỏi, đáp án tham khảo và rubric 10 điểm, lưu ở trạng thái chờ duyệt. Mỗi mức được chọn tạo một câu.
        </p>
      </div>

      {errorMsg && (
        <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-sm flex items-center gap-2.5">
          <AlertCircle className="w-5 h-5 shrink-0 text-rose-400" />
          <span>{errorMsg}</span>
        </div>
      )}

      {/* Generated Result Notice */}
      {generatedResult && (
        <div className="p-6 rounded-2xl bg-gradient-to-r from-emerald-950/60 to-slate-900 border border-emerald-500/30 space-y-4">
          <div className="flex items-center gap-2.5 text-emerald-400">
            <CheckCircle2 className="w-6 h-6" />
            <h3 className="font-bold text-base text-white">
              Đã sinh thành công {generatedResult.length} câu hỏi mới!
            </h3>
          </div>
          <p className="text-sm text-slate-300">
            Các câu hỏi đã được lưu vào hệ thống ở trạng thái <strong className="text-amber-400">Chờ duyệt (PENDING_REVIEW)</strong>. Bạn có thể duyệt từng câu hoặc duyệt hàng loạt ngay bây giờ.
          </p>
          <div className="flex gap-3 pt-2">
            <Link
              to="/lecturer/questions?status=PENDING_REVIEW"
              className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-sm shadow-lg shadow-emerald-600/30 transition-all"
            >
              Mở giao diện Duyệt AI ngay
              <ArrowRight className="w-4 h-4" />
            </Link>
            <button
              onClick={() => setGeneratedResult(null)}
              className="px-4 py-2.5 rounded-xl bg-slate-800 text-slate-300 hover:text-white text-sm font-semibold transition-colors cursor-pointer"
            >
              Sinh tiếp câu khác
            </button>
          </div>
        </div>
      )}

      {/* Form */}
      <form onSubmit={handleGenerate} className="bg-slate-900 border border-slate-800 rounded-2xl p-6 sm:p-8 shadow-xl space-y-6">
        <fieldset disabled={loading} className="space-y-6 disabled:opacity-70">
        
        <div>
          <label htmlFor="generation-subject" className="block text-xs font-semibold text-slate-300 uppercase mb-1.5">
            1. Chọn môn học mục tiêu
          </label>
          <select
            id="generation-subject"
            required
            value={subjectId}
            onChange={(e) => setSubjectId(e.target.value)}
            className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2.5 text-white focus:outline-none focus:border-indigo-500 text-sm"
          >
            {subjects.length === 0 && <option value="">Chưa có môn học được phân công</option>}
            {subjects.map((s) => (
              <option key={s.id} value={s.id}>
                {s.code} - {s.name}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label htmlFor="generation-topic" className="block text-xs font-semibold text-slate-300 uppercase mb-1.5">
            2. Chủ đề kiến thức / Khái niệm kỹ thuật
          </label>
          <input
            id="generation-topic"
            maxLength={200}
            type="text"
            value={topic}
            onChange={(e) => setTopic(e.target.value)}
            placeholder="Ví dụ: Cơ chế xác thực JWT và Refresh Token, Transaction Isolation Level trong PostgreSQL..."
            required
            className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2.5 text-white focus:outline-none focus:border-indigo-500 text-sm"
          />
          <p className="text-xs text-slate-500 mt-1">
            Gợi ý: Nhập chủ đề càng cụ thể, câu hỏi sinh ra sẽ càng bám sát kiến trúc kỹ thuật.
          </p>
        </div>

        <div>
          <label htmlFor="generation-material" className="block text-xs font-semibold text-slate-300 uppercase mb-1.5">Học liệu PDF, DOCX hoặc TXT UTF-8</label>
          <input id="generation-material" type="file" accept=".pdf,.docx,.txt" required
            onChange={(e) => setMaterialFile(e.target.files[0] || null)}
            className="w-full rounded-xl border border-slate-700 bg-slate-950 p-3 text-sm" />
          <p className="mt-2 text-xs text-slate-400">Tối đa 10 MB / 100.000 ký tự; PDF tối đa 100 trang. PDF scan cần OCR trước. Tệp không được lưu vào kho học liệu.</p>
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-300 uppercase mb-2">
            3. Chọn các mức nhận thức Bloom muốn AI sinh (Chọn nhiều)
          </label>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {BLOOM_OPTIONS.map((b) => {
              const isChecked = selectedLevels.includes(b.value);
              return (
                <label
                  key={b.value}
                  className={`p-3.5 rounded-xl border transition-all cursor-pointer flex items-start gap-3 ${
                    isChecked
                      ? 'bg-indigo-600/10 border-indigo-500/50 text-white'
                      : 'bg-slate-950 border-slate-800/80 text-slate-400 hover:border-slate-700'
                  }`}
                >
                  <input
                    type="checkbox"
                    checked={isChecked}
                    onChange={() => toggleLevel(b.value)}
                    className="mt-1 rounded border-slate-700 bg-slate-900 text-indigo-600 focus:ring-indigo-500"
                  />
                  <span>
                    <span className="font-semibold text-sm block leading-tight">{b.label}</span>
                    <span className="text-xs text-slate-400 block mt-0.5">{b.desc}</span>
                  </span>
                </label>
              );
            })}
          </div>
        </div>

        <label className="flex items-start gap-3 text-sm text-slate-300">
          <input type="checkbox" required checked={allowAiTransfer} onChange={(e) => setAllowAiTransfer(e.target.checked)} className="mt-1" />
          <span>Tôi có quyền sử dụng học liệu và đồng ý gửi văn bản trích xuất đến Google Gemini. Không tải dữ liệu cá nhân/bí mật. API có thể tiêu thụ quota hoặc phát sinh phí.</span>
        </label>

        <div className="pt-4 border-t border-slate-800 flex justify-end">
          <button
            type="submit"
            disabled={loading || subjects.length === 0}
            className="px-6 py-3 rounded-xl bg-gradient-to-r from-amber-500 to-amber-600 hover:from-amber-400 hover:to-amber-500 text-slate-950 font-bold text-sm shadow-lg shadow-amber-500/25 flex items-center gap-2 transition-all disabled:opacity-50 cursor-pointer"
          >
            {loading ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin text-slate-950" />
                Gemini đang tạo câu hỏi, đáp án và rubric...
              </>
            ) : (
              <>
                <Sparkles className="w-4 h-4 text-slate-950" />
                Sinh {selectedLevels.length} câu bằng Gemini
              </>
            )}
          </button>
        </div>
        </fieldset>
      </form>

    </div>
  );
}
