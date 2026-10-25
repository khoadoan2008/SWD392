# Nghiệm thu Excel import trên database thật — 25/10/2026

## Kết quả

Đã hoàn tất hai bước kỹ thuật được người dùng cho phép: backup/migration DB thật và kiểm thử tích hợp FE -> BE -> PostgreSQL thật. Bản bàn giao code dùng nhánh riêng `feature/be-question-excel-import`, không tự merge vào main/develop. Không gọi Gemini. Không đổi mật khẩu hay cơ chế xác thực PostgreSQL.

## 1. Backup trước khi thay đổi

- Đã xác minh đích thực tế: PostgreSQL 17.11, DB `aives`, localhost:5432, user `postgres`.
- Lấy backup custom-format bằng pg_dump, đọc được bằng pg_restore --list.
- Đã thử restore toàn bộ archive vào DB tạm riêng và đối chiếu số dòng 8 bảng nghiệp vụ. Restore thành công; sau đó xóa đúng DB kiểm tra vừa tạo, giữ nguyên DB nguồn.
- Backup và manifest được giữ bên ngoài repo ở workspace nghiệm thu; không thêm vào GitHub. Dump chứa dữ liệu tài khoản/hash mật khẩu nên không chia sẻ công khai.
- Archive 26.090 byte. SHA-256: `A356F1FDC567713AC5DBE0DD7842D8082906E65A6D021C38D47644E597688714`.

## 2. Migration đã áp dụng

Script: `database/migrations/V20261024__question_import_and_subject_rubrics.sql`.

- Thêm difficulty mặc định MEDIUM và notes vào questions.
- Tạo bản sao `rubrics_backup_20261024` với 25 dòng cũ.
- Điền subject_id của rubric từ môn của câu hỏi cũ; không mất rubric nào.
- Giữ question_id như dấu vết lịch sử nhưng bỏ ràng buộc bắt buộc và cascade cũ, tránh xóa câu hỏi làm mất rubric chung.
- Không chạy lại script khởi tạo có DROP TABLE; không gộp/xóa/chia lại điểm rubric.
- Khởi động BE với `ddl-auto=validate`; Hibernate xác nhận schema tương thích, không tự chỉnh schema thêm khi chạy nghiệm thu.

### Đối chiếu dữ liệu trước/sau

| Bảng | Trước | Sau khi migration và dọn test | Nội dung gốc |
| --- | ---: | ---: | --- |
| questions | 14 | 14 | Hash các cột gốc trùng |
| rubrics | 25 | 25 | Hash các cột gốc trùng |
| users | 6 | 6 | Hash trùng |
| subjects | 3 | 3 | Hash trùng |
| lecturer_subjects | 3 | 3 | Hash trùng |
| interview_sessions | 0 | 0 | Không đổi |
| interview_turns | 0 | 0 | Không đổi |
| system_configs | 4 | 4 | Hash trùng |

Các cột mới difficulty/notes được loại khỏi phép hash nội dung câu hỏi cũ; subject_id mới được loại khỏi hash rubric cũ. Kiểm tra riêng xác nhận không có difficulty hoặc rubric.subject_id bị null.

## 3. Kiểm thử tích hợp thật

Chrome headless mở FE ở localhost:5173, dùng form/nút đăng nhập giảng viên thật. FE gọi BE localhost:8080 qua Vite proxy và lưu DB `aives`. Không inject phiên đăng nhập, không mock/intercept API.

**22 kiểm tra đạt** trong lượt xác minh cuối:

1. Tải workbook template từ API thật.
2. Đăng nhập từ giao diện bằng tài khoản giảng viên demo hiện có.
3. Upload workbook 20 câu, preview có 20 hợp lệ và 0 đã lưu.
4. Danh sách ngân hàng không thay đổi trước khi xác nhận.
5. Xác nhận từ FE, nhập đủ 20 câu và hiện thông báo ngay trong modal.
6. Danh sách tải lại và hiện các câu vừa nhập.
7. DB/API đọc lại đủ 20 bản ghi source=IMPORT, status=PENDING_REVIEW.
8. Cột Nguồn ở FE hiển thị đúng Nhập Excel.
9. Bloom, độ khó và ghi chú lưu đúng; không tự ghi ghi chú thành đáp án tham khảo.
10. Form sửa đọc lại đúng độ khó/ghi chú.
11. Sửa ghi chú trên FE, lưu và đọc lại qua API thật thành công.
12. Route duyệt thông thường chấp nhận câu import khi môn có rubric.
13. Header màn duyệt hiển thị đúng nguồn Excel, không gắn nhầm nhãn AI.
14. Bấm phê duyệt trên màn duyệt FE cập nhật trạng thái APPROVED thật.
15. Gửi lại lô cũ bị 422, không tạo bản ghi trùng.
16. File có lỗi hiện chi tiết ở UI và khóa nút xác nhận.
17. File lỗi có 1 dòng hợp lệ/2 dòng lỗi: nhận đúng lỗi nội dung rỗng, Bloom CREATE, độ khó sai, môn không thuộc phân công; không nhập một phần.
18. Chọn APPROVED cho môn chưa có rubric bị chặn.
19. Chọn APPROVED cho môn có rubric nhập thành công, ghi reviewer theo quy trình.
20. Tài khoản STUDENT bị chặn API import.
21. Yêu cầu ẩn danh bị chặn API import.
22. Không có JavaScript exception chưa xử lý trong trình duyệt.

