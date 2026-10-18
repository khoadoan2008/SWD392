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

export default function DashboardPage() {
  const { user } = useAuth();
  const [stats, setStats] = useState(null);
  const [sessionStats, setSessionStats] = useState(null);
  const [userStats, setUserStats] = useState(null);

  useEffect(() => {
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
