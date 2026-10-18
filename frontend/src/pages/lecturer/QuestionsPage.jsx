import React, { useEffect, useState } from 'react';
import { useSearchParams, Link, useNavigate } from 'react-router-dom';
import { questionApi } from '../../api/questionApi';
import { 
  HelpCircle, 
  Plus, 
  BrainCircuit, 
  Search, 
  Filter, 
  CheckCircle2, 
  XCircle, 
  Clock, 
  Edit2, 
  Trash2, 
  FileText, 
  ArrowLeft, 
  Save, 
  ChevronRight, 
  Loader2, 
  AlertCircle,
  ExternalLink
} from 'lucide-react';

const BLOOM_LEVELS = [
  { value: 'REMEMBER', label: 'Nhớ', color: 'bg-blue-500/10 text-blue-400 border-blue-500/20' },
  { value: 'UNDERSTAND', label: 'Hiểu', color: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' },
  { value: 'APPLY', label: 'Vận dụng', color: 'bg-amber-500/10 text-amber-400 border-amber-500/20' },
  { value: 'ANALYZE', label: 'Phân tích', color: 'bg-purple-500/10 text-purple-400 border-purple-500/20' },
  { value: 'EVALUATE', label: 'Đánh giá', color: 'bg-rose-500/10 text-rose-400 border-rose-500/20' },
  { value: 'CREATE', label: 'Sáng tạo', color: 'bg-cyan-500/10 text-cyan-400 border-cyan-500/20' },
];

const STATUS_LABELS = {
  DRAFT: { label: 'Bản nháp', color: 'bg-slate-800 text-slate-400 border-slate-700' },
  PENDING_REVIEW: { label: 'Chờ duyệt', color: 'bg-amber-500/10 text-amber-400 border-amber-500/20' },
  APPROVED: { label: 'Đã duyệt', color: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' },
  REJECTED: { label: 'Loại bỏ', color: 'bg-rose-500/10 text-rose-400 border-rose-500/20' },
};

export default function QuestionsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();

  const isReviewMode = searchParams.get('status') === 'PENDING_REVIEW';
  const currParam = searchParams.get('curr');

  const [questions, setQuestions] = useState([]);
  const [subjects, setSubjects] = useState([]);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  // Filters
  const [subjectFilter, setSubjectFilter] = useState(searchParams.get('subjectId') || '');
  const [bloomFilter, setBloomFilter] = useState(searchParams.get('bloom') || '');
  const [statusFilter, setStatusFilter] = useState(searchParams.get('status') || '');
  const [searchQuery, setSearchQuery] = useState(searchParams.get('q') || '');

  // Review mode active question
  const [selectedReviewId, setSelectedReviewId] = useState(null);
  const [reviewForm, setReviewForm] = useState({
    content: '',
    referenceAnswer: '',
    topic: '',
    bloomLevel: 'UNDERSTAND',
  });
  const [saveLoading, setSaveLoading] = useState(false);

  const fetchQuestions = async () => {
    setLoading(true);
    try {
      const params = {};
      if (subjectFilter) params.subjectId = subjectFilter;
      if (bloomFilter) params.bloom = bloomFilter;
      if (statusFilter) params.status = statusFilter;
      if (searchQuery.trim()) params.q = searchQuery.trim();

      const [resQ, resStats, resSub] = await Promise.all([
        questionApi.search(params),
        questionApi.getStats(),
        questionApi.getGenerationSubjects()
      ]);

      if (resQ.success) {
        setQuestions(resQ.data);
        if (isReviewMode && resQ.data.length > 0) {
          const target = currParam 
            ? resQ.data.find(q => q.id === parseInt(currParam, 10)) || resQ.data[0]
            : resQ.data[0];
          selectQuestionForReview(target);
        }
      }
      if (resStats.success) setStats(resStats.data);
      if (resSub.success) setSubjects(resSub.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchQuestions();
  }, [subjectFilter, bloomFilter, statusFilter]);

  const selectQuestionForReview = (q) => {
    if (!q) return;
    setSelectedReviewId(q.id);
    setReviewForm({
      content: q.content || '',
      referenceAnswer: q.referenceAnswer || '',
      topic: q.topic || '',
      bloomLevel: q.bloomLevel || 'UNDERSTAND',
    });
  };

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    fetchQuestions();
  };

  const handleStatusChange = async (id, newStatus) => {
    try {
      await questionApi.updateStatus(id, newStatus);
      fetchQuestions();
    } catch (err) {
      alert(err.response?.data?.message || 'Lỗi cập nhật trạng thái');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Bạn có chắc chắn muốn xoá câu hỏi này?')) return;
    try {
      await questionApi.delete(id);
      fetchQuestions();
    } catch (err) {
      alert(err.response?.data?.message || 'Lỗi xoá câu hỏi');
    }
  };

  const handleSaveReview = async (e) => {
    e.preventDefault();
    if (!selectedReviewId) return;
    setSaveLoading(true);
    try {
      const currentQ = questions.find(q => q.id === selectedReviewId);
      const payload = {
        subjectId: currentQ.subject.id,
        content: reviewForm.content.trim(),
        referenceAnswer: reviewForm.referenceAnswer.trim(),
        topic: reviewForm.topic.trim(),
        bloomLevel: reviewForm.bloomLevel,
        status: currentQ.status,
      };
      await questionApi.update(selectedReviewId, payload);
      alert('Đã lưu nội dung chỉnh sửa!');
      fetchQuestions();
    } catch (err) {
      alert(err.response?.data?.message || 'Lỗi lưu câu hỏi');
    } finally {
      setSaveLoading(false);
    }
  };

  const currentReviewQ = questions.find(q => q.id === selectedReviewId);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      
      {/* =========================================================================
          TRƯỜNG HỢP 1: DUYỆT BATCH CÂU HỎI AI (Figma Screen 3)
          ========================================================================= */}
      {isReviewMode ? (
        <div className="space-y-6">
          {/* Top Bar with Progress */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-5 rounded-2xl bg-slate-900 border border-slate-800">
            <div className="space-y-1.5 flex-1 max-w-xl">
              <div className="flex items-center justify-between text-xs">
                <span className="font-semibold text-white flex items-center gap-1.5">
                  <BrainCircuit className="w-4 h-4 text-amber-400" />
                  Tiến độ duyệt câu hỏi AI
                </span>
                <span className="text-slate-400 font-mono">
                  {stats?.APPROVED || 0} đã duyệt / {questions.length} câu trong danh sách
                </span>
              </div>
              <div className="w-full h-2 rounded-full bg-slate-800 overflow-hidden">
                <div 
                  className="h-full bg-gradient-to-r from-amber-500 to-emerald-500 transition-all duration-300"
                  style={{ width: `${Math.min(100, Math.round(((stats?.APPROVED || 1) / Math.max(1, (stats?.TOTAL || 1))) * 100))}%` }}
                />
              </div>
            </div>

            <Link
              to="/lecturer/questions"
              className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-sm font-semibold transition-colors"
            >
              <ArrowLeft className="w-4 h-4" />
              Lưu &amp; Quay lại ngân hàng
            </Link>
          </div>

          {questions.length === 0 ? (
            <div className="p-16 text-center rounded-2xl bg-slate-900 border border-slate-800 text-slate-400">
              <CheckCircle2 className="w-12 h-12 text-emerald-400 mx-auto mb-3" />
              <h3 className="text-lg font-bold text-white">Không còn câu hỏi nào chờ duyệt!</h3>
              <p className="text-sm text-slate-400 mt-1">Tất cả câu hỏi AI đã được xử lý xong.</p>
              <Link to="/lecturer/questions/generate" className="mt-4 inline-flex items-center gap-2 text-indigo-400 text-sm font-semibold hover:underline">
                <Plus className="w-4 h-4" /> Sinh thêm câu hỏi AI mới
              </Link>
            </div>
          ) : (
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
              
              {/* Left Column: Question List */}
              <div className="lg:col-span-4 bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden flex flex-col h-[700px]">
                <div className="p-4 border-b border-slate-800 bg-slate-950/60 flex items-center justify-between">
                  <h2 className="font-bold text-white text-sm">Danh sách câu hỏi AI</h2>
                  <span className="text-xs text-amber-400 font-mono font-bold bg-amber-500/10 px-2 py-0.5 rounded border border-amber-500/20">
                    {questions.length} câu
                  </span>
                </div>

                <div className="flex-1 overflow-y-auto divide-y divide-slate-800/60 p-2 space-y-1">
                  {questions.map((q, idx) => {
                    const isSelected = q.id === selectedReviewId;
                    const bloomObj = BLOOM_LEVELS.find(b => b.value === q.bloomLevel);
                    return (
                      <button
                        key={q.id}
                        type="button"
                        onClick={() => selectQuestionForReview(q)}
                        className={`w-full text-left p-3.5 rounded-xl transition-all cursor-pointer ${
                          isSelected 
                            ? 'bg-indigo-600/15 border border-indigo-500/40 text-white shadow-md' 
                            : 'hover:bg-slate-800/60 text-slate-300'
                        }`}
                      >
                        <div className="flex items-center justify-between mb-1.5">
                          <span className="text-xs font-bold text-slate-400">Câu #{idx + 1}</span>
                          <span className={`text-[10px] font-semibold px-2 py-0.5 rounded-full border ${bloomObj?.color || 'text-slate-400'}`}>
                            {bloomObj?.label || q.bloomLevel}
                          </span>
                        </div>
                        <p className="text-xs font-medium line-clamp-2 leading-relaxed">
                          {q.content}
                        </p>
                        {q.topic && (
                          <span className="text-[10px] text-slate-500 block mt-1 truncate">
                            Chủ đề: {q.topic}
                          </span>
                        )}
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Right Column: Detail & Rubric Review */}
              <div className="lg:col-span-8 bg-slate-900 border border-slate-800 rounded-2xl p-6 flex flex-col justify-between space-y-6">
                {currentReviewQ ? (
                  <form onSubmit={handleSaveReview} className="space-y-6">
                    
                    {/* Header */}
                    <div className="flex items-start justify-between pb-4 border-b border-slate-800">
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="text-xs font-mono font-bold text-indigo-400 bg-indigo-500/10 px-2 py-0.5 rounded">
                            NHCH-{currentReviewQ.id}
                          </span>
                          <span className="text-xs text-slate-400">Môn: {currentReviewQ.subject?.code}</span>
                        </div>
                        <h2 className="text-lg font-bold text-white mt-1">Chi tiết chỉnh sửa & Duyệt ngân hàng</h2>
                      </div>
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-bold px-2.5 py-1 rounded-full bg-amber-500/10 text-amber-400 border border-amber-500/20">
                          AI sinh (RAG)
                        </span>
                      </div>
                    </div>

                    {/* Form Fields */}
                    <div className="space-y-4 text-sm">
                      <div>
                        <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">
                          Nội dung câu hỏi
                        </label>
                        <textarea
                          rows="3"
                          value={reviewForm.content}
                          onChange={(e) => setReviewForm({ ...reviewForm, content: e.target.value })}
                          required
                          className="w-full bg-slate-950 border border-slate-800 rounded-xl p-3 text-white focus:outline-none focus:border-indigo-500 text-sm leading-relaxed"
                        />
                      </div>

                      <div>
                        <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">
                          Đáp án kỳ vọng (Ý chính sinh viên cần trình bày)
                        </label>
                        <textarea
                          rows="3"
                          value={reviewForm.referenceAnswer}
                          onChange={(e) => setReviewForm({ ...reviewForm, referenceAnswer: e.target.value })}
                          className="w-full bg-slate-950 border border-slate-800 rounded-xl p-3 text-white focus:outline-none focus:border-indigo-500 text-sm leading-relaxed"
                        />
                      </div>

                      <div className="grid grid-cols-2 gap-4">
                        <div>
                          <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">
                            Chủ đề / Nguồn trích dẫn
                          </label>
                          <input
                            type="text"
                            value={reviewForm.topic}
                            onChange={(e) => setReviewForm({ ...reviewForm, topic: e.target.value })}
                            placeholder="Giáo trình trang..."
                            className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-indigo-500 text-sm"
                          />
                        </div>

                        <div>
                          <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">
                            Mức nhận thức Bloom
                          </label>
                          <select
                            value={reviewForm.bloomLevel}
                            onChange={(e) => setReviewForm({ ...reviewForm, bloomLevel: e.target.value })}
                            className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-indigo-500 text-sm"
                          >
                            {BLOOM_LEVELS.map(b => (
                              <option key={b.value} value={b.value}>{b.label} ({b.value})</option>
                            ))}
                          </select>
                        </div>
                      </div>
                    </div>

                    {/* Rubric Preview Box (Figma Screen 3) */}
                    <div className="rounded-xl border border-slate-800 bg-slate-950/70 p-4 space-y-3">
                      <div className="flex items-center justify-between">
                        <div>
                          <span className="font-bold text-sm text-white block">Rubric tiêu chí chấm điểm</span>
                          <span className="text-xs text-slate-400">Thang điểm 10 chuẩn hóa</span>
                        </div>
                        <Link
                          to={`/lecturer/questions/${currentReviewQ.id}/edit#rubric`}
                          className="text-xs font-semibold text-indigo-400 hover:text-indigo-300 flex items-center gap-1"
                        >
                          <ExternalLink className="w-3.5 h-3.5" />
                          Mở trình sửa Rubric
                        </Link>
                      </div>

                      <div className="overflow-x-auto">
                        <table className="w-full text-left text-xs">
                          <thead className="bg-slate-900 text-slate-400 uppercase">
                            <tr>
                              <th className="px-3 py-2">Tiêu chí</th>
                              <th className="px-3 py-2">Từ khóa hỏi xoáy</th>
                              <th className="px-3 py-2 text-right">Điểm</th>
                            </tr>
                          </thead>
                          <tbody className="divide-y divide-slate-800 text-slate-300">
                            {currentReviewQ.rubrics?.length > 0 ? (
                              currentReviewQ.rubrics.map((r, i) => (
                                <tr key={r.id || i}>
                                  <td className="px-3 py-2 font-medium text-white">{r.criterion}</td>
                                  <td className="px-3 py-2 text-slate-400 font-mono">{r.keywords || '—'}</td>
                                  <td className="px-3 py-2 text-right font-bold text-cyan-400">{r.maxScore}đ</td>
                                </tr>
                              ))
                            ) : (
                              <>
                                <tr>
                                  <td className="px-3 py-2 font-medium text-white">Lập luận kỹ thuật</td>
                                  <td className="px-3 py-2 text-slate-400">độ trễ, tin cậy, giao thức</td>
                                  <td className="px-3 py-2 text-right font-bold text-cyan-400">4.0đ</td>
                                </tr>
                                <tr>
                                  <td className="px-3 py-2 font-medium text-white">Ví dụ thực tế</td>
                                  <td className="px-3 py-2 text-slate-400">voip, livestream, game</td>
                                  <td className="px-3 py-2 text-right font-bold text-cyan-400">3.0đ</td>
                                </tr>
                                <tr>
                                  <td className="px-3 py-2 font-medium text-white">Tính nhất quán</td>
                                  <td className="px-3 py-2 text-slate-400">kết luận, giải pháp</td>
                                  <td className="px-3 py-2 text-right font-bold text-cyan-400">3.0đ</td>
                                </tr>
                              </>
                            )}
                          </tbody>
                        </table>
                      </div>
                    </div>

                    {/* Action Buttons */}
                    <div className="flex flex-wrap items-center justify-between gap-3 pt-4 border-t border-slate-800">
                      <button
                        type="button"
                        onClick={() => handleStatusChange(selectedReviewId, 'REJECTED')}
                        className="px-4 py-2.5 rounded-xl bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 border border-rose-500/30 text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer"
                      >
                        <Trash2 className="w-4 h-4" />
                        Loại bỏ câu hỏi
                      </button>

                      <div className="flex items-center gap-3">
                        <button
                          type="submit"
                          disabled={saveLoading}
                          className="px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer"
                        >
                          <Save className="w-4 h-4" />
                          Lưu chỉnh sửa
                        </button>

                        <button
                          type="button"
                          onClick={() => handleStatusChange(selectedReviewId, 'APPROVED')}
                          className="px-5 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold flex items-center gap-1.5 shadow-lg shadow-emerald-600/30 transition-all cursor-pointer"
                        >
                          <CheckCircle2 className="w-4 h-4" />
                          Phê duyệt vào ngân hàng
                        </button>
                      </div>
                    </div>

                  </form>
                ) : (
                  <div className="p-12 text-center text-slate-500">Chọn một câu hỏi ở danh sách bên trái để duyệt.</div>
                )}
              </div>

            </div>
          )}
        </div>
      ) : (
        /* =========================================================================
           TRƯỜNG HỢP 2: DANH SÁCH NGÂN HÀNG CÂU HỎI TIÊU CHUẨN
           ========================================================================= */
        <div className="space-y-6">
          
          {/* Header */}
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            <div>
              <div className="flex items-center gap-2 text-indigo-400 text-xs font-semibold uppercase tracking-wider mb-1">
                <HelpCircle className="w-4 h-4" />
                Giảng viên &amp; Khảo thí
              </div>
              <h1 className="text-2xl font-bold text-white tracking-tight">Ngân hàng câu hỏi Viva</h1>
              <p className="text-sm text-slate-400">Quản trị các câu hỏi thi vấn đáp, kiểm duyệt và gán tiêu chí rubric</p>
            </div>

            <div className="flex items-center gap-2.5">
              <Link
                to="/lecturer/questions/generate"
                className="inline-flex items-center gap-2 bg-gradient-to-r from-amber-500 to-amber-600 hover:from-amber-400 hover:to-amber-500 text-slate-950 font-bold text-sm px-4 py-2.5 rounded-xl shadow-lg shadow-amber-500/20 transition-all"
              >
                <BrainCircuit className="w-4 h-4" />
                Sinh câu hỏi AI
              </Link>

              <Link
                to="/lecturer/questions/new"
                className="inline-flex items-center gap-2 bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-sm px-4 py-2.5 rounded-xl shadow-lg shadow-indigo-600/30 transition-all"
              >
                <Plus className="w-4 h-4" />
                Tạo thủ công
              </Link>
            </div>
          </div>

          {/* Filters Bar */}
          <div className="p-4 rounded-2xl bg-slate-900 border border-slate-800 flex flex-wrap items-center gap-3">
            
            {/* Subject Filter */}
            <select
              value={subjectFilter}
              onChange={(e) => setSubjectFilter(e.target.value)}
              className="bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500"
            >
              <option value="">Tất cả môn học</option>
              {subjects.map(s => (
                <option key={s.id} value={s.id}>{s.code} - {s.name}</option>
              ))}
            </select>

            {/* Status Filter */}
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500"
            >
              <option value="">Tất cả trạng thái</option>
              <option value="DRAFT">Bản nháp</option>
              <option value="PENDING_REVIEW">Chờ duyệt (AI)</option>
              <option value="APPROVED">Đã duyệt (Official)</option>
              <option value="REJECTED">Loại bỏ</option>
            </select>

            {/* Bloom Filter */}
            <select
              value={bloomFilter}
              onChange={(e) => setBloomFilter(e.target.value)}
              className="bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500"
            >
              <option value="">Tất cả mức Bloom</option>
              {BLOOM_LEVELS.map(b => (
                <option key={b.value} value={b.value}>{b.label}</option>
              ))}
            </select>

            {/* Search Input */}
            <form onSubmit={handleSearchSubmit} className="flex-1 flex gap-2 min-w-[200px]">
              <div className="relative flex-1">
                <Search className="w-3.5 h-3.5 text-slate-500 absolute left-3 top-2.5" />
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Tìm theo nội dung, chủ đề..."
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl pl-9 pr-3 py-1.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500"
                />
              </div>
              <button
                type="submit"
                className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-xl text-xs font-semibold cursor-pointer"
              >
                Tìm
              </button>
            </form>

          </div>

          {/* Table */}
          <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
            {loading ? (
              <div className="p-12 text-center text-slate-400 flex items-center justify-center gap-2">
                <Loader2 className="w-5 h-5 animate-spin" />
                Đang tải danh sách câu hỏi...
              </div>
            ) : questions.length === 0 ? (
              <div className="p-12 text-center text-slate-500">Không có câu hỏi nào khớp với bộ lọc.</div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm text-slate-300">
                  <thead className="bg-slate-950/60 text-xs uppercase tracking-wider text-slate-400 border-b border-slate-800">
                    <tr>
                      <th className="px-6 py-4">Mã / Môn</th>
                      <th className="px-6 py-4">Nội dung câu hỏi</th>
                      <th className="px-6 py-4">Mức Bloom</th>
                      <th className="px-6 py-4">Trạng thái</th>
                      <th className="px-6 py-4">Nguồn</th>
                      <th className="px-6 py-4 text-right">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60">
                    {questions.map((q) => {
                      const bloomObj = BLOOM_LEVELS.find(b => b.value === q.bloomLevel);
                      const statusObj = STATUS_LABELS[q.status] || STATUS_LABELS.DRAFT;
                      return (
                        <tr key={q.id} className="hover:bg-slate-850/50 transition-colors">
                          <td className="px-6 py-4">
                            <span className="font-mono font-bold text-white block">#{q.id}</span>
                            <span className="text-xs text-indigo-400 font-semibold">{q.subject?.code}</span>
                          </td>
                          <td className="px-6 py-4 max-w-md">
                            <span className="font-medium text-slate-200 line-clamp-2">{q.content}</span>
                            {q.topic && (
                              <span className="text-xs text-slate-500 block mt-1">Chủ đề: {q.topic}</span>
                            )}
                          </td>
                          <td className="px-6 py-4">
                            <span className={`inline-block text-xs font-semibold px-2.5 py-1 rounded-full border ${bloomObj?.color || 'text-slate-400'}`}>
                              {bloomObj?.label || q.bloomLevel}
                            </span>
                          </td>
                          <td className="px-6 py-4">
                            <span className={`inline-block text-xs font-semibold px-2.5 py-1 rounded-full border ${statusObj.color}`}>
                              {statusObj.label}
                            </span>
                          </td>
                          <td className="px-6 py-4 text-xs font-semibold text-slate-400">
                            {q.source === 'AI' ? (
                              <span className="text-amber-400 flex items-center gap-1 font-mono">
                                <BrainCircuit className="w-3.5 h-3.5" /> AI sinh
                              </span>
                            ) : (
                              'Thủ công'
                            )}
                          </td>
                          <td className="px-6 py-4 text-right space-x-2">
                            <Link
                              to={`/lecturer/questions/${q.id}/edit`}
                              className="p-1.5 inline-block text-slate-400 hover:text-indigo-400 hover:bg-slate-800 rounded-lg transition-colors"
                              title="Chỉnh sửa câu hỏi & rubric"
                            >
                              <Edit2 className="w-4 h-4" />
                            </Link>
                            <button
                              onClick={() => handleDelete(q.id)}
                              className="p-1.5 text-slate-400 hover:text-rose-400 hover:bg-slate-800 rounded-lg transition-colors cursor-pointer"
                              title="Xoá câu hỏi"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>

        </div>
      )}

    </div>
  );
}
