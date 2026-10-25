import React, { useEffect, useRef, useState } from 'react';
import { Upload, Download, X, CheckCircle2, Loader2 } from 'lucide-react';
import { questionApi } from '../api/questionApi';

export default function QuestionImportModal({ onClose, onImported }) {
  const dialogRef = useRef(null);
  const busyRef = useRef(false);
  const [file, setFile] = useState(null);
  const [status, setStatus] = useState('PENDING_REVIEW');
  const [preview, setPreview] = useState(null);
  const [busy, setBusy] = useState(false);
  const [downloading, setDownloading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState('');
  const [done, setDone] = useState(null);

  useEffect(() => {
    const dialog = dialogRef.current;
    const focused = document.activeElement;
    const overflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    dialog.showModal();
    return () => {
      if (dialog.open) dialog.close();
      document.body.style.overflow = overflow;
      if (focused instanceof HTMLElement && focused.isConnected) focused.focus({ preventScroll: true });
    };
  }, []);

  const chooseFile = (selected) => {
    if (busyRef.current || done) return;
    setPreview(null); setError(''); setProgress(0);
    if (!selected || !selected.name.toLowerCase().endsWith('.xlsx') || selected.size > 10 * 1024 * 1024 || selected.size === 0) {
      setFile(null); setError('Chọn tệp .xlsx không rỗng, tối đa 10 MB.'); return;
    }
    setFile(selected);
  };
  const download = async () => {
    if (downloading) return;
    setDownloading(true); setError('');
    try {
      const blob = await questionApi.downloadImportTemplate();
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a'); link.href = url; link.download = 'Question_Template.xlsx'; link.click();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch { setError('Không tải được file mẫu. Kiểm tra đăng nhập và thử lại.'); }
    finally { setDownloading(false); }
  };
  const submit = async (confirm) => {
    if (!file || busyRef.current || done || (confirm && (!preview || preview.invalidRows > 0))) return;
    busyRef.current = true; setBusy(true); setError(''); setProgress(0);
    try {
      const result = await questionApi.importExcel(file, {
        confirm, status, previewHash: confirm ? preview.fileHash : undefined,
        onUploadProgress: (event) => setProgress(event.total ? Math.min(100, Math.round(event.loaded * 100 / event.total)) : 0),
      });
      setPreview(result.data);
      if (!result.success) setError(result.message);
      else if (confirm) { setDone(result.data); onImported(); }
    } catch (err) {
      if (err.response?.data?.data?.rows) setPreview(err.response.data.data);
      else if (confirm) setPreview(null); // A retry must preview again; don't blindly resend an ambiguous commit.
      setError(err.response?.data?.message || 'Không nhận được kết quả. Kiểm tra ngân hàng câu hỏi trước khi thử lại.');
    } finally { busyRef.current = false; setBusy(false); }
  };
  const fieldClass = 'w-full rounded-lg border border-slate-700 bg-slate-950 p-3 text-sm text-white';
  return (
    <dialog ref={dialogRef} aria-labelledby="question-import-title" aria-modal="true"
      onCancel={(e) => { e.preventDefault(); if (!busyRef.current) onClose(); }}
      className="fixed inset-0 m-auto w-[calc(100%-2rem)] max-w-5xl max-h-[90dvh] overflow-y-auto rounded-2xl border border-slate-700 bg-slate-900 p-0 text-slate-100 shadow-2xl backdrop:bg-slate-950/80">
      <div className="sticky top-0 z-10 flex items-center justify-between border-b border-slate-700 bg-slate-900 p-5">
        <h2 id="question-import-title" className="text-xl font-bold">Import câu hỏi từ Excel</h2>
        <button type="button" aria-label="Đóng import" disabled={busy} onClick={onClose} className="rounded-lg p-2 hover:bg-slate-800 disabled:opacity-40"><X /></button>
      </div>
      <div className="space-y-5 p-5">
        {done ? <div role="status" className="rounded-xl border border-emerald-500 bg-emerald-950/60 p-6">
          <CheckCircle2 className="mb-3 text-emerald-400" />
          <h3 className="text-lg font-bold">Đã nhập thành công {done.importedCount} câu hỏi!</h3>
          <p className="mt-2">Trạng thái: {status === 'APPROVED' ? 'Đã duyệt' : 'Chờ duyệt'}. Danh sách và thống kê đã được yêu cầu cập nhật.</p>
          <button type="button" onClick={onClose} className="mt-4 rounded-lg bg-emerald-600 px-5 py-3 font-bold">Đóng và xem ngân hàng câu hỏi</button>
        </div> : <>
          <p className="text-sm text-slate-300">5 cột: Mã môn, Nội dung câu hỏi, Thang Bloom, Độ khó, Ghi chú/Gợi ý. Tối đa 1000 dòng. Preview chưa lưu dữ liệu; cần sửa hết dòng lỗi trước khi xác nhận.</p>
          <button type="button" disabled={busy || downloading} onClick={download} className="flex items-center gap-2 rounded-lg bg-slate-800 px-4 py-2.5 disabled:opacity-40"><Download size={18} />{downloading ? 'Đang tải mẫu...' : 'Tải file mẫu Excel'}</button>
          <div onDragOver={(e) => e.preventDefault()} onDrop={(e) => { e.preventDefault(); chooseFile(e.dataTransfer.files[0]); }} className="rounded-xl border-2 border-dashed border-slate-600 p-5">
            <label htmlFor="question-import-file" className="mb-3 block text-sm font-semibold">Kéo thả hoặc chọn tệp .xlsx</label>
            <input id="question-import-file" type="file" accept=".xlsx" disabled={busy} onChange={(e) => chooseFile(e.target.files[0])} className="max-w-full text-sm" />
            {file && <p className="mt-3 break-all text-sm text-cyan-300">Đã chọn: {file.name}</p>}
          </div>
          <label className="block text-sm">Trạng thái khi nhập
            <select aria-label="Trạng thái khi nhập" value={status} disabled={busy} onChange={(e) => { setStatus(e.target.value); setPreview(null); setError(''); }} className={`${fieldClass} mt-2`}>
              <option value="PENDING_REVIEW">Chờ duyệt (mặc định)</option><option value="APPROVED">Lưu chính thức (cần rubric môn học)</option>
            </select>
          </label>
          {busy && <div role="status"><p className="mb-2 text-sm">{progress < 100 ? `Đang tải lên: ${progress}%` : 'Đang kiểm tra và xử lý tệp...'}</p><progress aria-label="Tiến trình tải Excel" max="100" value={progress} className="w-full" /></div>}
          {error && <div role="alert" className="rounded-lg border border-rose-500 bg-rose-950/60 p-4 text-rose-200">{error}</div>}
          {preview && <section aria-label="Kết quả xem trước" className="space-y-3">
            <p className="font-semibold">Tổng: {preview.totalRows} · Hợp lệ: {preview.validRows} · Lỗi: {preview.invalidRows} · Đã lưu: {preview.importedCount}</p>
            <div className="max-h-80 overflow-auto rounded-lg border border-slate-700">
              <table className="w-full min-w-[760px] text-left text-sm"><thead className="sticky top-0 bg-slate-800"><tr>{['Dòng', 'Môn', 'Câu hỏi', 'Bloom', 'Độ khó', 'Ghi chú', 'Kiểm tra'].map(label => <th key={label} className="p-3">{label}</th>)}</tr></thead>
                <tbody>{preview.rows.map(row => <tr key={row.rowNumber} className={`border-t border-slate-800 ${row.errors.length ? 'bg-rose-950/30' : ''}`}>
                  <td className="p-3">{row.rowNumber}</td><td className="p-3">{row.subjectCode}</td><td className="max-w-xs whitespace-pre-wrap break-words p-3">{row.content}</td><td className="p-3">{row.bloomLevel}</td><td className="p-3">{row.difficulty}</td><td className="max-w-xs break-words p-3">{row.notes}</td><td className="p-3">{row.errors.length ? row.errors.map(message => <p key={message} className="text-rose-300">{message}</p>) : <span className="text-emerald-400">Hợp lệ</span>}</td>
                </tr>)}</tbody></table>
            </div>
          </section>}
          <div className="sticky bottom-0 flex flex-wrap justify-end gap-3 border-t border-slate-700 bg-slate-900 py-4">
            <button type="button" disabled={!file || busy} onClick={() => submit(false)} className="rounded-lg bg-slate-700 px-4 py-3 font-semibold disabled:opacity-40">Xem trước và kiểm tra</button>
            <button type="button" disabled={!preview || preview.invalidRows > 0 || busy} onClick={() => submit(true)} className="flex items-center gap-2 rounded-lg bg-emerald-600 px-4 py-3 font-bold disabled:opacity-40">{busy ? <Loader2 className="animate-spin" size={18} /> : <Upload size={18} />}Xác nhận nạp vào hệ thống</button>
          </div>
        </>}
      </div>
    </dialog>
  );
}
