# Chức năng 1: Excel import — nhật ký thay đổi, luồng xử lý và hướng dẫn học code

Ngày triển khai: 24/10/2026. Nền code: `1061b43` của leader. Tài liệu này ghi những thay đổi trong lần bổ sung Excel import, không mô tả toàn bộ dự án.

## 1. Kiến trúc được giữ nguyên

Backend hiện là **monolith có phân lớp**. Chức năng mới không phải microservice, không tạo backend riêng và không chuyển dự án sang một kiểu kiến trúc khác.

```text
Tầng giao diện React
  QuestionsPage -> QuestionImportModal -> questionApi -> axiosClient
                                                       |
                                                       v HTTP + JWT
Tầng tiếp nhận API: LecturerQuestionController
                                                       |
                                                       v
Tầng nghiệp vụ: QuestionImportService
  -> QuestionAccessService: tài khoản và môn được phân công
  -> QuestionApprovalPolicy: điều kiện được duyệt
                                                       |
                                                       v
Tầng truy cập dữ liệu: SubjectRepository / QuestionRepository / RubricRepository
                                                       |
                                                       v
PostgreSQL: questions, subjects, rubrics, users, lecturer_subjects
```

“Monolith” nói về cách tổ chức/triển khai backend; “phân lớp” nói về trách nhiệm bên trong. Hai khái niệm không loại trừ nhau. Không mặc nhiên gọi các folder controller/service/repository là ba server vật lý.

Controller chỉ nhận multipart, lấy người dùng đã xác thực và đóng gói response. Service đọc file/kiểm tra nghiệp vụ/điều phối transaction. Repository thực hiện truy vấn; không viết SQL hay gọi DB trong component React.

## 2. Đã thay đổi gì?

Các đường dẫn sau tính từ thư mục dự án.

### BE và dữ liệu

| File | Thay đổi | Đọc để hiểu |
| --- | --- | --- |
| `backend/src/main/java/com/aives/model/enums/DifficultyLevel.java` | Mới: EASY, MEDIUM, HARD | Độ khó khác với mức Bloom |
| `backend/src/main/java/com/aives/model/entity/Question.java` | Thêm difficulty, notes | Hai trường lưu vào bảng questions; không tạo bảng question bank mới |
| `backend/src/main/java/com/aives/dto/request/QuestionRequest.java` | Nhận độ khó và ghi chú; giới hạn notes 2000 ký tự | Dữ liệu tạo/sửa thủ công |
| `backend/src/main/java/com/aives/dto/response/QuestionResponse.java` | Trả difficulty/notes | FE đọc lại thông tin đã lưu |
| `backend/src/main/java/com/aives/dto/response/QuestionImportResponse.java` | Mới: báo cáo cả file và từng dòng | Số dòng, số lỗi, hash file, số thực sự lưu, thời gian xử lý |
| `backend/src/main/java/com/aives/service/QuestionImportService.java` | Mới: template(), process(), readUpload(), parse(), normalizeContent() | Toàn bộ luồng nhập Excel |
| `backend/src/main/java/com/aives/service/QuestionApprovalPolicy.java` | Mới: requireApprovable() | Dùng lại một quy tắc duyệt cho import, sửa/thêm thủ công và route duyệt |
| `backend/src/main/java/com/aives/service/QuestionService.java` | Lưu metadata và dùng policy duyệt chung | Client cũ không gửi metadata sẽ không xóa dữ liệu import |
| `backend/src/main/java/com/aives/repository/QuestionRepository.java` | Thêm truy vấn nội dung theo môn | Kiểm tra trùng với dữ liệu trong DB |
| `backend/src/main/java/com/aives/repository/SubjectRepository.java` | Thêm lockById() | Khóa môn theo thứ tự ID khi xác nhận để chống hai import đồng thời |
| `backend/src/main/java/com/aives/controller/LecturerQuestionController.java` | Thêm API template và import | Điểm vào HTTP của luồng |
| `database/aives_schema.sql` | Dữ liệu khởi tạo mới có difficulty/notes, rubric theo subject_id | CHỈ dành cho DB mới; script có DROP TABLE |
| `database/aives_demo_data.sql` | Rubric mẫu dùng chung theo môn, tổng 10 điểm/môn | Đồng bộ demo với entity leader đã đổi |
| `database/migrations/V20261024__question_import_and_subject_rubrics.sql` | Migration DB cũ có transaction và backup rubric | Không tự chạy trên DB thật; xem mục 6 |

