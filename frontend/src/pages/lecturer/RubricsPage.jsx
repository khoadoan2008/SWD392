import React, { useEffect, useState } from 'react';
import { subjectApi } from '../../api/subjectApi';
import { questionApi } from '../../api/questionApi';
import { rubricApi } from '../../api/rubricApi';
import { 
  FileText, 
  Plus, 
  Edit2, 
  Trash2, 
  Tag, 
  HelpCircle, 
  CheckCircle2, 
  AlertCircle, 
  Loader2 
} from 'lucide-react';

export default function RubricsPage() {
  const [subjects, setSubjects] = useState([]);
  const [selectedSubjectId, setSelectedSubjectId] = useState('');
  const [questions, setQuestions] = useState([]);
  const [selectedQuestionId, setSelectedQuestionId] = useState('');
  const [rubrics, setRubrics] = useState([]);
  const [loading, setLoading] = useState(false);

  // Modal
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRubric, setEditingRubric] = useState(null);
  const [criterion, setCriterion] = useState('');
  const [keywords, setKeywords] = useState('');
  const [maxScore, setMaxScore] = useState('3.0');
  const [sortOrder, setSortOrder] = useState('0');
  const [saveLoading, setSaveLoading] = useState(false);

  useEffect(() => {
    subjectApi.getAll().then((res) => {
      if (res.success && res.data.length > 0) {
        setSubjects(res.data);
        setSelectedSubjectId(res.data[0].id);
      }
    });
  }, []);

  useEffect(() => {
    if (selectedSubjectId) {
      questionApi.search({ subjectId: selectedSubjectId }).then((res) => {
        if (res.success) {
          setQuestions(res.data);
          if (res.data.length > 0) {
            setSelectedQuestionId(res.data[0].id);
          } else {
            setSelectedQuestionId('');
            setRubrics([]);
          }
        }
      });
    }
  }, [selectedSubjectId]);

  useEffect(() => {
    if (selectedQuestionId) {
      setLoading(true);
      rubricApi.getByQuestion(selectedQuestionId)
        .then((res) => {
          if (res.success) {
            setRubrics(res.data);
          }
        })
        .finally(() => setLoading(false));
    } else {
      setRubrics([]);
    }
  }, [selectedQuestionId]);

  const selectedQuestion = questions.find((q) => q.id === parseInt(selectedQuestionId, 10));
  const totalScore = rubrics.reduce((acc, r) => acc + (parseFloat(r.maxScore) || 0), 0);

  const openCreateModal = () => {
    setEditingRubric(null);
    setCriterion('');
    setKeywords('');
    setMaxScore('3.0');
    setSortOrder((rubrics.length + 1).toString());
    setModalOpen(true);
  };

  const openEditModal = (r) => {
    setEditingRubric(r);
    setCriterion(r.criterion);
    setKeywords(r.keywords || '');
    setMaxScore(r.maxScore?.toString() || '3.0');
    setSortOrder(r.sortOrder?.toString() || '0');
    setModalOpen(true);
  };

  const handleSaveRubric = async (e) => {
    e.preventDefault();
    if (!criterion.trim()) {
      alert('Vui lòng nhập tên tiêu chí.');
      return;
    }
    const score = parseFloat(maxScore);
    if (isNaN(score) || score <= 0) {
      alert('Điểm tiêu chí phải lớn hơn 0.');
      return;
    }

    setSaveLoading(true);
    try {
      const payload = {
        criterion: criterion.trim(),
        keywords: keywords.trim(),
        maxScore: score,
        sortOrder: parseInt(sortOrder, 10) || 0,
      };

      if (editingRubric) {
        await rubricApi.update(editingRubric.id, selectedQuestionId, payload);
      } else {
        await rubricApi.create(selectedQuestionId, payload);
      }
      setModalOpen(false);
      // Reload rubrics
      const res = await rubricApi.getByQuestion(selectedQuestionId);
      if (res.success) setRubrics(res.data);
    } catch (err) {
      alert(err.response?.data?.message || 'Lỗi lưu tiêu chí rubric');
    } finally {
      setSaveLoading(false);
    }
  };

  const handleDeleteRubric = async (id) => {
    if (!window.confirm('Xoá tiêu chí rubric này?')) return;
    try {
      await rubricApi.delete(id);
      setRubrics(rubrics.filter((r) => r.id !== id));
    } catch (err) {
      alert(err.response?.data?.message || 'Lỗi xoá tiêu chí');
    }
  };

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 text-indigo-400 text-xs font-semibold uppercase tracking-wider mb-1">
            <FileText className="w-4 h-4" />
            Khảo thí &amp; Đánh giá
          </div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Quản lý Tiêu chí Rubric</h1>
          <p className="text-sm text-slate-400">
            Thiết lập các tiêu chí đánh giá, thang điểm và từ khóa mục tiêu phục vụ cơ chế hỏi xoáy của AI
          </p>
        </div>

        {selectedQuestionId && (
          <button
            onClick={openCreateModal}
            className="inline-flex items-center gap-2 bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-sm px-4 py-2.5 rounded-xl shadow-lg shadow-indigo-600/30 transition-all cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            Thêm tiêu chí mới
          </button>
        )}
      </div>

      {/* Selector Filters */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 p-5 rounded-2xl bg-slate-900 border border-slate-800">
        <div>
          <label className="block text-xs font-semibold text-slate-300 uppercase mb-1.5">Môn học</label>
          <select
            value={selectedSubjectId}
            onChange={(e) => setSelectedSubjectId(e.target.value)}
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
          <label className="block text-xs font-semibold text-slate-300 uppercase mb-1.5">Câu hỏi trong ngân hàng</label>
          <select
            value={selectedQuestionId}
            onChange={(e) => setSelectedQuestionId(e.target.value)}
            disabled={questions.length === 0}
            className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2.5 text-white focus:outline-none focus:border-indigo-500 text-sm disabled:opacity-50"
          >
            {questions.length === 0 ? (
              <option value="">(Môn này chưa có câu hỏi nào)</option>
            ) : (
              questions.map((q) => (
                <option key={q.id} value={q.id}>
                  #{q.id} · {q.content.length > 60 ? q.content.substring(0, 60) + '...' : q.content}
                </option>
              ))
            )}
          </select>
        </div>
      </div>

      {/* Question Details Banner */}
      {selectedQuestion && (
        <div className="p-5 rounded-2xl bg-slate-950 border border-slate-800 flex items-start justify-between gap-4">
          <div className="space-y-1">
            <span className="text-xs font-mono font-bold text-indigo-400 bg-indigo-500/10 px-2 py-0.5 rounded">
              NHCH-{selectedQuestion.id} · {selectedQuestion.bloomLevel}
            </span>
            <p className="text-sm font-semibold text-white mt-1 leading-relaxed">{selectedQuestion.content}</p>
            {selectedQuestion.referenceAnswer && (
              <p className="text-xs text-slate-400 italic mt-1">
                Đáp án mẫu: {selectedQuestion.referenceAnswer}
              </p>
            )}
          </div>

          <div className="px-4 py-2 rounded-xl bg-slate-900 border border-slate-800 shrink-0 text-right">
            <span className="text-xs text-slate-400 block">Tổng điểm rubric</span>
            <span className={`text-xl font-bold font-mono ${totalScore === 10 ? 'text-emerald-400' : 'text-amber-400'}`}>
              {totalScore} / 10.0đ
            </span>
          </div>
        </div>
      )}

      {/* Rubrics List */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-xl p-6">
        <h3 className="text-base font-bold text-white mb-4 flex items-center gap-2">
          <FileText className="w-4 h-4 text-cyan-400" />
          Danh sách các tiêu chí đánh giá ({rubrics.length})
        </h3>

        {loading ? (
          <div className="p-12 text-center text-slate-400 flex items-center justify-center gap-2">
            <Loader2 className="w-5 h-5 animate-spin" />
            Đang tải tiêu chí...
          </div>
        ) : rubrics.length === 0 ? (
          <div className="p-12 text-center text-slate-500">
            Chưa có tiêu chí nào cho câu hỏi này. Bấm "Thêm tiêu chí mới" để tạo.
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {rubrics.map((r, i) => (
              <div key={r.id} className="p-5 rounded-xl bg-slate-950 border border-slate-800 flex flex-col justify-between space-y-4">
                <div>
                  <div className="flex items-center justify-between gap-2 mb-2">
                    <span className="text-xs font-mono font-bold text-slate-400">Tiêu chí #{i + 1}</span>
                    <span className="px-2.5 py-0.5 rounded-full text-xs font-bold font-mono bg-cyan-500/10 text-cyan-400 border border-cyan-500/20">
                      {r.maxScore} điểm
                    </span>
                  </div>
                  <h4 className="font-semibold text-white text-sm">{r.criterion}</h4>
                  
                  {r.keywords && (
                    <div className="mt-3 pt-3 border-t border-slate-900">
                      <span className="text-[10px] text-slate-500 uppercase tracking-wider block mb-1">
                        Từ khóa hỏi xoáy AI:
                      </span>
                      <div className="flex flex-wrap gap-1.5">
                        {r.keywords.split(',').map((kw, idx) => (
                          <span key={idx} className="px-2 py-0.5 rounded bg-slate-800 text-slate-300 font-mono text-[11px]">
                            {kw.trim()}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}
                </div>

                <div className="flex justify-end gap-2 pt-2 border-t border-slate-900">
                  <button
                    onClick={() => openEditModal(r)}
                    className="p-1.5 text-slate-400 hover:text-indigo-400 hover:bg-slate-800 rounded-lg transition-colors cursor-pointer"
                    title="Chỉnh sửa"
                  >
                    <Edit2 className="w-4 h-4" />
                  </button>
                  <button
                    onClick={() => handleDeleteRubric(r.id)}
                    className="p-1.5 text-slate-400 hover:text-rose-400 hover:bg-slate-800 rounded-lg transition-colors cursor-pointer"
                    title="Xoá"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Modal */}
      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
          <div className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-2xl relative">
            <h2 className="text-lg font-bold text-white mb-4">
              {editingRubric ? 'Chỉnh sửa tiêu chí' : 'Thêm tiêu chí Rubric mới'}
            </h2>

            <form onSubmit={handleSaveRubric} className="space-y-4 text-sm">
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">Tên tiêu chí *</label>
                <input
                  type="text"
                  value={criterion}
                  onChange={(e) => setCriterion(e.target.value)}
                  placeholder="Ví dụ: Lập luận kiến trúc kỹ thuật..."
                  required
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-indigo-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">
                  Từ khóa trọng tâm (cách nhau bởi dấu phẩy)
                </label>
                <input
                  type="text"
                  value={keywords}
                  onChange={(e) => setKeywords(e.target.value)}
                  placeholder="Ví dụ: caching, latency, throughput, consistency..."
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-indigo-500 font-mono text-xs"
                />
                <p className="text-[11px] text-slate-500 mt-1">
                  AI sẽ tự động phát hiện nếu sinh viên không nhắc đến các từ khóa này để đặt câu hỏi xoáy sâu.
                </p>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">Điểm tối đa *</label>
                  <input
                    type="number"
                    step="0.5"
                    min="0.5"
                    max="10"
                    value={maxScore}
                    onChange={(e) => setMaxScore(e.target.value)}
                    required
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-indigo-500 font-mono"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">Thứ tự sắp xếp</label>
                  <input
                    type="number"
                    value={sortOrder}
                    onChange={(e) => setSortOrder(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-indigo-500 font-mono"
                  />
                </div>
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setModalOpen(false)}
                  className="px-4 py-2 rounded-xl text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
                >
                  Huỷ
                </button>
                <button
                  type="submit"
                  disabled={saveLoading}
                  className="px-5 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-semibold shadow-lg shadow-indigo-600/30 flex items-center gap-2 cursor-pointer"
                >
                  {saveLoading && <Loader2 className="w-4 h-4 animate-spin" />}
                  Lưu tiêu chí
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
}
