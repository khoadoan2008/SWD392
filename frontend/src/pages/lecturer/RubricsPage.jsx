import React, { useEffect, useState } from 'react';
import { questionApi } from '../../api/questionApi';
import { rubricApi } from '../../api/rubricApi';
import { 
  FileText, 
  Plus, 
  Edit2, 
  Trash2, 
  CheckCircle2, 
  AlertCircle, 
  Loader2,
  BookOpen
} from 'lucide-react';

export default function RubricsPage() {
  const [subjects, setSubjects] = useState([]);
  const [selectedSubjectId, setSelectedSubjectId] = useState('');
  const [rubrics, setRubrics] = useState([]);
  const [loading, setLoading] = useState(false);

  // Modal State
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRubric, setEditingRubric] = useState(null);
  const [criterion, setCriterion] = useState('');
  const [keywords, setKeywords] = useState('');
  const [maxScore, setMaxScore] = useState('3.0');
  const [sortOrder, setSortOrder] = useState('0');
  const [saveLoading, setSaveLoading] = useState(false);

  useEffect(() => {
    questionApi.getGenerationSubjects().then((res) => {
      if (res.success && res.data.length > 0) {
        setSubjects(res.data);
        setSelectedSubjectId(res.data[0].id.toString());
      }
    });
  }, []);

  const fetchRubrics = async (subjectId) => {
    if (!subjectId) {
      setRubrics([]);
      return;
    }
    setLoading(true);
    try {
      const res = await rubricApi.getBySubject(subjectId);
      if (res.success) {
        setRubrics(res.data);
      }
    } catch (err) {
      console.error('Lỗi tải rubric:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (selectedSubjectId) {
      fetchRubrics(selectedSubjectId);
    }
  }, [selectedSubjectId]);

  const selectedSubject = subjects.find((s) => s.id === parseInt(selectedSubjectId, 10));
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
        await rubricApi.update(editingRubric.id, selectedSubjectId, payload);
      } else {
        await rubricApi.create(selectedSubjectId, payload);
      }
      setModalOpen(false);
      await fetchRubrics(selectedSubjectId);
    } catch (err) {
      alert(err.response?.data?.message || 'Lỗi lưu tiêu chí rubric');
    } finally {
      setSaveLoading(false);
    }
  };

  const handleDeleteRubric = async (id) => {
    if (!window.confirm('Xoá tiêu chí rubric này của môn học?')) return;
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
            Khảo thí &amp; Đánh giá chuẩn đầu ra
          </div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Quản lý Tiêu chí Rubric theo Môn học</h1>
          <p className="text-sm text-slate-400">
            Thiết lập các tiêu chí đánh giá, thang điểm và từ khóa mục tiêu dùng chung cho tất cả các câu hỏi của môn học
          </p>
        </div>

        {selectedSubjectId && (
          <button
            onClick={openCreateModal}
            className="inline-flex items-center gap-2 bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-sm px-4 py-2.5 rounded-xl shadow-lg shadow-indigo-600/30 transition-all cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            Thêm tiêu chí mới
          </button>
        )}
      </div>

      {/* Subject Filter & Summary Banner */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800 md:col-span-2">
          <label className="block text-xs font-semibold text-slate-300 uppercase mb-2">Chọn Môn học</label>
          <select
            value={selectedSubjectId}
            onChange={(e) => setSelectedSubjectId(e.target.value)}
            className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-3 text-white focus:outline-none focus:border-indigo-500 text-sm font-medium"
          >
            {subjects.map((s) => (
              <option key={s.id} value={s.id}>
                {s.code} - {s.name}
              </option>
            ))}
          </select>
        </div>

        <div className="p-5 rounded-2xl bg-slate-900 border border-slate-800 flex flex-col justify-between">
          <span className="text-xs font-semibold text-slate-400 uppercase">Tổng điểm Rubric chuẩn</span>
          <div className="flex items-baseline gap-2 mt-2">
            <span className={`text-3xl font-extrabold font-mono ${totalScore === 10 ? 'text-emerald-400' : 'text-amber-400'}`}>
              {totalScore.toFixed(1)}
            </span>
            <span className="text-sm text-slate-500">/ 10.0 điểm</span>
          </div>
          <div className="mt-2 text-xs">
            {totalScore === 10 ? (
              <span className="text-emerald-400 flex items-center gap-1 font-medium">
                <CheckCircle2 className="w-3.5 h-3.5" /> Chuẩn 10.0 điểm thi vấn đáp
              </span>
            ) : (
              <span className="text-amber-400 flex items-center gap-1 font-medium">
                <AlertCircle className="w-3.5 h-3.5" /> Nên đủ 10.0 điểm trước khi duyệt câu hỏi
              </span>
            )}
          </div>
        </div>
      </div>

      {/* Selected Subject Banner */}
      {selectedSubject && (
        <div className="p-4 rounded-xl bg-slate-950/80 border border-slate-800/80 flex items-center gap-3">
          <div className="w-9 h-9 rounded-lg bg-indigo-500/10 text-indigo-400 flex items-center justify-center shrink-0">
            <BookOpen className="w-5 h-5" />
          </div>
          <div>
            <span className="font-mono text-xs font-bold text-indigo-400 uppercase">{selectedSubject.code}</span>
            <h2 className="text-sm font-semibold text-white">{selectedSubject.name}</h2>
          </div>
        </div>
      )}

      {/* Rubrics List */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-xl p-6">
        <h3 className="text-base font-bold text-white mb-4 flex items-center gap-2">
          <FileText className="w-4 h-4 text-cyan-400" />
          Các tiêu chí đánh giá của môn học ({rubrics.length})
        </h3>

        {loading ? (
          <div className="p-12 text-center text-slate-400 flex items-center justify-center gap-2">
            <Loader2 className="w-5 h-5 animate-spin" />
            Đang tải tiêu chí...
          </div>
        ) : rubrics.length === 0 ? (
          <div className="p-12 text-center text-slate-500">
            Môn học này chưa có tiêu chí Rubric nào. Bấm "Thêm tiêu chí mới" để tạo.
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
              {editingRubric ? 'Chỉnh sửa tiêu chí' : 'Thêm tiêu chí Rubric cho môn học'}
            </h2>

            <form onSubmit={handleSaveRubric} className="space-y-4 text-sm">
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">Tên tiêu chí *</label>
                <input
                  type="text"
                  value={criterion}
                  onChange={(e) => setCriterion(e.target.value)}
                  placeholder="Ví dụ: Hiểu rõ nguyên lý và cơ chế hoạt động..."
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
                  placeholder="Ví dụ: latency, throughput, consistency, thread safety..."
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-indigo-500 font-mono text-xs"
                />
                <p className="text-[11px] text-slate-500 mt-1">
                  AI Giám khảo sẽ tự động phát hiện nếu sinh viên không nhắc đến các từ khóa này để đặt câu hỏi xoáy sâu.
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
                  className="px-4 py-2 rounded-xl text-slate-400 hover:text-white hover:bg-slate-800 transition-colors cursor-pointer"
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
