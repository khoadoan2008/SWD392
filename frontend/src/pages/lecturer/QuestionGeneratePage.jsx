import React, { useEffect, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { questionApi } from '../../api/questionApi';
import { subjectApi } from '../../api/subjectApi';
import { 
  BrainCircuit, 
  Sparkles, 
  ArrowLeft, 
  CheckCircle2, 
  AlertCircle, 
  Loader2, 
  ArrowRight,
  Layers
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
  const navigate = useNavigate();
  const [subjects, setSubjects] = useState([]);
  const [subjectId, setSubjectId] = useState('');
  const [topic, setTopic] = useState('');
  const [selectedLevels, setSelectedLevels] = useState(['UNDERSTAND', 'APPLY', 'ANALYZE']);
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [generatedResult, setGeneratedResult] = useState(null);

  useEffect(() => {
    subjectApi.getAll().then((res) => {
      if (res.success && res.data.length > 0) {
        setSubjects(res.data);
        setSubjectId(res.data[0].id);
      }
    });
  }, []);

  const toggleLevel = (val) => {
    setSelectedLevels((prev) =>
      prev.includes(val) ? prev.filter((v) => v !== val) : [...prev, val]
    );
  };

  const handleGenerate = async (e) => {
    e.preventDefault();
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

    setLoading(true);
    try {
      const payload = {
        subjectId: parseInt(subjectId, 10),
        topic: topic.trim(),
        levels: selectedLevels,
      };
      const res = await questionApi.generate(payload);
      if (res.success && res.data) {
        setGeneratedResult(res.data);
      }
    } catch (err) {
      setErrorMsg(err.response?.data?.message || err.message || 'Lỗi khi AI sinh câu hỏi');
    } finally {
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
          AI Generator · RAG Template Engine
        </div>
        <h1 className="text-2xl font-bold text-white tracking-tight">Sinh câu hỏi tự động bằng AI</h1>
        <p className="text-sm text-slate-400">
          Chỉ định môn học, chủ đề kiến thức và các mức thang đo Bloom. AI sẽ tự động sinh các câu hỏi và chuyển vào danh sách chờ duyệt.
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
        
        <div>
          <label className="block text-xs font-semibold text-slate-300 uppercase mb-1.5">
            1. Chọn môn học mục tiêu
          </label>
          <select
            value={subjectId}
            onChange={(e) => setSubjectId(e.target.value)}
            className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2.5 text-white focus:outline-none focus:border-indigo-500 text-sm"
          >
            {subjects.map((s) => (
              <option key={s.id} value={s.id}>
                {s.code} - {s.name}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-300 uppercase mb-1.5">
            2. Chủ đề kiến thức / Khái niệm kỹ thuật
          </label>
          <input
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
          <label className="block text-xs font-semibold text-slate-300 uppercase mb-2">
            3. Chọn các mức nhận thức Bloom muốn AI sinh (Chọn nhiều)
          </label>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {BLOOM_OPTIONS.map((b) => {
              const isChecked = selectedLevels.includes(b.value);
              return (
                <div
                  key={b.value}
                  onClick={() => toggleLevel(b.value)}
                  className={`p-3.5 rounded-xl border transition-all cursor-pointer flex items-start gap-3 ${
                    isChecked
                      ? 'bg-indigo-600/10 border-indigo-500/50 text-white'
                      : 'bg-slate-950 border-slate-800/80 text-slate-400 hover:border-slate-700'
                  }`}
                >
                  <input
                    type="checkbox"
                    checked={isChecked}
                    onChange={() => {}}
                    className="mt-1 rounded border-slate-700 bg-slate-900 text-indigo-600 focus:ring-indigo-500 pointer-events-none"
                  />
                  <div>
                    <span className="font-semibold text-sm block leading-tight">{b.label}</span>
                    <span className="text-xs text-slate-400 block mt-0.5">{b.desc}</span>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        <div className="pt-4 border-t border-slate-800 flex justify-end">
          <button
            type="submit"
            disabled={loading}
            className="px-6 py-3 rounded-xl bg-gradient-to-r from-amber-500 to-amber-600 hover:from-amber-400 hover:to-amber-500 text-slate-950 font-bold text-sm shadow-lg shadow-amber-500/25 flex items-center gap-2 transition-all disabled:opacity-50 cursor-pointer"
          >
            {loading ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin text-slate-950" />
                AI đang tạo câu hỏi...
              </>
            ) : (
              <>
                <Sparkles className="w-4 h-4 text-slate-950" />
                Bắt đầu sinh câu hỏi ({selectedLevels.length} mức)
              </>
            )}
          </button>
        </div>

      </form>

    </div>
  );
}
