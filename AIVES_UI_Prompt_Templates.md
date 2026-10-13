# BỘ PROMPT TẠO GIAO DIỆN MẪU (UI PROMPT TEMPLATES) - AIVES
> **Hệ thống thi vấn đáp thông minh sử dụng AI (AI-powered Viva Exam System)**  
> **Dành cho:** SWD392 - Software Architecture & Design  
> **Tương thích tốt nhất với:** [v0.dev](https://v0.dev), [Bolt.new](https://bolt.new), [Lovable.dev](https://lovable.dev), Claude Artifacts, ChatGPT Plus (Canvas)

---

## 📌 HƯỚNG DẪN DÀNH CHO DEVELOPER (CÁCH DÙNG PROMPT HIỆU QUẢ)
1. **Các công cụ sinh UI khuyên dùng:**
   - **v0.dev** (Của Vercel - Tốt nhất cho React + Tailwind CSS + Lucide Icons + Shadcn UI).
   - **Bolt.new** / **Lovable.dev** (Tạo ứng dụng chạy được cả Frontend và Mock Data đầy đủ).
   - **Claude 3.7 Sonnet / ChatGPT Canvas** (Tạo component đơn lẻ hoặc file HTML/Tailwind xem trực tiếp).
2. **Ngôn ngữ viết Prompt:** Các prompt bên dưới được viết bằng **tiếng Anh kỹ thuật cao cấp kết hợp mô tả nghiệp vụ**, vì các AI sinh giao diện (như v0, Lovable) hiểu tiếng Anh chuẩn xác nhất để xuất ra component đẹp, hiện đại và chuẩn Responsive.

---

## 🎨 DESIGN SYSTEM & THEME CHUNG (GLOBAL STYLE GUIDE)
* **Phong cách (Aesthetics):** Modern EdTech, Clean, SaaS Dashboard, Glassmorphism nhẹ, Dark Mode & Light Mode linh hoạt.
* **Bảng màu chủ đạo (Color Palette):**
  - **Primary:** Deep Indigo (`#4F46E5` / `bg-indigo-600`) - Thể hiện tính học thuật, tin cậy.
  - **Accent / AI Glow:** Cyan / Emerald (`#06B6D4` / `#10B981`) - Biểu thị trạng thái AI đang hoạt động, sóng âm giọng nói.
  - **Warning / Deep-dive:** Amber / Coral (`#F59E0B` / `#EF4444`) - Dành cho huy hiệu cảnh báo hỏi xoáy (Follow-up alert).
  - **Neutral:** Slate Gray (`bg-slate-900` cho Dark mode hoặc `bg-slate-50` cho Light mode).
* **Font chữ:** Inter hoặc Plus Jakarta Sans.

---

# 1. PROMPT MÀN HÌNH NHÓM 1: QUẢN LÝ NGÂN HÀNG CÂU HỎI & RUBRIC
> **Dành cho:** Phân hệ Giảng viên (Lecturer Workspace)  
> **Tính năng:** Danh sách câu hỏi, lọc theo Bloom Level & Độ khó, Modal "AI Sinh câu hỏi", Modal thiết lập ma trận Rubric tự động tính tổng 100%.

```markdown
Create a modern, high-polish Lecturer Dashboard for an AI-powered Viva Exam System (AIVES) using React, Tailwind CSS, Lucide icons, and Shadcn-like components.

### 1. Header & Subject Selector:
- Top navigation bar: Project logo "AIVES", Subject switcher dropdown (e.g., "SWD392 - Software Architecture & Design"), Lecturer profile badge ("Dr. Alan Turing"), and Dark/Light mode toggle.

### 2. Action & Filter Bar:
- Search input with magnifying glass icon: "Search questions by keyword or topic...".
- Dropdown Filter 1: Bloom's Taxonomy Level (All, Remember, Understand, Apply, Analyze, Evaluate, Create) with color-coded tags.
- Dropdown Filter 2: Difficulty Level (All, Easy, Medium, Hard).
- Action Button 1 (Secondary with Sparkles icon): "Generate with AI (RAG from Course Syllabus)" with an eye-catching gradient border.
- Action Button 2 (Primary): "+ Create Question Manually".

### 3. Questions Table & Card List:
- Columns: 
  - Checkbox (multi-select)
  - Question Content (Rich snippet with preview of sample answer)
  - Bloom Level (Pill badge: e.g., "Analyze" in Purple, "Understand" in Blue)
  - Difficulty (Badge: "Hard" in Red, "Medium" in Amber, "Easy" in Green)
  - Rubric Summary (e.g., "3 Criteria (10.0 pts)")
  - AI Generated Tag (Small badge indicating if generated via RAG)
  - Actions dropdown: Edit, Configure Rubric, Preview Viva Audio, Delete.

### 4. Interactive Modal 1: "Rubric Matrix Configuration"
- Triggered when clicking "Configure Rubric" for a question.
- Header: "Configure Evaluation Rubric for Question: [Question Title]".
- Total Score Indicator: "10.0 Pts" with a live progress bar showing Total Weight Percentage (must equal 100%). Green if 100%, Amber/Red if not 100%.
- Dynamic Criteria Rows (User can add/remove rows):
  - Row 1: Criteria Name ("Core architectural comparison"), Weight ("40%"), Max Score ("4.0"), Description textarea.
  - Row 2: Criteria Name ("Trade-off & scalability analysis"), Weight ("40%"), Max Score ("4.0"), Description textarea.
  - Row 3: Criteria Name ("Real-world industry example"), Weight ("20%"), Max Score ("2.0"), Description textarea.
- Button: "+ Add Evaluation Criterion".
- Footer: "Cancel" and "Save Rubric Matrix" buttons.

### 5. Interactive Modal 2: "AI Question Generator (RAG Assistant)"
- Upload Course Syllabus/Slide PDF area with drag-and-drop.
- Input: Topic or Chapter focus (e.g., "Microservices vs Monoliths, Saga Pattern").
- Slider: Target Bloom Level (from Remember to Create).
- Slider: Number of questions to generate (default 3).
- Preview cards for generated questions with an "Approve to Bank" or "Regenerate" button.
```

---

# 2. PROMPT MÀN HÌNH NHÓM 3: PHÒNG PHỎNG VẤN ẢO AI (AI VIVA ROOM - TÍNH NĂNG CỐT LÕI)
> **Dành cho:** Phân hệ Sinh viên (Candidate/Student Viva Arena)  
> **Tính năng:** Avatar AI Giám khảo ảo, Audio Waveform động, Bộ đếm thời gian, Trạng thái (AI Speaking / Listening / Evaluating), Cảnh báo hỏi xoáy (Adaptive Follow-up Badge), Transcript thời gian thực 2 chiều.

```markdown
Build a state-of-the-art, immersive AI Viva Exam Room (Oral Defense Interface) for candidates taking a real-time AI oral examination. The design must look extremely futuristic, calm, and professional, minimizing candidate anxiety while maximizing clarity.

### 1. Top Exam HUD (Heads-Up Display):
- Top bar showing:
  - Subject info: "SWD392 - Final Oral Defense".
  - Candidate info: Avatar, "Jane Doe", Student ID: "SE170123".
  - Exam Progress: Question 2 of 4 (Visual step indicator).
  - Countdown Timer: Big digital countdown badge with clock icon (e.g., "00:45s" for current answer, blinking softly when under 10 seconds).
  - Status indicator: "Microphone Active (Noise cancellation ON)", Connection: "Ultra-low Latency (32ms)".

### 2. Main Interface Layout: Split-screen (60% Left - Interactive AI Stage | 40% Right - Real-time Transcript & Logs)

#### A. Left Side - Virtual AI Examiner Stage:
- Central Visualizer Card:
  - Sleek Glowing AI Examiner Avatar/Orb with dynamic audio waveform animations (CSS pulse/wave animation).
  - Status Pills that toggle dynamically between 3 states:
    * State 1: 🔵 "AI Examiner is speaking..." (Audio waveform pulsing in Cyan/Indigo).
    * State 2: 🟢 "Listening to your answer... Speak now" (Green pulsing ring around mic).
    * State 3: 🟡 "AI is analyzing your answer & preparing follow-up..." (Subtle shimmering loader).
- Current Main Question Card:
  - Large typography: "Explain how Event-Driven Architecture achieves loose coupling, and what challenges arise regarding Data Consistency?"
- 🚨 Adaptive Follow-up Alert Banner (THE CORE DIFFERENTIATOR):
  - A highlighted high-contrast alert box appearing below the main question:
  - Tag: "⚡ DEEP-DIVE FOLLOW-UP (Level 1/2)" with an Amber glowing border.
  - Question: "You mentioned RabbitMQ message queues. What happens if the broker crashes before a consumer acknowledges? How would you guarantee idempotent processing?"
- Bottom Controls Bar:
  - Large central circular Microphone button with audio level meter ripples.
  - Secondary button: "Done Speaking / Submit Response" with keyboard shortcut hint `[Spacebar]`.
  - Emergency button: "Replay AI Question Audio".

#### B. Right Side - Real-time Conversation Transcript:
- Chat/Transcript feed styled like an interview courtroom transcript:
  - [10:02:10] AI Examiner bubble: Audio icon + question text.
  - [10:02:35] Candidate bubble: Voice wave icon + transcribed speech (with live typing effect simulator).
  - [10:03:00] AI Adaptive Follow-up bubble: Highlighted in amber border with badge "Clarification required: Missing Idempotency details".
- Candidate note: "Transcript is saved automatically for faculty grading review".
```

---

# 3. PROMPT MÀN HÌNH NHÓM 7: QUẢN TRỊ HỆ THỐNG & CẤU HÌNH AI
> **Dành cho:** Phân hệ Quản trị viên (Super Admin & Academic Operations)  
> **Tính năng:** Quản lý Người dùng (RBAC), Phân công Giảng viên - Môn học, Bảng điều khiển tham số AI Voice & Lõi hỏi xoáy.

```markdown
Create a comprehensive, enterprise-grade Administration Console for the AI-powered Viva Exam System (AIVES). The UI should feature clean SaaS navigation with 3 main management tabs.

### 1. Navigation & Layout:
- Sidebar or Top Tabs navigation:
  - Tab 1: "User Accounts & Role Management"
  - Tab 2: "Subject Allocation & Faculty Assignments"
  - Tab 3: "AI Speech & Engine Configurations"

### 2. Tab 1: User Accounts & RBAC (Role-Based Access Control)
- Stats counters at top: Total Users (1,240), Lecturers (48), Active Students (1,192), Suspended (2).
- Data Table with filter by Role (Admin, Lecturer, Student):
  - User Avatar, Full Name, Email, Role Badge (Admin: Indigo, Lecturer: Purple, Student: Slate).
  - Assigned Subjects count (e.g., "2 Subjects").
  - Account Status (Toggle Switch: Active / Inactive).
  - Action buttons: "Edit Permissions", "Reset 2FA", "Deactivate".
- Button: "+ Invite / Import Users via CSV".

### 3. Tab 2: Subject Allocation
- Grid of Cards representing Academic Subjects:
  - E.g., Card "SWD392 - Software Architecture & Design".
  - Code, Credits, Number of Questions in Bank (124).
  - Assigned Lead Lecturers with avatar group and "+ Assign Lecturer" modal button.
  - Toggle: "Viva Exam Mode Enabled / Disabled".

### 4. Tab 3: AI Speech & Adaptive Follow-up Parameter Settings (CRUCIAL)
- Settings form organized in clean cards with informative tooltips:
  - Card A: Speech Synthesis (TTS - Text-to-Speech)
    * Provider Dropdown: Google Cloud Neural2, OpenAI Whisper/TTS, Azure Speech.
    * Default Voice Select: "vi-VN-Neural2-A (Vietnamese Female)", "vi-VN-Neural2-B (Vietnamese Male)", "en-US-Journey".
    * Speech Rate & Pitch Sliders with a "Test Voice Preview" play button.
  - Card B: Speech Recognition (STT - Speech-to-Text)
    * Model: Whisper Large V3 / Google Chirp with specialized Vietnamese Tech Glossary tuning.
    * Silence detection timeout slider (e.g., 3.5 seconds before auto-submitting answer).
  - Card C: Adaptive Follow-up (Hỏi xoáy) Rules
    * Max Follow-up Depth per question: Stepper [ 2 ] (Min 1, Max 4).
    * Candidate Answer Time Limit: [ 90 ] seconds.
    * Rubric Gap Threshold: Slider [ 70% ] (If answer completeness is below 70%, trigger a deep-dive question).
- Sticky bottom action bar with "Discard Changes" and "Save & Apply Configuration".
```

---

# 4. PROMPT FULL APPLICATION (SINGLE-PAGE PROTOTYPE ĐỂ DEMO THUYẾT TRÌNH)
> **Mục đích:** Nếu bạn muốn đưa vào **v0.dev** hoặc **Bolt.new** để tạo ra **MỘT ỨNG DỤNG DUY NHẤT CÓ THANH CHUYỂN TAB** giúp trình chiếu cho giảng viên xem toàn bộ 3 nhóm chức năng cùng một lúc.

```markdown
Create an interactive, full-featured prototype application for "AIVES - AI-powered Viva Exam System" in a single cohesive React + Tailwind CSS dashboard.

Include a top Navigation Bar with a Role & View Switcher so I can seamlessly demonstrate all 3 required project scopes during my software presentation:
- View 1: "Lecturer: Question Bank & Rubric Matrix" (Scope 1)
- View 2: "Student: Live AI Viva Exam Room with Adaptive Follow-up" (Scope 3)
- View 3: "Admin: System & AI Engine Configuration" (Scope 7)

Make the prototype interactive:
1. In View 1 (Question Bank):
   - Allow clicking a question to open the Rubric Matrix modal.
   - The Rubric modal must calculate the sum of criteria weights in real-time and show a warning if not 100%.
2. In View 2 (AI Viva Room):
   - Include a working interactive mock: Clicking the "Microphone" button toggles recording state, animates the audio visualizer waves, starts the countdown timer from 45 seconds, and simulates the AI generating an "Adaptive Follow-up Question" badge after submitting.
3. In View 3 (Admin Settings):
   - Allow tweaking the "Max Follow-up Depth" and "TTS Voice" with visual toggle switches.

Use Tailwind CSS, Lucide-react icons, sleek dark/light modern tech theme (Slate-900 background with vibrant Indigo and Emerald neon accents for AI features).
```

---

## 💡 MẸO TRÌNH BÀY (TIPS CHO BUỔI BẢO VỆ ĐỒ ÁN)
* **Khoe tính năng "Adaptive Follow-up" (Nhóm 3):** Hãy nhấn mạnh với Hội đồng rằng giao diện của bạn có khu vực hiển thị trạng thái riêng cho **câu hỏi đào sâu/hỏi xoáy**, chứng minh đây là hệ thống thi vấn đáp tương tác thông minh chứ không phải là trắc nghiệm hay đọc câu hỏi tĩnh.
* **Khoe tính năng "Rubric Matrix" (Nhóm 1):** Chỉ rõ thanh trạng thái tự tính tổng $100\%$ điểm và trọng số, đảm bảo tính chặt chẽ về mặt nghiệp vụ trước khi đưa đề thi vào sử dụng.