### Thời gian

- Lần đầu: từ bấm xác nhận đến thông báo thành công khoảng 595 ms, service khoảng 526 ms.
- Lần chạy cuối: toàn đoạn UI xác nhận khoảng **258 ms**, service khoảng **207 ms**, cho 20 câu.
- Đây là hai lần đo ở máy local sau warmup. Không phải benchmark tải lớn hay bảo đảm mọi máy đều cùng thời gian.

### Dữ liệu test

Mỗi lượt dùng một prefix duy nhất, tạo 20 câu chờ duyệt + 1 câu duyệt thẳng. Hai lượt tạo tổng 42 câu kiểm thử rồi xóa đúng các câu của từng lượt qua API, sau khi kiểm tra ID không thuộc dữ liệu gốc. Không sửa/xóa câu hỏi có sẵn. Sequence ID của PostgreSQL tiếp tục tăng; không reset sequence để lấp khoảng trống.

## 4. Lỗi nhỏ phát hiện và sửa trong nghiệm thu

File sửa: `frontend/src/pages/lecturer/QuestionsPage.jsx`.

- Trước: badge Nguồn chỉ phân biệt AI và còn lại là Thủ công, nên câu source=IMPORT bị ghi sai.
- Sau: IMPORT -> Nhập Excel, AI -> AI sinh, MANUAL -> Thủ công; header màn duyệt cũng phân biệt đúng nguồn.
- Đổi tiêu đề/bộ lọc chờ duyệt sang trung tính vì danh sách này có cả câu AI lẫn Excel.
- Đã chạy lại đủ 22 kiểm tra tích hợp sau sửa và build production FE thành công.

## 5. Điểm dữ liệu nghiệp vụ còn cần giảng viên/leader chốt

Migration bảo toàn tất cả rubric cũ nên tổng rubric theo môn hiện là:

| Môn | Số tiêu chí | Tổng điểm |
| --- | ---: | ---: |
| PRJ301 | 21 | 50,50 |
| DBI202 | 4 | 6,00 |
| SWP391 | 0 | 0 |

Đây là dữ liệu được kế thừa từ các bộ rubric từng câu trước đây, không phải lỗi mất dữ liệu. Theo code hiện tại, duyệt câu nguồn AI yêu cầu rubric môn tổng đúng 10; vì vậy PRJ301/DBI202 sẽ bị chặn ở quy tắc này cho tới khi giảng viên thiết kế lại rubric môn. Tôi không tự xóa/chia điểm hoặc hạ quy tắc để làm test qua.

Excel import và quy trình duyệt câu IMPORT đã nghiệm thu theo quy tắc hiện có của leader: phải có rubric khi APPROVED. Việc định nghĩa lại bộ rubric chung là một quyết định nghiệp vụ riêng, không làm ngầm trong đợt migration này.

## 6. Trạng thái chạy và bước người dùng kiểm tra

- Đã chạy BE 8080 và FE 5173 để nghiệm thu. Kiểm tra lại trạng thái tiến trình nếu sử dụng ở một buổi khác.
- Mật khẩu DB chỉ được truyền trong môi trường tiến trình, không ghi vào code/config/tài liệu/commit. Khi tự khởi động lại BE, cần cấu hình DB_PASSWORD riêng trên máy; mật khẩu không được tự lưu lâu dài trong lượt này.
- File template và fixture đã dùng nằm trong workspace nghiệm thu, không nạp lại dữ liệu test vào DB sau khi đã dọn.
- Người dùng còn nên tự mở FE, dùng workbook mình chuẩn bị và tập demo để xác nhận trải nghiệm trên màn hình thực tế.

Backup/migration được thực hiện sau khi người dùng cho phép. Việc bàn giao code lên nhánh feature riêng cũng có yêu cầu riêng của người dùng; không bao gồm quyền merge vào nhánh chung.
