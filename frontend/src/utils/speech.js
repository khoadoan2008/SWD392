// Ưu tiên khớp giọng đọc chuẩn theo ngôn ngữ.
// Tuyệt đối không chọn nhầm giọng tiếng Nhật (Haruka) hoặc tiếng Anh khi đọc tiếng Việt.
export function pickVoice(voices, lang) {
  if (!voices || voices.length === 0 || !lang) return null;
  const target = lang.toLowerCase().replace('_', '-');
  const prefix = target.split('-')[0];

  // 1. Khớp chính xác mã ngôn ngữ (ví dụ: 'vi-vn')
  const exact = voices.find(v => v.lang && v.lang.toLowerCase().replace('_', '-') === target);
  if (exact) return exact;

  // 2. Khớp tiền tố ngôn ngữ (ví dụ: 'vi')
  const prefixMatch = voices.find(v => v.lang && v.lang.toLowerCase().replace('_', '-').startsWith(prefix));
  if (prefixMatch) return prefixMatch;

  // 3. Khớp theo tên cụ thể cho tiếng Việt (tuyệt đối loại trừ các ngôn ngữ khác như ja, en, zh, ko)
  if (prefix === 'vi') {
    const viByName = voices.find(v => {
      const vLang = (v.lang || '').toLowerCase();
      // Loại trừ các giọng tiếng Nhật, Anh, Trung, Hàn
      if (vLang.startsWith('ja') || vLang.startsWith('en') || vLang.startsWith('zh') || vLang.startsWith('ko')) {
        return false;
      }
      const vName = (v.name || '').toLowerCase();
      if (vName.includes('japanese') || vName.includes('haruka') || vName.includes('english') || vName.includes('david') || vName.includes('zira')) {
        return false;
      }
      return vName.includes('vietnam')
        || vName.includes('tiếng việt')
        || vName.includes('tieng viet')
        || vName.includes('hoaimy')
        || vName.includes('namminh');
    });
    if (viByName) return viByName;
  }

  // 4. Khớp theo tên cụ thể cho tiếng Anh
  if (prefix === 'en') {
    const enByName = voices.find(v => {
      const vLang = (v.lang || '').toLowerCase();
      if (vLang.startsWith('ja') || vLang.startsWith('vi') || vLang.startsWith('zh')) return false;
      const vName = (v.name || '').toLowerCase();
      return vName.includes('english') || vName.includes('david') || vName.includes('zira') || vName.includes('natural');
    });
    if (enByName) return enByName;
  }

  return null;
}

export function speechRate(lang) {
  return lang?.toLowerCase().startsWith('vi') ? 0.92 : 1.0;
}
