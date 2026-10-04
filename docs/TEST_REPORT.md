# AIVES – Báo cáo kiểm thử

- **Ngày chạy:** 04/10/2026
- **Môi trường:** Windows 11 · JDK 1.8.0_202 · Tomcat 9.0.95 · PostgreSQL 18 (instance riêng, port 5499) · thư mục `aives_devenv`
- **Kịch bản:** [TEST_GUIDE.md](TEST_GUIDE.md), dữ liệu khởi đầu từ `aives_demo_data.sql`
- **Cách chạy:** bước kiểm tra phía server chạy tự động bằng script HTTP; bước phụ thuộc JavaScript (ẩn/hiện form, tóm tắt cấu hình, đếm ngược, tự nộp, F5, giao diện điện thoại) chạy trên trình duyệt Chromium

## Tổng kết

| Tổng số bước | ✅ Đạt | ❌ Lỗi còn tồn tại | 🎧 Cần người kiểm tra |
|---|---|---|---|
| 85 | 80 | 0 | 5 |

### Lỗi phát hiện trong lúc test và đã sửa

| Mã | Lỗi | Cách sửa | Kết quả test lại |
|---|---|---|---|
| G3 | Trên điện thoại (375px), 3 trang bị tràn ngang: *Tài khoản* (646px), *Môn học* (428px), *AI sinh câu hỏi* (394px) | `style.css`: cột layout dùng `minmax(0, 1fr)` để bảng cuộn trong card thay vì kéo giãn trang; nút trong `.form-actions` được xuống dòng | ✅ 14/14 trang vừa 375px |

Ghi chú A3: lần chạy đầu script báo sai do kỳ vọng chuỗi chuyển trang; kiểm tra lại thì đăng nhập đưa về đúng trang *Tài khoản & phân quyền* → Đạt.

### Các bước cần người kiểm tra (cần loa / micro thật)

| Mã | Bước | Cách kiểm tra |
|---|---|---|
| C3 | Nghe thử giọng đọc | Admin → Cấu hình → *Nghe thử giọng đọc*: nghe được câu tiếng Việt |
| T2 | Kiểm tra micro | SV → Phỏng vấn AI → *Kiểm tra micro*, nói vài câu: thanh âm lượng nhảy, báo *Micro hoạt động tốt* |
| T3, T5 | Loa / đọc câu hỏi | *Kiểm tra loa*, *Nghe lại câu hỏi*: nghe rõ tiếng Việt |
| T6 | Trả lời bằng giọng nói | *Bắt đầu trả lời* và nói: chữ hiện dần trong ô câu trả lời |

## Chi tiết

### Phần 1 · Đăng nhập, đăng xuất & phân quyền

| Mã | Bước kiểm tra | Kết quả | Ghi chú |
|---|---|---|---|
| A1 | Chưa đăng nhập mở trang chủ | ✅ Đạt | 302 /aives_system/login |
| A2 | Sai mật khẩu | ✅ Đạt |  |
| A3 | Đăng nhập admin | ✅ Đạt | Đăng nhập -> / -> /home -> /admin/users (đúng trang Tài khoản). Lần chạy đầu báo FAIL do script test kỳ vọng sai chuỗi redirect. |
| A4 | Đăng xuất | ✅ Đạt |  |
| A5 | Chưa đăng nhập vào trang admin | ✅ Đạt |  |
| A6 | Đăng nhập xong quay lại đúng trang | ✅ Đạt | /aives_system/admin/configs |
| A7 | SV vào trang admin -> lời nhắc lịch sự, không 403 | ✅ Đạt | status=302 |
| A8 | Quay lại trang của tôi | ✅ Đạt | /aives_system/student/interview |
| A9 | GV vào trang admin | ✅ Đạt | status=302 |
| A10 | Sidebar theo vai trò | ✅ Đạt |  |

### Phần 2.1 · Tài khoản

