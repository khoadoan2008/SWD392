import React from 'react';
import { useAuth } from '../context/AuthContext';
import { Link, useLocation } from 'react-router-dom';
import { 
  Sparkles, 
  LogOut, 
  User as UserIcon, 
  BookOpen, 
  HelpCircle, 
  FileText, 
  Clock, 
  Settings, 
  Users, 
  LayoutDashboard,
  BrainCircuit,
  GraduationCap
} from 'lucide-react';

export default function Navbar() {
  const { user, logout } = useAuth();
  const location = useLocation();

  if (!user) return null;

  const role = user.role;
  const currentPath = location.pathname;

  const isActive = (path) => currentPath === path;

  return (
    <header className="sticky top-0 z-50 bg-slate-900/90 backdrop-blur-md border-b border-slate-800 text-slate-200">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        
        {/* Logo & Brand */}
        <div className="flex items-center gap-6">
          <Link to="/" className="flex items-center gap-2.5 group">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-indigo-600 to-cyan-500 flex items-center justify-center text-white shadow-lg shadow-indigo-500/20 group-hover:scale-105 transition-transform">
              <Sparkles className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-1.5">
                <span className="font-bold text-lg tracking-tight bg-gradient-to-r from-white via-slate-100 to-indigo-200 bg-clip-text text-transparent">
                  AIVES
                </span>
                <span className="text-[10px] font-semibold uppercase tracking-wider px-1.5 py-0.5 rounded bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                  AI Viva
                </span>
              </div>
              <p className="text-[10px] text-slate-400 leading-none">Smart Oral Exam System</p>
            </div>
          </Link>

          {/* Navigation links by Role */}
          <nav className="hidden md:flex items-center gap-1 text-sm font-medium">
            <Link
              to="/"
              className={`px-3 py-1.5 rounded-lg transition-colors flex items-center gap-1.5 ${
                isActive('/') ? 'bg-indigo-600/10 text-indigo-400 font-semibold' : 'text-slate-300 hover:text-white hover:bg-slate-800'
              }`}
            >
              <LayoutDashboard className="w-4 h-4" />
              Tổng quan
            </Link>

            {/* Lecturer / Admin */}
            {(role === 'LECTURER' || role === 'ADMIN') && (
              <>
                <Link
                  to="/lecturer/questions"
                  className={`px-3 py-1.5 rounded-lg transition-colors flex items-center gap-1.5 ${
                    currentPath.startsWith('/lecturer/questions') && !location.search.includes('PENDING_REVIEW')
                      ? 'bg-indigo-600/10 text-indigo-400 font-semibold'
                      : 'text-slate-300 hover:text-white hover:bg-slate-800'
                  }`}
                >
                  <HelpCircle className="w-4 h-4" />
                  Ngân hàng câu hỏi
                </Link>

                <Link
                  to="/lecturer/questions?status=PENDING_REVIEW"
                  className={`px-3 py-1.5 rounded-lg transition-colors flex items-center gap-1.5 ${
                    location.search.includes('PENDING_REVIEW')
                      ? 'bg-amber-500/10 text-amber-400 font-semibold border border-amber-500/30'
                      : 'text-amber-300/80 hover:text-amber-200 hover:bg-slate-800'
                  }`}
                >
                  <BrainCircuit className="w-4 h-4" />
                  Duyệt AI
                </Link>

                <Link
                  to="/lecturer/rubrics"
                  className={`px-3 py-1.5 rounded-lg transition-colors flex items-center gap-1.5 ${
                    currentPath.startsWith('/lecturer/rubrics') ? 'bg-indigo-600/10 text-indigo-400 font-semibold' : 'text-slate-300 hover:text-white hover:bg-slate-800'
                  }`}
                >
                  <FileText className="w-4 h-4" />
                  Tiêu chí Rubric
                </Link>

                <Link
                  to="/lecturer/sessions"
                  className={`px-3 py-1.5 rounded-lg transition-colors flex items-center gap-1.5 ${
                    currentPath.startsWith('/lecturer/sessions') ? 'bg-indigo-600/10 text-indigo-400 font-semibold' : 'text-slate-300 hover:text-white hover:bg-slate-800'
                  }`}
                >
                  <Clock className="w-4 h-4" />
                  Giám sát đợt thi
                </Link>
              </>
            )}

            {/* Student */}
            {(role === 'STUDENT' || role === 'ADMIN') && (
              <Link
                to="/student/interview"
                className={`px-3 py-1.5 rounded-lg transition-colors flex items-center gap-1.5 ${
                  currentPath.startsWith('/student/interview') ? 'bg-cyan-500/10 text-cyan-400 font-semibold border border-cyan-500/30' : 'text-cyan-300 hover:text-white hover:bg-slate-800'
                }`}
              >
                <GraduationCap className="w-4 h-4" />
                Vào phòng thi Viva
              </Link>
            )}

            {/* Admin Only */}
            {role === 'ADMIN' && (
              <>
                <Link
                  to="/admin/users"
                  className={`px-3 py-1.5 rounded-lg transition-colors flex items-center gap-1.5 ${
                    isActive('/admin/users') ? 'bg-indigo-600/10 text-indigo-400 font-semibold' : 'text-slate-300 hover:text-white hover:bg-slate-800'
                  }`}
                >
                  <Users className="w-4 h-4" />
                  Người dùng
                </Link>

                <Link
                  to="/admin/subjects"
                  className={`px-3 py-1.5 rounded-lg transition-colors flex items-center gap-1.5 ${
                    isActive('/admin/subjects') ? 'bg-indigo-600/10 text-indigo-400 font-semibold' : 'text-slate-300 hover:text-white hover:bg-slate-800'
                  }`}
                >
                  <BookOpen className="w-4 h-4" />
                  Môn học
                </Link>

                <Link
                  to="/admin/configs"
                  className={`px-3 py-1.5 rounded-lg transition-colors flex items-center gap-1.5 ${
                    isActive('/admin/configs') ? 'bg-indigo-600/10 text-indigo-400 font-semibold' : 'text-slate-300 hover:text-white hover:bg-slate-800'
                  }`}
                >
                  <Settings className="w-4 h-4" />
                  Cấu hình
                </Link>
              </>
            )}
          </nav>
        </div>

        {/* User profile & actions */}
        <div className="flex items-center gap-3">
          <div className="hidden sm:flex items-center gap-2.5 py-1 px-3 rounded-full bg-slate-800/80 border border-slate-700/60 text-xs">
            <div className="w-6 h-6 rounded-full bg-indigo-500/20 text-indigo-400 flex items-center justify-center font-bold">
              {user.fullName?.charAt(0) || user.username?.charAt(0)}
            </div>
            <div className="text-left">
              <span className="font-semibold text-slate-200 block leading-tight">{user.fullName || user.username}</span>
              <span className="text-[10px] text-slate-400 capitalize">{user.role}</span>
            </div>
          </div>

          <button
            onClick={logout}
            title="Đăng xuất"
            className="p-2 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 transition-colors"
          >
            <LogOut className="w-4 h-4" />
          </button>
        </div>

      </div>
    </header>
  );
}