### FE

| File | Thay đổi |
| --- | --- |
| `frontend/src/components/QuestionImportModal.jsx` | Mới: dialog native, tải mẫu, chọn/kéo thả file, upload progress, preview, lỗi từng dòng, xác nhận và thông báo thành công ngay trong dialog |
| `frontend/src/api/questionApi.js` | downloadImportTemplate(), importExcel(); multipart có boundary do browser tạo, JWT dùng interceptor cũ |
| `frontend/src/pages/lecturer/QuestionsPage.jsx` | Nút Import Excel; mở modal và tải lại danh sách/thống kê sau khi nhập |
| `frontend/src/pages/lecturer/QuestionFormPage.jsx` | Xem/sửa độ khó và ghi chú, giữ riêng đáp án tham khảo |

### Test

- `backend/src/test/java/com/aives/controller/QuestionImportTest.java`: 24 ca mới cho API/service, persistence, quyền, trùng, validation, concurrency, rollback, metadata và hiệu năng.
- `database/tests/question_import_migration_test.sql`: migration trên DB tách biệt, kiểm tra backup/backfill/idempotency, không cascade xóa rubric chung và rollback khi có orphan.
- `frontend/tests/question-import.browser.mjs`: 17 kiểm tra Chrome headless với HTTP giả lập. Không cần thêm thư viện npm test và không kết nối dữ liệu thật.

## 3. Quy tắc nghiệp vụ đã chọn

1. Một workbook `.xlsx`, đúng một sheet, dòng 1 có 5 header đúng thứ tự: **Mã môn | Nội dung câu hỏi | Thang Bloom | Độ khó | Ghi chú/Gợi ý**.
2. Tối đa 10 MB và 1000 dòng dữ liệu. Giới hạn thêm tổng dữ liệu ZIP giải nén 20 MB và 1000 entry trước khi POI đọc workbook. Không hỗ trợ workbook mã hóa/hỏng, macro, công thức hay dữ liệu ngoài 5 cột.
3. Bloom của import chỉ nhận REMEMBER, UNDERSTAND, APPLY, ANALYZE. Không xóa EVALUATE/CREATE khỏi những chức năng đã có.
4. Độ khó cần EASY/MEDIUM/HARD; notes tùy chọn, tối đa 2000 ký tự. Nội dung 1–10000 ký tự. Cột dữ liệu phải là văn bản, không công thức.
5. Mã môn phải tồn tại, hoạt động và thuộc phân công hiện tại của giảng viên; ADMIN được quản lý các môn đang hoạt động.
6. Chống trùng trong file và trong DB theo **cùng môn + nội dung chuẩn hóa Unicode NFC, khoảng trắng và chữ hoa/thường**. Không coi khác môn là trùng. Không tự dùng AI/phân tích ngữ nghĩa để đoán hai câu là tương đương.
7. Preview không lưu dữ liệu. Một dòng lỗi sẽ chặn cả lô khi xác nhận; không âm thầm nhập một phần.
8. Xác nhận gửi lại cùng file và hash trả về từ preview. BE đọc/kiểm tra lại file, quyền và trùng trong DB. Hash ràng buộc nội dung file, không phải token xác thực hay bằng chứng phê duyệt.
9. Mặc định PENDING_REVIEW; có thể chọn APPROVED. Nếu chọn APPROVED thì môn phải có rubric. Quy tắc hiện có của leader chỉ yêu cầu tổng rubric đúng 10 cho nguồn AI, không áp đặt thêm điều kiện tổng 10 cho nguồn IMPORT.
10. Lưu `source=IMPORT`, người tạo và reviewer nếu duyệt thẳng. Ghi chú không được nhập nhằng thành referenceAnswer. Import không tự tạo rubric riêng cho mỗi câu.
11. Transaction đảm bảo không có lô lưu dở khi DB lỗi. Khóa môn tuần tự bảo vệ giữa các import dùng service này; đây không phải unique constraint toàn DB bảo vệ mọi chương trình ngoài hệ thống.
12. UI chặn double click, đổi file/trạng thái làm mất hiệu lực preview. Lỗi xác nhận không rõ kết quả yêu cầu kiểm tra lại ngân hàng và preview mới, không tự retry mù.

