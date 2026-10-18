import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { questionApi } from '../../api/questionApi';
import { rubricApi } from '../../api/rubricApi';
import { 
  ArrowLeft, 
  Save, 
  HelpCircle, 
  FileText, 
  Plus, 
  Trash2, 
  CheckCircle2, 
  AlertCircle, 
  Loader2, 
  Check, 
  Tag
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
  const [status, setStatus] = useState('DRAFT');

  // Rubrics
  const [rubrics, setRubrics] = useState([]);
  const [newCriterion, setNewCriterion] = useState('');
  const [newKeywords, setNewKeywords] = useState('');
  const [newMaxScore, setNewMaxScore] = useState('3.0');

  const [loading, setLoading] = useState(false);
  const [saveLoading, setSaveLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  useEffect(() => {
    questionApi.getGenerationSubjects().then((res) => {
      if (res.success && res.data.length > 0) {
        setSubjects(res.data);
        if (!isEdit && !subjectId) {
          setSubjectId(res.data[0].id);
        }
      }
    });

    if (isEdit) {
      setLoading(true);
      questionApi.getById(id)
        .then((res) => {
          if (res.success && res.data) {
            const q = res.data;
            setSubjectId(q.subject?.id || '');
            setTopic(q.topic || '');
            setContent(q.content || '');
            setReferenceAnswer(q.referenceAnswer || '');
            setBloomLevel(q.bloomLevel || 'UNDERSTAND');
            setStatus(q.status || 'DRAFT');
            setRubrics(q.rubrics || []);
          }
        })
        .catch((err) => setErrorMsg(err.message))
        .finally(() => setLoading(false));
    }
  }, [id]);

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

  const handleAddRubric = async (e) => {
    e.preventDefault();
    if (!newCriterion.trim()) {
      alert('Vui lòng nhập tiêu chí rubric.');
      return;
    }
    const score = parseFloat(newMaxScore);
    if (isNaN(score) || score <= 0) {
      alert('Điểm tiêu chí phải là số dương lớn hơn 0.');
      return;
    }

    if (!isEdit) {
      alert('Vui lòng bấm "Lưu câu hỏi" trước khi thêm tiêu chí Rubric.');
      return;
    }

    try {
      const payload = {
        criterion: newCriterion.trim(),
        keywords: newKeywords.trim(),
        maxScore: score,
        sortOrder: rubrics.length + 1,
      };
      const res = await rubricApi.create(id, payload);
      if (res.success) {
        setRubrics([...rubrics, res.data]);
        setNewCriterion('');
        setNewKeywords('');
        setNewMaxScore('3.0');
      }
    } catch (err) {
      alert(err.response?.data?.message || 'Lỗi thêm rubric');
    }
  };

  const handleDeleteRubric = async (rubricId) => {
    if (!window.confirm('Xoá tiêu chí rubric này?')) return;
    try {
      await rubricApi.delete(rubricId);
      setRubrics(rubrics.filter((r) => r.id !== rubricId));
    } catch (err) {
      alert(err.response?.data?.message || 'Lỗi xoá rubric');
    }
  };

  const handleQuickApprove = async () => {
    if (rubrics.length === 0) {
      alert('Cần có ít nhất một tiêu chí rubric trước khi duyệt câu hỏi vào ngân hàng chính thức.');
      return;
    }
    try {
      await questionApi.updateStatus(id, 'APPROVED');
      setStatus('APPROVED');
      setSuccessMsg('Đã phê duyệt câu hỏi vào ngân hàng chính thức!');
    } catch (err) {
      alert(err.response?.data?.message || 'Lỗi phê duyệt');
    }
  };

  if (loading) {
    return (
      <div className="max-w-4xl mx-auto p-12 text-center text-slate-400 flex items-center justify-center gap-2">
        <Loader2 className="w-5 h-5 animate-spin" />
        Đang tải thông tin câu hỏi...
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      
      {/* Top Navigation */}
      <div className="flex items-center justify-between">
        <Link
          to="/lecturer/questions"
          className="inline-flex items-center gap-2 text-sm text-slate-400 hover:text-white transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          Quay lại Ngân hàng câu hỏi
        </Link>

        {isEdit && status !== 'APPROVED' && (
          <button
            type="button"
            onClick={handleQuickApprove}
            className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs shadow-lg shadow-emerald-600/30 transition-all cursor-pointer"
          >
            <Check className="w-4 h-4" />
            Phê duyệt vào ngân hàng (APPROVED)
          </button>
        )}
      </div>

      {/* Header */}
      <div>
        <div className="flex items-center gap-2 text-indigo-400 text-xs font-semibold uppercase tracking-wider mb-1">
          <HelpCircle className="w-4 h-4" />
          {isEdit ? `Chỉnh sửa câu hỏi NHCH-${id}` : 'Thêm mới câu hỏi'}
        </div>
        <h1 className="text-2xl font-bold text-white tracking-tight">
          {isEdit ? 'Chỉnh sửa Câu hỏi & Tiêu chí Rubric' : 'Soạn câu hỏi thi vấn đáp mới'}
        </h1>
        <p className="text-sm text-slate-400">
          Soạn thảo nội dung, đáp án kỳ vọng và thiết lập thang điểm rubric để AI phỏng vấn
        </p>
      </div>

      {successMsg && (
        <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 text-sm flex items-center gap-2.5">
          <CheckCircle2 className="w-5 h-5 shrink-0 text-emerald-400" />
          <span>{successMsg}</span>
        </div>
      )}

      {errorMsg && (
        <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-sm flex items-center gap-2.5">
          <AlertCircle className="w-5 h-5 shrink-0 text-rose-400" />
          <span>{errorMsg}</span>
        </div>
      )}

      {/* Question Form */}
      <form onSubmit={handleSaveQuestion} className="bg-slate-900 border border-slate-800 rounded-2xl p-6 sm:p-8 shadow-xl space-y-6">
        
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div>
            <label className="block text-xs font-semibold text-slate-300 uppercase mb-1.5">Môn học</label>
            <select
              value={subjectId}
              onChange={(e) => setSubjectId(e.target.value)}
              required
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
            <label className="block text-xs font-semibold text-slate-300 uppercase mb-1.5">Mức nhận thức Bloom</label>
            <select
              value={bloomLevel}
              onChange={(e) => setBloomLevel(e.target.value)}
              className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2.5 text-white focus:outline-none focus:border-indigo-500 text-sm"
            >
              {BLOOM_LEVELS.map((b) => (
                <option key={b.value} value={b.value}>
                  {b.label}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 uppercase mb-1.5">Trạng thái duyệt</label>
            <select
              value={status}
              onChange={(e) => setStatus(e.target.value)}
              className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2.5 text-white focus:outline-none focus:border-indigo-500 text-sm"
            >
              <option value="DRAFT">Bản nháp (DRAFT)</option>
              <option value="PENDING_REVIEW">Chờ duyệt (PENDING_REVIEW)</option>
              <option value="APPROVED">Đã duyệt (APPROVED)</option>
              <option value="REJECTED">Loại bỏ (REJECTED)</option>
            </select>
          </div>
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-300 uppercase mb-1.5">
            Chủ đề / Chương bài học / Nguồn trích dẫn
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
          <button
            type="submit"
            disabled={saveLoading}
            className="px-6 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-sm shadow-lg shadow-indigo-600/30 flex items-center gap-2 cursor-pointer transition-all disabled:opacity-50"
          >
            {saveLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
            {isEdit ? 'Lưu cập nhật câu hỏi' : 'Tạo câu hỏi & Chuyển sang thêm Rubric'}
          </button>
        </div>

      </form>

      {/* Embedded Rubric Editor */}
      {isEdit && (
        <div id="rubric" className="bg-slate-900 border border-slate-800 rounded-2xl p-6 sm:p-8 shadow-xl space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-slate-800">
            <div>
              <div className="flex items-center gap-2 text-indigo-400 text-xs font-semibold uppercase tracking-wider mb-1">
                <FileText className="w-4 h-4" />
                Thang điểm &amp; Tiêu chí Rubric
              </div>
              <h2 className="text-lg font-bold text-white">Tiêu chí đánh giá &amp; Từ khóa hỏi xoáy</h2>
              <p className="text-xs text-slate-400">
                AI sẽ căn cứ vào các từ khóa trong tiêu chí để sinh câu hỏi xoáy nếu sinh viên trả lời thiếu
              </p>
            </div>

            <div className="px-4 py-2 rounded-xl bg-slate-950 border border-slate-800 text-right">
              <span className="text-xs text-slate-400 block">Tổng điểm Rubric:</span>
              <span className={`text-xl font-bold font-mono ${totalScore === 10 ? 'text-emerald-400' : 'text-amber-400'}`}>
                {totalScore} / 10.0 điểm
              </span>
            </div>
          </div>

          {/* List of Rubrics */}
          {rubrics.length === 0 ? (
            <div className="p-8 text-center text-slate-500 border border-dashed border-slate-800 rounded-xl">
              Chưa có tiêu chí Rubric nào. Thêm tiêu chí bên dưới để hoàn tất câu hỏi.
            </div>
          ) : (
            <div className="space-y-3">
              {rubrics.map((r, i) => (
                <div key={r.id || i} className="p-4 rounded-xl bg-slate-950 border border-slate-800 flex items-start justify-between gap-4">
                  <div className="space-y-1.5 flex-1">
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-bold text-slate-400 font-mono">#{i + 1}</span>
                      <h4 className="font-semibold text-white text-sm">{r.criterion}</h4>
                    </div>

                    {r.keywords && (
                      <div className="flex items-center gap-2 text-xs text-slate-400">
                        <Tag className="w-3.5 h-3.5 text-cyan-400 shrink-0" />
                        <span className="text-cyan-300 font-mono text-[11px]">{r.keywords}</span>
                      </div>
                    )}
                  </div>

                  <div className="flex items-center gap-3 shrink-0">
                    <span className="px-2.5 py-1 rounded-full bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 text-xs font-bold font-mono">
                      {r.maxScore} điểm
                    </span>

                    <button
                      type="button"
                      onClick={() => handleDeleteRubric(r.id)}
                      className="p-1.5 text-slate-500 hover:text-rose-400 hover:bg-slate-850 rounded-lg transition-colors cursor-pointer"
                      title="Xoá tiêu chí"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Add Rubric Form */}
          <form onSubmit={handleAddRubric} className="p-5 rounded-xl bg-slate-950 border border-slate-800 space-y-4">
            <h3 className="text-xs font-semibold uppercase text-slate-300 tracking-wider flex items-center gap-1.5">
              <Plus className="w-4 h-4 text-indigo-400" />
              Thêm tiêu chí rubric mới
            </h3>

            <div className="grid grid-cols-1 sm:grid-cols-12 gap-3 text-sm">
              <div className="sm:col-span-6">
                <input
                  type="text"
                  value={newCriterion}
                  onChange={(e) => setNewCriterion(e.target.value)}
                  placeholder="Tên tiêu chí (vd: Lập luận kiến trúc microservice)"
                  className="w-full bg-slate-900 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-indigo-500 text-xs"
                />
              </div>

              <div className="sm:col-span-4">
                <input
                  type="text"
                  value={newKeywords}
                  onChange={(e) => setNewKeywords(e.target.value)}
                  placeholder="Từ khoá cách nhau bởi dấu phẩy (vd: api gateway, discovery, resilience)"
                  className="w-full bg-slate-900 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-indigo-500 text-xs font-mono"
                />
              </div>

              <div className="sm:col-span-2 flex gap-2">
                <input
                  type="number"
                  step="0.5"
                  min="0.5"
                  max="10"
                  value={newMaxScore}
                  onChange={(e) => setNewMaxScore(e.target.value)}
                  placeholder="Điểm"
                  className="w-20 bg-slate-900 border border-slate-800 rounded-xl px-2 py-2 text-white focus:outline-none focus:border-indigo-500 text-xs font-mono text-center"
                />
                <button
                  type="submit"
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-xs font-semibold cursor-pointer shrink-0 transition-colors"
                >
                  Thêm
                </button>
              </div>
            </div>
          </form>

        </div>
      )}

    </div>
  );
}
