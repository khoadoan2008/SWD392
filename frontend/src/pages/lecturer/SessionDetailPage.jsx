import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { sessionApi } from '../../api/sessionApi';
import { 
  ArrowLeft, 
  Clock, 
  GraduationCap, 
  BookOpen, 
  CheckCircle2, 
  HelpCircle, 
  MessageSquare, 
  BrainCircuit, 
  User, 
  Loader2 
} from 'lucide-react';

export default function SessionDetailPage() {
  const { id } = useParams();
  const [session, setSession] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    sessionApi.getDetail(id)
      .then((res) => {
        if (res.success) {
          setSession(res.data);
        }
      })
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) {
    return (
      <div className="max-w-4xl mx-auto p-12 text-center text-slate-400 flex items-center justify-center gap-2">
        <Loader2 className="w-5 h-5 animate-spin" />
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

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      
      {/* Back button */}
      <div>
        <Link
          to="/lecturer/sessions"
          className="inline-flex items-center gap-2 text-sm text-slate-400 hover:text-white transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          Quay lại danh sách đợt thi
        </Link>
      </div>

      {/* Session Metadata Card */}
      <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800 shadow-xl space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-slate-800">
          <div>
            <span className="text-xs font-mono font-bold text-indigo-400 bg-indigo-500/10 px-2 py-0.5 rounded">
              LƯỢT THI #{session.id}
            </span>
            <h1 className="text-xl font-bold text-white mt-1">Biên bản thi vấn đáp AI</h1>
          </div>
          <span className="px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
            {session.status}
          </span>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-xs">
          <div>
            <span className="text-slate-500 block">Sinh viên:</span>
            <span className="font-semibold text-white text-sm">{session.student?.fullName || session.student?.username}</span>
            <span className="text-cyan-400 font-mono block">MSSV: {session.student?.studentCode || '—'}</span>
          </div>

          <div>
            <span className="text-slate-500 block">Môn học:</span>
            <span className="font-semibold text-indigo-400 text-sm font-mono">{session.subject?.code}</span>
            <span className="text-slate-300 block">{session.subject?.name}</span>
          </div>

          <div>
            <span className="text-slate-500 block">Thời gian quy định:</span>
            <span className="font-semibold text-white text-sm font-mono">{session.answerTimeLimitSec} giây / câu</span>
            <span className="text-slate-400 block">Tối đa {session.maxFollowups} câu xoáy</span>
          </div>

          <div>
            <span className="text-slate-500 block">Thời điểm bắt đầu:</span>
            <span className="font-semibold text-white block">
              {session.startedAt ? new Date(session.startedAt).toLocaleString('vi-VN') : '—'}
            </span>
            {session.endedAt && (
              <span className="text-slate-400 block">
                Kết thúc: {new Date(session.endedAt).toLocaleTimeString('vi-VN')}
              </span>
            )}
          </div>
        </div>
      </div>

      {/* Chronological Q&A Thread */}
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
