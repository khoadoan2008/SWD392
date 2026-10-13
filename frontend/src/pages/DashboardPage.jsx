import React from 'react';
import { useAuth } from '../context/AuthContext';
import { 
  Layers, 
  HelpCircle, 
  Mic, 
  Settings, 
  CheckCircle2, 
  Cpu, 
  ShieldCheck, 
  Database,
  ArrowUpRight 
} from 'lucide-react';

export default function DashboardPage() {
  const { user } = useAuth();

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Welcome Banner */}
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-r from-indigo-950 via-slate-900 to-slate-900 border border-indigo-900/50 p-6 sm:p-8">
        <div className="relative z-10 max-w-3xl">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-xs font-semibold mb-3">
            <CheckCircle2 className="w-3.5 h-3.5" />
            Đã kết nối thành công: Spring Boot 3 + PostgreSQL (JWT Authenticated)
          </div>
          <h2 className="text-2xl sm:text-3xl font-extrabold text-white">
            Xin chào, {user.fullName}!
          </h2>
          <p className="mt-2 text-sm sm:text-base text-slate-300">
            Hệ thống đang hoạt động trên kiến trúc <strong className="text-indigo-400">Decoupled Architecture</strong>. 
            Mọi yêu cầu dữ liệu hiện được thực hiện thông qua chuẩn <strong className="text-cyan-400">RESTful API</strong> và bảo vệ bởi <strong className="text-emerald-400">Spring Security + JJWT</strong>.
          </p>
        </div>
      </div>

      {/* Architecture Highlights & Status */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        <div className="p-5 rounded-2xl bg-slate-900/70 border border-slate-800 flex items-start gap-4">
          <div className="p-3 rounded-xl bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
            <Cpu className="w-6 h-6" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-white uppercase tracking-wider">Backend Core</h3>
            <p className="text-xs text-slate-400 mt-1">Spring Boot 3.3.4 (Java 21)</p>
            <p className="text-xs text-slate-500 mt-0.5">REST Controller · Service Layer · Spring Data JPA</p>
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-slate-900/70 border border-slate-800 flex items-start gap-4">
          <div className="p-3 rounded-xl bg-cyan-500/10 text-cyan-400 border border-cyan-500/20">
            <Layers className="w-6 h-6" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-white uppercase tracking-wider">Frontend SPA</h3>
            <p className="text-xs text-slate-400 mt-1">React 19 + Vite + Tailwind CSS v4</p>
            <p className="text-xs text-slate-500 mt-0.5">Axios Interceptors · JWT State Context</p>
          </div>
        </div>

        <div className="p-5 rounded-2xl bg-slate-900/70 border border-slate-800 flex items-start gap-4">
          <div className="p-3 rounded-xl bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
            <Database className="w-6 h-6" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-white uppercase tracking-wider">Database</h3>
            <p className="text-xs text-slate-400 mt-1">PostgreSQL (Database: aives)</p>
            <p className="text-xs text-slate-500 mt-0.5">PBKDF2 Password Verifier · Hibernate Dialect</p>
          </div>
        </div>
      </div>

      {/* Modules Overview */}
      <div>
        <h3 className="text-lg font-bold text-white mb-4">Các phân hệ nghiệp vụ AIVES (SWD392)</h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Feature 1 */}
          <div className="group p-6 rounded-2xl bg-slate-900/50 hover:bg-slate-900 border border-slate-800 hover:border-indigo-500/40 transition-all flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between mb-4">
                <span className="p-2.5 rounded-xl bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                  <HelpCircle className="w-5 h-5" />
                </span>
                <span className="text-xs font-semibold px-2 py-0.5 rounded bg-slate-800 text-indigo-300">Nhóm 1</span>
              </div>
              <h4 className="text-base font-bold text-white group-hover:text-indigo-300 transition-colors">
                Ngân hàng Câu hỏi & Rubric
              </h4>
              <p className="text-xs text-slate-400 mt-2 leading-relaxed">
                Quản lý câu hỏi theo môn học, thang nhận thức Bloom, độ khó và ma trận tiêu chí đánh giá Rubric 100%.
              </p>
            </div>
            <div className="mt-6 pt-4 border-t border-slate-800/80 flex items-center justify-between text-xs text-indigo-400 font-medium">
              <span>Sẵn sàng kết nối API</span>
              <ArrowUpRight className="w-4 h-4" />
            </div>
          </div>

          {/* Feature 3 */}
          <div className="group p-6 rounded-2xl bg-slate-900/50 hover:bg-slate-900 border border-slate-800 hover:border-cyan-500/40 transition-all flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between mb-4">
                <span className="p-2.5 rounded-xl bg-cyan-500/10 text-cyan-400 border border-cyan-500/20">
                  <Mic className="w-5 h-5" />
                </span>
                <span className="text-xs font-semibold px-2 py-0.5 rounded bg-slate-800 text-cyan-300">Nhóm 3</span>
              </div>
              <h4 className="text-base font-bold text-white group-hover:text-cyan-300 transition-colors">
                Phòng thi Vấn đáp AI (Viva Room)
              </h4>
              <p className="text-xs text-slate-400 mt-2 leading-relaxed">
                Mô phỏng giám khảo AI tương tác thoại hai chiều, phân tích câu trả lời và tự động hỏi xoáy thích ứng.
              </p>
            </div>
            <div className="mt-6 pt-4 border-t border-slate-800/80 flex items-center justify-between text-xs text-cyan-400 font-medium">
              <span>Sẵn sàng kết nối API</span>
              <ArrowUpRight className="w-4 h-4" />
            </div>
          </div>

          {/* Feature 7 */}
          <div className="group p-6 rounded-2xl bg-slate-900/50 hover:bg-slate-900 border border-slate-800 hover:border-rose-500/40 transition-all flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between mb-4">
                <span className="p-2.5 rounded-xl bg-rose-500/10 text-rose-400 border border-rose-500/20">
                  <Settings className="w-5 h-5" />
                </span>
                <span className="text-xs font-semibold px-2 py-0.5 rounded bg-slate-800 text-rose-300">Nhóm 7</span>
              </div>
              <h4 className="text-base font-bold text-white group-hover:text-rose-300 transition-colors">
                Quản trị Hệ thống (Admin RBAC)
              </h4>
              <p className="text-xs text-slate-400 mt-2 leading-relaxed">
                Quản lý tài khoản, phân công giảng viên - môn học và tinh chỉnh cấu hình tham số động cho phòng thi.
              </p>
            </div>
            <div className="mt-6 pt-4 border-t border-slate-800/80 flex items-center justify-between text-xs text-rose-400 font-medium">
              <span>Đang hoạt động (Auth RBAC)</span>
              <ArrowUpRight className="w-4 h-4" />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
