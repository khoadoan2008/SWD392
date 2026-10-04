# AIVES – Thiết kế giao diện (UI/UX)

Giao diện bám theo bản thiết kế Figma của nhóm (sidebar tối, điểm nhấn tím, thẻ thống kê), triển khai bằng JSP + CSS thuần.

## 1. Design system — [`assets/style.css`](../src/main/webapp/assets/style.css)

| Token | Giá trị | Dùng cho |
|---|---|---|
| `--primary` | `#6d28d9` | Nút chính, mục menu đang chọn, chip được chọn, AI |
| `--sidebar` | `#0f172a` | Sidebar, nền trang đăng nhập |
| `--bg` / `--surface` | `#f4f5fa` / `#ffffff` | Nền trang / card |
| `--success` `--warning` `--danger` | xanh / cam / đỏ | Đã duyệt · Chờ duyệt · Loại bỏ, khoá |
| Font | Inter (Google Fonts), fallback system-ui | |
| Bo góc | 12px card, 8px input/nút | |

**Thành phần dùng chung:** `.card`, `.stats > .stat` (thẻ thống kê có icon), `.filters` (thanh lọc + ô tìm kiếm),
`.table-wrap table`, `.btn` (`primary/success/danger/ghost/sm/lg/block`), `.badge` (tự đổi màu theo trạng thái:
`APPROVED`, `PENDING_REVIEW`, `IN_PROGRESS`...), `.bloom` (6 màu theo mức Bloom), `.chips` (radio/checkbox dạng viên thuốc),
`.switch` (công tắc), `.alert` (success/error/warning/info), `.split` (nội dung + cột phải sticky), `.empty` (trạng thái rỗng).

**Icon:** sprite SVG [`assets/icons.svg`](../src/main/webapp/assets/icons.svg) — `<svg class="icon"><use href="${ico}#i-users"/></svg>`.

**Khung trang:** [`common/header.jspf`](../src/main/webapp/WEB-INF/views/common/header.jspf) dựng sidebar theo vai trò + tiêu đề trang.
Mỗi JSP set 3 thuộc tính trước khi include: `pageTitle`, `pageSection`, `activeNav`.

**Responsive:** < 1100px cột phải xuống dưới; < 900px sidebar thành thanh menu ngang; < 640px form 1 cột.

## 2. Bản đồ màn hình

### Luồng 1 · Quản lý ngân hàng câu hỏi & rubric (Giảng viên)

| Màn hình | URL | Nội dung chính |
|---|---|---|
| Ngân hàng câu hỏi | `/lecturer/questions` | 4 thẻ thống kê (tổng / đã duyệt / chờ duyệt / nháp, bấm để lọc), banner nhắc câu AI chờ duyệt, lọc môn–trạng thái–Bloom, bảng câu hỏi |
| Tạo câu hỏi bằng AI | `/lecturer/questions?action=generate` | Chọn môn, chủ đề, chip mức Bloom; cột phải: quy trình kiểm duyệt 4 bước |
| Biên tập & duyệt | `/lecturer/questions?action=edit&id=` | Form câu hỏi + chip Bloom; cột phải: trạng thái, nút **Duyệt / Về nháp / Loại bỏ**, bảng tra Bloom |
| Rubric | (cùng trang, `#rubrics`) | Sửa từng tiêu chí trực tiếp, dòng thêm mới, tổng điểm |

### Luồng 2 · Lõi phỏng vấn AI (Sinh viên)

| Màn hình | URL | Nội dung chính |
|---|---|---|
| Sẵn sàng thi | `/student/interview` | Banner, chọn môn, lưu ý, **kiểm tra micro (thanh âm lượng) và loa**, lịch sử thi |
| AI đang hỏi | `/student/interview?id=` | Quả cầu AI + sóng âm (đổi trạng thái *đang hỏi / đang nghe / mời trả lời*), câu hỏi, nút nghe lại / ghi âm, ô transcript; cột phải: đồng hồ + thanh thời gian, tiến độ từng câu |
| AI hỏi xoáy | (cùng URL) | Như trên + khung "Dựa trên câu trả lời trước của bạn" |
| Hoàn thành | (cùng URL khi đã xong) | "Bài thi đã được ghi nhận an toàn", 3 số liệu tổng hợp, transcript dạng hội thoại |

Giảng viên xem lại: `/lecturer/sessions` (thẻ thống kê + bảng) và `/lecturer/sessions?action=view&id=` (hội thoại + thông tin & thống kê lượt thi).

### Luồng 3 · Quản trị hệ thống (Admin)

| Màn hình | URL | Nội dung chính |
|---|---|---|
| Tài khoản & phân quyền | `/admin/users` | 4 thẻ thống kê, bảng có avatar; **panel bên phải** để thêm/sửa (chip vai trò, chip môn phân công, công tắc cho phép đăng nhập) |
| Môn học | `/admin/subjects` | Bảng + panel thêm/sửa |
| Cấu hình hệ thống | `/admin/configs` | Card giọng nói (chọn ngôn ngữ, nghe thử), card phỏng vấn AI; cột phải: tóm tắt một lượt thi tính trực tiếp |

### Chung

| Màn hình | Ghi chú |
|---|---|
| Đăng nhập | Bố cục 2 cột (giới thiệu + form). Hiện lời nhắc lịch sự khi chưa đăng nhập / không đủ quyền / vừa đăng xuất |
| Lỗi | 404 / 500 thân thiện, nút về trang chủ |

## 3. Nguyên tắc UX đã áp dụng

- **AI chỉ hỗ trợ, giảng viên chốt** — nhắc lại ở trang cấu hình, transcript và quy trình duyệt.
- Câu AI sinh luôn ở **Chờ duyệt**, được đánh dấu nguồn "AI sinh" và có banner nhắc.
- Thao tác xoá luôn có hộp xác nhận; xoá bị chặn thì gợi ý cách thay thế (khoá tài khoản, ngừng mở môn).
- Phỏng vấn: hết giờ **tự nộp**, sinh viên **sửa được transcript** trước khi gửi, không có micro vẫn **gõ được**.
- Trạng thái rỗng luôn có hướng dẫn hành động kế tiếp.
