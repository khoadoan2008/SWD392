import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { Lock, User, ArrowRight, Sparkles, Shield, AlertCircle } from 'lucide-react';

export default function LoginPage() {
  const { login, loading } = useAuth();
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('admin123');
  const [errorMessage, setErrorMessage] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    const result = await login(username, password);
    if (!result.success) {
      setErrorMessage(result.message);
    }
  };

  const handleSelectDemo = (u, p) => {
    setUsername(u);
    setPassword(p);
    setErrorMessage('');
  };

  return (
    <div className="min-h-[calc(100vh-4rem)] flex items-center justify-center p-4 relative overflow-hidden bg-slate-950">
      {/* Background Glow Orbs */}
      <div className="absolute top-1/4 -left-20 w-96 h-96 bg-indigo-600/20 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-1/4 -right-20 w-96 h-96 bg-cyan-600/15 rounded-full blur-3xl pointer-events-none" />

      <div className="w-full max-w-md relative z-10">
        {/* Card Header */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 border border-indigo-500/20 text-indigo-300 text-xs font-medium mb-4">
            <Sparkles className="w-3.5 h-3.5 text-cyan-400" />
            Hệ thống vấn đáp thông minh AI Viva
          </div>
          <h1 className="text-3xl font-extrabold text-white tracking-tight">
            Đăng nhập AIVES
          </h1>
          <p className="mt-2 text-sm text-slate-400">
            Nâng cấp kiến trúc <span className="text-indigo-400 font-semibold">Spring Boot 3</span> + <span className="text-cyan-400 font-semibold">React JS</span>
          </p>
        </div>

        {/* Login Form Box */}
        <div className="backdrop-blur-xl bg-slate-900/70 border border-slate-800 rounded-2xl p-6 sm:p-8 shadow-2xl shadow-indigo-950/50">
          {errorMessage && (
            <div className="mb-6 p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/30 flex items-start gap-3 text-rose-300 text-sm">
              <AlertCircle className="w-5 h-5 flex-shrink-0 text-rose-400 mt-0.5" />
              <span>{errorMessage}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-2">
                Tên đăng nhập
              </label>
              <div className="relative">
                <User className="w-5 h-5 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  required
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  placeholder="Nhập tên đăng nhập..."
                  className="w-full pl-11 pr-4 py-3 bg-slate-950/60 border border-slate-800 rounded-xl text-slate-100 placeholder-slate-600 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all text-sm"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300 mb-2">
                Mật khẩu
              </label>
              <div className="relative">
                <Lock className="w-5 h-5 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Nhập mật khẩu..."
                  className="w-full pl-11 pr-4 py-3 bg-slate-950/60 border border-slate-800 rounded-xl text-slate-100 placeholder-slate-600 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all text-sm"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3 px-4 rounded-xl font-medium text-sm text-white bg-gradient-to-r from-indigo-600 to-indigo-500 hover:from-indigo-500 hover:to-indigo-400 active:scale-[0.99] transition-all shadow-lg shadow-indigo-600/30 flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
            >
              {loading ? (
                <span>Đang xác thực với máy chủ...</span>
              ) : (
                <>
                  <span>Xác thực & Đăng nhập</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          {/* Quick Demo Accounts */}
          <div className="mt-8 pt-6 border-t border-slate-800">
            <p className="text-xs font-medium text-slate-400 mb-3 flex items-center gap-1.5">
              <Shield className="w-3.5 h-3.5 text-indigo-400" />
              Tài khoản mẫu từ cơ sở dữ liệu:
            </p>
            <div className="grid grid-cols-3 gap-2">
              <button
                type="button"
                onClick={() => handleSelectDemo('admin', 'admin123')}
                className={`px-2.5 py-2 rounded-lg text-xs font-medium border text-center transition-all cursor-pointer ${
                  username === 'admin'
                    ? 'bg-rose-500/20 text-rose-300 border-rose-500/40'
                    : 'bg-slate-950/40 text-slate-400 border-slate-800 hover:border-slate-700'
                }`}
              >
                Admin
              </button>
              <button
                type="button"
                onClick={() => handleSelectDemo('lecturer1', '123456')}
                className={`px-2.5 py-2 rounded-lg text-xs font-medium border text-center transition-all cursor-pointer ${
                  username === 'lecturer1'
                    ? 'bg-indigo-500/20 text-indigo-300 border-indigo-500/40'
                    : 'bg-slate-950/40 text-slate-400 border-slate-800 hover:border-slate-700'
                }`}
              >
                Giảng viên
              </button>
              <button
                type="button"
                onClick={() => handleSelectDemo('student1', '123456')}
                className={`px-2.5 py-2 rounded-lg text-xs font-medium border text-center transition-all cursor-pointer ${
                  username === 'student1'
                    ? 'bg-emerald-500/20 text-emerald-300 border-emerald-500/40'
                    : 'bg-slate-950/40 text-slate-400 border-slate-800 hover:border-slate-700'
                }`}
              >
                Sinh viên
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