## 4. Luồng chính để vẽ activity/sequence sau này

### Tải mẫu và xem trước

1. Giảng viên mở Ngân hàng câu hỏi -> Import Excel -> Tải file mẫu.
2. FE gọi GET template; controller trả workbook do POI tạo tại runtime.
3. Giảng viên điền các câu hỏi, chọn file và trạng thái muốn lưu.
4. FE gọi POST import với `confirm=false`.
5. Service kiểm tra actor, giới hạn/định dạng, header, từng dòng, quyền môn, trùng và điều kiện rubric nếu APPROVED.
6. API trả preview + lỗi + fileHash; DB chưa có câu hỏi mới.

### Xác nhận

7. Không có lỗi -> người dùng bấm Xác nhận nạp vào hệ thống.
8. FE gửi lại file, `confirm=true`, cùng status và previewHash.
9. Service xác minh hash, khóa các môn theo ID, kiểm tra lại quyền/trùng/rubric.
10. Hợp lệ hết -> QuestionRepository.saveAllAndFlush() trong transaction.
11. API trả số đã lưu. Modal hiện thành công ngay trong màn hình; trang yêu cầu refresh danh sách/thống kê.

### Nhánh lỗi cần đưa lên sơ đồ

- Sai file/header/rỗng/quá giới hạn -> HTTP 400, không lưu.
- Sai dữ liệu/quyền môn/trùng/rubric -> preview trả HTTP 200 nhưng success=false và lỗi theo dòng; confirm trả HTTP 422, không lưu cả lô.
- Không xác thực/role không được phép -> Spring Security chặn; tài khoản không hoạt động bị service chặn.
- File thay đổi hoặc thiếu previewHash khi confirm -> HTTP 400, yêu cầu preview lại.
- Có người nhập câu trùng hoặc bị thu hồi phân công sau preview -> kiểm tra lại lúc confirm, không lưu.
- DB lỗi giữa lô -> rollback; không hiện thông báo thành công.
- Mất kết nối khi confirm -> kết quả có thể không rõ; kiểm tra ngân hàng trước khi preview/nhập lại.

## 5. Cách test thủ công qua Swagger và FE

### Swagger

1. Đăng nhập bằng tài khoản giảng viên đang dùng và Authorize token. Không chia sẻ token/API key trong ảnh hoặc báo cáo.
2. GET `/api/lecturer/questions/import/template`; tải workbook từ response, điền mã môn thực sự được phân công.
3. POST `/api/lecturer/questions/import`: chọn `file`, `confirm=false`, `status=PENDING_REVIEW`, để previewHash trống.
4. Execute -> xem data.rows, validRows, invalidRows; lưu fileHash. Xác minh danh sách chưa có câu mới.
5. Cùng file: đổi confirm=true, previewHash=fileHash ở bước trước -> Execute. Kết quả importedCount phải đúng số dòng.
6. Gửi confirm lại cùng file -> 422 do trùng; tổng số câu không tăng.
7. Thử APPROVED khi môn chưa có rubric -> lỗi rõ ràng, không lưu. Cấu hình rubric theo môn rồi preview lại và nhập chính thức.

### FE

1. Login Giảng viên -> Ngân hàng câu hỏi -> Import Excel -> Tải file mẫu Excel.
2. Điền 2 câu khác nhau, chọn file, Xem trước và kiểm tra. Thấy 2 hợp lệ, 0 lỗi, 0 đã lưu.
3. Xác nhận -> thấy thông báo ngay trong modal, không phải cuộn lên đầu trang. Đóng modal để kiểm tra danh sách/thống kê.
4. Nếu đang lọc, bỏ bộ lọc/tìm kiếm để thấy câu mới. Câu chờ duyệt nằm trong danh sách Chờ duyệt.
5. Mở sửa một câu đã import -> kiểm tra độ khó và ghi chú. Sửa nội dung/lưu lại -> metadata không mất.
6. Thử một file có câu trống/CREATE/độ khó sai/mã môn khác: thấy dòng Excel cụ thể và nút xác nhận bị khóa.

