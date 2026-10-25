// Browser regression with mocked HTTP. Backend/PostgreSQL behavior is tested separately.
// Requires Node 22+ and Chrome. No npm test dependencies or production hooks.
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { spawn } from 'node:child_process';
import assert from 'node:assert/strict';

const base = process.env.AIVES_UI_TEST_URL || 'http://127.0.0.1:5175';
if (!['localhost', '127.0.0.1'].includes(new URL(base).hostname)) throw new Error('Tests only run against loopback.');
const output = path.resolve(process.env.AIVES_UI_TEST_OUTPUT || 'target/import-browser');
await fs.mkdir(output, { recursive: true });
const profile = await fs.mkdtemp(path.join(os.tmpdir(), 'aives-import-ui-'));
const fixture = path.join(profile, 'questions.xlsx');
// This fixture is deliberately not a workbook: these tests exercise FE state with a mocked API.
await fs.writeFile(fixture, 'UI fixture; Excel parsing covered by Java tests');
const invalidFixture = path.join(profile, 'wrong.csv'); await fs.writeFile(invalidFixture, 'wrong');
const chrome = spawn(process.env.CHROME_PATH || 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  ['--headless=new', '--no-first-run', '--disable-gpu', '--remote-debugging-port=0', `--user-data-dir=${profile}`, 'about:blank'], { windowsHide: true, stdio: 'ignore' });
let socket; const results = []; const counters = { preview: 0, confirm: 0, list: 0, download: 0 };
let mode = 'valid';
const pause = ms => new Promise(resolve => setTimeout(resolve, ms));
const until = async (fn, title) => { for (let n = 0; n < 100; n++) { if (await fn()) return; await pause(100); } throw new Error(`Timeout: ${title}`); };
let send;
try {
  let port;
  await until(async () => { try { port = (await fs.readFile(path.join(profile, 'DevToolsActivePort'), 'utf8')).split('\n')[0]; return true; } catch { return false; } }, 'Chrome startup');
  const target = await (await fetch(`http://127.0.0.1:${port}/json/new?about:blank`, { method: 'PUT' })).json();
  socket = new WebSocket(target.webSocketDebuggerUrl);
  await new Promise((resolve, reject) => { socket.onopen = resolve; socket.onerror = reject; });
  let id = 0; const pending = new Map();
  send = (method, params = {}) => new Promise((resolve, reject) => {
    const key = ++id;
    const timer = setTimeout(() => { pending.delete(key); reject(new Error(`CDP timeout: ${method}`)); }, 15000);
    pending.set(key, { resolve, reject, timer }); socket.send(JSON.stringify({ id: key, method, params }));
  });
  const ok = data => ({ success: true, message: 'Thành công', data });
  const row = { rowNumber: 2, subjectCode: 'PRJ301', content: 'Câu hỏi kiểm thử FE', bloomLevel: 'UNDERSTAND', difficulty: 'MEDIUM', notes: 'Gợi ý', errors: [] };
  const report = bad => ({ fileHash: 'test-file-hash', totalRows: 1, validRows: bad ? 0 : 1, invalidRows: bad ? 1 : 0, importedCount: 0, durationMs: 10, rows: [{ ...row, errors: bad ? ['Câu hỏi trùng trong tệp.'] : [] }] });
  const errors = [];
  socket.onmessage = async event => {
    const message = JSON.parse(event.data);
    if (message.id) {
      const p = pending.get(message.id); if (!p) return; clearTimeout(p.timer); pending.delete(message.id);
      message.error ? p.reject(new Error(message.error.message)) : p.resolve(message.result); return;
    }
    if (message.method === 'Runtime.exceptionThrown') errors.push(message.params.exceptionDetails.text);
    if (message.method !== 'Fetch.requestPaused') return;
    try {
      const { requestId, request } = message.params; const url = new URL(request.url);
      let body; let responseCode = 200;
      if (url.pathname.endsWith('/import/template')) { counters.download++; body = 'mock template download'; }
      else if (url.pathname.endsWith('/import')) {
        assert.match(request.headers['Content-Type'] || request.headers['content-type'], /^multipart\/form-data; boundary=/);
        const confirm = url.searchParams.get('confirm') === 'true';
        if (confirm) {
          counters.confirm++; assert.equal(url.searchParams.get('previewHash'), 'test-file-hash');
          await pause(250);
          if (mode === 'server-error') { body = { success: false, message: 'Lỗi kiểm thử kết nối.' }; responseCode = 500; }
          else if (mode === 'stale') { body = { success: false, message: 'Tệp có dòng lỗi. Chưa lưu câu hỏi nào.', data: report(true) }; responseCode = 422; }
          else body = ok({ ...report(false), importedCount: 1 });
        } else { counters.preview++; body = mode === 'invalid' ? { success: false, message: 'Tệp có dòng lỗi.', data: report(true) } : ok(report(false)); }
      } else if (url.pathname.endsWith('/generation-subjects')) body = ok([{ id: 1, code: 'PRJ301', name: 'Java Web' }]);
      else if (url.pathname.endsWith('/stats')) body = ok({ TOTAL: 1, APPROVED: 0, PENDING_REVIEW: 1 });
      else if (url.pathname === '/api/lecturer/questions') { counters.list++; body = ok([]); }
      else body = ok([]);
      await send('Fetch.fulfillRequest', { requestId, responseCode, responseHeaders: [{ name: 'Content-Type', value: typeof body === 'string' ? 'application/octet-stream' : 'application/json' }], body: Buffer.from(typeof body === 'string' ? body : JSON.stringify(body)).toString('base64') });
    } catch (error) { errors.push(error.message); }
  };
  const evaluate = async expression => {
    const r = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
    if (r.exceptionDetails) throw new Error(r.exceptionDetails.exception?.description || r.exceptionDetails.text);
    return r.result?.value;
  };
  const button = label => `[...document.querySelectorAll('button')].find(e=>e.textContent.trim()===${JSON.stringify(label)})`;
  const click = label => evaluate(`${button(label)}.click()`);
  const selectFile = async selected => {
    const { root } = await send('DOM.getDocument');
    const { nodeId } = await send('DOM.querySelector', { nodeId: root.nodeId, selector: '#question-import-file' });
    await send('DOM.setFileInputFiles', { nodeId, files: [selected] });
    await until(() => evaluate(`document.querySelector('dialog').innerText.includes(${JSON.stringify(path.basename(selected))}) || !!document.querySelector('[role=alert]')`), 'file selection');
  };
  const check = (name, condition) => { assert.ok(condition, name); results.push({ name, passed: true }); };
  await send('Runtime.enable'); await send('Page.enable'); await send('Fetch.enable', { patterns: [{ urlPattern: new URL('/api/*', base).href }] });
  await send('Emulation.setDeviceMetricsOverride', { width: 1366, height: 900, deviceScaleFactor: 1, mobile: false });
  await send('Page.addScriptToEvaluateOnNewDocument', { source: "localStorage.setItem('token','ui-test-only');localStorage.setItem('user',JSON.stringify({id:1,username:'lecturer1',fullName:'Giảng viên kiểm thử',role:'LECTURER'}));" });
  await send('Page.navigate', { url: base + '/lecturer/questions' });
  await until(() => evaluate(`!!${button('Import Excel')}`), 'question list');
  await click('Import Excel');
  await until(() => evaluate("!!document.querySelector('dialog[open]')"), 'modal open');
  check('native modal opens', await evaluate("document.querySelector('dialog').matches(':modal')"));
  check('cannot confirm before preview', await evaluate(`${button('Xác nhận nạp vào hệ thống')}.disabled`));
  await click('Tải file mẫu Excel'); await until(() => counters.download === 1, 'template download'); check('template request', counters.download === 1);
  await selectFile(invalidFixture); check('wrong file extension displays error', await evaluate("document.querySelector('[role=alert]').innerText.includes('.xlsx')"));
  await selectFile(fixture); await click('Xem trước và kiểm tra');
  await until(() => evaluate("!!document.querySelector('[aria-label=\"Kết quả xem trước\"]')"), 'preview');
  check('preview does not commit', counters.confirm === 0);
  check('valid preview enables confirm', await evaluate(`!${button('Xác nhận nạp vào hệ thống')}.disabled`));
  await evaluate("(()=>{const e=document.querySelector('[aria-label=\"Trạng thái khi nhập\"]');Object.getOwnPropertyDescriptor(HTMLSelectElement.prototype,'value').set.call(e,'APPROVED');e.dispatchEvent(new Event('change',{bubbles:true}));})()");
  await until(() => evaluate(`${button('Xác nhận nạp vào hệ thống')}.disabled`), 'status invalidates preview');
  check('status change requires fresh preview', true);
  await click('Xem trước và kiểm tra'); await until(() => evaluate(`!${button('Xác nhận nạp vào hệ thống')}.disabled`), 'second preview');
  const listsBefore = counters.list;
  await evaluate(`${button('Xác nhận nạp vào hệ thống')}.click();${button('Xác nhận nạp vào hệ thống')}.click();`);
  await until(() => evaluate("document.querySelector('dialog').innerText.includes('Đã nhập thành công 1 câu hỏi!')"), 'success');
  check('double click sends one commit', counters.confirm === 1);
  check('success remains visible in modal', await evaluate("(()=>{const r=document.querySelector('dialog').getBoundingClientRect();return r.top>=0&&r.bottom<=innerHeight;})()"));
  await until(() => counters.list > listsBefore, 'list refresh'); check('successful import refreshes bank', true);
  await fs.writeFile(path.join(output, 'success.png'), Buffer.from((await send('Page.captureScreenshot', { format: 'png' })).data, 'base64'));
  await click('Đóng và xem ngân hàng câu hỏi'); await until(() => evaluate("!document.querySelector('dialog')"), 'close');
  check('body scroll restored', await evaluate("document.body.style.overflow!== 'hidden'"));
  await click('Import Excel'); mode = 'invalid'; await selectFile(fixture); await click('Xem trước và kiểm tra');
  await until(() => evaluate("!!document.querySelector('[aria-label=\"Kết quả xem trước\"]')"), 'invalid preview');
  check('row errors block confirmation', await evaluate(`${button('Xác nhận nạp vào hệ thống')}.disabled && document.querySelector('dialog').innerText.includes('Câu hỏi trùng trong tệp.')`));
  mode = 'valid'; await click('Xem trước và kiểm tra'); await until(() => evaluate(`!${button('Xác nhận nạp vào hệ thống')}.disabled`), 'valid retry');
  mode = 'stale'; await click('Xác nhận nạp vào hệ thống'); await until(() => evaluate("document.querySelector('dialog').innerText.includes('Chưa lưu câu hỏi nào.')"), 'revalidation error');
  check('commit revalidation error stays visible', await evaluate(`${button('Xác nhận nạp vào hệ thống')}.disabled`));
  mode = 'valid'; await click('Xem trước và kiểm tra'); await until(() => evaluate(`!${button('Xác nhận nạp vào hệ thống')}.disabled`), 'retry preview');
  mode = 'server-error'; await click('Xác nhận nạp vào hệ thống');
  await until(() => evaluate("!!document.querySelector('[role=alert]') && document.querySelector('[role=alert]').innerText.includes('Lỗi kiểm thử')"), 'server error');
  check('ambiguous failure requires preview again', await evaluate(`${button('Xác nhận nạp vào hệ thống')}.disabled`));
  await send('Emulation.setDeviceMetricsOverride', { width: 390, height: 844, deviceScaleFactor: 1, mobile: true });
  check('modal fits mobile viewport', await evaluate("(()=>{const r=document.querySelector('dialog').getBoundingClientRect();return r.width<=innerWidth&&r.left>=0&&r.right<=innerWidth;})()"));
  await fs.writeFile(path.join(output, 'mobile.png'), Buffer.from((await send('Page.captureScreenshot', { format: 'png' })).data, 'base64'));
  await send('Input.dispatchKeyEvent', { type: 'keyDown', key: 'Escape', code: 'Escape', windowsVirtualKeyCode: 27 });
  await send('Input.dispatchKeyEvent', { type: 'keyUp', key: 'Escape', code: 'Escape', windowsVirtualKeyCode: 27 });
  await until(() => evaluate("!document.querySelector('dialog')"), 'escape'); check('Escape closes idle modal', true);
  check('no uncaught browser or mock errors', errors.length === 0);
  await fs.writeFile(path.join(output, 'results.json'), JSON.stringify({ kind: 'FE mocked HTTP browser regression', passed: results.length, counters, results }, null, 2));
  console.log(JSON.stringify({ passed: results.length, output }));
} catch (error) {
  await fs.writeFile(path.join(output, 'failure.json'), JSON.stringify({ message: error.message, results, counters }, null, 2));
  throw error;
} finally {
  if (send) { try { await send('Browser.close'); } catch {} }
  socket?.close(); chrome.kill();
}
