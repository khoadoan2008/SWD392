import React, { useState, useEffect, useRef } from 'react';
import { userApi } from '../api/userApi';
import { subjectApi } from '../api/subjectApi';
import {
  FileSpreadsheet,
  Download,
  UploadCloud,
  CheckCircle2,
  XCircle,
  AlertCircle,
  X,
  Loader2,
  RefreshCw,
  BookOpen,
  Key,
  ShieldCheck,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';

function parseImportResponse(res) {
  const raw = res?.data !== undefined ? res.data : res;

  let total = 0;
  let successCount = 0;
  let failedCount = 0;
  let errors = [];
  let successes = [];
  const message = res?.message || '';

  if (typeof raw === 'number') {
    successCount = raw;
    total = raw;
  } else if (raw && typeof raw === 'object') {
    total = raw.total ?? raw.totalCount ?? raw.totalRows ?? 0;
    successCount =
      raw.successCount ??
      raw.successfulCount ??
      raw.success ??
      raw.importedCount ??
      raw.createdCount ??
      0;
    failedCount =
      raw.failedCount ??
      raw.failureCount ??
      raw.failed ??
      raw.errorCount ??
      raw.errorsCount ??
      0;

    const errList =
      raw.errors ||
      raw.failedList ||
      raw.errorList ||
      raw.failedRows ||
      raw.failures ||
      raw.details ||
      [];
    if (Array.isArray(errList)) {
      errors = errList.map((err, idx) => {
        if (typeof err === 'string') {
          return { row: idx + 1, reason: err };
        }
        return {
          row: err.row ?? err.line ?? err.rowIndex ?? idx + 1,
          studentCode: err.studentCode ?? err.mssv ?? err.code ?? '',
          username: err.username ?? '',
          fullName: err.fullName ?? err.name ?? '',
          email: err.email ?? '',
          reason:
            err.reason ??
            err.message ??
            err.error ??
            err.detail ??
            'Dữ liệu trùng lặp hoặc không hợp lệ',
        };
      });
    }

    const succList =
      raw.successList ||
      raw.successfulRows ||
      raw.createdList ||
      raw.createdUsers ||
      raw.successes ||
      [];
    if (Array.isArray(succList)) {
      successes = succList.map((s, idx) => {
        if (typeof s === 'string') return { text: s };
        return {
          studentCode: s.studentCode ?? s.mssv ?? '',
          username: s.username ?? '',
          fullName: s.fullName ?? s.name ?? '',
          email: s.email ?? '',
        };
      });
    }

    if (!total && (successCount || failedCount)) {
      total = successCount + failedCount;
    } else if (total && !failedCount && errors.length > 0) {
      failedCount = errors.length;
    }
  }

  return {
    total,
    successCount,
    failedCount,
    errors,
    successes,
    message,
  };
}

export default function ImportUsersModal({ isOpen, onClose, onSuccess }) {
  const [selectedFile, setSelectedFile] = useState(null);
  const [subjects, setSubjects] = useState([]);
  const [selectedSubjectId, setSelectedSubjectId] = useState('');
  const [isDragging, setIsDragging] = useState(false);
  const [downloadLoading, setDownloadLoading] = useState(false);
  const [importLoading, setImportLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [result, setResult] = useState(null);
  const [showSuccessList, setShowSuccessList] = useState(false);

  const fileInputRef = useRef(null);

  useEffect(() => {
    if (isOpen) {
      subjectApi.getAll()
        .then((res) => {
          if (res.success && Array.isArray(res.data)) {
            setSubjects(res.data.filter((s) => s.active));
          }
        })
        .catch(() => {});
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleDownloadTemplate = async () => {
    setDownloadLoading(true);
    setErrorMsg('');
    try {
      const responseData = await userApi.downloadImportTemplate();
      const blob = new Blob([responseData], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });
      const downloadUrl = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = downloadUrl;
      link.setAttribute('download', 'mau_import_sinh_vien.xlsx');
      document.body.appendChild(link);
      link.click();
      link.parentNode?.removeChild(link);
      window.URL.revokeObjectURL(downloadUrl);
    } catch (err) {
      console.error('Download template error:', err);
      window.open('/api/admin/users/import-template', '_blank');
    } finally {
      setDownloadLoading(false);
    }
  };

  const validateAndSetFile = (file) => {
    setErrorMsg('');
    if (!file) return;

    const validExtensions = ['.xlsx', '.xls', '.csv'];
    const lowerName = file.name.toLowerCase();
    const isValidExt = validExtensions.some((ext) => lowerName.endsWith(ext));

    if (!isValidExt) {
      setErrorMsg('Vui lòng chọn tệp định dạng Excel (.xlsx, .xls) hoặc .csv');
      return;
    }

    if (file.size > 15 * 1024 * 1024) {
      setErrorMsg('Kích thước tệp không được vượt quá 15MB');
      return;
    }

    setSelectedFile(file);
    setResult(null);
  };

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      validateAndSetFile(e.target.files[0]);
    }
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      validateAndSetFile(e.dataTransfer.files[0]);
    }
  };

  const handleImport = async (e) => {
    e.preventDefault();
    if (!selectedFile) {
      setErrorMsg('Vui lòng chọn tệp danh sách sinh viên.');
      return;
    }

    setImportLoading(true);
    setErrorMsg('');
    try {
      const res = await userApi.importUsers(selectedFile, selectedSubjectId || null);
      const parsed = parseImportResponse(res);
      setResult(parsed);

      if (onSuccess) {
        onSuccess();
      }
    } catch (err) {
      console.error('Import error:', err);
      setErrorMsg(
        err.response?.data?.message ||
          err.message ||
          'Không thể import file. Vui lòng kiểm tra lại cấu trúc các cột trong file Excel.'
      );
    } finally {
      setImportLoading(false);
    }
  };

  const handleReset = () => {
    setSelectedFile(null);
    setSelectedSubjectId('');
    setResult(null);
    setErrorMsg('');
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const handleClose = () => {
    handleReset();
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm overflow-y-auto">
      <div className="w-full max-w-2xl bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl relative my-8 overflow-hidden">
        
        {/* Modal Header */}
        <div className="flex items-center justify-between p-6 border-b border-slate-800">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 flex items-center justify-center">
              <FileSpreadsheet className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-white">Import Danh sách Sinh viên</h2>
              <p className="text-xs text-slate-400">
                Cấp hàng loạt tài khoản sinh viên thi vấn đáp từ tệp Excel (.xlsx, .xls) hoặc CSV
              </p>
            </div>
          </div>
          <button
            onClick={handleClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6 space-y-6">

          {/* Error Banner */}
          {errorMsg && (
            <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs sm:text-sm flex items-start gap-2.5">
              <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-400" />
              <div className="flex-1">{errorMsg}</div>
            </div>
          )}

          {/* Result View */}
          {result ? (
            <div className="space-y-5">
              {/* Summary Stats */}
              <div className="grid grid-cols-3 gap-3">
                <div className="p-3.5 rounded-xl bg-slate-950 border border-slate-800 text-center">
                  <span className="text-[11px] font-semibold text-slate-400 uppercase block">Tổng số dòng</span>
                  <span className="text-2xl font-bold font-mono text-white mt-0.5 block">
                    {result.total}
                  </span>
                </div>

                <div className="p-3.5 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-center">
                  <span className="text-[11px] font-semibold text-emerald-400 uppercase block flex items-center justify-center gap-1">
                    <CheckCircle2 className="w-3.5 h-3.5" /> Tạo thành công
                  </span>
                  <span className="text-2xl font-bold font-mono text-emerald-300 mt-0.5 block">
                    {result.successCount}
                  </span>
                </div>

                <div className="p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/20 text-center">
                  <span className="text-[11px] font-semibold text-rose-400 uppercase block flex items-center justify-center gap-1">
                    <XCircle className="w-3.5 h-3.5" /> Thất bại / Trùng
                  </span>
                  <span className="text-2xl font-bold font-mono text-rose-300 mt-0.5 block">
                    {result.failedCount}
                  </span>
                </div>
              </div>

              {/* Status Message */}
              {result.failedCount === 0 ? (
                <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 text-xs flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 shrink-0 text-emerald-400" />
                  <span>
                    Toàn bộ {result.successCount} sinh viên đã được cấp tài khoản thành công (Role: STUDENT, mật khẩu khởi tạo mặc định theo MSSV).
                  </span>
                </div>
              ) : (
                <div className="p-3.5 rounded-xl bg-amber-500/10 border border-amber-500/30 text-amber-300 text-xs flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 shrink-0 text-amber-400" />
                  <span>
                    Đã tạo thành công {result.successCount} tài khoản. Có {result.failedCount} bản ghi bị trùng lặp hoặc lỗi:
                  </span>
                </div>
              )}

              {/* Errors Detail Table */}
              {result.errors.length > 0 && (
                <div className="space-y-2">
                  <h3 className="text-xs font-semibold text-slate-300 uppercase tracking-wider flex items-center gap-1.5">
                    <XCircle className="w-3.5 h-3.5 text-rose-400" />
                    Bảng chi tiết các bản ghi thất bại ({result.errors.length})
                  </h3>
                  <div className="border border-slate-800 rounded-xl overflow-hidden max-h-56 overflow-y-auto">
                    <table className="w-full text-left text-xs text-slate-300">
                      <thead className="bg-slate-950 text-slate-400 sticky top-0 border-b border-slate-800 font-semibold uppercase text-[10px]">
                        <tr>
                          <th className="px-3 py-2.5 w-14 text-center">Dòng</th>
                          <th className="px-3 py-2.5">MSSV</th>
                          <th className="px-3 py-2.5">Họ và tên</th>
                          <th className="px-3 py-2.5">Email</th>
                          <th className="px-3 py-2.5">Lý do lỗi / Trùng</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-800/60 bg-slate-900/60">
                        {result.errors.map((err, i) => (
                          <tr key={i} className="hover:bg-slate-850/50">
                            <td className="px-3 py-2 text-center font-mono text-slate-400">{err.row}</td>
                            <td className="px-3 py-2 font-mono text-cyan-300 font-bold">
                              {err.studentCode || err.username || '—'}
                            </td>
                            <td className="px-3 py-2 text-slate-200">{err.fullName || '—'}</td>
                            <td className="px-3 py-2 text-slate-400">{err.email || '—'}</td>
                            <td className="px-3 py-2 text-rose-400 font-medium">{err.reason}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}

              {/* Success Detail (collapsible) */}
              {result.successes.length > 0 && (
                <div className="space-y-2">
                  <button
                    type="button"
                    onClick={() => setShowSuccessList(!showSuccessList)}
                    className="text-xs font-semibold text-emerald-400 hover:text-emerald-300 flex items-center gap-1 cursor-pointer"
                  >
                    {showSuccessList ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
                    Xem danh sách {result.successes.length} tài khoản vừa tạo
                  </button>

                  {showSuccessList && (
                    <div className="border border-slate-800 rounded-xl overflow-hidden max-h-48 overflow-y-auto">
                      <table className="w-full text-left text-xs text-slate-300">
                        <thead className="bg-slate-950 text-slate-400 sticky top-0 border-b border-slate-800 font-semibold uppercase text-[10px]">
                          <tr>
                            <th className="px-3 py-2.5">MSSV / Username</th>
                            <th className="px-3 py-2.5">Họ và tên</th>
                            <th className="px-3 py-2.5">Email trường</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-800/60 bg-slate-900/60">
                          {result.successes.map((s, i) => (
                            <tr key={i} className="hover:bg-slate-850/50">
                              <td className="px-3 py-2 font-mono text-cyan-300 font-bold">{s.studentCode || s.username || '—'}</td>
                              <td className="px-3 py-2 text-slate-200">{s.fullName || s.text || '—'}</td>
                              <td className="px-3 py-2 text-slate-400">{s.email || '—'}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
              )}

              {/* Result Actions */}
              <div className="flex justify-end gap-3 pt-4 border-t border-slate-800">
                <button
                  type="button"
                  onClick={handleReset}
                  className="px-4 py-2 rounded-xl text-slate-300 hover:text-white bg-slate-800 hover:bg-slate-700 text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer"
                >
                  <RefreshCw className="w-3.5 h-3.5" />
                  Import file khác
                </button>
                <button
                  type="button"
                  onClick={handleClose}
                  className="px-5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-xs shadow-lg shadow-emerald-600/30 transition-all cursor-pointer"
                >
                  Hoàn tất &amp; Đóng
                </button>
              </div>
            </div>
          ) : (
            /* Upload & Template View */
            <div className="space-y-5">
              
              {/* Step 1: Download Template */}
              <div className="p-4 rounded-xl bg-slate-950/70 border border-slate-800/80 space-y-3">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="w-5 h-5 rounded-full bg-indigo-500/20 text-indigo-400 text-xs font-bold flex items-center justify-center">1</span>
                      <span className="text-sm font-semibold text-white">File Excel mẫu danh sách lớp</span>
                    </div>
                    <p className="text-xs text-slate-400 pl-7">
                      Cấu trúc 4 cột chuẩn: <code className="text-cyan-400 font-mono">STT</code>, <code className="text-cyan-400 font-mono">Mã số sinh viên (MSSV)</code>, <code className="text-cyan-400 font-mono">Họ và tên</code>, <code className="text-cyan-400 font-mono">Email trường</code>
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={handleDownloadTemplate}
                    disabled={downloadLoading}
                    className="inline-flex items-center gap-2 px-3.5 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold transition-colors disabled:opacity-50 shrink-0 cursor-pointer self-start sm:self-auto border border-slate-700"
                  >
                    {downloadLoading ? (
                      <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    ) : (
                      <Download className="w-3.5 h-3.5 text-indigo-400" />
                    )}
                    Tải file mẫu (.xlsx)
                  </button>
                </div>

                <div className="text-[11px] text-slate-500 pl-7 flex items-center gap-2">
                  <ShieldCheck className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                  <span>
                    Hệ thống sẽ tự động cấp tài khoản Role <strong className="text-slate-300">STUDENT</strong>, kích hoạt <strong className="text-slate-300">active = true</strong>, khởi tạo mật khẩu mặc định (theo MSSV) và kiểm tra chống trùng lặp.
                  </span>
                </div>
              </div>

              {/* Optional Subject Assignment */}
              <div className="space-y-1.5">
                <label className="block text-xs font-semibold text-slate-300 uppercase flex items-center gap-2">
                  <BookOpen className="w-3.5 h-3.5 text-indigo-400" />
                  Gán vào môn thi cụ thể (Tùy chọn):
                </label>
                <select
                  value={selectedSubjectId}
                  onChange={(e) => setSelectedSubjectId(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-indigo-500"
                >
                  <option value="">-- Không gán ngay (chỉ tạo tài khoản sinh viên) --</option>
                  {subjects.map((sub) => (
                    <option key={sub.id} value={sub.id}>
                      {sub.code} - {sub.name}
                    </option>
                  ))}
                </select>
              </div>

              {/* Step 2: Upload File Area */}
              <div className="space-y-2">
                <div className="flex items-center gap-2">
                  <span className="w-5 h-5 rounded-full bg-emerald-500/20 text-emerald-400 text-xs font-bold flex items-center justify-center">2</span>
                  <span className="text-sm font-semibold text-white">Tải lên file danh sách sinh viên</span>
                </div>

                <div
                  onDragOver={handleDragOver}
                  onDragLeave={handleDragLeave}
                  onDrop={handleDrop}
                  onClick={() => fileInputRef.current?.click()}
                  className={`border-2 border-dashed rounded-2xl p-6 text-center cursor-pointer transition-all ${
                    isDragging
                      ? 'border-emerald-500 bg-emerald-500/5'
                      : selectedFile
                      ? 'border-emerald-500/50 bg-emerald-500/5'
                      : 'border-slate-800 hover:border-slate-700 bg-slate-950/40'
                  }`}
                >
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept=".xlsx, .xls, .csv"
                    onChange={handleFileChange}
                    className="hidden"
                  />

                  {selectedFile ? (
                    <div className="flex items-center justify-between max-w-md mx-auto p-3 rounded-xl bg-slate-900 border border-emerald-500/30">
                      <div className="flex items-center gap-3 text-left overflow-hidden">
                        <FileSpreadsheet className="w-8 h-8 text-emerald-400 shrink-0" />
                        <div className="truncate">
                          <p className="text-xs font-bold text-white truncate">{selectedFile.name}</p>
                          <p className="text-[11px] text-slate-400 font-mono">
                            {(selectedFile.size / 1024).toFixed(1)} KB
                          </p>
                        </div>
                      </div>
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleReset();
                        }}
                        className="p-1 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-slate-800 transition-colors"
                        title="Xoá file"
                      >
                        <X className="w-4 h-4" />
                      </button>
                    </div>
                  ) : (
                    <div className="space-y-2">
                      <div className="w-12 h-12 rounded-2xl bg-slate-800/60 text-slate-400 flex items-center justify-center mx-auto">
                        <UploadCloud className="w-6 h-6 text-slate-400" />
                      </div>
                      <p className="text-xs font-semibold text-slate-200">
                        Kéo thả file vào đây hoặc <span className="text-emerald-400 underline">bấm để chọn file</span>
                      </p>
                      <p className="text-[11px] text-slate-500">
                        Hỗ trợ .xlsx, .xls, .csv (Xử lý 50+ sinh viên dưới 3 giây)
                      </p>
                    </div>
                  )}
                </div>
              </div>

              {/* Action Buttons */}
              <div className="flex justify-end gap-3 pt-4 border-t border-slate-800">
                <button
                  type="button"
                  onClick={handleClose}
                  className="px-4 py-2 rounded-xl text-slate-400 hover:text-white hover:bg-slate-800 text-xs font-semibold transition-colors cursor-pointer"
                >
                  Huỷ
                </button>
                <button
                  type="button"
                  onClick={handleImport}
                  disabled={!selectedFile || importLoading}
                  className="px-5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-xs shadow-lg shadow-emerald-600/30 disabled:opacity-50 flex items-center gap-2 transition-all cursor-pointer"
                >
                  {importLoading ? (
                    <>
                      <Loader2 className="w-4 h-4 animate-spin" />
                      Đang xử lý import...
                    </>
                  ) : (
                    <>
                      <FileSpreadsheet className="w-4 h-4" />
                      Bắt đầu Import
                    </>
                  )}
                </button>
              </div>

            </div>
          )}

        </div>
      </div>
    </div>
  );
}