| Mã | Bước kiểm tra | Kết quả | Ghi chú |
|---|---|---|---|
| U1 | Thẻ thống kê tài khoản | ✅ Đạt | [6, 2, 3, 0] |
| U2 | Tìm kiếm 'khoa' | ✅ Đạt |  |
| U3 | Lọc vai trò Giảng viên | ✅ Đạt | 2 |
| U4 | Mở panel Thêm tài khoản (phần server) | ✅ Đạt |  |
| U4 | Panel thêm tài khoản: ẩn/hiện theo vai trò (trình duyệt) | ✅ Đạt | SV: hiện MSSV; GV: hiện chip môn; Admin: ẩn cả hai |
| U5 | Thêm SV student4 | ✅ Đạt | [7, 2, 4, 0] |
| U6 | Trùng username | ✅ Đạt |  |
| U7 | SV thiếu MSSV | ✅ Đạt |  |
| U8 | Mật khẩu ngắn | ✅ Đạt |  |
| U9 | Gán thêm PRJ301 cho lecturer2 | ✅ Đạt | {'2', '1'} |
| U10 | Khoá student4 | ✅ Đạt | [7, 2, 4, 1] |
| U11 | Đăng nhập tài khoản bị khoá | ✅ Đạt |  |
| U12 | Đổi mật khẩu + mở khoá | ✅ Đạt |  |
| U13 | Tự khoá chính mình | ✅ Đạt |  |
| U14 | Xoá chính mình | ✅ Đạt |  |
| U15 | Xoá lecturer1 (có dữ liệu) | ✅ Đạt |  |
| U16 | Xoá student4 | ✅ Đạt |  |

### Phần 2.2 · Môn học

| Mã | Bước kiểm tra | Kết quả | Ghi chú |
|---|---|---|---|
| S1 | Danh sách 3 môn | ✅ Đạt |  |
| S2 | Thêm môn, mã tự viết hoa | ✅ Đạt |  |
| S3 | Trùng mã môn | ✅ Đạt |  |
| S4 | Xoá PRJ301 (có câu hỏi) | ✅ Đạt |  |
| S5 | Xoá MAS291 | ✅ Đạt |  |
| S6 | Ngừng mở SWP391 | ✅ Đạt |  |

### Phần 2.3 · Cấu hình hệ thống

| Mã | Bước kiểm tra | Kết quả | Ghi chú |
|---|---|---|---|
| C1 | Giá trị cấu hình mặc định (phần server) | ✅ Đạt |  |
| C2 | Tóm tắt lượt thi cập nhật trực tiếp (trình duyệt) | ✅ Đạt | 3 câu / 120s -> '3 câu / 9 câu (gồm hỏi xoáy) / 18 phút' (khớp C1) |
| C3 | Nghe thử giọng đọc | 🎧 Cần người | cần nghe bằng tai (đã xác nhận trình duyệt có speechSynthesis) |
| C4 | Thời gian trả lời 5 giây bị từ chối | ✅ Đạt |  |
| C4b | Ô thời gian trả lời chặn < 15 phía trình duyệt | ✅ Đạt | input min=15 |
| C5 | Lưu cấu hình 2 câu / 30s / 2 xoáy | ✅ Đạt |  |

### Phần 3 · Ngân hàng câu hỏi & rubric

| Mã | Bước kiểm tra | Kết quả | Ghi chú |
|---|---|---|---|
| Q1 | Thẻ thống kê + banner chờ duyệt | ✅ Đạt | [8, 4, 3, 1] |
| Q2 | Lọc Chờ duyệt | ✅ Đạt |  |
| Q3 | Lọc Bloom + tìm không phân biệt hoa thường | ✅ Đạt |  |
| Q4 | Thêm câu hỏi mới -> Nháp | ✅ Đạt | /aives_system/lecturer/questions?action=edit&id=11 |
| Q5 | Duyệt khi chưa có rubric | ✅ Đạt |  |
| Q6 | Thêm 2 tiêu chí rubric | ✅ Đạt |  |
| Q7 | Sửa điểm tiêu chí | ✅ Đạt |  |
| Q8 | Điểm âm bị từ chối | ✅ Đạt |  |
| Q9 | Xoá tiêu chí | ✅ Đạt |  |
| Q10 | Duyệt câu hỏi | ✅ Đạt |  |
| Q11 | Câu AI sinh có cảnh báo + Loại bỏ | ✅ Đạt |  |
| Q12 | Loại bỏ từ danh sách | ✅ Đạt |  |
| Q13 | Xoá câu hỏi | ✅ Đạt | 9 -> 8 |
| Q14 | AI sinh 3 câu Listener | ✅ Đạt |  |
| Q15 | Sinh câu hỏi không chọn mức Bloom | ✅ Đạt |  |
| Q16 | GV mở câu hỏi môn không được phân công | ✅ Đạt |  |
| Q17 | lecturer2 thấy DBI202 + PRJ301 | ✅ Đạt |  |

### Phần 4 · Phỏng vấn AI