Các test cần người dùng nghiệm thu trên môi trường thật: FE gọi BE đang chạy, dữ liệu phân công thật, workbook do Excel/WPS của bạn lưu, schema PostgreSQL đã migrate và giao diện trên màn hình bạn sẽ demo. Test tự động không thay thế hoàn toàn bước này.

## 6. Trước khi chạy trên database cũ

**Cập nhật 25/10/2026:** migration đã áp dụng thành công vào DB thật `aives`, sau khi backup và thử restore. Đã nghiệm thu FE -> BE -> DB thật; xem `docs/NGHIEM_THU_EXCEL_IMPORT_20261025.md`. Các hướng dẫn dưới đây dành cho triển khai ở DB/máy khác; không cần chạy lại script khởi tạo trên máy đã migrate.

1. Dừng BE bằng Ctrl+C trong đúng terminal đang chạy nó. Backup DB bằng pg_dump; không đưa dump vào GitHub vì có dữ liệu người dùng/hash mật khẩu.
2. Xác nhận đúng tên DB/host/user trong cấu hình local, không chia sẻ file chứa key. Lệnh mẫu dưới đây dùng giá trị mặc định `localhost:5432/aives` và PostgreSQL 17, phải đổi nếu máy đang cấu hình khác.

```powershell
cd C:\Users\User\Downloads\SWD392\SWD392_project
& "C:\Program Files\PostgreSQL\17\bin\pg_dump.exe" -h localhost -p 5432 -U postgres -d aives -Fc -f "C:\Users\User\Downloads\aives_before_import_20261024.dump"
# Chỉ chạy migration nếu backup phía trên thành công, và đã xác nhận đúng DB.
& "C:\Program Files\PostgreSQL\17\bin\psql.exe" -h localhost -p 5432 -U postgres -d aives -v ON_ERROR_STOP=1 -f "database\migrations\V20261024__question_import_and_subject_rubrics.sql"
```

Migration tạo `rubrics_backup_20261024`, ánh xạ subject_id theo question_id cũ và giữ question_id làm dấu vết lịch sử. Bỏ NOT NULL và FK cascade cũ để câu hỏi bị xóa không xóa rubric dùng chung. Không tự xóa rubric cũ, gộp tiêu chí hoặc chia lại điểm. Sau migration, giảng viên cần rà soát bộ rubric theo môn; tổng từ nhiều bộ cũ có thể khác 10 và khiến duyệt câu AI bị chặn đúng quy tắc.

Nếu có rubric không thể ánh xạ về môn, transaction rollback. Không sửa bằng cách xóa dữ liệu để cho migration chạy qua. Khôi phục đầy đủ cần backup pg_dump; bảng backup rubric chỉ là lớp dự phòng bổ sung.

### Chạy BE và FE ở hai terminal riêng

```powershell
# Terminal BE
cd C:\Users\User\Downloads\SWD392\SWD392_project\backend
& "C:\Tools\apache-maven-3.10.0\bin\mvn.cmd" spring-boot:run
```

```powershell
# Terminal FE
cd C:\Users\User\Downloads\SWD392\SWD392_project\frontend
npm run dev
```

Không cần Gemini key để dùng Excel import. Không thay đổi config.local.properties trong lần triển khai này.

## 7. Test tự động và bằng chứng

```powershell
# Trong backend: test H2 và Gemini giả lập, không chạm DB thật
& "C:\Tools\apache-maven-3.10.0\bin\mvn.cmd" -o test
# Trong frontend
npm run build
npm run lint
# FE browser regression: chạy Vite riêng trên cổng 5175 trước
npm run dev -- --host 127.0.0.1 --port 5175 --strictPort
# Terminal khác trong frontend, Node 22+ và Chrome đã cài
node tests/question-import.browser.mjs
```

Test PostgreSQL: tạo DB/cluster tách biệt, dùng account test; không trỏ test profile `ddl-auto=create-drop` vào DB thật. File `database/tests/question_import_migration_test.sql` cũng chỉ được chạy trong DB thử nghiệm.

