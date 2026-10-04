# AIVES – Hướng dẫn test toàn bộ hệ thống

Thời gian test hết: khoảng **30–40 phút**. Đánh dấu ✅ / ❌ vào cột cuối khi test.

---

## Phần 0. Chuẩn bị môi trường (làm 1 lần)

### 0.1. Phần mềm cần có

| Phần mềm | Ghi chú |
|---|---|
| JDK 8 | Đã có (`C:\Program Files\Java\jdk1.8.0_202`) |
| PostgreSQL | Đã có (bản 18) – cần nhớ **mật khẩu user `postgres`** đặt lúc cài |
| NetBeans | Đã có (NetBeans 13) |
| **Apache Tomcat 9** | **Chưa có** → tải bản *9.0.x – Core – zip* tại https://tomcat.apache.org/download-90.cgi, giải nén ra ví dụ `C:\apache-tomcat-9.0.95`. ⚠️ Không dùng Tomcat 10/11 (code dùng `javax.servlet`) |
| Chrome hoặc Edge | Để test giọng nói (STT/TTS). Edge thường có sẵn giọng đọc tiếng Việt tốt hơn |

### 0.2. Tạo database + dữ liệu demo

Mở **PowerShell** tại thư mục `D:\AIVES\aives_system` rồi chạy lần lượt (mỗi lệnh sẽ hỏi mật khẩu `postgres`):

```powershell
& "C:\Program Files\PostgreSQL\18\bin\createdb.exe" -U postgres -E UTF8 -T template0 aives
```
```powershell
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -d aives -f database\aives_schema.sql
```
```powershell
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -d aives -f database\aives_demo_data.sql
```

> Các dòng `NOTICE: table ... does not exist, skipping` ở lệnh thứ 2 là **bình thường**.
> Cách khác: mở **pgAdmin** → tạo database `aives` → Query Tool → mở lần lượt 2 file `.sql` → bấm Execute (F5).

Kiểm tra nhanh (phải ra `6 | 10`):
```powershell
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -d aives -c "SELECT (SELECT COUNT(*) FROM users), (SELECT COUNT(*) FROM questions)"
```

### 0.3. Cấu hình kết nối

Mở `src/main/resources/db.properties`, sửa `db.password` thành mật khẩu `postgres` của máy bạn:
```properties
db.url=jdbc:postgresql://localhost:5432/aives
db.user=postgres
db.password=<mật khẩu của bạn>
```

### 0.4. Chạy app bằng NetBeans

1. **Tools → Servers → Add Server… → Apache Tomcat or TomEE** → chọn thư mục Tomcat vừa giải nén → đặt username/password tuỳ ý → Finish.
2. ⚠️ Máy này **cổng 8080 đang bị chương trình khác chiếm**. Trong cửa sổ Servers, chọn Tomcat vừa thêm → tab **Connection** → đổi **Server Port** sang `8089` (hoặc cổng trống khác).
3. Chuột phải project → **Properties → Run** → Server: *Apache Tomcat 9*, Context Path: `/aives_system` → OK.
4. Chuột phải project → **Clean and Build**, rồi **Run** (F6).
5. Mở http://localhost:8089/aives_system

