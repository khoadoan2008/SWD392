import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { sessionApi } from '../../api/sessionApi';
import { subjectApi } from '../../api/subjectApi';
import { 
  Clock, 
  ExternalLink, 
  Trash2, 
  Loader2,
  BookOpen
} from 'lucide-react';

const STATUS_MAP = {
  IN_PROGRESS: { label: 'Đang thi', color: 'bg-cyan-500/10 text-cyan-400 border-cyan-500/20' },
  COMPLETED: { label: 'Hoàn thành', color: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' },
  ABORTED: { label: 'Đã huỷ', color: 'bg-rose-500/10 text-rose-400 border-rose-500/20' },
};

export default function SessionsPage() {
  const [sessions, setSessions] = useState([]);
  const [subjects, setSubjects] = useState([]);
  const [selectedSubjectId, setSelectedSubjectId] = useState('');
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchSessions = async () => {
    setLoading(true);
    try {
      const params = {};
      if (selectedSubjectId) params.subjectId = selectedSubjectId;

      const [resSessions, resStats, resSubjects] = await Promise.all([
        sessionApi.search(params),
        sessionApi.getStats(),
        subjectApi.getAll()
      ]);

      if (resSessions.success) setSessions(resSessions.data);
      if (resStats.success) setStats(resStats.data);
      if (resSubjects.success) setSubjects(resSubjects.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSessions();
  }, [selectedSubjectId]);

  const handleDelete = async (id, studentName) => {
    if (!window.confirm(`Xoá lượt thi vấn đáp của sinh viên "${studentName}"?`)) return;
    try {
      await sessionApi.delete(id);
      fetchSessions();
    } catch (err) {
      alert(err.response?.data?.message || 'Lỗi xoá lượt thi');
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      
      {/* Header */}
      <div>
        <div className="flex items-center gap-2 text-indigo-400 text-xs font-semibold uppercase tracking-wider mb-1">
          <Clock className="w-4 h-4" />
          Giám sát phòng thi
        </div>
        <h1 className="text-2xl font-bold text-white tracking-tight">Đợt thi vấn đáp &amp; Biên bản hỏi đáp</h1>
        <p className="text-sm text-slate-400">
          Theo dõi toàn bộ các buổi thi vấn đáp của sinh viên, xem văn bản câu trả lời, gợi ý chấm điểm của AI và điểm chốt chính thức
        </p>
      </div>

      {/* Stats Cards */}
      {stats && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <div className="p-4 rounded-xl bg-slate-900 border border-slate-800">
            <span className="text-xs text-slate-400 font-semibold block uppercase">Tổng lượt thi</span>
            <span className="text-2xl font-bold text-white mt-1 block font-mono">{stats.TOTAL || 0}</span>
          </div>

          <div className="p-4 rounded-xl bg-cyan-500/10 border border-cyan-500/20">
            <span className="text-xs text-cyan-400 font-semibold block uppercase">Đang thi</span>
            <span className="text-2xl font-bold text-cyan-300 mt-1 block font-mono">{stats.IN_PROGRESS || 0}</span>
          </div>

          <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/20">
            <span className="text-xs text-emerald-400 font-semibold block uppercase">Hoàn thành</span>
            <span className="text-2xl font-bold text-emerald-300 mt-1 block font-mono">{stats.COMPLETED || 0}</span>
          </div>

          <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/20">
            <span className="text-xs text-rose-400 font-semibold block uppercase">Đã huỷ</span>
            <span className="text-2xl font-bold text-rose-300 mt-1 block font-mono">{stats.ABORTED || 0}</span>
          </div>
        </div>
      )}

      {/* Filter */}
      <div className="p-4 rounded-2xl bg-slate-900 border border-slate-800 flex items-center gap-3">
        <BookOpen className="w-4 h-4 text-slate-500" />
        <select
          value={selectedSubjectId}
          onChange={(e) => setSelectedSubjectId(e.target.value)}
          className="bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500"
        >
          <option value="">Tất cả môn học</option>
          {subjects.map((s) => (
            <option key={s.id} value={s.id}>
              {s.code} - {s.name}
            </option>
          ))}
        </select>
      </div>

      {/* Table */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
        {loading ? (
          <div className="p-12 text-center text-slate-400 flex items-center justify-center gap-2">
            <Loader2 className="w-5 h-5 animate-spin" />
            Đang tải danh sách lượt thi...
          </div>
        ) : sessions.length === 0 ? (
          <div className="p-12 text-center text-slate-500">Chưa có lượt thi vấn đáp nào.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-300">
              <thead className="bg-slate-950/60 text-xs uppercase tracking-wider text-slate-400 border-b border-slate-800">
                <tr>
                  <th className="px-6 py-4">Mã lượt</th>
                  <th className="px-6 py-4">Sinh viên</th>
                  <th className="px-6 py-4">Môn học</th>
                  <th className="px-6 py-4">Trạng thái</th>
                  <th className="px-6 py-4 text-center">Điểm số</th>
                  <th className="px-6 py-4">Thời gian thi</th>
                  <th className="px-6 py-4 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {sessions.map((s) => {
                  const statusObj = STATUS_MAP[s.status] || STATUS_MAP.IN_PROGRESS;
                  return (
                    <tr key={s.id} className="hover:bg-slate-850/50 transition-colors">
                      <td className="px-6 py-4 font-mono font-bold text-white">#{s.id}</td>
                      <td className="px-6 py-4">
                        <span className="font-semibold text-white block">{s.student?.fullName || s.student?.username}</span>
                        <span className="text-xs text-cyan-400 font-mono">MSSV: {s.student?.studentCode || '—'}</span>
                      </td>
                      <td className="px-6 py-4 font-medium text-slate-200">
                        <span className="font-mono text-indigo-400 font-bold block">{s.subject?.code}</span>
                        <span className="text-xs text-slate-400">{s.subject?.name}</span>
                      </td>
                      <td className="px-6 py-4">
                        <span className={`inline-block text-xs font-semibold px-2.5 py-1 rounded-full border ${statusObj.color}`}>
                          {statusObj.label}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-center">
                        {s.finalScore != null ? (
                          <div className="inline-flex flex-col items-center">
                            <span className="px-2.5 py-0.5 rounded-full text-xs font-bold font-mono bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                              {s.finalScore} / 10đ
                            </span>
                            <span className="text-[10px] text-emerald-500 mt-0.5 font-medium">Đã chốt điểm</span>
                          </div>
                        ) : s.aiSuggestedScore != null ? (
                          <div className="inline-flex flex-col items-center">
                            <span className="px-2 py-0.5 rounded text-xs font-mono font-semibold bg-indigo-500/10 text-indigo-300 border border-indigo-500/20">
                              AI: {s.aiSuggestedScore}đ
                            </span>
                            <span className="text-[10px] text-amber-400 mt-0.5 font-medium">Chờ GV chốt</span>
                          </div>
                        ) : (
                          <span className="text-xs text-slate-500 font-mono">—</span>
                        )}
                      </td>
                      <td className="px-6 py-4 text-xs text-slate-400">
                        <div>Bắt đầu: {s.startedAt ? new Date(s.startedAt).toLocaleString('vi-VN') : '—'}</div>
                        {s.endedAt && <div>Kết thúc: {new Date(s.endedAt).toLocaleString('vi-VN')}</div>}
                      </td>
                      <td className="px-6 py-4 text-right space-x-2">
                        <Link
                          to={`/lecturer/sessions/${s.id}`}
                          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-indigo-600/10 hover:bg-indigo-600/20 text-indigo-400 text-xs font-semibold transition-colors"
                          title="Xem biên bản & Chấm điểm"
                        >
                          <ExternalLink className="w-3.5 h-3.5" />
                          {s.finalScore != null ? 'Xem biên bản' : 'Chấm điểm'}
                        </Link>
                        <button
                          onClick={() => handleDelete(s.id, s.student?.fullName || s.student?.username)}
                          className="p-1.5 text-slate-500 hover:text-rose-400 hover:bg-slate-800 rounded-lg transition-colors cursor-pointer"
                          title="Xoá lượt thi"
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
  );
}
