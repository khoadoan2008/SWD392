import React from 'react';
import { useAuth } from '../context/AuthContext';
import { LogOut, User as UserIcon, Shield, Sparkles } from 'lucide-react';

export default function Navbar() {
  const { user, logout } = useAuth();

  const getRoleBadge = (role) => {
    switch (role) {
      case 'ADMIN':
        return <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-rose-500/20 text-rose-300 border border-rose-500/30">ADMIN</span>;
      case 'LECTURER':
        return <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">GIẢNG VIÊN</span>;
      case 'STUDENT':
        return <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">SINH VIÊN</span>;
      default:
        return null;
    }
  };

  return (
    <header className="sticky top-0 z-50 backdrop-blur-md bg-slate-900/80 border-b border-slate-800">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        {/* Logo */}
        <div className="flex items-center gap-3">
          <div className="h-10 w-10 rounded-xl bg-gradient-to-br from-indigo-500 to-cyan-500 flex items-center justify-center shadow-lg shadow-indigo-500/20">
            <Sparkles className="w-5 h-5 text-white" />
          </div>
          <div>
            <span className="text-lg font-bold bg-gradient-to-r from-white via-indigo-200 to-cyan-400 bg-clip-text text-transparent">
              AIVES
            </span>
            <span className="hidden sm:inline-block ml-2 text-xs px-2 py-0.5 rounded bg-slate-800 text-slate-400 border border-slate-700">
              Spring Boot + React
            </span>
          </div>
        </div>

        {/* User Info & Actions */}
        {user && (
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-3 text-right">
              <div>
                <p className="text-sm font-medium text-slate-200">{user.fullName}</p>
                <div className="flex items-center gap-1.5 justify-end">
                  {getRoleBadge(user.role)}
                  {user.studentCode && (
                    <span className="text-xs text-slate-400">({user.studentCode})</span>
                  )}
                </div>
              </div>
              <div className="w-9 h-9 rounded-full bg-slate-800 border border-slate-700 flex items-center justify-center text-slate-300">
                <UserIcon className="w-4 h-4" />
              </div>
            </div>

            <button
              onClick={logout}
              className="flex items-center gap-2 px-3.5 py-2 text-sm text-slate-300 hover:text-white bg-slate-800/80 hover:bg-rose-500/20 border border-slate-700 hover:border-rose-500/30 rounded-lg transition-all"
              title="Đăng xuất"
            >
              <LogOut className="w-4 h-4" />
              <span className="hidden sm:inline">Đăng xuất</span>
            </button>
          </div>
        )}
      </div>
    </header>
  );
}
