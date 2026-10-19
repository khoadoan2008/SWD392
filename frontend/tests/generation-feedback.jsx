// Isolated browser regression harness. No backend, credentials, paid AI calls or database writes.
import React, { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { MemoryRouter, Route, Routes, Link, useLocation } from 'react-router-dom';
import QuestionGeneratePage from '../src/pages/lecturer/QuestionGeneratePage';
import { questionApi } from '../src/api/questionApi';
import '../src/index.css';

let mode = 'one';
let callCount = 0;
let completePending;

questionApi.getGenerationSubjects = async () => ({
  success: true, data: [{ id: 1, code: 'PRJ301', name: 'Java Web Application Development' }],
});
questionApi.generateFromMaterial = async (form) => {
  const responseMode = mode;
  callCount += 1;
  window.dispatchEvent(new CustomEvent('test-generation-request', { detail: callCount }));
  if (!form.get('materialFile')?.size) throw new Error('Test material missing');
  if (responseMode === 'deferred') await new Promise((resolve) => { completePending = resolve; });
  if (responseMode === 'error') {
    throw Object.assign(new Error('provider failure'), { response: { data: { message: 'Gemini đang bận. Vui lòng thử lại sau.' } } });
  }
  if (responseMode === 'timeout') throw Object.assign(new Error('timeout'), { code: 'ECONNABORTED' });
  if (responseMode === 'empty') return { success: true, data: [] };
  if (responseMode === 'rejected') return { success: false, message: 'Yêu cầu sinh câu hỏi bị từ chối.' };
  return { success: true, data: responseMode === 'three' ? [{ id: 12001 }, { id: 12002 }, { id: 12003 }] : [{ id: 12001 }] };
};

export function ReviewDestination() {
  const location = useLocation();
  return <section className="p-8">
    <h1>Review destination (test only)</h1>
    <output id="review-location">{location.pathname + location.search}</output>
    <p><Link to="/lecturer/questions/generate">Back to generation test</Link></p>
  </section>;
}

export function Harness() {
  const [count, setCount] = useState(callCount);
  useEffect(() => {
    const reportRequest = (event) => setCount(event.detail);
    window.addEventListener('test-generation-request', reportRequest);
    return () => window.removeEventListener('test-generation-request', reportRequest);
  }, []);
  const attachMaterial = () => {
    const input = document.getElementById('generation-material');
    const transfer = new DataTransfer();
    transfer.items.add(new File(['Public test material: TCP provides reliable ordered delivery. UDP sends independent datagrams.'], 'public-test-material.txt', { type: 'text/plain' }));
    input.files = transfer.files;
    input.dispatchEvent(new Event('change', { bubbles: true }));
  };
  return <MemoryRouter initialEntries={['/lecturer/questions/generate']}>
    <aside className="sticky top-0 z-40 flex flex-wrap items-center gap-3 border-b border-slate-600 bg-slate-800 p-3 text-sm text-white">
      <strong>UI TEST ONLY · Mock API</strong>
      <label>Test response <select aria-label="Test response" defaultValue="one" onChange={(e) => { mode = e.target.value; }} className="bg-slate-950 p-1">
        <option value="one">Success: one question</option><option value="three">Success: three questions</option>
        <option value="deferred">Delayed success</option><option value="error">Provider error</option>
        <option value="timeout">Timeout</option><option value="empty">Empty response</option>
        <option value="rejected">Rejected request</option>
      </select></label>
      <button type="button" onClick={attachMaterial} className="rounded bg-indigo-600 p-2">Attach public test material</button>
      <button type="button" onClick={() => { completePending?.(); completePending = undefined; }} className="rounded bg-indigo-600 p-2">Complete delayed request</button>
      <output aria-label="Request count">Requests: {count}</output>
    </aside>
    <Routes>
      <Route path="/lecturer/questions/generate" element={<QuestionGeneratePage />} />
      <Route path="/lecturer/questions" element={<ReviewDestination />} />
    </Routes>
  </MemoryRouter>;
}

createRoot(document.getElementById('root')).render(<Harness />);