| Mã | Bước kiểm tra | Kết quả | Ghi chú |
|---|---|---|---|
| T1 | Chọn môn: có DBI202, PRJ301, không có SWP391 | ✅ Đạt |  |
| T2 | Kiểm tra micro | 🎧 Cần người | cần nói vào micro thật |
| T3 | Kiểm tra loa | 🎧 Cần người | cần nghe bằng tai |
| T4 | Bắt đầu thi PRJ301 | ✅ Đạt | /aives_system/student/interview?id=1 |
| T5 | Nghe lại câu hỏi (TTS) | 🎧 Cần người | cần nghe bằng tai |
| T6 | Trả lời bằng giọng nói (STT) | 🎧 Cần người | cần nói vào micro thật |
| T7 | Trả lời ngắn -> hỏi xoáy 'khá ngắn' | ✅ Đạt |  |
| T8 | Trả lời lạc đề -> hỏi xoáy theo rubric | ✅ Đạt | Bạn chưa đề cập đến ý "Phân biệt thời điểm include (dịch / chạy)". Bạn có thể trình bày thêm về ý này không? |
| T9 | Đủ 2 câu xoáy -> sang câu chính 2 | ✅ Đạt | Trong mô hình MVC, Servlet, JSP và JavaBean đóng vai trò gì? |
| T10 | Trả lời đầy đủ -> không hỏi xoáy, kết thúc | ✅ Đạt | Trong mô hình MVC, Servlet, JSP và JavaBean đóng vai trò gì? |
| T11 | Màn hình hoàn thành 4/4, 2 câu xoáy | ✅ Đạt | 4/4 / 2 |
| T12 | Hết giờ tự nộp + đồng hồ đỏ (trình duyệt) | ✅ Đạt | ≤15s: class 'timer low'; hết giờ tự gửi -> AI hỏi xoáy 'còn khá ngắn', khung 'Dựa trên câu trả lời trước: (không có câu trả lời)' |
| T13 | Bắt đầu lại DBI202 -> tiếp tục đúng lượt dở | ✅ Đạt | 2 /aives_system/student/interview?id=2 |
| T14 | F5 không reset đồng hồ (trình duyệt) | ✅ Đạt | Trước F5 0:20 -> sau F5 0:14 (tiếp tục, không về 0:30) |
| T15 | Kết thúc sớm | ✅ Đạt |  |
| T16 | Xem lại lượt PRJ301 | ✅ Đạt |  |
| T17 | SV khác không xem được bài | ✅ Đạt | 404 |
| T18 | Trình duyệt không hỗ trợ nhận giọng nói | ✅ Đạt | Máy không có Firefox -> giả lập trình duyệt thiếu SpeechRecognition: nút ghi âm bị tắt, báo 'Trình duyệt chưa hỗ trợ nhận giọng nói — bạn vui lòng gõ câu trả lời', vẫn gõ và gửi được |

### Phần 5 · Giảng viên xem lượt phỏng vấn

| Mã | Bước kiểm tra | Kết quả | Ghi chú |
|---|---|---|---|
| V1 | lecturer1 chỉ thấy lượt PRJ301 | ✅ Đạt | [1, 1, 0, 0] |
| V2 | Xem transcript | ✅ Đạt |  |
| V3 | lecturer2 thấy DBI202 (kết thúc sớm) + PRJ301 | ✅ Đạt | [2, 1, 0, 1] |
| V4 | Xoá lượt DBI202 | ✅ Đạt |  |
| V5 | Admin thấy mọi lượt | ✅ Đạt |  |
| V6 | Xoá câu hỏi, transcript vẫn giữ nội dung | ✅ Đạt | So sánh JSP include directive và jsp:include action. Khi nào nên dùng mỗi loại? |

### Phần 6 · Giao diện

| Mã | Bước kiểm tra | Kết quả | Ghi chú |
|---|---|---|---|
| G1 | Tiếng Việt hiển thị đúng trên 14 trang | ✅ Đạt | [] |
| G2 | Menu đang mở được tô sáng | ✅ Đạt |  |
| G3 | Giao diện điện thoại 375px (14 trang) | ✅ Đạt | Lần đầu FAIL: 3 trang bị tràn ngang (users 646px, subjects 428px, generate 394px). Đã sửa CSS (minmax(0,1fr) + form-actions wrap), test lại: tất cả 375px |
| G4 | URL sai -> trang 404 thân thiện | ✅ Đạt |  |
| G5 | Mọi nút xoá đều có xác nhận | ✅ Đạt | [] |
