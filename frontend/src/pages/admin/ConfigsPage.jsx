import React, { useEffect, useState } from 'react';
import { configApi } from '../../api/configApi';
import { 
  Settings, 
  Save, 
  CheckCircle2, 
  AlertCircle, 
  Loader2, 
  Mic, 
  Volume2, 
  Clock, 
  HelpCircle, 
  Layers,
  Globe,
  Radio
} from 'lucide-react';

export default function ConfigsPage() {
  const [configs, setConfigs] = useState({
    'speech.language': 'vi-VN',
    'interview.answer_time': '120',
    'interview.max_followups': '2',
    'interview.main_questions': '3',
  });
  const [loading, setLoading] = useState(true);
  const [saveLoading, setSaveLoading] = useState(false);
  const [testingVoice, setTestingVoice] = useState(false);
  const [successMsg, setSuccessMsg] = useState('');
  const [errorMsg, setErrorMsg] = useState('');

  useEffect(() => {
    configApi.getAll()
      .then((res) => {
        if (res.success && res.data) {
          setConfigs((prev) => ({ ...prev, ...res.data }));
          if (res.data['speech.language']) {
            localStorage.setItem('speech.language', res.data['speech.language']);
            localStorage.setItem('system_speech_language', res.data['speech.language']);
          }
        }
      })
      .catch((err) => console.error(err))
      .finally(() => setLoading(false));
  }, []);

  const handleChange = (key, val) => {
    setConfigs((prev) => ({ ...prev, [key]: val }));
    setSuccessMsg('');
    setErrorMsg('');
  };

  const handleTestVoice = () => {
    if (!('speechSynthesis' in window)) {
      alert('Trình duyệt của bạn không hỗ trợ Web Speech Synthesis API.');
      return;
    }

    if (testingVoice) {
      window.speechSynthesis.cancel();
      setTestingVoice(false);
      return;
    }

    window.speechSynthesis.cancel();
    const lang = configs['speech.language'] || 'vi-VN';
    const sampleText =
      lang === 'vi-VN'
        ? 'Xin chào, đây là giọng đọc thử nghiệm tiếng Việt trong phòng thi vấn đáp thông minh AIVES.'
        : 'Hello, this is a test of English speech synthesis in the AIVES viva exam room.';

    const utterance = new SpeechSynthesisUtterance(sampleText);
    utterance.lang = lang;
    utterance.rate = lang.startsWith('vi') ? 0.95 : 1.0;

    const voices = window.speechSynthesis.getVoices();
    const langPrefix = lang.split('-')[0].toLowerCase();
    const matchedVoice =
      voices.find((v) => v.lang.toLowerCase() === lang.toLowerCase()) ||
      voices.find((v) => v.lang.toLowerCase().startsWith(langPrefix)) ||
      voices.find((v) => v.lang.toLowerCase().includes(langPrefix));

    if (matchedVoice) {
      utterance.voice = matchedVoice;
    }

    utterance.onstart = () => setTestingVoice(true);
    utterance.onend = () => setTestingVoice(false);
    utterance.onerror = () => setTestingVoice(false);

    window.speechSynthesis.speak(utterance);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setSuccessMsg('');
    setErrorMsg('');

    const answerTime = parseInt(configs['interview.answer_time'], 10);
    const maxFollowups = parseInt(configs['interview.max_followups'], 10);
    const mainQuestions = parseInt(configs['interview.main_questions'], 10);

    if (isNaN(answerTime) || answerTime < 15 || answerTime > 900) {
      setErrorMsg('Thời gian trả lời mỗi câu phải từ 15 đến 900 giây.');
      return;
    }
    if (isNaN(maxFollowups) || maxFollowups < 0 || maxFollowups > 5) {
      setErrorMsg('Số câu hỏi xoáy tối đa từ 0 đến 5.');
      return;
    }
    if (isNaN(mainQuestions) || mainQuestions < 1 || mainQuestions > 20) {
      setErrorMsg('Số câu hỏi chính mỗi lượt thi từ 1 đến 20.');
      return;
    }

    setSaveLoading(true);
    try {
      const res = await configApi.update(configs);
      if (res.success) {
        localStorage.setItem('speech.language', configs['speech.language']);
        localStorage.setItem('system_speech_language', configs['speech.language']);
        setSuccessMsg('Đã lưu cấu hình hệ thống thành công! Ngôn ngữ STT & TTS đã được áp dụng đồng bộ.');
      }
    } catch (err) {
      setErrorMsg(err.response?.data?.message || err.message || 'Lỗi lưu cấu hình');
    } finally {
      setSaveLoading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      
      {/* Header */}
      <div>
        <div className="flex items-center gap-2 text-indigo-400 text-xs font-semibold uppercase tracking-wider mb-1">
          <Settings className="w-4 h-4" />
          Quản trị hệ thống
        </div>
        <h1 className="text-2xl font-bold text-white tracking-tight">Cấu hình Hệ thống & Phòng thi AI</h1>
        <p className="text-sm text-slate-400">Tham số mặc định điều phối luồng thi vấn đáp, thời gian trả lời và ngôn ngữ STT/TTS</p>
      </div>

      {successMsg && (
        <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 text-sm flex items-center gap-2.5">
          <CheckCircle2 className="w-5 h-5 shrink-0 text-emerald-400" />
          <span>{successMsg}</span>
        </div>
      )}

      {errorMsg && (
        <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-sm flex items-center gap-2.5">
          <AlertCircle className="w-5 h-5 shrink-0 text-rose-400" />
          <span>{errorMsg}</span>
        </div>
      )}

      {loading ? (
        <div className="p-12 text-center text-slate-400 flex justify-center items-center gap-2">
          <Loader2 className="w-5 h-5 animate-spin" />
          Đang tải cấu hình...
        </div>
      ) : (
        <form onSubmit={handleSave} className="bg-slate-900 border border-slate-800 rounded-2xl p-6 sm:p-8 shadow-xl space-y-8">
          
          {/* Config Group 1: Speech & AI (STT & TTS Synchronization) */}
          <div className="space-y-4">
            <div className="flex items-center justify-between pb-2 border-b border-slate-800">
              <h2 className="text-base font-semibold text-white flex items-center gap-2">
                <Globe className="w-4 h-4 text-cyan-400" />
                Cấu hình Ngôn ngữ Giọng nói (STT / TTS)
              </h2>
              <span className="text-xs px-2.5 py-0.5 rounded-full bg-cyan-500/10 text-cyan-400 border border-cyan-500/20 font-mono">
                Đồng bộ phòng thi
              </span>
            </div>

            <div className="p-4 rounded-xl bg-slate-950/70 border border-slate-800 space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase mb-2">
                  Ngôn ngữ nhận dạng (STT) &amp; phát âm đọc đề (TTS):
                </label>
                <div className="flex flex-col sm:flex-row sm:items-center gap-3">
                  <select
                    value={configs['speech.language']}
                    onChange={(e) => handleChange('speech.language', e.target.value)}
                    className="w-full sm:w-80 bg-slate-900 border border-slate-700 rounded-xl px-3.5 py-2.5 text-white focus:outline-none focus:border-cyan-500 text-sm font-semibold"
                  >
                    <option value="vi-VN">Tiếng Việt (vi-VN) - Thi vấn đáp tiếng Việt</option>
                    <option value="en-US">English (en-US) - Thi vấn đáp tiếng Anh</option>
                  </select>

                  <button
                    type="button"
                    onClick={handleTestVoice}
                    className={`inline-flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-semibold transition-all cursor-pointer ${
                      testingVoice
                        ? 'bg-cyan-500 text-white animate-pulse shadow-lg shadow-cyan-500/30'
                        : 'bg-slate-800 hover:bg-slate-700 text-cyan-400 hover:text-white border border-slate-700'
                    }`}
                  >
                    <Volume2 className="w-4 h-4" />
                    {testingVoice ? 'Đang phát âm thử...' : 'Nghe thử giọng TTS'}
                  </button>
                </div>
              </div>

              {/* STT and TTS Sync Notice */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2 text-xs">
                <div className="p-3 rounded-lg bg-slate-900/60 border border-slate-800 space-y-1">
                  <div className="flex items-center gap-1.5 text-cyan-400 font-semibold">
                    <Mic className="w-3.5 h-3.5" />
                    Speech-to-Text (STT):
                  </div>
                  <p className="text-slate-400">
                    Nhận diện giọng nói của thí sinh qua Microphone với bộ lọc ngôn ngữ <strong className="text-white font-mono">{configs['speech.language']}</strong>.
                  </p>
                </div>

                <div className="p-3 rounded-lg bg-slate-900/60 border border-slate-800 space-y-1">
                  <div className="flex items-center gap-1.5 text-indigo-400 font-semibold">
                    <Volume2 className="w-3.5 h-3.5" />
                    Text-to-Speech (TTS):
                  </div>
                  <p className="text-slate-400">
                    Tự động đọc to nội dung câu hỏi chính và câu hỏi xoáy thích ứng bằng giọng đọc <strong className="text-white font-mono">{configs['speech.language']}</strong>.
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* Config Group 2: Interview Flow */}
          <div className="space-y-4 pt-2">
            <h2 className="text-base font-semibold text-white flex items-center gap-2 pb-2 border-b border-slate-800">
              <Clock className="w-4 h-4 text-amber-400" />
              Quy tắc thi vấn đáp (Viva Exam Rules)
            </h2>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase mb-1.5">
                  Thời gian trả lời / câu (giây)
                </label>
                <input
                  type="number"
                  min="15"
                  max="900"
                  value={configs['interview.answer_time']}
                  onChange={(e) => handleChange('interview.answer_time', e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2.5 text-white focus:outline-none focus:border-indigo-500 text-sm font-mono"
                />
                <p className="text-xs text-slate-500 mt-1">Đếm ngược (mặc định 120s = 2 phút).</p>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase mb-1.5">
                  Số câu hỏi xoáy tối đa
                </label>
                <input
                  type="number"
                  min="0"
                  max="5"
                  value={configs['interview.max_followups']}
                  onChange={(e) => handleChange('interview.max_followups', e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2.5 text-white focus:outline-none focus:border-indigo-500 text-sm font-mono"
                />
                <p className="text-xs text-slate-500 mt-1">Tối đa cho mỗi câu chính (mặc định: 2).</p>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase mb-1.5">
                  Số câu hỏi chính / đợt thi
                </label>
                <input
                  type="number"
                  min="1"
                  max="20"
                  value={configs['interview.main_questions']}
                  onChange={(e) => handleChange('interview.main_questions', e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3.5 py-2.5 text-white focus:outline-none focus:border-indigo-500 text-sm font-mono"
                />
                <p className="text-xs text-slate-500 mt-1">Chọn ngẫu nhiên từ ngân hàng đã duyệt (mặc định: 3).</p>
              </div>
            </div>
          </div>

          <div className="pt-6 border-t border-slate-800 flex justify-end">
            <button
              type="submit"
              disabled={saveLoading}
              className="px-6 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-sm shadow-lg shadow-indigo-600/30 disabled:opacity-50 flex items-center gap-2 transition-all cursor-pointer"
            >
              {saveLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
              Lưu thay đổi cấu hình
            </button>
          </div>

        </form>
      )}

    </div>
  );
}