Kết quả đã xác minh ở bản triển khai:

- Toàn bộ BE: **94 test H2 đạt**, gồm 24 ca import mới và 70 ca có sẵn.
- Chrome headless: **17 kiểm tra FE đạt**, API giả lập; đã xem ảnh desktop thành công và mobile lỗi để kiểm tra bố cục.
- FE production build đạt. Lint không có error; các cảnh báo ở trang có sẵn vẫn còn, không tự refactor phần ngoài phạm vi.
- Migration PostgreSQL: backup/backfill, chạy lại, bỏ cascade cũ và rollback orphan đều đạt; schema + demo mới cũng chạy thành công trên DB tạm.
- Lần PostgreSQL đầu: 20/20 ca import đạt, lô 20 câu lưu khoảng 201 ms sau warmup. Đây là thời gian service/transaction trong môi trường thử, không phải bảo đảm mọi máy hay tổng thời gian mở Excel/upload/UI đều dưới 2 giây.
- Chạy lại bản cuối: **24/24 ca import đạt trên PostgreSQL 17**, gồm rollback khi DB lỗi và nhập đồng thời. Lô 20 câu ở lần này khoảng **83 ms** sau warmup; giới hạn phép đo như trên.

Bản bàn giao dùng nhánh riêng `feature/be-question-excel-import`, gồm cả BE/Database và FE của chức năng 1. Không commit/push trực tiếp vào main/develop; không sửa source của Archify hay các chức năng 2–4. Không đưa config.local.properties, database dump hoặc credential lên GitHub.

## 8. Điểm mở code nhanh khi thầy hỏi

Các số dòng dưới đây là mốc của bản triển khai 24/10; sẽ thay đổi nếu sửa code tiếp.

| Câu hỏi | File / phương thức / dòng |
| --- | --- |
| API tải mẫu ở đâu? | LecturerQuestionController.java:42, downloadImportTemplate() |
| API import ở đâu? | LecturerQuestionController.java:50, importQuestions() |
| Luồng nghiệp vụ import? | QuestionImportService.java:39, process() |
| Đọc file, chặn file sai/quá lớn? | QuestionImportService.java:104, readUpload() |
| Kiểm tra header, từng dòng và 4 Bloom? | QuestionImportService.java:132, parse() |
| Chuẩn hóa để so trùng? | QuestionImportService.java:179, normalizeContent() |
| File mẫu được tạo ở đâu? | QuestionImportService.java:190, template() |
| Điều kiện được approve? | QuestionApprovalPolicy.java:17, requireApprovable() |
| Query nội dung trong DB? | QuestionRepository.java:43, findContentsBySubjectId() |
| Khóa để hai import không chạy đua? | SubjectRepository.java:19, lockById() |
| FE chọn file? | QuestionImportModal.jsx:30, chooseFile() |
| FE gửi preview/confirm? | QuestionImportModal.jsx:49, submit() |
| Nút mở import? | QuestionsPage.jsx:460 |

## 9. Ghi chú bàn giao cho leader

- Phạm vi: template Excel, preview/xác nhận import, validation/quyền/chống trùng, metadata độ khó/ghi chú, migration rubric theo môn và giao diện import.
- Không thêm Maven/npm dependency; không sửa pom.xml, application.yml, SecurityConfig, package.json, App.jsx hay axiosClient.js.
- Có thay đổi SQL: máy đang dùng DB cũ phải backup rồi chạy migration trước khi khởi động code mới. Không chạy lại script khởi tạo có DROP TABLE trên DB có dữ liệu.
- Nghiệm thu DB thật và lưu ý rubric cũ: xem `docs/NGHIEM_THU_EXCEL_IMPORT_20261025.md`.
- API mới: GET `/api/lecturer/questions/import/template`; POST multipart `/api/lecturer/questions/import` với file, confirm=false/true, status=PENDING_REVIEW/APPROVED và previewHash khi xác nhận.
- Cần người review kiểm tra cả BE và FE; việc merge vào nhánh chung do leader quyết định, không tự merge trong lần bàn giao này.
