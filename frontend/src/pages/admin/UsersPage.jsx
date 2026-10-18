import React, { useEffect, useState } from 'react';
import { userApi } from '../../api/userApi';
import { subjectApi } from '../../api/subjectApi';
import { Users, Plus, Edit2, Trash2, Search, CheckCircle2, XCircle, Shield, UserCheck, GraduationCap, AlertCircle, Loader2 } from 'lucide-react';

export default function UsersPage() {
  const [users, setUsers] = useState([]);
  const [subjects, setSubjects] = useState([]);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  
  // Filters
  const [roleFilter, setRoleFilter] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  // Modal Form State
  const [modalOpen, setModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [role, setRole] = useState('STUDENT');
  const [studentCode, setStudentCode] = useState('');
  const [active, setActive] = useState(true);
  const [selectedSubjectIds, setSelectedSubjectIds] = useState([]);
  const [errorMsg, setErrorMsg] = useState('');
  const [saveLoading, setSaveLoading] = useState(false);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const params = {};
      if (roleFilter) params.role = roleFilter;
      if (searchQuery.trim()) params.q = searchQuery.trim();

      const [resUsers, resStats, resSubjects] = await Promise.all([
        userApi.search(params),
        userApi.getStats(),
        subjectApi.getAll()
      ]);

      if (resUsers.success) setUsers(resUsers.data);
      if (resStats.success) setStats(resStats.data);
      if (resSubjects.success) setSubjects(resSubjects.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, [roleFilter]);

  const handleSearch = (e) => {
    e.preventDefault();
    fetchUsers();
  };

  const openCreateModal = () => {
    setEditingUser(null);
    setUsername('');
    setPassword('');
    setFullName('');
    setEmail('');
    setRole('STUDENT');
    setStudentCode('');
    setActive(true);
    setSelectedSubjectIds([]);
    setErrorMsg('');
    setModalOpen(true);
  };

  const openEditModal = (u) => {
    setEditingUser(u);
    setUsername(u.username);
    setPassword('');
    setFullName(u.fullName);
    setEmail(u.email || '');
    setRole(u.role);
    setStudentCode(u.studentCode || '');
    setActive(u.active);
    setSelectedSubjectIds(u.assignedSubjectIds || []);
    setErrorMsg('');
    setModalOpen(true);
  };

  const handleSubjectToggle = (subId) => {
    setSelectedSubjectIds((prev) =>
      prev.includes(subId) ? prev.filter((id) => id !== subId) : [...prev, subId]
    );
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setErrorMsg('');

    if (!editingUser && !username.trim()) {
      setErrorMsg('Tên đăng nhập không được để trống.');
      return;
    }
    if (!editingUser && (!password || password.length < 6)) {
      setErrorMsg('Mật khẩu tối thiểu 6 ký tự.');
      return;
    }
    if (!fullName.trim()) {
      setErrorMsg('Họ tên không được để trống.');
      return;
    }
    if (role === 'STUDENT' && !studentCode.trim()) {
      setErrorMsg('Sinh viên bắt buộc phải có MSSV.');
      return;
    }

    setSaveLoading(true);
    try {
      if (editingUser) {
        const payload = {
          fullName: fullName.trim(),
          email: email.trim(),
          role,
          studentCode: role === 'STUDENT' ? studentCode.trim() : null,
          active,
          newPassword: password.trim() ? password.trim() : null,
          subjectIds: role === 'LECTURER' ? selectedSubjectIds : [],
        };
        await userApi.update(editingUser.id, payload);
      } else {
        const payload = {
          username: username.trim(),
          password: password.trim(),
          fullName: fullName.trim(),
          email: email.trim(),
          role,
          studentCode: role === 'STUDENT' ? studentCode.trim() : null,
          active,
          subjectIds: role === 'LECTURER' ? selectedSubjectIds : [],
        };
        await userApi.create(payload);
      }
      setModalOpen(false);
      fetchUsers();
    } catch (err) {
      setErrorMsg(err.response?.data?.message || err.message || 'Lỗi lưu thông tin người dùng');
    } finally {
      setSaveLoading(false);
    }
  };

  const handleDelete = async (id, name) => {
    if (!window.confirm(`Bạn có chắc chắn muốn xoá người dùng "${name}"?`)) return;
    try {
      await userApi.delete(id);
      fetchUsers();
    } catch (err) {
      alert(err.response?.data?.message || 'Không thể xoá tài khoản đã có dữ liệu thi.');
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 text-indigo-400 text-xs font-semibold uppercase tracking-wider mb-1">
            <Users className="w-4 h-4" />
            Quản trị người dùng
          </div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Danh sách tài khoản</h1>
          <p className="text-sm text-slate-400">Phân quyền Admin, Giảng viên bộ môn và Sinh viên tham gia thi</p>
        </div>

        <button
          onClick={openCreateModal}
          className="inline-flex items-center gap-2 bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-sm px-4 py-2.5 rounded-xl shadow-lg shadow-indigo-600/30 transition-all cursor-pointer"
        >
          <Plus className="w-4 h-4" />
          Thêm người dùng mới
        </button>
      </div>

      {/* Role Stats Filter Pills */}
      {stats && (
        <div className="flex flex-wrap items-center gap-2 text-xs font-medium">
          <button
            onClick={() => setRoleFilter('')}
            className={`px-3 py-1.5 rounded-xl transition-all cursor-pointer ${
              roleFilter === '' ? 'bg-indigo-600 text-white shadow-md' : 'bg-slate-900 border border-slate-800 text-slate-400 hover:text-white'
            }`}
          >
            Tất cả ({stats.TOTAL || 0})
          </button>
          <button
            onClick={() => setRoleFilter('ADMIN')}
            className={`px-3 py-1.5 rounded-xl transition-all cursor-pointer ${
              roleFilter === 'ADMIN' ? 'bg-rose-600 text-white shadow-md' : 'bg-slate-900 border border-slate-800 text-rose-400 hover:bg-rose-500/10'
            }`}
          >
            Quản trị viên ({stats.ADMIN || 0})
          </button>
          <button
            onClick={() => setRoleFilter('LECTURER')}
            className={`px-3 py-1.5 rounded-xl transition-all cursor-pointer ${
              roleFilter === 'LECTURER' ? 'bg-amber-600 text-white shadow-md' : 'bg-slate-900 border border-slate-800 text-amber-400 hover:bg-amber-500/10'
            }`}
          >
            Giảng viên ({stats.LECTURER || 0})
          </button>
          <button
            onClick={() => setRoleFilter('STUDENT')}
            className={`px-3 py-1.5 rounded-xl transition-all cursor-pointer ${
              roleFilter === 'STUDENT' ? 'bg-cyan-600 text-white shadow-md' : 'bg-slate-900 border border-slate-800 text-cyan-400 hover:bg-cyan-500/10'
            }`}
          >
            Sinh viên ({stats.STUDENT || 0})
          </button>
        </div>
      )}

      {/* Search Input */}
      <form onSubmit={handleSearch} className="flex gap-2 max-w-md">
        <div className="relative flex-1">
          <Search className="w-4 h-4 text-slate-500 absolute left-3.5 top-3 pointer-events-none" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Tìm theo username, họ tên, MSSV, email..."
            className="w-full bg-slate-900 border border-slate-800 rounded-xl pl-10 pr-4 py-2 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500"
          />
        </div>
        <button
          type="submit"
          className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-xl text-sm font-semibold transition-colors cursor-pointer"
        >
          Tìm kiếm
        </button>
      </form>

      {/* Table */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-xl">
        {loading ? (
          <div className="p-12 flex justify-center items-center text-slate-400 gap-2">
            <Loader2 className="w-5 h-5 animate-spin" />
            Đang tải dữ liệu...
          </div>
        ) : users.length === 0 ? (
          <div className="p-12 text-center text-slate-500">Không tìm thấy người dùng phù hợp.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-300">
              <thead className="bg-slate-950/60 text-xs uppercase tracking-wider text-slate-400 border-b border-slate-800">
                <tr>
                  <th className="px-6 py-4">Tài khoản</th>
                  <th className="px-6 py-4">Họ và tên</th>
                  <th className="px-6 py-4">Vai trò</th>
                  <th className="px-6 py-4">MSSV / Môn phụ trách</th>
                  <th className="px-6 py-4">Trạng thái</th>
                  <th className="px-6 py-4 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {users.map((u) => (
                  <tr key={u.id} className="hover:bg-slate-850/50 transition-colors">
                    <td className="px-6 py-4">
                      <span className="font-mono font-bold text-white block">{u.username}</span>
                      <span className="text-xs text-slate-500">{u.email || '—'}</span>
                    </td>
                    <td className="px-6 py-4 font-medium text-slate-200">{u.fullName}</td>
                    <td className="px-6 py-4">
                      {u.role === 'ADMIN' && (
                        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-rose-500/10 text-rose-400 border border-rose-500/20">
                          <Shield className="w-3.5 h-3.5" />
                          ADMIN
                        </span>
                      )}
                      {u.role === 'LECTURER' && (
                        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-amber-500/10 text-amber-400 border border-amber-500/20">
                          <UserCheck className="w-3.5 h-3.5" />
                          GIẢNG VIÊN
                        </span>
                      )}
                      {u.role === 'STUDENT' && (
                        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-cyan-500/10 text-cyan-400 border border-cyan-500/20">
                          <GraduationCap className="w-3.5 h-3.5" />
                          SINH VIÊN
                        </span>
                      )}
                    </td>
                    <td className="px-6 py-4 text-xs text-slate-400">
                      {u.role === 'STUDENT' ? (
                        <span className="font-mono bg-slate-800 px-2 py-0.5 rounded text-cyan-300">
                          {u.studentCode || 'Chưa cập nhật'}
                        </span>
                      ) : u.role === 'LECTURER' ? (
                        <div className="flex flex-wrap gap-1">
                          {u.assignedSubjectIds?.length > 0 ? (
                            u.assignedSubjectIds.map((id) => {
                              const s = subjects.find((sub) => sub.id === id);
                              return (
                                <span key={id} className="px-2 py-0.5 rounded bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 font-mono">
                                  {s ? s.code : id}
                                </span>
                              );
                            })
                          ) : (
                            <span className="text-slate-600 italic">Chưa gán môn</span>
                          )}
                        </div>
                      ) : (
                        <span className="text-slate-600">Toàn quyền hệ thống</span>
                      )}
                    </td>
                    <td className="px-6 py-4">
                      {u.active ? (
                        <span className="inline-flex items-center gap-1 text-xs text-emerald-400 font-medium">
                          <CheckCircle2 className="w-3.5 h-3.5" />
                          Hoạt động
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 text-xs text-slate-500 font-medium">
                          <XCircle className="w-3.5 h-3.5" />
                          Bị khóa
                        </span>
                      )}
                    </td>
                    <td className="px-6 py-4 text-right space-x-2">
                      <button
                        onClick={() => openEditModal(u)}
                        className="p-1.5 text-slate-400 hover:text-indigo-400 hover:bg-slate-800 rounded-lg transition-colors cursor-pointer"
                        title="Chỉnh sửa"
                      >
                        <Edit2 className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => handleDelete(u.id, u.fullName)}
                        className="p-1.5 text-slate-400 hover:text-rose-400 hover:bg-slate-800 rounded-lg transition-colors cursor-pointer"
                        title="Xoá"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modal */}
      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm overflow-y-auto">
          <div className="w-full max-w-lg bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-2xl relative my-8">
            <h2 className="text-lg font-bold text-white mb-4">
              {editingUser ? `Chỉnh sửa: ${editingUser.username}` : 'Thêm người dùng mới'}
            </h2>

            {errorMsg && (
              <div className="mb-4 p-3 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-400 text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{errorMsg}</span>
              </div>
            )}

            <form onSubmit={handleSave} className="space-y-4 text-sm">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">Tên đăng nhập</label>
                  <input
                    type="text"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    disabled={!!editingUser}
                    placeholder="vd: lecturer2..."
                    required
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-indigo-500 font-mono disabled:opacity-50"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">
                    {editingUser ? 'Mật khẩu mới (bỏ trống nếu giữ nguyên)' : 'Mật khẩu'}
                  </label>
                  <input
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder={editingUser ? 'Nhập nếu muốn đổi...' : 'Tối thiểu 6 ký tự'}
                    required={!editingUser}
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-indigo-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">Họ và tên</label>
                  <input
                    type="text"
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    placeholder="vd: Nguyễn Văn A"
                    required
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-indigo-500"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">Email</label>
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="email@example.com"
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-indigo-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">Vai trò</label>
                  <select
                    value={role}
                    onChange={(e) => setRole(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-indigo-500"
                  >
                    <option value="STUDENT">Sinh viên (STUDENT)</option>
                    <option value="LECTURER">Giảng viên (LECTURER)</option>
                    <option value="ADMIN">Quản trị viên (ADMIN)</option>
                  </select>
                </div>

                {role === 'STUDENT' && (
                  <div>
                    <label className="block text-xs font-semibold text-slate-300 uppercase mb-1">Mã số sinh viên (MSSV)</label>
                    <input
                      type="text"
                      value={studentCode}
                      onChange={(e) => setStudentCode(e.target.value)}
                      placeholder="vd: SE170123"
                      required
                      className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-indigo-500 font-mono"
                    />
                  </div>
                )}
              </div>

              {/* Lecturer Subject Assignment */}
              {role === 'LECTURER' && (
                <div className="p-3.5 rounded-xl bg-slate-950 border border-slate-800">
                  <label className="block text-xs font-semibold text-slate-300 uppercase mb-2">
                    Môn học phân công phụ trách:
                  </label>
                  <div className="grid grid-cols-2 gap-2 max-h-36 overflow-y-auto">
                    {subjects.map((s) => (
                      <label key={s.id} className="flex items-center gap-2 text-xs text-slate-300 hover:text-white cursor-pointer">
                        <input
                          type="checkbox"
                          checked={selectedSubjectIds.includes(s.id)}
                          onChange={() => handleSubjectToggle(s.id)}
                          className="rounded border-slate-700 bg-slate-900 text-indigo-600 focus:ring-indigo-500"
                        />
                        <span className="font-mono text-indigo-400 font-semibold">{s.code}</span> - {s.name}
                      </label>
                    ))}
                  </div>
                </div>
              )}

              <div className="flex items-center gap-2 pt-2">
                <input
                  type="checkbox"
                  id="userActive"
                  checked={active}
                  onChange={(e) => setActive(e.target.checked)}
                  className="rounded border-slate-800 bg-slate-950 text-indigo-600 focus:ring-indigo-500"
                />
                <label htmlFor="userActive" className="text-sm text-slate-300 cursor-pointer">
                  Tài khoản đang hoạt động
                </label>
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setModalOpen(false)}
                  className="px-4 py-2 rounded-xl text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
                >
                  Huỷ
                </button>
                <button
                  type="submit"
                  disabled={saveLoading}
                  className="px-5 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-semibold shadow-lg shadow-indigo-600/30 disabled:opacity-50 flex items-center gap-2 cursor-pointer"
                >
                  {saveLoading && <Loader2 className="w-4 h-4 animate-spin" />}
                  Lưu người dùng
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
}
