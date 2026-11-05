// Không tìm thấy giọng đúng ngôn ngữ thì trả về null để trình duyệt tự chọn theo utterance.lang,
// tránh đọc tiếng Việt bằng giọng tiếng Anh.
export function pickVoice(voices, lang) {
  if (!voices || voices.length === 0 || !lang) return null;
  const target = lang.toLowerCase();
  const prefix = target.split('-')[0];
  return voices.find(v => v.lang?.toLowerCase() === target)
    || voices.find(v => v.lang?.toLowerCase().replace('_', '-').split('-')[0] === prefix)
    || null;
}

export function speechRate(lang) {
  return lang?.toLowerCase().startsWith('vi') ? 0.92 : 1.0;
}
