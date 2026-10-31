import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { sessionApi } from '../../api/sessionApi';
import { 
  ArrowLeft, 
  Clock, 
  CheckCircle2, 
  HelpCircle, 
  MessageSquare, 
  BrainCircuit, 
  User, 
  Loader2,
  Sparkles,
  RefreshCw,
  Award,
  FileCheck,
  Save,
  Printer,
  CopyCheck,
  AlertCircle,
  Lock
} from 'lucide-react';

export default function SessionDetailPage() {
  const { id } = useParams();
  const [session, setSession] = useState(null);
  const [loading, setLoading] = useState(true);

  // Grading Form State
  const [rubricGrades, setRubricGrades] = useState({});
  const [finalScore, setFinalScore] = useState('');
  const [lecturerFeedback, setLecturerFeedback] = useState('');
  const [savingGrade, setSavingGrade] = useState(false);
  const [reEvaluating, setReEvaluating] = useState(false);
  const [saveSuccessMsg, setSaveSuccessMsg] = useState('');
  const [saveErrorMsg, setSaveErrorMsg] = useState('');

  const loadSession = () => {
    return sessionApi.getDetail(id)
      .then((res) => {
        if (res.success && res.data) {
          const s = res.data;
          setSession(s);

          // Populate grading form state
          setFinalScore(s.finalScore != null ? String(s.finalScore) : (s.aiSuggestedScore != null ? String(s.aiSuggestedScore) : ''));
          setLecturerFeedback(s.lecturerFeedback || '');

          const rMap = {};
          if (s.rubricScores && s.rubricScores.length > 0) {
            s.rubricScores.forEach((rs) => {
              rMap[rs.rubricId] = {
                finalScore: rs.finalScore != null ? rs.finalScore : (rs.aiScore != null ? rs.aiScore : ''),
                comment: rs.comment || ''
              };
            });
          }
          setRubricGrades(rMap);
        }
      });
  };

  useEffect(() => {
    loadSession().finally(() => setLoading(false));
  }, [id]);

  // AI chấm nền sau khi buổi thi kết thúc: tự tải lại mỗi 5 giây (tối đa ~3 phút) cho tới khi có kết quả
  const aiPendingForPoll = !!session && session.status === 'COMPLETED' && !session.aiGradingSource && session.finalScore == null;
  useEffect(() => {
    if (!aiPendingForPoll) return undefined;
    let tries = 0;
    const timer = setInterval(() => {
      tries += 1;
      if (tries > 36) {
        clearInterval(timer);
        return;
      }
      loadSession();
    }, 5000);
    return () => clearInterval(timer);
  }, [aiPendingForPoll, id]);

  // Handle Lecturer input for a rubric item
  const handleRubricScoreChange = (rubricId, value, maxScore) => {
    let numVal = value === '' ? '' : parseFloat(value);
    if (!isNaN(numVal)) {
      if (numVal < 0) numVal = 0;
      if (maxScore != null && numVal > maxScore) numVal = maxScore;
    }

    setRubricGrades((prev) => {
      const updated = {
        ...prev,
        [rubricId]: {
          ...(prev[rubricId] || {}),
          finalScore: numVal
        }
      };

      // Tổng điểm các tiêu chí quy về thang 10 theo tổng điểm tối đa của rubric
      let sum = 0;
      let hasAny = false;
      Object.values(updated).forEach((item) => {
        if (item && item.finalScore !== '' && !isNaN(parseFloat(item.finalScore))) {
          sum += parseFloat(item.finalScore);
          hasAny = true;
        }
      });
      const maxSum = (session?.rubricScores || []).reduce((acc, rs) => acc + (rs.maxScore ? parseFloat(rs.maxScore) : 0), 0);
      if (hasAny && maxSum > 0) {
        setFinalScore(String(Math.min(10.0, Math.round((sum / maxSum) * 10 * 100) / 100)));
      }

      return updated;
    });
  };

  const handleRubricCommentChange = (rubricId, comment) => {
    setRubricGrades((prev) => ({
      ...prev,
      [rubricId]: {
        ...(prev[rubricId] || {}),
        comment
      }
    }));
  };

  // Quick Apply: Copy all AI suggestions into Lecturer's form
  const handleQuickApplyAi = () => {
    if (!session) return;
    if (session.aiSuggestedScore != null) {
      setFinalScore(String(session.aiSuggestedScore));
    }
    if (session.aiFeedback) {
      setLecturerFeedback(session.aiFeedback);
    }
    if (session.rubricScores && session.rubricScores.length > 0) {
      const newMap = {};
      session.rubricScores.forEach((rs) => {
        newMap[rs.rubricId] = {
          finalScore: rs.aiScore != null ? rs.aiScore : '',
          comment: rs.comment || ''
        };
      });
      setRubricGrades(newMap);
    }
    setSaveSuccessMsg('Đã điền nhanh điểm và nhận xét gợi ý từ AI vào biểu mẫu!');
    setTimeout(() => setSaveSuccessMsg(''), 4000);
  };

  // Save official grade
  const handleSaveGrade = async (e) => {
    e.preventDefault();
    setSaveSuccessMsg('');
    setSaveErrorMsg('');

    const parsedFinal = parseFloat(finalScore);
    if (isNaN(parsedFinal) || parsedFinal < 0 || parsedFinal > 10) {
      setSaveErrorMsg('Vui lòng nhập điểm tổng kết hợp lệ (từ 0.0 đến 10.0 điểm).');
      return;
    }
    if (!window.confirm(`Xác nhận lưu và KHOÁ điểm chính thức ${parsedFinal} / 10?\nSau khi khoá sẽ không thể chỉnh sửa điểm hoặc yêu cầu AI chấm lại.`)) {
      return;
    }

    const rubricItems = Object.keys(rubricGrades).map((rId) => {
      const item = rubricGrades[rId];
      return {
        rubricId: parseInt(rId, 10),
        finalScore: item.finalScore !== '' && !isNaN(parseFloat(item.finalScore)) ? parseFloat(item.finalScore) : 0.0,
        comment: item.comment || ''
      };
    });

    setSavingGrade(true);
    try {
      const res = await sessionApi.grade(id, {
        finalScore: parsedFinal,
        lecturerFeedback,
        rubricGrades: rubricItems
      });

      if (res.success) {
        setSession(res.data);
        setSaveSuccessMsg('Đã lưu & khoá điểm chính thức thành công!');
        setTimeout(() => setSaveSuccessMsg(''), 4000);
      }
    } catch (err) {
      setSaveErrorMsg(err.response?.data?.message || 'Có lỗi xảy ra khi lưu điểm.');
    } finally {
      setSavingGrade(false);
    }
  };

  // Re-evaluate with AI
  const handleReEvaluateAi = async () => {
    if (!window.confirm('Yêu cầu AI Gemini phân tích và chấm lại buổi thi này theo tiêu chí Rubric?')) return;
    setReEvaluating(true);
    setSaveSuccessMsg('');
    setSaveErrorMsg('');
    try {
      const res = await sessionApi.reEvaluate(id);
      if (res.success && res.data) {
        setSession(res.data);
        if (res.data.aiGradingSource === 'GEMINI') {
          setSaveSuccessMsg('AI Gemini đã hoàn tất đánh giá lại!');
        } else {
          setSaveErrorMsg('Gemini chưa phản hồi, hệ thống đã chấm tạm theo từ khoá rubric. Bạn có thể thử lại sau.');
        }
        setTimeout(() => { setSaveSuccessMsg(''); setSaveErrorMsg(''); }, 6000);
      }
    } catch (err) {
      setSaveErrorMsg(err.response?.data?.message || 'Lỗi khi yêu cầu AI chấm lại.');
    } finally {
      setReEvaluating(false);
    }
  };

  if (loading) {
    return (
      <div className="max-w-4xl mx-auto p-12 text-center text-slate-400 flex items-center justify-center gap-2">
        <Loader2 className="w-5 h-5 animate-spin text-indigo-400" />
        Đang tải biên bản buổi thi...
      </div>
    );
  }

  if (!session) {
    return (
      <div className="max-w-4xl mx-auto p-12 text-center text-slate-500">
        Không tìm thấy lượt thi vấn đáp này.
      </div>
    );
  }

  // Group turns by mainIndex
  const groupedTurns = {};
  if (session.turns) {
    session.turns.forEach((t) => {
      if (!groupedTurns[t.mainIndex]) {
        groupedTurns[t.mainIndex] = [];
      }
      groupedTurns[t.mainIndex].push(t);
    });
  }

  const isGraded = session.isGraded || session.finalScore != null;
  const isInProgress = session.status === 'IN_PROGRESS';
  const aiPending = !isGraded && session.status === 'COMPLETED' && !session.aiGradingSource;
  const canGrade = !isGraded && !isInProgress;
  const aiSourceBadge = {
    GEMINI: { text: 'Chấm bởi Gemini', cls: 'bg-emerald-500/10 text-emerald-300 border-emerald-500/30' },
    RULE_BASED: { text: 'Chấm dự phòng theo từ khoá (Gemini chưa phản hồi)', cls: 'bg-amber-500/10 text-amber-300 border-amber-500/30' },
    NO_RUBRIC: { text: 'Môn chưa có rubric', cls: 'bg-slate-700/40 text-slate-300 border-slate-600' },
  }[session.aiGradingSource];

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      
      {/* Top Navigation & Print Actions */}
      <div className="flex items-center justify-between no-print">
        <Link
          to="/lecturer/sessions"
          className="inline-flex items-center gap-2 text-sm text-slate-400 hover:text-white transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          Quay lại danh sách đợt thi
        </Link>

        <button
          onClick={() => window.print()}
          className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold border border-slate-700 shadow transition-colors cursor-pointer"
        >
          <Printer className="w-4 h-4 text-cyan-400" />
          In biên bản thi / Xuất PDF
        </button>
      </div>

      {/* Session Metadata Card */}
      <div className="p-6 sm:p-8 rounded-2xl bg-slate-900 border border-slate-800 shadow-xl space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-6 border-b border-slate-800">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <span className="text-xs font-mono font-bold text-indigo-400 bg-indigo-500/10 px-2.5 py-0.5 rounded border border-indigo-500/20">
                LƯỢT THI #{session.id}
              </span>
              {isGraded ? (
                <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 flex items-center gap-1">
                  <CheckCircle2 className="w-3 h-3" />
                  ĐÃ CHẤM XONG ({session.finalScore} / 10.0đ)
                </span>
              ) : (
                <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-500/10 text-amber-400 border border-amber-500/20 flex items-center gap-1">
                  <Clock className="w-3 h-3" />
                  CHỜ GIẢNG VIÊN CHẤM
                </span>
              )}
            </div>
            <h1 className="text-2xl font-bold text-white tracking-tight">Biên bản Đánh giá &amp; Chấm thi Vấn đáp AI</h1>
          </div>

          <div className="text-right flex flex-col items-end">
            <span className="text-xs text-slate-400">Trạng thái phòng thi</span>
            <span className="text-sm font-semibold text-emerald-400 font-mono">
              {session.status}
            </span>
          </div>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-6 text-xs">
          <div>
            <span className="text-slate-500 block mb-0.5">Sinh viên dự thi:</span>
            <span className="font-semibold text-white text-sm block">{session.student?.fullName || session.student?.username}</span>
            <span className="text-cyan-400 font-mono block">MSSV: {session.student?.studentCode || '—'}</span>
            <span className="text-slate-500 block mt-0.5">{session.student?.email}</span>
          </div>

          <div>
            <span className="text-slate-500 block mb-0.5">Môn học vấn đáp:</span>
            <span className="font-bold text-indigo-400 text-sm font-mono block">{session.subject?.code}</span>
            <span className="text-slate-300 font-medium block">{session.subject?.name}</span>
          </div>

          <div>
            <span className="text-slate-500 block mb-0.5">Thời gian quy định:</span>
            <span className="font-semibold text-white text-sm font-mono block">{session.answerTimeLimitSec} giây / câu</span>
            <span className="text-slate-400 block">Tối đa {session.maxFollowups} câu hỏi xoáy</span>
          </div>

          <div>
            <span className="text-slate-500 block mb-0.5">Thời gian thực hiện:</span>
            <span className="font-semibold text-white block">
              {session.startedAt ? new Date(session.startedAt).toLocaleString('vi-VN') : '—'}
            </span>
            {session.endedAt && (
              <span className="text-slate-400 block mt-0.5">
                Hoàn thành: {new Date(session.endedAt).toLocaleTimeString('vi-VN')}
              </span>
            )}
          </div>
        </div>

        {/* Graded By Footer Info if Graded */}
        {isGraded && (
          <div className="pt-4 border-t border-slate-800/80 flex items-center justify-between text-xs text-slate-400 bg-emerald-950/20 -mx-6 -mb-6 p-4 rounded-b-2xl border-emerald-900/40">
            <div className="flex items-center gap-2">
              <FileCheck className="w-4 h-4 text-emerald-400" />
              <span>Điểm số chính thức đã được phê duyệt và lưu vào học bạ bởi:</span>
              <strong className="text-white">{session.gradedBy?.fullName || session.gradedBy?.username || 'Giảng viên'}</strong>
            </div>
            {session.gradedAt && (
              <span className="font-mono text-slate-400">
                Lúc: {new Date(session.gradedAt).toLocaleString('vi-VN')}
              </span>
            )}
          </div>
        )}
      </div>

      {/* Notifications */}
      {saveSuccessMsg && (
        <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 text-sm flex items-center gap-2 shadow-lg">
          <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
          <span>{saveSuccessMsg}</span>
        </div>
      )}

      {saveErrorMsg && (
        <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-sm flex items-center gap-2 shadow-lg">
          <AlertCircle className="w-5 h-5 text-rose-400 shrink-0" />
          <span>{saveErrorMsg}</span>
        </div>
      )}

      {/* ========================================================================= */}
      {/* KHỐI 1: GỢI Ý ĐÁNH GIÁ TỪ AI THEO RUBRIC (AI SUGGESTED ASSESSMENT)        */}
      {/* ========================================================================= */}
      <div className="rounded-2xl bg-slate-900 border border-indigo-950/60 shadow-xl overflow-hidden">
        <div className="bg-gradient-to-r from-indigo-950/80 via-slate-900 to-slate-900 px-6 py-4 border-b border-indigo-900/30 flex items-center justify-between flex-wrap gap-3">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-indigo-500/20 text-indigo-400 flex items-center justify-center border border-indigo-500/30">
              <Sparkles className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-base font-bold text-white tracking-tight flex items-center gap-2">
                Gợi ý Đánh giá từ Trợ lý AI (Gemini / Heuristic Engine)
              </h2>
              <p className="text-xs text-slate-400">
                Phân tích đối sánh câu trả lời của sinh viên với ma trận tiêu chí Rubric của môn học
              </p>
              {aiSourceBadge && (
                <span className={`inline-block mt-1.5 px-2 py-0.5 rounded-full text-[11px] font-semibold border ${aiSourceBadge.cls}`}>
                  {aiSourceBadge.text}
                </span>
              )}
            </div>
          </div>

          <div className="flex items-center gap-3">
            <div className="px-3.5 py-1.5 rounded-xl bg-indigo-500/10 border border-indigo-500/30 text-right">
              <span className="text-[10px] text-indigo-300 uppercase font-semibold block">Điểm AI Đề xuất</span>
              <span className="text-lg font-bold text-indigo-300 font-mono">
                {session.aiSuggestedScore != null ? `${session.aiSuggestedScore} / 10.0` : '—'}
              </span>
            </div>

            {canGrade && (
            <button
              type="button"
              onClick={handleReEvaluateAi}
              disabled={reEvaluating}
              className="px-3 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white text-xs font-semibold border border-slate-700 flex items-center gap-1.5 transition-colors cursor-pointer disabled:opacity-50"
              title="Yêu cầu AI phân tích và chấm lại"
            >
              <RefreshCw className={`w-3.5 h-3.5 text-indigo-400 ${reEvaluating ? 'animate-spin' : ''}`} />
              <span>{reEvaluating ? 'Đang chấm lại...' : 'Chấm lại bằng AI'}</span>
            </button>
            )}
          </div>
        </div>

        <div className="p-6 space-y-6">
          {aiPending && (
            <div className="p-4 rounded-xl bg-indigo-500/10 border border-indigo-500/30 text-indigo-200 text-sm flex items-center gap-2">
              <Loader2 className="w-4 h-4 animate-spin text-indigo-300 shrink-0" />
              <span>AI đang phân tích biên bản và chấm điểm theo rubric... Trang sẽ tự cập nhật khi có kết quả.</span>
            </div>
          )}
          {isInProgress && (
            <div className="p-4 rounded-xl bg-amber-500/10 border border-amber-500/30 text-amber-200 text-sm flex items-center gap-2">
              <Clock className="w-4 h-4 text-amber-300 shrink-0" />
              <span>Sinh viên đang làm bài. AI sẽ tự chấm khi buổi thi kết thúc.</span>
            </div>
          )}

          {/* AI General Feedback */}
          {session.aiFeedback && (
            <div className="p-4 rounded-xl bg-slate-950/70 border border-indigo-900/30 space-y-1.5">
              <span className="text-xs font-bold text-indigo-400 flex items-center gap-1.5 uppercase tracking-wider">
                <BrainCircuit className="w-3.5 h-3.5" />
                Nhận xét tổng quan của AI:
              </span>
              <p className="text-sm text-slate-200 leading-relaxed italic whitespace-pre-line">
                "{session.aiFeedback}"
              </p>
            </div>
          )}

          {/* Rubric Breakdown List */}
          <div className="space-y-4">
            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400">
              Chi tiết điểm đề xuất theo từng tiêu chí Rubric:
            </h3>

            {(!session.rubricScores || session.rubricScores.length === 0) ? (
              <div className="p-6 text-center rounded-xl bg-slate-950 border border-slate-800 text-slate-500 text-sm">
                Môn học này chưa có tiêu chí Rubric hoặc AI chưa thực hiện chấm điểm.
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {session.rubricScores.map((rs) => {
                  const max = rs.maxScore ? parseFloat(rs.maxScore) : 10.0;
                  const score = rs.aiScore ? parseFloat(rs.aiScore) : 0.0;
                  const pct = max > 0 ? Math.min(100, Math.round((score / max) * 100)) : 0;

                  return (
                    <div key={rs.id || rs.rubricId} className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-2.5">
                      <div className="flex items-start justify-between gap-2">
                        <span className="text-sm font-semibold text-white leading-tight">
                          {rs.criterion}
                        </span>
                        <span className="px-2 py-0.5 rounded text-xs font-mono font-bold bg-indigo-500/10 text-indigo-300 border border-indigo-500/20 shrink-0">
                          {score} / {max}đ
                        </span>
                      </div>

                      {rs.keywords && (
                        <div className="text-[11px] text-slate-400">
                          <span className="text-slate-500 font-medium">Từ khóa đối sánh: </span>
                          <span className="font-mono text-cyan-400/90">{rs.keywords}</span>
                        </div>
                      )}

                      {/* Progress Bar */}
                      <div className="w-full bg-slate-800 rounded-full h-1.5 overflow-hidden">
                        <div 
                          className="bg-indigo-500 h-1.5 rounded-full transition-all duration-500" 
                          style={{ width: `${pct}%` }} 
                        />
                      </div>

                      {rs.comment && (
                        <p className="text-xs text-slate-300 italic bg-slate-900/60 p-2.5 rounded-lg border border-slate-800/80">
                          "{rs.comment}"
                        </p>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* ========================================================================= */}
      {/* KHỐI 2: FORM CHẤM ĐIỂM CHÍNH THỨC CỦA GIẢNG VIÊN (OFFICIAL GRADING FORM)   */}
      {/* ========================================================================= */}
      <form onSubmit={handleSaveGrade} className="rounded-2xl bg-slate-900 border border-slate-800 shadow-xl overflow-hidden space-y-6">
        <div className="bg-slate-950 px-6 py-4 border-b border-slate-800 flex items-center justify-between flex-wrap gap-4">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-emerald-500/20 text-emerald-400 flex items-center justify-center border border-emerald-500/30">
              <Award className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-base font-bold text-white tracking-tight flex items-center gap-2">
                Hội đồng Giảng viên: Chấm điểm &amp; Phê duyệt Điểm số Chính thức
              </h2>
              <p className="text-xs text-slate-400">
                Giảng viên giữ quyền quyết định cuối cùng; có thể chỉnh sửa điểm từng tiêu chí hoặc áp dụng nhanh gợi ý từ AI
              </p>
            </div>
          </div>

          {canGrade && (
          <button
            type="button"
            onClick={handleQuickApplyAi}
            className="px-3.5 py-1.5 rounded-xl bg-indigo-600/20 hover:bg-indigo-600/30 text-indigo-300 border border-indigo-500/30 text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer"
            title="Điền nhanh toàn bộ điểm và nhận xét của AI vào biểu mẫu"
          >
            <CopyCheck className="w-3.5 h-3.5 text-indigo-400" />
            <span>Sử dụng gợi ý của AI (Quick Apply)</span>
          </button>
          )}
        </div>

        {isGraded && (
          <div className="mx-6 p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-200 text-sm flex items-center gap-2">
            <Lock className="w-4 h-4 text-emerald-300 shrink-0" />
            <span>Điểm chính thức đã được khoá. Không thể chỉnh sửa điểm hoặc yêu cầu AI chấm lại.</span>
          </div>
        )}
        {isInProgress && (
          <div className="mx-6 p-4 rounded-xl bg-amber-500/10 border border-amber-500/30 text-amber-200 text-sm flex items-center gap-2">
            <Clock className="w-4 h-4 text-amber-300 shrink-0" />
            <span>Chỉ có thể chấm điểm sau khi sinh viên hoàn thành buổi thi.</span>
          </div>
        )}
        <fieldset disabled={!canGrade} className="p-6 space-y-6 disabled:opacity-80">
          {/* Rubric Criteria Evaluation Table */}
          {session.rubricScores && session.rubricScores.length > 0 && (
            <div className="space-y-3">
              <span className="text-xs font-bold uppercase tracking-wider text-slate-400 block">
                Bảng thẩm định chi tiết theo tiêu chí Rubric:
              </span>

              <div className="overflow-x-auto rounded-xl border border-slate-800 bg-slate-950">
                <table className="w-full text-left text-sm text-slate-300">
                  <thead className="bg-slate-900 text-xs uppercase tracking-wider text-slate-400 border-b border-slate-800">
                    <tr>
                      <th className="px-4 py-3">Tiêu chí đánh giá</th>
                      <th className="px-3 py-3 w-28 text-center">Gợi ý AI</th>
                      <th className="px-3 py-3 w-36 text-center">Điểm chốt của GV</th>
                      <th className="px-4 py-3">Nhận xét của Giảng viên</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60">
                    {session.rubricScores.map((rs) => {
                      const gradeItem = rubricGrades[rs.rubricId] || {};
                      const max = rs.maxScore ? parseFloat(rs.maxScore) : 10.0;

                      return (
                        <tr key={rs.rubricId} className="hover:bg-slate-900/40 transition-colors">
                          <td className="px-4 py-3 align-top">
                            <span className="font-semibold text-white block">{rs.criterion}</span>
                            {rs.keywords && (
                              <span className="text-xs text-slate-500 font-mono block mt-0.5">
                                Từ khóa: {rs.keywords}
                              </span>
                            )}
                          </td>

                          <td className="px-3 py-3 text-center align-middle">
                            <span className="font-mono text-xs text-indigo-300 font-semibold px-2 py-1 bg-indigo-500/10 rounded border border-indigo-500/20">
                              {rs.aiScore != null ? rs.aiScore : '—'} / {max}đ
                            </span>
                          </td>

                          <td className="px-3 py-3 text-center align-middle">
                            <div className="flex items-center justify-center gap-1">
                              <input
                                type="number"
                                step="0.25"
                                min="0"
                                max={max}
                                value={gradeItem.finalScore !== undefined ? gradeItem.finalScore : ''}
                                onChange={(e) => handleRubricScoreChange(rs.rubricId, e.target.value, max)}
                                className="w-20 bg-slate-900 border border-slate-700 rounded-lg px-2.5 py-1.5 text-center text-sm font-mono font-bold text-emerald-400 focus:outline-none focus:border-emerald-500"
                                placeholder="0.0"
                              />
                              <span className="text-xs text-slate-500 font-mono">/{max}</span>
                            </div>
                          </td>

                          <td className="px-4 py-3 align-middle">
                            <input
                              type="text"
                              value={gradeItem.comment || ''}
                              onChange={(e) => handleRubricCommentChange(rs.rubricId, e.target.value)}
                              placeholder="Nhận xét cụ thể cho tiêu chí này..."
                              className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-1.5 text-xs text-slate-200 placeholder-slate-600 focus:outline-none focus:border-indigo-500"
                            />
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* Final Score and Lecturer Summary Feedback */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-6 pt-2">
            <div className="sm:col-span-1 space-y-2">
              <label className="text-xs font-bold text-slate-300 uppercase tracking-wider block">
                Tổng điểm chính thức (Thang 10): <span className="text-rose-400">*</span>
              </label>
              <div className="flex items-center gap-2">
                <input
                  type="number"
                  step="0.1"
                  min="0"
                  max="10"
                  required
                  value={finalScore}
                  onChange={(e) => setFinalScore(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-700 rounded-xl px-4 py-3 text-xl font-mono font-bold text-emerald-400 focus:outline-none focus:border-emerald-500"
                  placeholder="0.0"
                />
                <span className="text-sm font-mono text-slate-500 font-bold">/ 10.0đ</span>
              </div>
              <p className="text-[11px] text-slate-500">
                Điểm tổng kết chính thức được ghi nhận vào bảng điểm của sinh viên.
              </p>
            </div>

            <div className="sm:col-span-2 space-y-2">
              <label className="text-xs font-bold text-slate-300 uppercase tracking-wider block">
                Nhận xét tổng kết của Giảng viên / Hội đồng:
              </label>
              <textarea
                rows="3"
                value={lecturerFeedback}
                onChange={(e) => setLecturerFeedback(e.target.value)}
                placeholder="Nhận xét chung về năng lực phản biện, độ sâu kiến trúc và tác phong của sinh viên..."
                className="w-full bg-slate-950 border border-slate-700 rounded-xl p-3 text-sm text-slate-200 placeholder-slate-600 focus:outline-none focus:border-indigo-500 leading-relaxed"
              />
            </div>
          </div>

          {/* Action Footer */}
          {canGrade && (
          <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-800">
            <button
              type="submit"
              disabled={savingGrade}
              className="px-6 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-sm shadow-lg shadow-emerald-600/20 flex items-center gap-2 transition-all cursor-pointer disabled:opacity-50"
            >
              {savingGrade ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Đang lưu điểm...</span>
                </>
              ) : (
                <>
                  <Save className="w-4 h-4" />
                  <span>Lưu &amp; Khoá điểm chính thức</span>
                </>
              )}
            </button>
          </div>
          )}
        </fieldset>
      </form>

      {/* ========================================================================= */}
      {/* KHỐI 3: TIẾN TRÌNH HỎI ĐÁP & VĂN BẢN GHI NHẬN (Q&A TRANSCRIPTS)           */}
      {/* ========================================================================= */}
      <div className="space-y-6">
        <h2 className="text-base font-bold text-white flex items-center gap-2">
          <MessageSquare className="w-4 h-4 text-cyan-400" />
          Tiến trình hỏi đáp &amp; Văn bản ghi nhận (Transcripts)
        </h2>

        {Object.keys(groupedTurns).length === 0 ? (
          <div className="p-8 text-center rounded-2xl bg-slate-900 border border-slate-800 text-slate-500">
            Chưa có lượt hỏi đáp nào được ghi nhận.
          </div>
        ) : (
          Object.keys(groupedTurns).map((mainIdx) => {
            const turns = groupedTurns[mainIdx];
            return (
              <div key={mainIdx} className="rounded-2xl bg-slate-900 border border-slate-800 overflow-hidden shadow-xl">
                <div className="bg-slate-950 px-6 py-3 border-b border-slate-800 flex items-center justify-between">
                  <span className="font-bold text-sm text-indigo-400 font-mono">
                    CHỦ ĐỀ CÂU HỎI CHÍNH #{mainIdx}
                  </span>
                  <span className="text-xs text-slate-500">
                    {turns.length} lượt tương tác (Gồm câu chính và câu hỏi xoáy)
                  </span>
                </div>

                <div className="p-6 space-y-6 divide-y divide-slate-800/60">
                  {turns.map((t, idx) => (
                    <div key={t.id} className={idx > 0 ? 'pt-6 space-y-4' : 'space-y-4'}>
                      
                      {/* Question Box */}
                      <div className="flex items-start gap-3">
                        <div className={`w-8 h-8 rounded-xl flex items-center justify-center shrink-0 ${
                          t.isFollowUp || t.turnType === 'FOLLOW_UP' 
                            ? 'bg-amber-500/10 text-amber-400 border border-amber-500/20' 
                            : 'bg-indigo-500/10 text-indigo-400 border border-indigo-500/20'
                        }`}>
                          {t.isFollowUp || t.turnType === 'FOLLOW_UP' ? (
                            <BrainCircuit className="w-4 h-4" />
                          ) : (
                            <HelpCircle className="w-4 h-4" />
                          )}
                        </div>
                        <div className="flex-1 space-y-1">
                          <div className="flex items-center justify-between">
                            <span className="text-xs font-bold text-slate-400">
                              {t.isFollowUp || t.turnType === 'FOLLOW_UP'
                                ? `AI Hỏi xoáy (Follow-up #${t.followupIndex})`
                                : `Câu hỏi chính #${t.mainIndex}`}
                            </span>
                            {t.responseTimeSec != null && (
                              <span className="text-[11px] text-cyan-400 font-mono flex items-center gap-1">
                                <Clock className="w-3 h-3" />
                                Thời gian trả lời: {t.responseTimeSec}s
                              </span>
                            )}
                          </div>
                          <p className="text-sm font-semibold text-white leading-relaxed">
                            {t.questionText}
                          </p>
                        </div>
                      </div>

                      {/* Student Answer Transcript Box */}
                      <div className="ml-11 p-4 rounded-xl bg-slate-950 border border-slate-800/80 space-y-2">
                        <div className="flex items-center gap-1.5 text-xs text-slate-400 font-semibold">
                          <User className="w-3.5 h-3.5 text-cyan-400" />
                          <span>Câu trả lời của sinh viên:</span>
                        </div>
                        <p className="text-sm text-slate-200 leading-relaxed italic">
                          {t.transcript ? `"${t.transcript}"` : <span className="text-slate-600 not-italic">(Chưa có câu trả lời hoặc hết giờ)</span>}
                        </p>
                      </div>

                    </div>
                  ))}
                </div>
              </div>
            );
          })
        )}
      </div>

    </div>
  );
}
