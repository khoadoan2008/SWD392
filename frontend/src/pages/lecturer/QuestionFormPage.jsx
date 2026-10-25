import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { questionApi } from '../../api/questionApi';
import { rubricApi } from '../../api/rubricApi';
import { 
  ArrowLeft, 
  Save, 
  HelpCircle, 
  FileText, 
  CheckCircle2, 
  AlertCircle, 
  Loader2, 
  Check, 
  Tag,
  ExternalLink
} from 'lucide-react';

const BLOOM_LEVELS = [
  { value: 'REMEMBER', label: 'Nhớ (Remember)' },
  { value: 'UNDERSTAND', label: 'Hiểu (Understand)' },
  { value: 'APPLY', label: 'Vận dụng (Apply)' },
  { value: 'ANALYZE', label: 'Phân tích (Analyze)' },
  { value: 'EVALUATE', label: 'Đánh giá (Evaluate)' },
  { value: 'CREATE', label: 'Sáng tạo (Create)' },
];

export default function QuestionFormPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEdit = Boolean(id);

  const [subjects, setSubjects] = useState([]);
  const [subjectId, setSubjectId] = useState('');
  const [topic, setTopic] = useState('');
  const [content, setContent] = useState('');
  const [referenceAnswer, setReferenceAnswer] = useState('');
  const [bloomLevel, setBloomLevel] = useState('UNDERSTAND');
  const [difficulty, setDifficulty] = useState('MEDIUM');
  const [notes, setNotes] = useState('');
  const [status, setStatus] = useState('DRAFT');

  // Rubrics of the selected subject
  const [rubrics, setRubrics] = useState([]);

  const [loading, setLoading] = useState(false);
  const [saveLoading, setSaveLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  useEffect(() => {
    questionApi.getGenerationSubjects().then((res) => {
      if (res.success && res.data.length > 0) {
        setSubjects(res.data);
        if (!isEdit && !subjectId) {
          setSubjectId(res.data[0].id.toString());
        }
      }
    });

    if (isEdit) {
      setLoading(true);
      questionApi.getById(id)
        .then((res) => {
          if (res.success && res.data) {
            const q = res.data;
            setSubjectId(q.subject?.id ? q.subject.id.toString() : '');
            setTopic(q.topic || '');
            setContent(q.content || '');
            setReferenceAnswer(q.referenceAnswer || '');
            setBloomLevel(q.bloomLevel || 'UNDERSTAND');
            setDifficulty(q.difficulty || 'MEDIUM');
            setNotes(q.notes || '');
            setStatus(q.status || 'DRAFT');
          }
        })
        .catch((err) => setErrorMsg(err.message))
        .finally(() => setLoading(false));
    }
  }, [id]);

  useEffect(() => {
    if (subjectId) {
      rubricApi.getBySubject(subjectId)
        .then((res) => {
          if (res.success) {
            setRubrics(res.data || []);
          }
        })
        .catch(() => setRubrics([]));
    } else {
      setRubrics([]);
    }
  }, [subjectId]);

  const totalScore = rubrics.reduce((acc, r) => acc + (parseFloat(r.maxScore) || 0), 0);

  const handleSaveQuestion = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setSuccessMsg('');

    if (!subjectId) {
      setErrorMsg('Vui lòng chọn môn học.');
      return;
    }
    if (!content.trim()) {
      setErrorMsg('Vui lòng nhập nội dung câu hỏi.');
      return;
    }

    setSaveLoading(true);
    try {
      const payload = {
        subjectId: parseInt(subjectId, 10),
        topic: topic.trim(),
        content: content.trim(),
        referenceAnswer: referenceAnswer.trim(),
        bloomLevel,
        difficulty,
        notes,
        status,
      };

      if (isEdit) {
        await questionApi.update(id, payload);
        setSuccessMsg('Đã cập nhật câu hỏi thành công!');
      } else {
        const res = await questionApi.create(payload);
        setSuccessMsg('Đã tạo câu hỏi thành công!');
        navigate(`/lecturer/questions/${res.data.id}/edit`);
      }
    } catch (err) {
      setErrorMsg(err.response?.data?.message || err.message || 'Lỗi lưu câu hỏi');
    } finally {
      setSaveLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-[50vh] flex items-center justify-center">
        <Loader2 className="w-8 h-8 text-indigo-500 animate-spin" />
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      
      {/* Top Navigation */}
      <div className="flex items-center justify-between">
        <Link
          to="/lecturer/questions"
          className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-400 hover:text-white transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          Quay lại Ngân hàng câu hỏi
        </Link>

        {isEdit && (
          <span className="text-xs font-mono text-slate-500">
            ID Câu hỏi: #{id}
          </span>
        )}
      </div>

      {/* Main Title Banner */}
      <div>
        <h1 className="text-2xl font-bold text-white tracking-tight">
          {isEdit ? 'Chỉnh sửa Câu hỏi Vấn đáp' : 'Thêm mới Câu hỏi Vấn đáp'}
        </h1>
        <p className="text-sm text-slate-400 mt-1">
          Biên soạn nội dung câu hỏi, định hướng đáp án và mức nhận thức Bloom tương ứng
        </p>
      </div>

      {/* Alerts */}
      {errorMsg && (
        <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-sm flex items-center gap-2">
          <AlertCircle className="w-5 h-5 shrink-0" />
          <span>{errorMsg}</span>
        </div>
      )}

      {successMsg && (
        <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-sm flex items-center gap-2">
          <CheckCircle2 className="w-5 h-5 shrink-0" />
          <span>{successMsg}</span>
        </div>
      )}

      {/* Form */}
      <form onSubmit={handleSaveQuestion} className="bg-slate-900 border border-slate-800 rounded-2xl p-6 sm:p-8 shadow-xl space-y-6">
        
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-semibold text-slate-300 uppercase mb-1.5">
              Môn học được phân công *
            </label>
            <select
              value={subjectId}
              onChange={(e) => setSubjectId(e.target.value)}
              required
              className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2.5 text-white focus:outline-none focus:border-indigo-500 text-sm"
            >
              <option value="">-- Chọn môn học --</option>
              {subjects.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.code} - {s.name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 uppercase mb-1.5">
              Mức độ nhận thức (Thang Bloom) *
            </label>
            <select
              value={bloomLevel}
              onChange={(e) => setBloomLevel(e.target.value)}
              required
              className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2.5 text-white focus:outline-none focus:border-indigo-500 text-sm"
            >
              {BLOOM_LEVELS.map((b) => (
                <option key={b.value} value={b.value}>
                  {b.label}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-300 uppercase mb-1.5">
            Chủ đề / Chương bài giảng
          </label>
          <input
            type="text"
            value={topic}
            onChange={(e) => setTopic(e.target.value)}
            placeholder="Ví dụ: Chương 3: Kiến trúc Microservices / JPA Caching..."
            className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2.5 text-white focus:outline-none focus:border-indigo-500 text-sm"
          />
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-300 uppercase mb-1.5">
            Nội dung câu hỏi thi vấn đáp *
          </label>
          <textarea
            rows="4"
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="Nhập nội dung câu hỏi đặt ra cho sinh viên trong buổi thi vấn đáp..."
            required
            className="w-full bg-slate-950 border border-slate-800 rounded-xl p-3.5 text-white focus:outline-none focus:border-indigo-500 text-sm leading-relaxed"
          />
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-300 uppercase mb-1.5">
            Đáp án kỳ vọng / Hướng dẫn trả lời mẫu
          </label>
          <textarea
            rows="3"
            value={referenceAnswer}
            onChange={(e) => setReferenceAnswer(e.target.value)}
            placeholder="Tóm tắt các luận điểm trọng tâm mà sinh viên cần đạt để được trọn điểm..."
            className="w-full bg-slate-950 border border-slate-800 rounded-xl p-3.5 text-white focus:outline-none focus:border-indigo-500 text-sm leading-relaxed"
          />
        </div>

        <div className="flex justify-end pt-2">
          <div className="mr-auto w-full max-w-xl space-y-3 pr-4">
            <label className="block text-sm text-slate-300">Độ khó
              <select aria-label="Độ khó" value={difficulty} onChange={(e) => setDifficulty(e.target.value)} className="mt-2 w-full rounded-xl border border-slate-700 bg-slate-950 p-3 text-white">
                <option value="EASY">Dễ (EASY)</option><option value="MEDIUM">Trung bình (MEDIUM)</option><option value="HARD">Khó (HARD)</option>
              </select>
            </label>
            <label className="block text-sm text-slate-300">Ghi chú/Gợi ý (không phải đáp án)
              <textarea aria-label="Ghi chú/Gợi ý" value={notes} maxLength={2000} onChange={(e) => setNotes(e.target.value)} rows={3} className="mt-2 w-full rounded-xl border border-slate-700 bg-slate-950 p-3 text-white" />
            </label>
          </div>
          <button
            type="submit"
            disabled={saveLoading}
            className="px-6 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-sm shadow-lg shadow-indigo-600/30 flex items-center gap-2 cursor-pointer transition-all disabled:opacity-50"
          >
            {saveLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
            {isEdit ? 'Lưu cập nhật câu hỏi' : 'Tạo câu hỏi mới'}
          </button>
        </div>

      </form>

      {/* Shared Subject Rubrics Info */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 sm:p-8 shadow-xl space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-slate-800">
          <div>
            <div className="flex items-center gap-2 text-indigo-400 text-xs font-semibold uppercase tracking-wider mb-1">
              <FileText className="w-4 h-4" />
              Tiêu chí Rubric môn học áp dụng
            </div>
            <h2 className="text-base font-bold text-white">Thang chấm &amp; Từ khóa hỏi xoáy của Môn học</h2>
            <p className="text-xs text-slate-400">
              Tất cả các câu hỏi của môn học này đều sử dụng chung bộ tiêu chí Rubric chuẩn bên dưới
            </p>
          </div>

          <div className="flex items-center gap-3 shrink-0">
            <div className="px-3 py-1.5 rounded-xl bg-slate-950 border border-slate-800 text-right">
              <span className="text-[11px] text-slate-400 block">Tổng điểm:</span>
              <span className={`text-base font-bold font-mono ${totalScore === 10 ? 'text-emerald-400' : 'text-amber-400'}`}>
                {totalScore.toFixed(1)} / 10.0đ
              </span>
            </div>

            <Link
              to="/lecturer/rubrics"
              className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl bg-indigo-600/10 hover:bg-indigo-600/20 text-indigo-400 text-xs font-semibold transition-colors"
            >
              Cấu hình Rubric môn học
              <ExternalLink className="w-3.5 h-3.5" />
            </Link>
          </div>
        </div>

        {rubrics.length === 0 ? (
          <div className="p-6 text-center text-slate-500 border border-dashed border-slate-800 rounded-xl text-xs">
            Môn học này hiện chưa có tiêu chí Rubric chuẩn. Vui lòng bấm "Cấu hình Rubric môn học" để tạo.
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {rubrics.map((r, i) => (
              <div key={r.id || i} className="p-3.5 rounded-xl bg-slate-950 border border-slate-800 space-y-2">
                <div className="flex items-center justify-between gap-2">
                  <span className="text-[11px] font-bold text-slate-400 font-mono">Tiêu chí #{i + 1}</span>
                  <span className="px-2 py-0.5 rounded-full bg-cyan-500/10 text-cyan-400 border border-cyan-500/20 text-[11px] font-bold font-mono">
                    {r.maxScore}đ
                  </span>
                </div>
                <h4 className="font-medium text-white text-xs leading-snug">{r.criterion}</h4>
                {r.keywords && (
                  <div className="flex items-center gap-1.5 text-[11px] text-slate-400">
                    <Tag className="w-3 h-3 text-cyan-400 shrink-0" />
                    <span className="text-cyan-300 font-mono text-[10px] truncate">{r.keywords}</span>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

    </div>
  );
}
