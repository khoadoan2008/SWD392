import React, { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { Link } from 'react-router-dom';
import { 
  Sparkles, 
  HelpCircle, 
  FileText, 
  Clock, 
  Users, 
  BookOpen, 
  Settings, 
  GraduationCap, 
  BrainCircuit, 
  ArrowRight,
  CheckCircle2,
  AlertTriangle
} from 'lucide-react';
import { questionApi } from '../api/questionApi';
import { sessionApi } from '../api/sessionApi';
import { userApi } from '../api/userApi';
import { interviewApi } from '../api/interviewApi';
import { 
  Award, 
  MessageSquare, 
  X, 
  Eye, 
  CheckCircle 
} from 'lucide-react';

export default function DashboardPage() {
  const { user } = useAuth();
  const [stats, setStats] = useState(null);
  const [sessionStats, setSessionStats] = useState(null);
  const [userStats, setUserStats] = useState(null);
  const [studentHistory, setStudentHistory] = useState([]);
  const [studentLoading, setStudentLoading] = useState(false);
  const [selectedSession, setSelectedSession] = useState(null);

  useEffect(() => {
    if (user.role === 'STUDENT' || user.role === 'ADMIN') {
      setStudentLoading(true);
      interviewApi.getHome()
        .then(res => {
          if (res.success && res.data?.history) {
            setStudentHistory(res.data.history);
          }
        })
        .catch(err => console.error(err))
        .finally(() => setStudentLoading(false));
    }
    if (user.role === 'LECTURER' || user.role === 'ADMIN') {
      questionApi.getStats()
        .then(res => res.success && setStats(res.data))
        .catch(() => {});

      sessionApi.getStats()
        .then(res => res.success && setSessionStats(res.data))
        .catch(() => {});
    }

    if (user.role === 'ADMIN') {
      userApi.getStats()
        .then(res => res.success && setUserStats(res.data))
        .catch(() => {});
    }
  }, [user]);

  const completedSessions = studentHistory.filter(s => s.status === 'COMPLETED');
  const gradedSessions = studentHistory.filter(s => s.finalScore != null);
  const avgScore = gradedSessions.length > 0
    ? (gradedSessions.reduce((acc, curr) => acc + Number(curr.finalScore || 0), 0) / gradedSessions.length).toFixed(1)
    : null;
  const pendingGradingCount = completedSessions.length - gradedSessions.length;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      
      {/* Welcome Banner */}
      <div className="relative rounded-2xl bg-gradient-to-r from-indigo-900/40 via-slate-900 to-slate-900 border border-indigo-500/20 p-6 sm:p-8 overflow-hidden shadow-xl">
        <div className="relative z-10 max-w-2xl">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 border border-indigo-500/20 text-indigo-400 text-xs font-semibold mb-3">
            <Sparkles className="w-3.5 h-3.5" />
            Hệ thống AIVES (SWD392)
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
            Xin chào, {user.fullName || user.username}!
          </h1>
          <p className="text-slate-400 text-sm mt-2 leading-relaxed">
            Hệ thống thi vấn đáp thích ứng sử dụng AI (AI Adaptive Oral Examination). Hỗ trợ ngân hàng câu hỏi phân cấp Bloom, sinh câu hỏi RAG/AI, tiêu chí rubric tự động và cơ chế hỏi xoáy tương tác thời gian thực.
          </p>
        </div>

        {/* Action button in banner */}
        <div className="mt-6 sm:mt-0 sm:absolute sm:right-8 sm:bottom-8 z-10">
          {user.role === 'STUDENT' ? (
            <Link
              to="/student/interview"
              className="inline-flex items-center gap-2 bg-gradient-to-r from-cyan-500 to-indigo-600 hover:from-cyan-400 hover:to-indigo-500 text-white font-semibold text-sm px-5 py-3 rounded-xl shadow-lg shadow-cyan-500/20 transition-all"
            >
              <GraduationCap className="w-4 h-4" />
              Bắt đầu thi vấn đáp
              <ArrowRight className="w-4 h-4" />
            </Link>
          ) : (
            <Link
              to="/lecturer/questions?status=PENDING_REVIEW"
              className="inline-flex items-center gap-2 bg-gradient-to-r from-amber-500 to-amber-600 hover:from-amber-400 hover:to-amber-500 text-slate-950 font-bold text-sm px-5 py-3 rounded-xl shadow-lg shadow-amber-500/20 transition-all"
            >
              <BrainCircuit className="w-4 h-4" />
              Duyệt câu hỏi AI ({stats?.PENDING_REVIEW || 0})
              <ArrowRight className="w-4 h-4" />
            </Link>
          )}
        </div>
      </div>

      {/* Stats Cards */}
      {(user.role === 'LECTURER' || user.role === 'ADMIN') && stats && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <div className="p-4 rounded-xl bg-slate-900 border border-slate-800">
            <span className="text-xs font-semibold text-slate-400 block uppercase">Tổng câu hỏi</span>
            <span className="text-2xl font-bold text-white mt-1 block">{stats.TOTAL || 0}</span>
          </div>

          <div className="p-4 rounded-xl bg-amber-500/10 border border-amber-500/20">
            <span className="text-xs font-semibold text-amber-400 block uppercase">Chờ duyệt (AI)</span>
            <span className="text-2xl font-bold text-amber-300 mt-1 block">{stats.PENDING_REVIEW || 0}</span>
          </div>

          <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/20">
            <span className="text-xs font-semibold text-emerald-400 block uppercase">Đã duyệt (Official)</span>
            <span className="text-2xl font-bold text-emerald-300 mt-1 block">{stats.APPROVED || 0}</span>
          </div>

          <div className="p-4 rounded-xl bg-slate-800/50 border border-slate-700/50">
            <span className="text-xs font-semibold text-slate-400 block uppercase">Lượt thi vấn đáp</span>
            <span className="text-2xl font-bold text-cyan-400 mt-1 block">{sessionStats?.TOTAL || 0}</span>
          </div>
        </div>
      )}

      {/* Admin User Stats */}
      {user.role === 'ADMIN' && userStats && (
        <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800">
          <h2 className="text-base font-semibold text-white mb-4 flex items-center gap-2">
            <Users className="w-4 h-4 text-indigo-400" />
            Thống kê người dùng hệ thống
          </h2>
          <div className="grid grid-cols-3 gap-4">
            <div className="p-4 rounded-xl bg-slate-950 border border-slate-800/80">
              <span className="text-xs text-slate-400">Quản trị viên</span>
              <span className="text-xl font-bold text-rose-400 block mt-1">{userStats.ADMIN || 0}</span>
            </div>
            <div className="p-4 rounded-xl bg-slate-950 border border-slate-800/80">
              <span className="text-xs text-slate-400">Giảng viên</span>
              <span className="text-xl font-bold text-amber-400 block mt-1">{userStats.LECTURER || 0}</span>
            </div>
            <div className="p-4 rounded-xl bg-slate-950 border border-slate-800/80">
              <span className="text-xs text-slate-400">Sinh viên</span>
              <span className="text-xl font-bold text-cyan-400 block mt-1">{userStats.STUDENT || 0}</span>
            </div>
          </div>
        </div>
      )}

      {/* Student Stats Overview */}
      {(user.role === 'STUDENT' || user.role === 'ADMIN') && studentHistory.length > 0 && (
        <div className="space-y-4">
          <h2 className="text-base font-bold text-white flex items-center gap-2">
            <GraduationCap className="w-4 h-4 text-cyan-400" />
            Tổng quan kết quả thi vấn đáp của bạn
          </h2>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 shadow-md">
              <span className="text-xs font-semibold text-slate-400 block uppercase">Tổng lượt thi</span>
              <span className="text-2xl font-bold text-white mt-1 block font-mono">{studentHistory.length}</span>
            </div>

            <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/20 shadow-md">
              <span className="text-xs font-semibold text-emerald-400 block uppercase">Đã chốt điểm</span>
              <span className="text-2xl font-bold text-emerald-400 mt-1 block font-mono">{gradedSessions.length} môn</span>
            </div>

            <div className="p-4 rounded-xl bg-cyan-500/10 border border-cyan-500/20 shadow-md">
              <span className="text-xs font-semibold text-cyan-400 block uppercase">Điểm trung bình (GPA)</span>
              <span className="text-2xl font-bold text-cyan-300 mt-1 block font-mono">
                {avgScore != null ? `${avgScore} / 10` : '—'}
              </span>
            </div>

            <div className="p-4 rounded-xl bg-amber-500/10 border border-amber-500/20 shadow-md">
              <span className="text-xs font-semibold text-amber-400 block uppercase">Chờ GV chấm điểm</span>
              <span className="text-2xl font-bold text-amber-300 mt-1 block font-mono">{pendingGradingCount}</span>
            </div>
          </div>
        </div>
      )}

      {/* Student Exam History & Score Table */}
      {(user.role === 'STUDENT' || user.role === 'ADMIN') && (
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 sm:p-8 shadow-xl space-y-5">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div>
              <h2 className="text-base font-bold text-white flex items-center gap-2">
                <Award className="w-4 h-4 text-cyan-400" />
                Lịch sử Thi &amp; Bảng điểm Vấn đáp ({studentHistory.length})
              </h2>
              <p className="text-xs text-slate-400 mt-0.5">
                Xem kết quả điểm số chính thức, nhận xét của giảng viên và chi tiết barem rubric
              </p>
            </div>

            <Link
              to="/student/interview"
              className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-gradient-to-r from-cyan-500 to-indigo-600 hover:from-cyan-400 hover:to-indigo-500 text-white font-semibold text-xs shadow-md shadow-cyan-500/20 transition-all self-start sm:self-auto"
            >
              <GraduationCap className="w-3.5 h-3.5" />
              Vào phòng thi mới
              <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          {studentLoading ? (
            <div className="p-8 text-center text-slate-500 text-xs">Đang tải lịch sử thi...</div>
          ) : studentHistory.length === 0 ? (
            <div className="p-8 text-center text-slate-500 text-xs border border-dashed border-slate-800 rounded-xl">
              Bạn chưa có lượt thi vấn đáp nào. Hãy bấm "Vào phòng thi mới" để bắt đầu kỳ thi!
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm text-slate-300">
                <thead className="bg-slate-950 text-xs uppercase tracking-wider text-slate-400 border-b border-slate-800">
                  <tr>
                    <th className="px-5 py-3">Mã lượt</th>
                    <th className="px-5 py-3">Môn học</th>
                    <th className="px-5 py-3">Trạng thái</th>
                    <th className="px-5 py-3">Điểm số &amp; Nhận xét</th>
                    <th className="px-5 py-3">Thời gian thi</th>
                    <th className="px-5 py-3 text-right">Chi tiết</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800 text-xs">
                  {studentHistory.map((h) => (
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
                      <td className="px-5 py-3">
                        {h.status === 'COMPLETED' ? (
                          h.finalScore != null ? (
                            <div className="flex flex-col gap-1">
                              <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-bold bg-emerald-500/15 text-emerald-400 border border-emerald-500/30 w-fit font-mono">
                                <Award className="w-3.5 h-3.5" />
                                {h.finalScore} / 10.0
                              </span>
                              {h.lecturerFeedback && (
                                <span className="text-[11px] text-slate-400 truncate max-w-[220px]" title={h.lecturerFeedback}>
                                  "{h.lecturerFeedback}"
                                </span>
                              )}
                            </div>
                          ) : (
                            <div className="flex flex-col gap-0.5">
                              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-medium bg-amber-500/10 text-amber-400 border border-amber-500/20 w-fit">
                                Chờ GV chấm điểm
                              </span>
                              {h.aiSuggestedScore != null && (
                                <span className="text-[10px] text-slate-500 font-mono">
                                  AI gợi ý: {h.aiSuggestedScore}đ
                                </span>
                              )}
                            </div>
                          )
                        ) : (
                          <span className="text-xs text-slate-500">—</span>
                        )}
                      </td>
                      <td className="px-5 py-3 text-xs text-slate-400">
                        {h.startedAt ? new Date(h.startedAt).toLocaleString('vi-VN') : '—'}
                      </td>
                      <td className="px-5 py-3 text-right">
                        {h.status === 'IN_PROGRESS' ? (
                          <Link
                            to={`/student/interview/${h.id}`}
                            className="inline-flex items-center gap-1 text-xs font-semibold px-3 py-1.5 rounded-xl bg-cyan-600 hover:bg-cyan-500 text-white transition-all shadow-md shadow-cyan-600/20"
                          >
                            Tiếp tục thi →
                          </Link>
                        ) : (
                          <button
                            type="button"
                            onClick={() => setSelectedSession(h)}
                            className="inline-flex items-center gap-1.5 text-xs font-semibold px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-cyan-400 hover:text-white transition-all border border-slate-700 hover:border-slate-600 cursor-pointer"
                          >
                            <Eye className="w-3.5 h-3.5" />
                            Xem bảng điểm
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* Student Scorecard & Exam Result Modal */}
      {selectedSession && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="relative w-full max-w-4xl max-h-[90vh] overflow-y-auto bg-slate-900 border border-slate-800 rounded-3xl p-6 sm:p-8 shadow-2xl space-y-6">
            
            {/* Modal Header */}
            <div className="flex items-start justify-between pb-4 border-b border-slate-800">
              <div className="space-y-1">
                <div className="flex items-center gap-2 text-xs font-mono font-bold text-cyan-400">
                  <BookOpen className="w-3.5 h-3.5" />
                  {selectedSession.subject?.code} - {selectedSession.subject?.name}
                </div>
                <h2 className="text-xl sm:text-2xl font-extrabold text-white">
                  Bảng điểm &amp; Biên bản Vấn đáp #{selectedSession.id}
                </h2>
                <p className="text-xs text-slate-400">
                  Thời gian thi: {selectedSession.startedAt ? new Date(selectedSession.startedAt).toLocaleString('vi-VN') : '—'}
                  {selectedSession.endedAt && ` • Hoàn thành: ${new Date(selectedSession.endedAt).toLocaleTimeString('vi-VN')}`}
                </p>
              </div>

              <button
                type="button"
                onClick={() => setSelectedSession(null)}
                className="p-2 rounded-xl text-slate-400 hover:text-white hover:bg-slate-800 transition-colors cursor-pointer"
                title="Đóng"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Scorecard Hero Banner */}
            <div className="p-6 rounded-2xl bg-gradient-to-br from-slate-950 via-slate-950 to-indigo-950/70 border border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-6">
              <div className="space-y-2 text-center sm:text-left">
                <span className="text-xs font-bold uppercase tracking-wider px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                  Kết quả đánh giá
                </span>
                <h3 className="text-lg font-bold text-white">
                  {selectedSession.finalScore != null ? 'Điểm thi chính thức từ Giảng viên' : 'Đang chờ Giảng viên đánh giá Barem'}
                </h3>
                <p className="text-xs text-slate-400 max-w-md">
                  {selectedSession.finalScore != null 
                    ? `Buổi thi đã được đánh giá và chốt điểm bởi ${selectedSession.gradedBy?.fullName || 'Giảng viên phụ trách'}.`
                    : 'Bài thi đã được ghi nhận đầy đủ câu hỏi và văn bản trả lời. Giảng viên sẽ công bố điểm chính thức sớm nhất.'}
                </p>
              </div>

              <div className="flex flex-col items-center justify-center p-4 rounded-2xl bg-slate-900 border border-slate-800 min-w-[180px]">
                {selectedSession.finalScore != null ? (
                  <>
                    <span className="text-[11px] font-bold uppercase tracking-wider text-emerald-400 mb-1 flex items-center gap-1">
                      <Award className="w-3.5 h-3.5" />
                      Điểm chốt
                    </span>
                    <span className="text-4xl font-black text-emerald-400 font-mono">
                      {selectedSession.finalScore}
                      <span className="text-sm font-normal text-slate-500"> / 10</span>
                    </span>
                    {selectedSession.gradedAt && (
                      <span className="text-[10px] text-slate-500 mt-1">
                        {new Date(selectedSession.gradedAt).toLocaleDateString('vi-VN')}
                      </span>
                    )}
                  </>
                ) : (
                  <>
                    <Clock className="w-6 h-6 text-amber-400 mb-1" />
                    <span className="text-sm font-bold text-amber-400">Chờ chấm</span>
                    {selectedSession.aiSuggestedScore != null && (
                      <span className="text-[11px] text-slate-500 font-mono mt-1">
                        AI gợi ý: {selectedSession.aiSuggestedScore}đ
                      </span>
                    )}
                  </>
                )}
              </div>
            </div>

            {/* Lecturer Official Feedback */}
            {selectedSession.lecturerFeedback && (
              <div className="p-5 rounded-2xl bg-slate-950 border border-indigo-500/20 space-y-2">
                <div className="flex items-center gap-2 text-xs font-bold text-indigo-400 uppercase tracking-wider">
                  <MessageSquare className="w-4 h-4" />
                  Lời nhận xét của Giảng viên:
                </div>
                <p className="text-sm text-slate-200 leading-relaxed italic pl-3 border-l-2 border-indigo-500/50">
                  "{selectedSession.lecturerFeedback}"
                </p>
              </div>
            )}

            {/* Rubric Breakdown */}
            {selectedSession.rubricScores && selectedSession.rubricScores.length > 0 && (
              <div className="space-y-3">
                <h4 className="text-sm font-bold text-white flex items-center gap-2">
                  <Award className="w-4 h-4 text-cyan-400" />
                  Đánh giá chi tiết theo Barem Rubric ({selectedSession.rubricScores.length} tiêu chí)
                </h4>
                <div className="overflow-x-auto border border-slate-800 rounded-xl">
                  <table className="w-full text-left text-xs text-slate-300">
                    <thead className="bg-slate-950 uppercase tracking-wider text-slate-400 border-b border-slate-800">
                      <tr>
                        <th className="px-4 py-2.5">Tiêu chí</th>
                        <th className="px-4 py-2.5 text-center">Trọng số</th>
                        <th className="px-4 py-2.5 text-center">Điểm đạt</th>
                        <th className="px-4 py-2.5">Đánh giá chi tiết</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-800">
                      {selectedSession.rubricScores.map((r, idx) => (
                        <tr key={r.id || idx} className="hover:bg-slate-850/50">
                          <td className="px-4 py-3 font-medium text-white max-w-xs">
                            {r.criterion}
                          </td>
                          <td className="px-4 py-3 text-center font-mono font-bold text-slate-400">
                            {r.maxScore}đ
                          </td>
                          <td className="px-4 py-3 text-center">
                            <span className="inline-block px-2.5 py-1 rounded-lg font-mono font-bold text-sm bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                              {r.finalScore != null ? r.finalScore : (r.aiScore != null ? r.aiScore : '—')}
                            </span>
                          </td>
                          <td className="px-4 py-3 text-slate-400 italic">
                            {r.comment || 'Không có ghi chú thêm.'}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {/* Question & Transcript turns */}
            {selectedSession.turns && selectedSession.turns.length > 0 && (
              <div className="space-y-3">
                <h4 className="text-sm font-bold text-white flex items-center gap-2">
                  <FileText className="w-4 h-4 text-indigo-400" />
                  Biên bản câu hỏi &amp; câu trả lời ({selectedSession.turns.length} lượt)
                </h4>
                <div className="space-y-3">
                  {selectedSession.turns.map((t, idx) => (
                    <div key={t.id || idx} className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-2 text-xs">
                      <div className="flex items-center justify-between">
                        <span className={`font-bold uppercase tracking-wider px-2.5 py-0.5 rounded-full border ${
                          t.isFollowUp || t.turnType === 'FOLLOW_UP'
                            ? 'bg-amber-500/10 text-amber-400 border-amber-500/20'
                            : 'bg-indigo-500/10 text-indigo-400 border-indigo-500/20'
                        }`}>
                          {t.isFollowUp || t.turnType === 'FOLLOW_UP'
                            ? `AI Hỏi xoáy #${t.followupIndex}`
                            : `Câu hỏi chính #${t.mainIndex}`}
                        </span>
                        {t.responseTimeSec != null && (
                          <span className="text-slate-500 font-mono">
                            Thời gian: {t.responseTimeSec}s
                          </span>
                        )}
                      </div>
                      <p className="text-white font-medium">
                        <strong>Hỏi:</strong> {t.questionText}
                      </p>
                      <p className="text-slate-300 italic bg-slate-900/60 p-2.5 rounded-lg border border-slate-850">
                        <strong className="text-cyan-400 not-italic">Trả lời:</strong> {t.transcript ? `"${t.transcript}"` : '(Thí sinh không ghi nhận câu trả lời)'}
                      </p>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Modal Footer */}
            <div className="pt-2 flex justify-end gap-3 border-t border-slate-800">
              <button
                type="button"
                onClick={() => setSelectedSession(null)}
                className="px-5 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-white font-semibold text-xs transition-colors cursor-pointer"
              >
                Đóng
              </button>
            </div>

          </div>
        </div>
      )}

      {/* Quick Shortcuts */}
      <div>
        <h2 className="text-base font-semibold text-slate-300 mb-4">Các tính năng chính:</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          
          {(user.role === 'LECTURER' || user.role === 'ADMIN') && (
            <>
              <Link
                to="/lecturer/questions"
                className="group p-5 rounded-2xl bg-slate-900 border border-slate-800 hover:border-indigo-500/50 hover:bg-slate-850 transition-all flex items-start gap-4"
              >
                <div className="w-10 h-10 rounded-xl bg-indigo-500/10 text-indigo-400 flex items-center justify-center shrink-0 group-hover:scale-110 transition-transform">
                  <HelpCircle className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-semibold text-white group-hover:text-indigo-300 transition-colors">Ngân hàng câu hỏi</h3>
                  <p className="text-xs text-slate-400 mt-1">Quản lý câu hỏi, phân loại mức Bloom và bộ lọc theo môn học.</p>
                </div>
              </Link>

              <Link
                to="/lecturer/questions/generate"
                className="group p-5 rounded-2xl bg-slate-900 border border-slate-800 hover:border-amber-500/50 hover:bg-slate-850 transition-all flex items-start gap-4"
              >
                <div className="w-10 h-10 rounded-xl bg-amber-500/10 text-amber-400 flex items-center justify-center shrink-0 group-hover:scale-110 transition-transform">
                  <BrainCircuit className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-semibold text-white group-hover:text-amber-300 transition-colors">Sinh câu hỏi bằng AI</h3>
                  <p className="text-xs text-slate-400 mt-1">Tự động tạo câu hỏi theo chủ đề và các mức nhận thức Bloom.</p>
                </div>
              </Link>

              <Link
                to="/lecturer/rubrics"
                className="group p-5 rounded-2xl bg-slate-900 border border-slate-800 hover:border-indigo-500/50 hover:bg-slate-850 transition-all flex items-start gap-4"
              >
                <div className="w-10 h-10 rounded-xl bg-indigo-500/10 text-indigo-400 flex items-center justify-center shrink-0 group-hover:scale-110 transition-transform">
                  <FileText className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-semibold text-white group-hover:text-indigo-300 transition-colors">Thiết kế Rubric chấm điểm</h3>
                  <p className="text-xs text-slate-400 mt-1">Tạo tiêu chí, từ khóa trọng tâm để AI dùng hỏi xoáy sinh viên.</p>
                </div>
              </Link>

              <Link
                to="/lecturer/sessions"
                className="group p-5 rounded-2xl bg-slate-900 border border-slate-800 hover:border-cyan-500/50 hover:bg-slate-850 transition-all flex items-start gap-4"
              >
                <div className="w-10 h-10 rounded-xl bg-cyan-500/10 text-cyan-400 flex items-center justify-center shrink-0 group-hover:scale-110 transition-transform">
                  <Clock className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-semibold text-white group-hover:text-cyan-300 transition-colors">Giám sát & Biên bản thi</h3>
                  <p className="text-xs text-slate-400 mt-1">Xem chi tiết các lượt hỏi đáp, câu hỏi xoáy và văn bản trả lời của sinh viên.</p>
                </div>
              </Link>
            </>
          )}

          {(user.role === 'STUDENT' || user.role === 'ADMIN') && (
            <Link
              to="/student/interview"
              className="group p-5 rounded-2xl bg-slate-900 border border-slate-800 hover:border-cyan-500/50 hover:bg-slate-850 transition-all flex items-start gap-4"
            >
              <div className="w-10 h-10 rounded-xl bg-cyan-500/10 text-cyan-400 flex items-center justify-center shrink-0 group-hover:scale-110 transition-transform">
                <GraduationCap className="w-5 h-5" />
              </div>
              <div>
                <h3 className="font-semibold text-white group-hover:text-cyan-300 transition-colors">Phòng thi vấn đáp AI</h3>
                <p className="text-xs text-slate-400 mt-1">Lựa chọn môn học, trả lời câu hỏi trực tiếp bằng giọng nói hoặc gõ phím.</p>
              </div>
            </Link>
          )}

          {user.role === 'ADMIN' && (
            <>
              <Link
                to="/admin/users"
                className="group p-5 rounded-2xl bg-slate-900 border border-slate-800 hover:border-indigo-500/50 hover:bg-slate-850 transition-all flex items-start gap-4"
              >
                <div className="w-10 h-10 rounded-xl bg-indigo-500/10 text-indigo-400 flex items-center justify-center shrink-0 group-hover:scale-110 transition-transform">
                  <Users className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-semibold text-white group-hover:text-indigo-300 transition-colors">Quản lý người dùng</h3>
                  <p className="text-xs text-slate-400 mt-1">Phân quyền Admin, Giảng viên, Sinh viên và gán môn học phụ trách.</p>
                </div>
              </Link>

              <Link
                to="/admin/subjects"
                className="group p-5 rounded-2xl bg-slate-900 border border-slate-800 hover:border-indigo-500/50 hover:bg-slate-850 transition-all flex items-start gap-4"
              >
                <div className="w-10 h-10 rounded-xl bg-indigo-500/10 text-indigo-400 flex items-center justify-center shrink-0 group-hover:scale-110 transition-transform">
                  <BookOpen className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-semibold text-white group-hover:text-indigo-300 transition-colors">Quản lý môn học</h3>
                  <p className="text-xs text-slate-400 mt-1">Thêm, sửa mã môn, tên môn và trạng thái kích hoạt thi.</p>
                </div>
              </Link>

              <Link
                to="/admin/configs"
                className="group p-5 rounded-2xl bg-slate-900 border border-slate-800 hover:border-indigo-500/50 hover:bg-slate-850 transition-all flex items-start gap-4"
              >
                <div className="w-10 h-10 rounded-xl bg-indigo-500/10 text-indigo-400 flex items-center justify-center shrink-0 group-hover:scale-110 transition-transform">
                  <Settings className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-semibold text-white group-hover:text-indigo-300 transition-colors">Cấu hình hệ thống</h3>
                  <p className="text-xs text-slate-400 mt-1">Thời gian thi, ngôn ngữ giọng nói (vi-VN, en-US), số câu hỏi xoáy tối đa.</p>
                </div>
              </Link>
            </>
          )}

        </div>
      </div>

    </div>
  );
}
