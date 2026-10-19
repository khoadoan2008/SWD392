import React, { useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, CheckCircle2, X } from 'lucide-react';

export default function GenerationSuccessDialog({ questions, onDismiss, returnFocusRef }) {
  const dialogRef = useRef(null);
  const reviewLinkRef = useRef(null);
  const firstId = questions[0]?.id;
  const reviewPath = '/lecturer/questions?status=PENDING_REVIEW'
    + (Number.isSafeInteger(firstId) && firstId > 0 ? `&curr=${firstId}` : '');

  useEffect(() => {
    const dialog = dialogRef.current;
    const previousFocus = returnFocusRef?.current || document.activeElement;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    dialog.showModal();
    reviewLinkRef.current?.focus({ preventScroll: true });

    return () => {
      if (dialog.open) dialog.close();
      document.body.style.overflow = previousOverflow;
      if (previousFocus instanceof HTMLElement && previousFocus.isConnected) {
        previousFocus.focus({ preventScroll: true });
      }
    };
  }, [returnFocusRef]);

  return (
    <dialog
      ref={dialogRef}
      aria-labelledby="generation-success-title"
      aria-describedby="generation-success-description"
      aria-modal="true"
      onCancel={(event) => {
        event.preventDefault();
        onDismiss();
      }}
      onKeyDown={(event) => {
        if (event.key !== 'Tab') return;
        const controls = dialogRef.current.querySelectorAll('a[href], button:not([disabled])');
        const first = controls[0];
        const last = controls[controls.length - 1];
        if (event.shiftKey && document.activeElement === first) {
          event.preventDefault();
          last.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
          event.preventDefault();
          first.focus();
        }
      }}
      className="fixed inset-0 m-auto max-h-[calc(100dvh-2rem)] w-[calc(100%-2rem)] max-w-lg overflow-y-auto rounded-2xl border border-emerald-500/40 bg-slate-900 p-0 text-slate-100 shadow-2xl backdrop:bg-slate-950/75 backdrop:backdrop-blur-sm"
    >
      <div className="relative p-6 sm:p-8">
        <button
          type="button"
          aria-label="Đóng thông báo"
          onClick={onDismiss}
          className="absolute right-3 top-3 rounded-lg p-2 text-slate-400 transition hover:bg-slate-800 hover:text-white focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-400"
        >
          <X aria-hidden="true" className="h-5 w-5" />
        </button>
        <div className="mb-5 flex h-14 w-14 items-center justify-center rounded-full bg-emerald-500/15 text-emerald-400">
          <CheckCircle2 aria-hidden="true" className="h-8 w-8" />
        </div>
        <h2 id="generation-success-title" className="pr-5 text-xl font-bold text-white">
          Đã sinh thành công {questions.length} câu hỏi!
        </h2>
        <p id="generation-success-description" className="mt-3 text-sm leading-6 text-slate-300">
          Câu hỏi, đáp án và rubric đã được lưu ở trạng thái{' '}
          <strong className="text-amber-400">Chờ duyệt (PENDING_REVIEW)</strong>.
          Bạn có thể mở câu hỏi vừa tạo để kiểm tra và phê duyệt.
        </p>
        <div className="mt-6 flex flex-col gap-3 sm:flex-row">
          <Link
            ref={reviewLinkRef}
            to={reviewPath}
            onClick={onDismiss}
            className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-emerald-600 px-4 py-3 text-sm font-bold text-white transition hover:bg-emerald-500 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-400"
          >
            Xem câu hỏi vừa tạo
            <ArrowRight aria-hidden="true" className="h-4 w-4" />
          </Link>
          <button
            type="button"
            onClick={onDismiss}
            className="min-h-11 rounded-xl bg-slate-800 px-4 py-3 text-sm font-semibold text-slate-200 transition hover:bg-slate-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-400"
          >
            Tiếp tục tạo
          </button>
        </div>
      </div>
    </dialog>
  );
}
