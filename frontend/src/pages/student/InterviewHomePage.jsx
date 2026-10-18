import React, { useEffect, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { interviewApi } from '../../api/interviewApi';
import { 
  GraduationCap, 
  Sparkles, 
  ArrowRight, 
  Clock, 
  BookOpen, 
  ShieldCheck, 
  Mic, 
  CheckCircle2, 
  AlertCircle, 
  Loader2 
} from 'lucide-react';

export default function InterviewHomePage() {
  const navigate = useNavigate();
  const [subjects, setSubjects] = useState([]);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedSubjectId, setSelectedSubjectId] = useState('');
  const [startLoading, setStartLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  useEffect(() => {
    interviewApi.getHome()
      .then((res) => {
        if (res.success && res.data) {
          setSubjects(res.data.subjects || []);
          setHistory(res.data.history || []);
          if (res.data.subjects?.length > 0) {
            setSelectedSubjectId(res.data.subjects[0].id);
          }
        }
      })
      .catch((err) => console.error(err))
      .finally(() => setLoading(false));
  }, []);

  const handleStartInterview = async (e) => {
    e.preventDefault();
    if (!selectedSubjectId) return;
    setErrorMsg('');
    setStartLoading(true);
    try {
      const res = await interviewApi.start(selectedSubjectId);
      if (res.success && res.data) {
        navigate(`/student/interview/${res.data.id}`);
      }
    } catch (err) {
      setErrorMsg(err.response?.data?.message || err.message || 'Lỗi bắt đầu phòng thi');
    } finally {
      setStartLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="max-w-4xl mx-auto p-12 text-center text-slate-400 flex items-center justify-center gap-2">
        <Loader2 className="w-5 h-5 animate-spin" />
        Đang tải thông tin phòng thi...
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      
      {/* Header Banner */}
      <div className="relative rounded-2xl bg-gradient-to-r from-cyan-950/60 via-slate-900 to-indigo-950/60 border border-cyan-500/20 p-6 sm:p-8 overflow-hidden shadow-2xl">
        <div className="relative z-10 max-w-2xl space-y-2">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-cyan-500/10 border border-cyan-500/20 text-cyan-400 text-xs font-semibold">
            <Sparkles className="w-3.5 h-3.5" />
            Phòng thi vấn đáp thông minh (AI Viva Exam)
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
            Kỳ thi Vấn đáp Thích ứng Thông minh
          </h1>
          <p className="text-sm text-slate-300 leading-relaxed">
            Hệ thống sẽ đặt các câu hỏi vấn đáp chính từ ngân hàng câu hỏi môn học. Dựa trên nội dung câu trả lời của bạn, AI sẽ tự động phân tích rubric và đặt câu hỏi xoáy sâu thích ứng.
          </p>
        </div>
      </div>

      {errorMsg && (
        <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-sm flex items-center gap-2.5">
          <AlertCircle className="w-5 h-5 shrink-0 text-rose-400" />
          <span>{errorMsg}</span>
        </div>
      )}

      {/* Grid: Start Exam Form + Exam Guidelines */}
      <div className="grid grid-cols-1 md:grid-cols-12 gap-6">
        
        {/* Left Form: Choose Subject & Enter Room */}
        <div className="md:col-span-7 bg-slate-900 border border-slate-800 rounded-2xl p-6 sm:p-7 shadow-xl space-y-6">
          <h2 className="text-base font-bold text-white flex items-center gap-2">
            <BookOpen className="w-4 h-4 text-cyan-400" />
            Lựa chọn môn học thi vấn đáp
          </h2>

          {subjects.length === 0 ? (
            <div className="p-8 text-center text-slate-500 border border-dashed border-slate-800 rounded-xl">
              Hiện tại chưa có môn học nào đang mở thi vấn đáp. Vui lòng liên hệ giảng viên.
            </div>
          ) : (
            <form onSubmit={handleStartInterview} className="space-y-5">
              <div className="space-y-2">
                {subjects.map((s) => {
                  const isSelected = parseInt(selectedSubjectId, 10) === s.id;
                  return (
                    <div
                      key={s.id}
                      onClick={() => setSelectedSubjectId(s.id)}
                      className={`p-4 rounded-xl border transition-all cursor-pointer flex items-center justify-between ${
                        isSelected
                          ? 'bg-cyan-500/10 border-cyan-500/40 text-white shadow-md'
                          : 'bg-slate-950 border-slate-800/80 text-slate-400 hover:border-slate-700'
                      }`}
                    >
                      <div>
                        <span className="font-mono font-bold text-cyan-400 block text-sm">{s.code}</span>
                        <span className="font-semibold text-white text-sm block mt-0.5">{s.name}</span>
                        {s.description && (
                          <span className="text-xs text-slate-500 block mt-1">{s.description}</span>
                        )}
                      </div>
                      <input
                        type="radio"
                        name="subjectSelect"
                        checked={isSelected}
                        onChange={() => {}}
                        className="rounded-full border-slate-700 bg-slate-900 text-cyan-500 focus:ring-cyan-500 pointer-events-none"
                      />
                    </div>
                  );
                })}
              </div>

              <button
                type="submit"
                disabled={startLoading}
                className="w-full bg-gradient-to-r from-cyan-500 to-indigo-600 hover:from-cyan-400 hover:to-indigo-500 text-white font-bold py-3.5 px-6 rounded-xl shadow-lg shadow-cyan-500/20 flex items-center justify-center gap-2 transition-all disabled:opacity-50 cursor-pointer text-sm"
              >
                {startLoading ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    Đang chuẩn bị đề thi...
                  </>
                ) : (
                  <>
                    Vào phòng thi ngay
                    <ArrowRight className="w-4 h-4" />
                  </>
                )}
              </button>
            </form>
          )}
        </div>

        {/* Right Info: Exam Guidelines */}
        <div className="md:col-span-5 bg-slate-900 border border-slate-800 rounded-2xl p-6 sm:p-7 shadow-xl space-y-5">
          <h2 className="text-base font-bold text-white flex items-center gap-2">
            <ShieldCheck className="w-4 h-4 text-emerald-400" />
            Quy định &amp; Lưu ý khi thi
          </h2>

          <ul className="space-y-3 text-xs text-slate-300 leading-relaxed">
            <li className="flex items-start gap-2.5">
              <Clock className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
              <span>
                <strong>Thời gian trả lời:</strong> Mỗi câu hỏi có thời gian đếm ngược cố định. Hệ thống sẽ tự động ghi nhận khi hết giờ.
              </span>
            </li>
            <li className="flex items-start gap-2.5">
              <Mic className="w-4 h-4 text-cyan-400 shrink-0 mt-0.5" />
              <span>
                <strong>Trả lời bằng giọng nói:</strong> Bạn có thể bật Microphone để nói trực tiếp, hệ thống tự động chuyển thành chữ (Speech-to-Text) hoặc gõ phím.
              </span>
            </li>
            <li className="flex items-start gap-2.5">
              <Sparkles className="w-4 h-4 text-indigo-400 shrink-0 mt-0.5" />
              <span>
                <strong>Cơ chế hỏi xoáy của AI:</strong> Nếu câu trả lời quá ngắn hoặc thiếu các từ khóa trong rubric, AI sẽ đặt tiếp 1-2 câu hỏi xoáy bổ sung.
              </span>
            </li>
          </ul>
        </div>

      </div>

      {/* Exam History */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 sm:p-8 shadow-xl space-y-4">
        <h2 className="text-base font-bold text-white flex items-center gap-2">
          <GraduationCap className="w-4 h-4 text-cyan-400" />
          Lịch sử các lượt thi vấn đáp của bạn ({history.length})
        </h2>

        {history.length === 0 ? (
          <div className="p-8 text-center text-slate-500">Bạn chưa tham gia buổi thi vấn đáp nào.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-300">
              <thead className="bg-slate-950 text-xs uppercase tracking-wider text-slate-400 border-b border-slate-800">
                <tr>
                  <th className="px-5 py-3">Mã lượt</th>
                  <th className="px-5 py-3">Môn học</th>
                  <th className="px-5 py-3">Trạng thái</th>
                  <th className="px-5 py-3">Thời gian thi</th>
                  <th className="px-5 py-3 text-right">Chi tiết</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {history.map((h) => (
                  <tr key={h.id} className="hover:bg-slate-850/50">
                    <td className="px-5 py-3 font-mono font-bold text-white">#{h.id}</td>
                    <td className="px-5 py-3 font-medium text-indigo-400 font-mono">
                      {h.subject?.code} - {h.subject?.name}
                    </td>
                    <td className="px-5 py-3">
                      <span className={`inline-block px-2.5 py-0.5 rounded-full text-xs font-semibold ${
                        h.status === 'COMPLETED' ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' :
                        h.status === 'IN_PROGRESS' ? 'bg-cyan-500/10 text-cyan-400 border border-cyan-500/20' :
                        'bg-rose-500/10 text-rose-400 border border-rose-500/20'
                      }`}>
                        {h.status === 'COMPLETED' ? 'Hoàn thành' : h.status === 'IN_PROGRESS' ? 'Đang thi' : 'Đã huỷ'}
                      </span>
                    </td>
                    <td className="px-5 py-3 text-xs text-slate-400">
                      {h.startedAt ? new Date(h.startedAt).toLocaleString('vi-VN') : '—'}
                    </td>
                    <td className="px-5 py-3 text-right">
                      <Link
                        to={`/student/interview/${h.id}`}
                        className="text-xs font-semibold text-cyan-400 hover:underline"
                      >
                        {h.status === 'IN_PROGRESS' ? 'Tiếp tục thi →' : 'Xem lại →'}
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

    </div>
  );
}
