import React, { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { interviewApi } from '../../api/interviewApi';
import { 
  Mic, 
  MicOff, 
  Volume2, 
  Clock, 
  Send, 
  AlertTriangle, 
  CheckCircle2, 
  ArrowLeft, 
  Sparkles, 
  BrainCircuit, 
  HelpCircle, 
  User, 
  Loader2,
  AudioLines
} from 'lucide-react';

export default function InterviewRoomPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [sessionData, setSessionData] = useState(null);
  const [currentTurn, setCurrentTurn] = useState(null);
  const [prevTurn, setPrevTurn] = useState(null);
  const [speechLang, setSpeechLang] = useState('vi-VN');
  const [remainingSec, setRemainingSec] = useState(120);
  const [transcript, setTranscript] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [isSpeaking, setIsSpeaking] = useState(false);
  const [micLevel, setMicLevel] = useState(0);
  const submitLockRef = useRef(false);
  const transcriptRef = useRef('');
  const turnRef = useRef(null);
  const streamRef = useRef(null);
  const audioContextRef = useRef(null);
  const animationRef = useRef(null);

  // Speech Recognition State
  const [isListening, setIsListening] = useState(false);
  const recognitionRef = useRef(null);
  const timerRef = useRef(null);

  useEffect(() => { transcriptRef.current = transcript; }, [transcript]);
  useEffect(() => { turnRef.current = currentTurn; submitLockRef.current = false; }, [currentTurn?.id]);

  const fetchTurnData = async () => {
    try {
      const res = await interviewApi.getTurn(id);
      if (res.success && res.data) {
        setSessionData(res.data.session);
        setCurrentTurn(res.data.currentTurn || null);
        setPrevTurn(res.data.prevTurn || null);
        setSpeechLang(res.data.speechLang || 'vi-VN');
        if (res.data.remainingSec != null) {
          setRemainingSec(res.data.remainingSec);
        }
        setTranscript('');
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTurnData();
  }, [id]);

  // A stable timer: never send requests inside a React state updater.
  useEffect(() => {
    if (!currentTurn) return;
    const interval = window.setInterval(() => setRemainingSec(value => Math.max(0, value - 1)), 1000);
    return () => window.clearInterval(interval);
  }, [currentTurn?.id]);

  useEffect(() => {
    if (currentTurn && remainingSec === 0) submitCurrentAnswer();
  }, [remainingSec, currentTurn?.id]);

  const stopAudioVisualizer = () => {
    if (animationRef.current) cancelAnimationFrame(animationRef.current);
    animationRef.current = null;
    streamRef.current?.getTracks().forEach(track => track.stop());
    streamRef.current = null;
    audioContextRef.current?.close();
    audioContextRef.current = null;
    setMicLevel(0);
  };

  const startAudioVisualizer = async () => {
    if (!navigator.mediaDevices?.getUserMedia) return;
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      streamRef.current = stream;
      const AudioContextClass = window.AudioContext || window.webkitAudioContext;
      if (!AudioContextClass) return;
      const context = new AudioContextClass();
      audioContextRef.current = context;
      const analyser = context.createAnalyser();
      analyser.fftSize = 256;
      context.createMediaStreamSource(stream).connect(analyser);
      const samples = new Uint8Array(analyser.frequencyBinCount);
      const draw = () => {
        analyser.getByteFrequencyData(samples);
        const level = samples.reduce((sum, v) => sum + v, 0) / samples.length;
        setMicLevel(level);
        animationRef.current = requestAnimationFrame(draw);
      };
      draw();
    } catch (error) {
      console.warn('Không thể hiển thị sóng âm:', error.name);
    }
  };

  useEffect(() => () => {
    recognitionRef.current?.abort();
    window.speechSynthesis?.cancel();
    if (animationRef.current) cancelAnimationFrame(animationRef.current);
    streamRef.current?.getTracks().forEach(track => track.stop());
    audioContextRef.current?.close();
  }, []);

  // Initialize Web Speech API Recognition
  useEffect(() => {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (SpeechRecognition) {
      const recognizer = new SpeechRecognition();
      recognizer.continuous = true;
      recognizer.interimResults = true;
      recognizer.lang = speechLang;

      recognizer.onresult = (event) => {
        let finalTrans = '';
        for (let i = 0; i < event.results.length; i++) {
          finalTrans += event.results[i][0].transcript + ' ';
        }
        transcriptRef.current = finalTrans.trim();
        setTranscript(finalTrans.trim());
      };

      recognizer.onerror = (e) => {
        console.error('Speech recognition error:', e.error);
        setIsListening(false);
        stopAudioVisualizer();
      };

      recognizer.onend = () => {
        setIsListening(false);
        stopAudioVisualizer();
      };

      recognitionRef.current = recognizer;
    }
  }, [speechLang]);

  const toggleMic = () => {
    if (!recognitionRef.current) {
      alert('Trình duyệt của bạn không hỗ trợ tính năng Web Speech API. Bạn có thể gõ câu trả lời vào khung văn bản.');
      return;
    }

    if (isListening) {
      recognitionRef.current.stop();
      stopAudioVisualizer();
      setIsListening(false);
    } else {
      try {
        window.speechSynthesis?.cancel();
        setIsSpeaking(false);
        recognitionRef.current.start();
        setIsListening(true);
        startAudioVisualizer();
      } catch (err) {
        console.error(err);
      }
    }
  };

  const handleSpeakQuestion = () => {
    if (!currentTurn?.questionText || !('speechSynthesis' in window)) return;
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(currentTurn.questionText);
    utterance.lang = speechLang;
    utterance.rate = 0.95;
    utterance.onstart = () => setIsSpeaking(true);
    utterance.onend = () => setIsSpeaking(false);
    utterance.onerror = () => setIsSpeaking(false);
    window.speechSynthesis.speak(utterance);
  };

  const submitCurrentAnswer = async () => {
    const turn = turnRef.current;
    if (!turn || submitLockRef.current) return;
    submitLockRef.current = true;
    recognitionRef.current?.stop();
    stopAudioVisualizer();
    setIsListening(false);
    window.speechSynthesis?.cancel();
    setIsSpeaking(false);
    setSubmitting(true);
    try {
      await interviewApi.submit(id, turn.id, transcriptRef.current);
      await fetchTurnData();
    } catch (err) {
      submitLockRef.current = false;
      alert(err.response?.data?.message || 'Lỗi gửi câu trả lời');
    } finally {
      setSubmitting(false);
    }
  };

  const handleSubmit = (e) => {
    e?.preventDefault();
    submitCurrentAnswer();
  };

  const handleAbort = async () => {
    if (!window.confirm('Bạn có chắc chắn muốn huỷ lượt thi này?')) return;
    try {
      await interviewApi.abort(id);
      navigate('/student/interview');
    } catch (err) {
      alert(err.response?.data?.message || 'Lỗi huỷ lượt thi');
    }
  };

  if (loading) {
    return (
      <div className="max-w-4xl mx-auto p-16 text-center text-slate-400 flex items-center justify-center gap-2">
        <Loader2 className="w-5 h-5 animate-spin" />
        Đang khởi động phòng thi vấn đáp...
      </div>
    );
  }

  // Session Completed Screen
  if (sessionData && sessionData.status === 'COMPLETED') {
    return (
      <div className="max-w-3xl mx-auto px-4 sm:px-6 py-12 text-center space-y-6">
        <div className="w-20 h-20 rounded-3xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 flex items-center justify-center mx-auto shadow-2xl shadow-emerald-500/20">
          <CheckCircle2 className="w-10 h-10" />
        </div>

        <div className="space-y-2">
          <h1 className="text-2xl sm:text-3xl font-extrabold text-white">
            Chúc mừng bạn đã hoàn thành buổi thi vấn đáp!
          </h1>
          <p className="text-slate-400 text-sm">
            Môn thi: <strong className="text-white font-mono">{sessionData.subject?.code} - {sessionData.subject?.name}</strong>
          </p>
          <p className="text-xs text-slate-500">
            Biên bản ghi nhận câu trả lời đã được gửi đến giảng viên bộ môn để đánh giá kết quả theo Rubric.
          </p>
        </div>

        <div className="pt-4 flex justify-center gap-4">
          <Link
            to="/student/interview"
            className="px-6 py-3 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-sm shadow-lg shadow-indigo-600/30 transition-all inline-flex items-center gap-2"
          >
            <ArrowLeft className="w-4 h-4" />
            Về trang chủ sinh viên
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      
      {/* Top Status Bar */}
      <div className="p-4 sm:p-5 rounded-2xl bg-slate-900 border border-slate-800 shadow-xl flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <Link
            to="/student/interview"
            className="p-2 rounded-xl text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
            title="Thoát phòng thi"
          >
            <ArrowLeft className="w-4 h-4" />
          </Link>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-mono font-bold text-cyan-400 text-sm">
                {sessionData?.subject?.code}
              </span>
              <span className="text-xs text-slate-400">· {sessionData?.subject?.name}</span>
            </div>
            <span className="text-[11px] text-slate-500 font-medium">Phòng thi vấn đáp AI trực tuyến</span>
          </div>
        </div>

        {/* Countdown Timer */}
        {currentTurn && (
          <div className={`flex items-center gap-2 px-4 py-2 rounded-xl border font-mono font-bold text-sm transition-colors ${
            remainingSec <= 20
              ? 'bg-rose-500/10 text-rose-400 border-rose-500/30 animate-pulse'
              : 'bg-slate-950 text-cyan-400 border-slate-800'
          }`}>
            <Clock className="w-4 h-4" />
            <span>
              {Math.floor(remainingSec / 60).toString().padStart(2, '0')}:
              {(remainingSec % 60).toString().padStart(2, '0')}
            </span>
          </div>
        )}
      </div>

      {/* Main Question Card */}
      {currentTurn ? (
        <div className="space-y-6">
          
          {/* Previous Turn Context (If this is an AI Follow-up) */}
          {prevTurn && (
            <div className="p-4 rounded-xl bg-slate-950 border border-amber-500/20 space-y-2">
              <div className="flex items-center gap-1.5 text-xs font-semibold text-amber-400">
                <BrainCircuit className="w-3.5 h-3.5" />
                <span>Ngữ cảnh câu trả lời trước của bạn:</span>
              </div>
              <p className="text-xs text-slate-300 italic pl-5 border-l-2 border-amber-500/30">
                "{prevTurn.transcript || '(Bạn chưa trả lời)'}"
              </p>
            </div>
          )}

          {/* Current Question */}
          <div className="p-6 sm:p-8 rounded-2xl bg-gradient-to-tr from-slate-900 to-slate-850 border border-slate-800 shadow-xl space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className={`text-xs font-bold uppercase tracking-wider px-3 py-1 rounded-full border ${
                  currentTurn.isFollowUp || currentTurn.turnType === 'FOLLOW_UP'
                    ? 'bg-amber-500/10 text-amber-400 border-amber-500/20'
                    : 'bg-indigo-500/10 text-indigo-400 border-indigo-500/20'
                }`}>
                  {currentTurn.isFollowUp || currentTurn.turnType === 'FOLLOW_UP'
                    ? `AI Hỏi xoáy (Follow-up #${currentTurn.followupIndex})`
                    : `Câu hỏi chính #${currentTurn.mainIndex}`}
                </span>
              </div>

              <button
                type="button"
                onClick={handleSpeakQuestion}
                className="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-cyan-400 transition-colors cursor-pointer flex items-center gap-1 text-xs"
                title="Đọc to câu hỏi"
              >
                <Volume2 className="w-4 h-4" />
                <span className="hidden sm:inline">Nghe lại câu hỏi</span>
              </button>
            </div>

            <p className="text-base sm:text-lg font-bold text-white leading-relaxed">
              {currentTurn.questionText}
            </p>
          </div>

          {/* Student Answer Box */}
          <form onSubmit={handleSubmit} className="p-6 sm:p-8 rounded-2xl bg-slate-900 border border-slate-800 shadow-xl space-y-4">
            <div className="flex items-center justify-between pb-2 border-b border-slate-800">
              <span className="text-xs font-semibold uppercase text-slate-300 flex items-center gap-2">
                <User className="w-4 h-4 text-cyan-400" />
                Câu trả lời của bạn:
              </span>

              {/* Microphone Toggle */}
              <button
                type="button"
                onClick={toggleMic}
                className={`px-3.5 py-1.5 rounded-xl text-xs font-bold flex items-center gap-2 transition-all cursor-pointer ${
                  isListening
                    ? 'bg-rose-500 text-white animate-pulse shadow-lg shadow-rose-500/30'
                    : 'bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white'
                }`}
              >
                {isListening ? (
                  <>
                    <Mic className="w-4 h-4" />
                    Đang ghi âm (Bấm dừng)...
                  </>
                ) : (
                  <>
                    <MicOff className="w-4 h-4 text-slate-400" />
                    Bật Micro trả lời bằng giọng nói
                  </>
                )}
              </button>
            </div>

            <div role="status" aria-live="polite" className="flex items-center gap-2 text-xs text-cyan-300">
              <AudioLines className="w-4 h-4" />
              {submitting ? 'AI đang phân tích câu trả lời...' : isSpeaking ? 'Giám khảo AI đang nói...' : isListening ? 'Đang lắng nghe câu trả lời...' : 'Sẵn sàng nhận câu trả lời'}
            </div>
            {isListening && (
              <div aria-label="Mức âm lượng micro" className="h-10 flex items-center justify-center gap-1 rounded-lg bg-slate-950">
                {Array.from({ length: 20 }, (_, i) => (
                  <span key={i} className="w-1 rounded bg-cyan-400 transition-all duration-75"
                    style={{ height: `${Math.max(4, Math.min(36, micLevel * (0.25 + ((i * 7) % 11) / 10)))}px` }} />
                ))}
              </div>
            )}
            <textarea
              rows="5"
              value={transcript}
              onChange={(e) => { transcriptRef.current = e.target.value; setTranscript(e.target.value); }}
              placeholder="Bạn có thể nói qua Micro (hệ thống sẽ tự nhận dạng thành văn bản) hoặc gõ trực tiếp câu trả lời vào đây..."
              className="w-full bg-slate-950 border border-slate-800 rounded-xl p-4 text-white focus:outline-none focus:border-cyan-500 text-sm leading-relaxed"
            />

            <div className="flex items-center justify-between pt-2">
              <button
                type="button"
                onClick={handleAbort}
                className="text-xs font-medium text-slate-500 hover:text-rose-400 transition-colors"
              >
                Huỷ buổi thi này
              </button>

              <button
                type="submit"
                disabled={submitting}
                className="px-6 py-3 rounded-xl bg-gradient-to-r from-cyan-500 to-indigo-600 hover:from-cyan-400 hover:to-indigo-500 text-white font-bold text-sm shadow-lg shadow-cyan-500/25 flex items-center gap-2 transition-all disabled:opacity-50 cursor-pointer"
              >
                {submitting ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    Đang gửi câu trả lời...
                  </>
                ) : (
                  <>
                    <Send className="w-4 h-4" />
                    Nộp câu trả lời này
                  </>
                )}
              </button>
            </div>
          </form>

        </div>
      ) : (
        <div className="p-12 text-center rounded-2xl bg-slate-900 border border-slate-800 text-slate-400">
          Buổi thi đã kết thúc hoặc không có câu hỏi nào.
        </div>
      )}

    </div>
  );
}