> Cách không dùng NetBeans: build ra `target\aives_system.war` → copy vào `C:\apache-tomcat-9.0.95\webapps\` → chạy `bin\startup.bat`.

### 0.5. Tài khoản test

| Username | Mật khẩu | Vai trò | Ghi chú |
|---|---|---|---|
| `admin` | `admin123` | Quản trị viên | |
| `lecturer1` | `123456` | Giảng viên | Dạy **PRJ301**, **SWP391** |
| `lecturer2` | `123456` | Giảng viên | Dạy **DBI202** |
| `student1`, `student2`, `student3` | `123456` | Sinh viên | |

> Mẹo: dùng **1 cửa sổ thường + 1 cửa sổ ẩn danh** (Ctrl+Shift+N) để đăng nhập 2 vai trò cùng lúc.

---

## Phần 1. Đăng nhập, đăng xuất & phân quyền (Feature 7)

| Mã | Thao tác | Kết quả mong đợi | ✓ |
|---|---|---|---|
| A1 | Mở http://localhost:8089/aives_system khi chưa đăng nhập | Về trang đăng nhập, **không** có lời nhắc | |
| A2 | Đăng nhập `admin` / sai mật khẩu | Báo *"Tên đăng nhập hoặc mật khẩu chưa chính xác…"*, vẫn giữ username đã nhập | |
| A3 | Đăng nhập `admin` / `admin123` | Vào trang **Tài khoản & phân quyền** | |
| A4 | Bấm icon đăng xuất (góc dưới sidebar) | Về trang đăng nhập, báo *"Bạn đã đăng xuất thành công…"* (khung xanh) | |
| A5 | Chưa đăng nhập, gõ thẳng URL `/aives_system/admin/configs` | Về trang đăng nhập, báo *"Vui lòng đăng nhập để tiếp tục…"* (khung vàng) | |
| A6 | Ngay sau A5, đăng nhập `admin` | Tự chuyển **đúng** vào trang Cấu hình hệ thống (trang định vào lúc nãy) | |
| A7 | Đăng nhập `student1`, gõ URL `/aives_system/admin/users` | **Không** hiện lỗi 403. Về trang đăng nhập, báo *"Rất tiếc, tài khoản của bạn hiện chưa được cấp quyền…"* + dòng *"Bạn đang đăng nhập với tài khoản student1"* + link **Quay lại trang của tôi** | |
| A8 | Bấm **Quay lại trang của tôi** | Về trang Phỏng vấn AI của sinh viên | |
| A9 | Đăng nhập `lecturer1`, gõ URL `/aives_system/admin/users` | Giống A7 (giảng viên không vào được trang admin) | |
| A10 | Sidebar mỗi vai trò | Admin: thấy cả 3 nhóm menu. Giảng viên: Ngân hàng câu hỏi + Vấn đáp. Sinh viên: chỉ Phỏng vấn AI | |

---

## Phần 2. Quản trị hệ thống – đăng nhập `admin` (Feature 7)

### 2.1. Tài khoản

| Mã | Thao tác | Kết quả mong đợi | ✓ |
|---|---|---|---|
| U1 | Mở **Tài khoản & phân quyền** | 4 thẻ thống kê: Tổng **6**, Giảng viên **2**, Sinh viên **3**, Tạm khoá **0** | |
| U2 | Gõ `khoa` vào ô tìm kiếm → Lọc | Chỉ còn *Đoàn Minh Khoa* | |
| U3 | Chọn lọc vai trò **Giảng viên** | Chỉ còn 2 giảng viên | |
| U4 | Bấm **Thêm tài khoản** | Panel bên phải mở ra. Chọn vai trò **Sinh viên** → hiện ô MSSV. Chọn **Giảng viên** → ô MSSV ẩn, hiện chip môn học | |
| U5 | Thêm SV: username `student4`, họ tên `Nguyễn Thị Test`, mật khẩu `123456`, MSSV `SE170004` → Lưu | Báo *"Đã tạo tài khoản"*, thẻ Sinh viên tăng lên **4**, tên tiếng Việt hiển thị đúng | |
| U6 | Thêm lại username `student4` | Báo lỗi *"Tên đăng nhập đã tồn tại"*, form giữ nguyên dữ liệu đã nhập | |
| U7 | Thêm SV nhưng để trống MSSV | Báo *"Sinh viên phải có MSSV"* | |
| U8 | Thêm tài khoản mật khẩu `123` | Báo *"Mật khẩu tối thiểu 6 ký tự"* | |
| U9 | Sửa `lecturer2` → tích thêm chip **PRJ301** → Lưu | Lưu thành công. (Kiểm tra lại ở L-phần 3: lecturer2 sẽ thấy câu hỏi PRJ301) | |
| U10 | Sửa `student4` → tắt công tắc **Cho phép đăng nhập** → Lưu | Badge chuyển **Tạm khoá**, thẻ Tạm khoá = 1 | |
| U11 | Đăng xuất, đăng nhập `student4` | Báo *"Tài khoản của bạn hiện đang tạm khoá…"* | |
| U12 | Admin sửa `student4`, nhập mật khẩu mới `654321`, bật lại đăng nhập → Lưu. Đăng nhập `student4`/`654321` | Đăng nhập được (mật khẩu cũ không còn dùng được) | |
| U13 | Sửa chính `admin` → tắt **Cho phép đăng nhập** → Lưu | Báo *"Không thể tự khoá hoặc tự hạ quyền…"* | |
| U14 | Xoá `admin` (đang đăng nhập) | Báo *"Không thể xoá tài khoản đang đăng nhập"* | |
| U15 | Xoá `lecturer1` | Bị chặn: *"Tài khoản đã có dữ liệu liên quan, hãy khoá… thay vì xoá"* | |
| U16 | Xoá `student4` (chưa thi lần nào) | Có hộp xác nhận → xoá thành công | |

### 2.2. Môn học

| Mã | Thao tác | Kết quả mong đợi | ✓ |
|---|---|---|---|
| S1 | Mở **Môn học** | Có 3 môn: DBI202, PRJ301, SWP391 | |
| S2 | Thêm môn mã `mas291` (chữ thường), tên `Xác suất thống kê` | Lưu thành công, mã tự viết hoa **MAS291** | |
| S3 | Thêm môn trùng mã `PRJ301` | Báo *"Mã môn đã tồn tại"* | |
| S4 | Xoá **PRJ301** | Bị chặn: *"Môn học đã có câu hỏi hoặc lượt thi…"* | |
| S5 | Xoá **MAS291** | Xoá thành công | |
| S6 | Sửa **SWP391** → tắt **Đang mở cho thi vấn đáp** | Badge **Ngừng**. (Sinh viên sẽ không thấy SWP391 khi chọn môn thi – kiểm ở T1) | |

### 2.3. Cấu hình hệ thống

| Mã | Thao tác | Kết quả mong đợi | ✓ |
|---|---|---|---|
| C1 | Mở **Cấu hình hệ thống** | Tiếng Việt được chọn; 3 / 120 / 2; cột phải: *3 câu · 9 câu (gồm hỏi xoáy) · 18 phút* | |
| C2 | Đổi số câu chính thành 2 | Ô tóm tắt bên phải **tự cập nhật** ngay khi gõ | |
| C3 | Bấm **Nghe thử giọng đọc** | Máy đọc 1 câu tiếng Việt (bật loa) | |
| C4 | Nhập thời gian trả lời `5` → Lưu | Trình duyệt chặn (min 15). Nếu vượt qua được, server báo *"Thời gian trả lời phải từ 15 đến 900 giây"* | |
| C5 | Đặt: câu chính **2**, thời gian **30**, hỏi xoáy **2** → Lưu | Báo *"Đã lưu cấu hình"* — **giữ cấu hình này** để test Phần 4 cho nhanh | |

---

## Phần 3. Ngân hàng câu hỏi & rubric – đăng nhập `lecturer1` (Feature 1)

| Mã | Thao tác | Kết quả mong đợi | ✓ |
|---|---|---|---|
| Q1 | Mở **Câu hỏi & rubric** | Thẻ thống kê: Tổng **8**, Đã duyệt **4**, Chờ duyệt **3**, Nháp **1** (chỉ tính môn của lecturer1, không thấy câu DBI202) + banner vàng *"Có 3 câu hỏi do AI sinh đang chờ…"* | |
| Q2 | Bấm thẻ **Chờ duyệt** | Bảng chỉ còn 3 câu *Filter* nguồn **AI sinh** | |
| Q3 | Lọc Bloom **Phân tích**, tìm `jsp` (chữ thường) | Ra câu *So sánh JSP include…* (tìm không phân biệt hoa/thường) | |
| Q4 | **Thêm câu hỏi**: môn PRJ301, chủ đề `Chương 7`, Bloom **Vận dụng**, nội dung `Hãy viết một Filter kiểm tra đăng nhập.` → Tạo | Chuyển sang trang biên tập, trạng thái **Nháp**, báo nhắc thêm rubric | |
| Q5 | Bấm **Duyệt vào ngân hàng chính thức** khi chưa có rubric | Báo lỗi *"Cần ít nhất một tiêu chí rubric trước khi duyệt"* | |
| Q6 | Ở phần **Rubric**: thêm tiêu chí `Kiểm tra session`, từ khoá `session, user`, điểm `2` → Thêm. Thêm tiêu chí thứ 2 `Chuyển hướng về login`, từ khoá `redirect, login`, điểm `1` | 2 dòng rubric hiện ra, **Tổng điểm rubric 3.00** | |
| Q7 | Sửa điểm dòng 2 thành `1.5` → bấm ✓ | Tổng điểm **3.50** | |
| Q8 | Nhập điểm `-1` cho dòng mới → Thêm | Báo lỗi *"…điểm tối đa phải là số > 0"* | |
| Q9 | Xoá dòng rubric 2 (icon thùng rác) | Có xác nhận, tổng điểm về **2.00** | |
| Q10 | Bấm **Duyệt** | Badge **Đã duyệt**, nút Duyệt biến mất | |
| Q11 | Mở 1 câu **Chờ duyệt** (Filter – AI sinh) | Có khung vàng *"Câu hỏi do AI sinh…"*. Bấm **Loại bỏ** → badge **Loại bỏ** | |
| Q12 | Về danh sách, ở câu Chờ duyệt bấm icon ✕ | Câu chuyển **Loại bỏ** ngay trong danh sách | |
| Q13 | Xoá câu *Phân tích ưu điểm… Filter* | Có xác nhận → xoá, thẻ Tổng giảm 1 | |
| Q14 | Menu **AI sinh câu hỏi**: môn PRJ301, chủ đề `Listener`, chọn chip Nhớ + Hiểu + Vận dụng → Sinh | Về danh sách lọc Chờ duyệt, thêm **3** câu mới về *Listener* | |
| Q15 | Sinh câu hỏi nhưng bỏ chọn hết chip Bloom | Báo lỗi yêu cầu chọn ít nhất 1 mức | |
| Q16 | Gõ URL `/aives_system/lecturer/questions?action=edit&id=9` (câu của DBI202 – không được phân công) | Về trang đăng nhập với lời nhắc *"chưa được cấp quyền"* | |
| Q17 | Đăng nhập `lecturer2` → Câu hỏi & rubric | Thấy câu DBI202 **và** PRJ301 (vì U9 đã gán thêm PRJ301). Nếu bỏ qua U9 thì chỉ thấy DBI202 | |

---

## Phần 4. Phỏng vấn AI – đăng nhập `student1` (Feature 3)

> Dùng **Chrome / Edge**, bật loa. Lần đầu trình duyệt hỏi quyền micro → **Allow**.
> Cấu hình đang là 2 câu chính · 30 giây/câu · tối đa 2 câu hỏi xoáy (đã đặt ở C5).

### 4.1. Màn hình chuẩn bị

| Mã | Thao tác | Kết quả mong đợi | ✓ |
|---|---|---|---|
| T1 | Mở **Phỏng vấn AI** | Banner *"Sẵn sàng cho bài thi vấn đáp?"*, chip chọn môn có DBI202 + PRJ301 (**không** có SWP391 vì đã tắt ở S6) | |
| T2 | Bấm **Kiểm tra micro** rồi nói vài câu | Thanh âm lượng nhảy theo giọng; sau 5 giây báo *"✅ Micro hoạt động tốt"* | |
| T3 | Bấm **Kiểm tra loa** | Nghe câu *"Xin chào, bạn có nghe rõ tôi nói không?"* | |

### 4.2. Luồng hỏi – hỏi xoáy

Câu hỏi được bốc **ngẫu nhiên** trong các câu **Đã duyệt** của PRJ301. AI hỏi xoáy theo 2 luật:
1. Câu trả lời **dưới 8 từ** → *"Câu trả lời của bạn còn khá ngắn…"*
2. Có tiêu chí rubric mà câu trả lời **chưa chứa từ khoá nào** → *"Bạn chưa đề cập đến ý "…""*

Câu trả lời **đầy đủ** mẫu (chứa đủ từ khoá → AI chuyển câu tiếp, không hỏi xoáy):

| Câu hỏi | Câu trả lời đầy đủ mẫu |
|---|---|
| Vòng đời Servlet | Container Tomcat nạp servlet và gọi init một lần, mỗi request gọi service rồi doGet hoặc doPost, khi tắt ứng dụng thì gọi destroy. |
| JSP include | Include directive gộp file lúc dịch trang nên là tĩnh, còn jsp:include chạy lúc runtime nên động, dùng directive cho header footer cố định. |
| Session và Cookie | Session lưu trên server còn cookie lưu ở trình duyệt client, server dùng cookie JSESSIONID để nhận diện, ví dụ khi đăng nhập ta lưu user vào session. |
| MVC | Servlet là controller nhận request, JSP là view hiển thị giao diện, còn JavaBean và DAO là model xử lý dữ liệu. |
| Filter kiểm tra đăng nhập (câu tạo ở Q4) | Filter lấy session của request, kiểm tra có attribute user hay chưa, nếu chưa thì chuyển hướng về trang đăng nhập. |

| Mã | Thao tác | Kết quả mong đợi | ✓ |
|---|---|---|---|
| T4 | Chọn **PRJ301** → **Bắt đầu phỏng vấn** | Màn hình *AI ĐANG HỎI*: quả cầu tím + sóng âm chuyển động, **máy đọc câu hỏi**; đồng hồ đếm ngược từ 0:30; cột Tiến độ có *Câu hỏi 1, Câu hỏi 2* | |
| T5 | Bấm **Nghe lại câu hỏi** | Đọc lại câu hỏi | |
| T6 | Bấm **Bắt đầu trả lời** và **nói** vài câu | Quả cầu chuyển xanh *ĐANG NGHE BẠN TRẢ LỜI*, chữ hiện dần trong ô câu trả lời. Bấm **Dừng ghi** để dừng. Sửa tay được nội dung | |
| T7 | Xoá ô trả lời, gõ `Em không nhớ ạ` → **Gửi câu trả lời** | Màn hình **AI HỎI XOÁY**: khung *"Dựa trên câu trả lời trước của bạn: "Em không nhớ ạ""*, câu *"Câu trả lời của bạn còn khá ngắn…"*; Tiến độ: Câu 1 ✓, thêm dòng *Câu hỏi xoáy 1* | |
| T8 | Trả lời dài nhưng lạc đề, ví dụ `Em nghĩ phần này khá quan trọng trong môn học và cần ôn tập thật kỹ trước khi thi` | Hỏi xoáy lần 2: *"Bạn chưa đề cập đến ý "<tên tiêu chí rubric>"…"* | |
| T9 | Trả lời bất kỳ | Đã đủ **2** câu hỏi xoáy (giới hạn) → AI chuyển sang **Câu hỏi 2** dù chưa đủ ý | |
| T10 | Câu 2: dán **câu trả lời đầy đủ mẫu** tương ứng ở bảng trên → Gửi | **Không** hỏi xoáy, chuyển thẳng màn hình hoàn thành | |
| T11 | Màn hình hoàn thành | *"Bài thi đã được ghi nhận an toàn"*, số liệu: Câu đã trả lời **4/4**, Câu hỏi xoáy **2**, thời gian trả lời; bên dưới là **hội thoại** AI – SV đúng thứ tự | |

### 4.3. Các tình huống đặc biệt

| Mã | Thao tác | Kết quả mong đợi | ✓ |
|---|---|---|---|
| T12 | Bắt đầu lượt mới môn **DBI202**, **không làm gì**, chờ hết 30 giây | Đồng hồ chuyển đỏ khi còn ≤ 15 giây, hết giờ **tự gửi**; câu tiếp theo là hỏi xoáy *"còn khá ngắn"* (vì câu trả lời rỗng) | |
| T13 | Đang thi giữa chừng → bấm **Phỏng vấn AI** ở sidebar → chọn lại **DBI202** → Bắt đầu | **Tiếp tục đúng lượt đang dở** (không tạo lượt mới); lịch sử chỉ có 1 dòng *Đang thi* cho DBI202 | |
| T14 | F5 (tải lại trang) khi đang thi | Đồng hồ **không reset** về 0:30 mà tiếp tục từ thời gian còn lại | |
| T15 | Bấm **Kết thúc sớm** → xác nhận | Màn hình *"Lượt thi đã kết thúc sớm"*; lịch sử hiện badge **Kết thúc sớm** | |
| T16 | Ở lịch sử bấm **Xem lại** lượt PRJ301 | Xem lại kết quả + hội thoại | |
| T17 | Đăng nhập `student2`, gõ URL `/aives_system/student/interview?id=1` (lượt của student1) | Trang *"Không tìm thấy nội dung"* (không xem được bài người khác) | |
| T18 | Mở trang thi bằng **Firefox** | Nút ghi âm bị tắt, báo *"Trình duyệt chưa hỗ trợ nhận giọng nói — bạn vui lòng gõ câu trả lời"*; vẫn gõ và gửi được | |

---

## Phần 5. Giảng viên xem lượt phỏng vấn (Feature 3)

| Mã | Thao tác | Kết quả mong đợi | ✓ |
|---|---|---|---|
| V1 | `lecturer1` → **Lượt phỏng vấn** | Thấy lượt **PRJ301** của student1 (Hoàn thành); **không** thấy lượt DBI202. Thẻ thống kê đúng số lượng | |
| V2 | Bấm **Transcript** | Hội thoại đầy đủ (câu hỏi xoáy có viền tím đứt nét), cột phải: thông tin SV, thống kê *Câu chính / Câu hỏi xoáy / Đã trả lời / Tổng thời gian nói* | |
| V3 | `lecturer2` → **Lượt phỏng vấn** | Thấy lượt **DBI202** (Kết thúc sớm), và cả lượt PRJ301 nếu đã làm U9 | |
| V4 | `lecturer2` xoá lượt DBI202 | Có xác nhận → xoá, transcript biến mất | |
| V5 | `admin` → **Lượt phỏng vấn** | Thấy lượt của mọi môn | |
| V6 | `lecturer1` xoá câu hỏi đã dùng trong lượt PRJ301 (ở Ngân hàng câu hỏi) → mở lại transcript | Transcript **vẫn giữ nguyên** nội dung câu hỏi (lưu snapshot) | |

---

## Phần 6. Giao diện & trải nghiệm

| Mã | Thao tác | Kết quả mong đợi | ✓ |
|---|---|---|---|
| G1 | Mọi trang có chữ tiếng Việt | Không bị lỗi font (`Ã`, `?`…) | |
| G2 | Menu sidebar | Mục đang mở được tô tím | |
| G3 | F12 → icon điện thoại (Toggle device) → chọn iPhone | Sidebar thành thanh menu ngang, bảng cuộn ngang được, form 1 cột, không bị tràn | |
| G4 | Gõ URL sai, ví dụ `/aives_system/abc` | Trang *"Không tìm thấy nội dung"* + nút Về trang chủ | |
| G5 | Mọi nút **Xoá** | Luôn có hộp xác nhận trước khi xoá | |

---

## Phần 7. Làm lại từ đầu

Muốn test lại với dữ liệu sạch: **dừng Tomcat**, chạy lại 2 lệnh `psql` ở mục 0.2 (file schema tự xoá và tạo lại bảng), rồi chạy lại app.

## Lỗi thường gặp

| Hiện tượng | Cách xử lý |
|---|---|
| Trang báo *"Hệ thống đang gặp sự cố"* ngay khi đăng nhập | Sai `db.password` hoặc chưa tạo DB → xem log Tomcat (tab Output trong NetBeans) |
| `password authentication failed for user "postgres"` | Sửa `db.password` trong `db.properties`, build lại |
| `relation "users" does not exist` | Chưa chạy `aives_schema.sql` vào đúng database `aives` |
| `duplicate key … users_username_key` khi chạy file demo | App đã tự tạo tài khoản trước đó → chạy lại `aives_schema.sql` rồi mới chạy `aives_demo_data.sql` |
| Tomcat báo `Address already in use` / port 8080 | Đổi cổng (mục 0.4 bước 2) |
| `ClassNotFoundException: javax.servlet…` | Đang dùng Tomcat 10+ → chuyển sang **Tomcat 9** |
| Máy không đọc câu hỏi / đọc giọng nước ngoài | Máy thiếu giọng tiếng Việt → thử **Edge**, hoặc cài giọng: Settings → Time & Language → Speech → Add voices → Tiếng Việt |
| Bấm ghi âm không nhận chữ | Phải dùng Chrome/Edge, cho phép micro, và truy cập qua `localhost` (không dùng IP LAN) |
