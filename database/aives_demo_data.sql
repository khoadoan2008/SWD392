-- =============================================================
-- AIVES - Dữ liệu demo (chạy SAU aives_schema.sql)
--   psql -U postgres -d aives -f database/aives_demo_data.sql
--
-- Tài khoản (mật khẩu đã băm PBKDF2, CHỈ DÙNG ĐỂ DEMO):
--   admin      / admin123   Quản trị viên
--   lecturer1  / 123456     Giảng viên  (PRJ301, SWP391)
--   lecturer2  / 123456     Giảng viên  (DBI202)
--   student1..3/ 123456     Sinh viên
-- =============================================================

-- Đảm bảo psql đọc file dạng UTF-8 (tránh lỗi font tiếng Việt trên Windows)
SET client_encoding = 'UTF8';

INSERT INTO subjects (code, name, description) VALUES
 ('SWP391', 'Software Development Project', 'Quy trình phát triển phần mềm theo nhóm');

INSERT INTO roles (name, description) VALUES
 ('ADMIN',    'Quản trị hệ thống'),
 ('LECTURER', 'Giảng viên'),
 ('STUDENT',  'Sinh viên');

INSERT INTO users (role_id, username, password_hash, full_name, email, student_code) VALUES
 ((SELECT id FROM roles WHERE name = 'ADMIN'),    'admin',     '65536:kfTNw/rdoXXxzRGQ0Y0Z2g==:gbVuZTCHO759D4Ro6V/opitW/x27fUOAIQ2MGhrAa24=', 'Quản trị hệ thống', 'admin@aives.edu.vn',     NULL),
 ((SELECT id FROM roles WHERE name = 'LECTURER'), 'lecturer1', '65536:svZ+c3lZ4+XK6sWTdHNtdQ==:0svGRpt9mLJNAthHviv1HgZn30rF5ANNYsens3GXX4E=', 'Nguyễn Văn Minh',   'minhnv@aives.edu.vn',    NULL),
 ((SELECT id FROM roles WHERE name = 'LECTURER'), 'lecturer2', '65536:svZ+c3lZ4+XK6sWTdHNtdQ==:0svGRpt9mLJNAthHviv1HgZn30rF5ANNYsens3GXX4E=', 'Trần Thị Lan',      'lantt@aives.edu.vn',     NULL),
 ((SELECT id FROM roles WHERE name = 'STUDENT'),  'student1',  '65536:svZ+c3lZ4+XK6sWTdHNtdQ==:0svGRpt9mLJNAthHviv1HgZn30rF5ANNYsens3GXX4E=', 'Lê Hoàng An',       'anlh@student.edu.vn',    'SE170001'),
 ((SELECT id FROM roles WHERE name = 'STUDENT'),  'student2',  '65536:svZ+c3lZ4+XK6sWTdHNtdQ==:0svGRpt9mLJNAthHviv1HgZn30rF5ANNYsens3GXX4E=', 'Phạm Thu Hà',       'hapt@student.edu.vn',    'SE170002'),
 ((SELECT id FROM roles WHERE name = 'STUDENT'),  'student3',  '65536:svZ+c3lZ4+XK6sWTdHNtdQ==:0svGRpt9mLJNAthHviv1HgZn30rF5ANNYsens3GXX4E=', 'Đoàn Minh Khoa',    'khoadm@student.edu.vn',  'SE170003');

INSERT INTO lecturer_subjects (lecturer_id, subject_id)
SELECT u.id, s.id FROM users u JOIN subjects s ON
       (u.username = 'lecturer1' AND s.code IN ('PRJ301', 'SWP391'))
    OR (u.username = 'lecturer2' AND s.code = 'DBI202');

-- Câu hỏi + rubric. Dùng DO block để lấy id vừa tạo (RETURNING ... INTO).
DO $$
DECLARE
    gv1  INT := (SELECT id FROM users WHERE username = 'lecturer1');
    gv2  INT := (SELECT id FROM users WHERE username = 'lecturer2');
    prj  INT := (SELECT id FROM subjects WHERE code = 'PRJ301');
    dbi  INT := (SELECT id FROM subjects WHERE code = 'DBI202');
    q    INT;
BEGIN
    -- ===== PRJ301: đã duyệt =====
    INSERT INTO questions (subject_id, topic, content, reference_answer, bloom_level, status, source, created_by, reviewed_by)
    VALUES (prj, 'Chương 2 - Servlet', 'Hãy trình bày vòng đời của một Servlet trong web container.',
            'Container nạp class, gọi init() một lần; mỗi request gọi service() -> doGet/doPost; khi undeploy gọi destroy().',
            'UNDERSTAND', 'APPROVED', 'MANUAL', gv1, gv1) RETURNING id INTO q;

    INSERT INTO questions (subject_id, topic, content, reference_answer, bloom_level, status, source, created_by, reviewed_by)
    VALUES (prj, 'Chương 3 - JSP', 'So sánh JSP include directive và jsp:include action. Khi nào nên dùng mỗi loại?',
            'Directive include tại thời điểm dịch (tĩnh), action include tại thời điểm chạy (động).',
            'ANALYZE', 'APPROVED', 'MANUAL', gv1, gv1) RETURNING id INTO q;

    INSERT INTO questions (subject_id, topic, content, reference_answer, bloom_level, status, source, created_by, reviewed_by)
    VALUES (prj, 'Chương 4 - Session', 'Session và Cookie khác nhau như thế nào? Cho ví dụ ứng dụng đăng nhập.',
            'Session lưu phía server, cookie lưu phía client; JSESSIONID là cookie trỏ tới session.',
            'APPLY', 'APPROVED', 'MANUAL', gv1, gv1) RETURNING id INTO q;

    INSERT INTO questions (subject_id, topic, content, reference_answer, bloom_level, status, source, created_by, reviewed_by)
    VALUES (prj, 'Chương 5 - MVC', 'Trong mô hình MVC, Servlet, JSP và JavaBean đóng vai trò gì?',
            'Servlet = Controller, JSP = View, JavaBean/DAO = Model.',
            'REMEMBER', 'APPROVED', 'MANUAL', gv1, gv1) RETURNING id INTO q;

    -- ===== PRJ301: Đã duyệt =====
    INSERT INTO questions (subject_id, topic, content, reference_answer, bloom_level, difficulty, status, source, created_by, reviewed_by) VALUES
     (prj, 'Filter', 'Hãy nêu định nghĩa của Filter trong Java Web.', 'Filter là một thành phần chặn request/response trước khi tới Servlet hoặc gửi về client để thực hiện kiểm tra, logging, xác thực.', 'REMEMBER', 'EASY', 'APPROVED', 'MANUAL', gv1, gv1),
     (prj, 'Filter', 'Cho một ví dụ thực tế áp dụng Filter và mô tả cách bạn triển khai.', 'Ví dụ AuthenticationFilter kiểm tra session/JWT trước khi cho phép vào các trang quản trị /admin.', 'APPLY', 'MEDIUM', 'APPROVED', 'MANUAL', gv1, gv1),
     (prj, 'Filter', 'Phân tích ưu điểm và nhược điểm của Filter.', 'Ưu điểm: Tái sử dụng logic, tách biệt mối quan tâm. Nhược điểm: Tăng độ trễ nếu có quá nhiều filter trong chain.', 'ANALYZE', 'MEDIUM', 'APPROVED', 'MANUAL', gv1, gv1),
     (prj, 'Chương 6 - JDBC', 'Thiết kế lớp DAO cho bảng sinh viên, giải thích vì sao nên dùng PreparedStatement.', 'PreparedStatement biên dịch trước câu lệnh SQL và tách biệt dữ liệu với mã thực thi, giúp ngăn chặn triệt để lỗ hổng SQL Injection và tối ưu hiệu năng.', 'CREATE', 'HARD', 'APPROVED', 'MANUAL', gv1, gv1);

    -- ===== PRJ301: Chủ đề Spring Boot & REST API =====
    INSERT INTO questions (subject_id, topic, content, reference_answer, bloom_level, difficulty, status, source, created_by, reviewed_by) VALUES
     (prj, 'Spring Boot & REST API',
      'Hãy giải thích sự khác biệt cơ bản giữa hai annotation @Controller và @RestController trong Spring Framework. Khi nào nên dùng mỗi loại?',
      '@Controller dùng trong kiến trúc Spring MVC truyền thống để trả về view template (JSP, Thymeleaf). @RestController là sự kết hợp của @Controller và @ResponseBody, dùng để xây dựng RESTful web services, mặc định serialize trực tiếp dữ liệu trả về thành JSON hoặc XML trong HTTP Response Body.',
      'REMEMBER', 'EASY', 'APPROVED', 'MANUAL', gv1, gv1),
     (prj, 'Spring Boot & REST API',
      'Trình bày nguyên lý Dependency Injection (DI) và Inversion of Control (IoC) trong Spring Boot. Có các cách nào để inject một dependency vào Spring Bean và cách nào được khuyến nghị nhất? Vì sao?',
      'IoC đảo ngược quyền kiểm soát việc khởi tạo và quản lý vòng đời đối tượng cho Spring IoC Container. DI là kỹ thuật triển khai IoC, inject phụ thuộc từ bên ngoài. Có 3 cách inject: Field Injection (@Autowired trên thuộc tính), Setter Injection, và Constructor Injection. Constructor Injection được khuyến nghị nhất vì đảm bảo tính bất biến (immutability), tránh NullPointerException khi test/khởi tạo, và dễ dàng viết Unit Test mà không cần Spring context.',
      'UNDERSTAND', 'MEDIUM', 'APPROVED', 'MANUAL', gv1, gv1),
     (prj, 'Spring Boot & REST API',
      'Trong thiết kế RESTful API, hãy so sánh phương thức HTTP PUT và PATCH khi cập nhật tài nguyên. Cho ví dụ cụ thể về trường hợp sử dụng mỗi loại.',
      'PUT dùng để thay thế toàn bộ (complete replacement) tài nguyên hiện có. Nếu một thuộc tính bị bỏ sót trong payload PUT, nó có thể bị ghi đè thành null hoặc giá trị mặc định. PATCH dùng để cập nhật một phần (partial update) chỉ các trường được chỉ định. Ví dụ: Cập nhật toàn bộ thông tin sinh viên (tên, ngày sinh, email, địa chỉ) thì dùng PUT /api/students/{id}. Nếu chỉ đổi số điện thoại hoặc trạng thái active thì dùng PATCH /api/students/{id} với body chỉ chứa { "phone": "0987654321" }.',
      'APPLY', 'MEDIUM', 'APPROVED', 'MANUAL', gv1, gv1),
     (prj, 'Spring Boot & REST API',
      'Trong Spring Data JPA / Hibernate, vấn đề N+1 query là gì? Nguyên nhân phát sinh và các giải pháp kỹ thuật để xử lý triệt để vấn đề này trong dự án?',
      'N+1 query xảy ra khi truy vấn 1 danh sách gồm N entity (1 query), sau đó Hibernate thực hiện thêm N câu lệnh SELECT riêng lẻ để nạp các quan hệ liên quan (Lazy loading) của từng entity đó, dẫn đến tổng cộng N+1 queries làm chậm database nghiêm trọng. Nguyên nhân do mặc định fetch type LAZY hoặc không join fetch. Giải pháp xử lý: 1) Sử dụng JOIN FETCH trong câu truy vấn JPQL/HQL; 2) Sử dụng @EntityGraph chỉ định thuộc tính nạp đồng thời; 3) Cấu hình batch size (hibernate.default_batch_fetch_size hoặc @BatchSize) để Hibernate gom các query con lại thành câu lệnh WHERE id IN (...).',
      'ANALYZE', 'HARD', 'APPROVED', 'MANUAL', gv1, gv1),
     (prj, 'Spring Boot & REST API',
      'Khi xử lý ngoại lệ (Exception Handling) trong ứng dụng Spring Boot RESTful, tại sao nên sử dụng @RestControllerAdvice kết hợp chuẩn RFC 7807 (ProblemDetail) thay vì trả về generic error message 500?',
      '@RestControllerAdvice cho phép tập trung hóa việc bắt và xử lý exception toàn ứng dụng (Global Exception Handling) theo nguyên lý Clean Layered Architecture, tách biệt logic nghiệp vụ khỏi logic xử lý lỗi. Chuẩn RFC 7807 (ProblemDetail) cung cấp cấu trúc phản hồi lỗi chuẩn hóa quốc tế gồm type, title, status, detail, instance. Điều này giúp frontend hoặc client bên ngoài dễ dàng parse, hiển thị lỗi chính xác cho người dùng và hỗ trợ debug mà không làm rò rỉ stack trace hay thông tin nhạy cảm của hệ thống.',
      'EVALUATE', 'HARD', 'APPROVED', 'MANUAL', gv1, gv1),
     (prj, 'Spring Boot & REST API',
      'Hãy thiết kế kiến trúc phân tầng (Clean Layered Architecture) cho tính năng đặt phòng/lịch hẹn trong Spring Boot. Nêu rõ trách nhiệm của Controller, DTO, Service, Entity, Repository và cách quản lý Transaction.',
      'Kiến trúc gồm: 1) Controller: nhận HTTP request, validate DTO đầu vào (@Valid), trả về ResponseEntity<ApiResponse<DTO>>; 2) DTO: đóng gói dữ liệu request/response, độc lập với cấu trúc DB; 3) Service: chứa toàn bộ business logic, nghiệp vụ kiểm tra xung đột thời gian, trừ tồn kho, tính phí; đặt @Transactional để đảm bảo ACID; 4) Repository (Spring Data JPA): tương tác DB, định nghĩa phương thức truy vấn tối ưu; 5) Entity: ánh xạ bảng cơ sở dữ liệu. Quản lý Transaction bằng @Transactional ở Service layer, cấu hình rollbackFor = Exception.class, isolation level phù hợp để tránh race condition.',
      'CREATE', 'HARD', 'APPROVED', 'MANUAL', gv1, gv1);

    -- ===== DBI202 =====
    INSERT INTO questions (subject_id, topic, content, reference_answer, bloom_level, difficulty, status, source, created_by, reviewed_by)
    VALUES (dbi, 'Chuẩn hoá', 'Giải thích dạng chuẩn 3NF và cho ví dụ một bảng vi phạm 3NF.',
            'Không có phụ thuộc bắc cầu của thuộc tính không khoá vào khoá chính.',
            'UNDERSTAND', 'MEDIUM', 'APPROVED', 'MANUAL', gv2, gv2) RETURNING id INTO q;

    INSERT INTO questions (subject_id, topic, content, reference_answer, bloom_level, difficulty, status, source, created_by, reviewed_by)
    VALUES (dbi, 'Transaction', 'Trình bày 4 tính chất ACID của transaction.',
            'Atomicity, Consistency, Isolation, Durability.',
            'REMEMBER', 'EASY', 'APPROVED', 'MANUAL', gv2, gv2) RETURNING id INTO q;

    INSERT INTO questions (subject_id, topic, content, reference_answer, bloom_level, difficulty, status, source, created_by, reviewed_by) VALUES
    (dbi, 'Indexing & Query Optimization', 
     'Chỉ mục (Index) trong cơ sở dữ liệu hoạt động như thế nào? Khi nào nên đánh index và khi nào KHÔNG nên đánh index?', 
     'Index (thường là B-Tree) là cấu trúc dữ liệu phụ giúp định vị nhanh các dòng dữ liệu mà không cần quét toàn bộ bảng (Full Table Scan). Nên đánh index trên các cột khóa ngoại, cột thường dùng trong WHERE, JOIN, ORDER BY. Không nên đánh index trên bảng quá nhỏ, cột có cardinality thấp (boolean), hoặc bảng có tần suất ghi (INSERT/UPDATE/DELETE) cực lớn.', 
     'APPLY', 'MEDIUM', 'APPROVED', 'MANUAL', gv2, gv2),
    (dbi, 'Transaction & Concurrency', 
     'Phân biệt 3 hiện tượng xung đột dữ liệu: Dirty Read, Non-repeatable Read và Phantom Read. 4 mức cô lập Transaction (Isolation Levels) giải quyết các hiện tượng này ra sao?', 
     'Dirty Read: đọc dữ liệu chưa commit của transaction khác. Non-repeatable Read: trong cùng transaction đọc lại 1 dòng nhưng thấy dữ liệu bị update bởi transaction khác đã commit. Phantom Read: đọc lại query thì thấy xuất hiện thêm các dòng mới được insert. 4 mức cô lập: Read Uncommitted, Read Committed, Repeatable Read, Serializable.', 
     'ANALYZE', 'HARD', 'APPROVED', 'MANUAL', gv2, gv2),
    (dbi, 'Database Design & Normalization', 
     'Tại sao chúng ta phải chuẩn hóa cơ sở dữ liệu lên 3NF? Có trường hợp nào trong thực tế lại chủ động phi chuẩn hóa (Denormalization) không?', 
     'Chuẩn hóa lên 3NF giúp loại bỏ dư thừa dữ liệu (data redundancy), ngăn ngừa các bất thường khi thao tác (Insert, Update, Delete Anomaly) và đảm bảo tính nhất quán. Trong thực tế, chủ động phi chuẩn hóa ở các hệ thống Data Warehouse, báo cáo BI hoặc các bảng đọc nhiều ghi ít để giảm bớt số lượng JOIN phức tạp, tăng tốc độ truy vấn đọc dữ liệu.', 
     'UNDERSTAND', 'MEDIUM', 'APPROVED', 'MANUAL', gv2, gv2),
    (dbi, 'SQL & Procedural SQL', 
     'So sánh Stored Procedure, Function và Trigger trong hệ quản trị cơ sở dữ liệu quan hệ. Đánh giá ưu nhược điểm của việc đặt business logic trong Trigger.', 
     'Function trả về một giá trị duy nhất hoặc bảng, có thể gọi trong SELECT. Stored Procedure thực thi nhiều thao tác phức tạp, quản lý transaction. Trigger tự động kích hoạt khi có INSERT/UPDATE/DELETE. Nhược điểm khi đặt business logic vào Trigger: Khó debug, chạy ngầm khó theo dõi luồng ứng dụng, gây overhead lớn lên transaction.', 
     'EVALUATE', 'HARD', 'APPROVED', 'MANUAL', gv2, gv2),
    (dbi, 'Indexing & Query Optimization', 
     'Cho một câu truy vấn SELECT với nhiều điều kiện WHERE và JOIN chạy rất chậm trên bảng hàng triệu dòng. Hãy trình bày phương pháp từng bước để phân tích và tối ưu hóa câu truy vấn đó.', 
     'Bước 1: Sử dụng EXPLAIN ANALYZE để đọc Query Execution Plan, tìm node tốn nhiều cost/thời gian nhất; Bước 2: Kiểm tra Seq Scan để tạo B-Tree index hoặc Composite Index phù hợp; Bước 3: Tránh dùng hàm trên cột index trong WHERE; Bước 4: Chỉ SELECT các cột cần thiết, tránh SELECT *; Bước 5: Cân nhắc phân vùng (Table Partitioning) nếu bảng quá lớn.', 
     'CREATE', 'HARD', 'APPROVED', 'MANUAL', gv2, gv2);

    -- ===== SWP391 =====
    INSERT INTO questions (subject_id, topic, content, reference_answer, bloom_level, difficulty, status, source, created_by, reviewed_by) VALUES
    (swp, 'Quy trình Agile & Scrum', 
     'Trong mô hình Scrum, hãy nêu rõ 3 vai trò (Roles) chính và 4 sự kiện (Events/Ceremonies) cơ bản của một Sprint.', 
     '3 vai trò: Product Owner, Scrum Master, Development Team. 4 sự kiện: Sprint Planning, Daily Scrum, Sprint Review, Sprint Retrospective.', 
     'REMEMBER', 'EASY', 'APPROVED', 'MANUAL', gv1, gv1),
    (swp, 'Quy trình Agile & Scrum', 
     'Giải thích khái niệm Definition of Done (DoD) trong dự án phần mềm và phân biệt DoD với Acceptance Criteria của một User Story.', 
     'Acceptance Criteria là tiêu chí chấp nhận cụ thể cho từng User Story riêng lẻ do Product Owner đặt ra. Definition of Done (DoD) là bộ tiêu chuẩn kỹ thuật chung áp dụng cho mọi User Story trước khi được coi là hoàn tất.', 
     'UNDERSTAND', 'MEDIUM', 'APPROVED', 'MANUAL', gv1, gv1),
    (swp, 'Git Workflow & Quản lý mã nguồn', 
     'Hãy trình bày quy trình Git Feature Branch Workflow chuẩn khi làm việc nhóm trong môn SWP. Mô tả các bước từ khi nhận task đến khi merge vào nhánh develop/main.', 
     '1) Pull code mới; 2) Tạo branch feat/ten-tinh-nang; 3) Commit chuẩn Conventional; 4) Push branch; 5) Tạo Pull Request; 6) Code review; 7) Merge và xóa branch.', 
     'APPLY', 'MEDIUM', 'APPROVED', 'MANUAL', gv1, gv1),
    (swp, 'Kiến trúc & Thiết kế hệ thống', 
     'Tại sao trong các dự án web hiện đại nhóm phát triển nên tách biệt Frontend (SPA như React) và Backend (REST API như Spring Boot) thay vì làm Monolith server-side rendering truyền thống? Phân tích ưu và nhược điểm.', 
     'Ưu điểm: Tách biệt trách nhiệm, phát triển song song, tái sử dụng API đa nền tảng, UX mượt mà. Nhược điểm: Phức tạp trong cấu hình CORS, JWT stateless, quản lý state và SEO.', 
     'ANALYZE', 'HARD', 'APPROVED', 'MANUAL', gv1, gv1),
    (swp, 'Kiểm thử & CI/CD', 
     'Trong dự án phần mềm, tại sao cần duy trì quy trình Code Review và tự động hóa kiểm thử (CI pipeline) trước khi merge code? Nêu hậu quả nếu bỏ qua bước này.', 
     'Code Review giúp phát hiện sớm bug, bảo mật và chia sẻ kiến thức. CI tự động build/test bảo đảm không break hệ thống.', 
     'EVALUATE', 'HARD', 'APPROVED', 'MANUAL', gv1, gv1),
    (swp, 'Kiến trúc & Thiết kế hệ thống', 
     'Hãy đề xuất kiến trúc xử lý khi hệ thống gặp lỗi hoặc quá tải trong đợt đăng ký môn học hoặc thi online có lưu lượng truy cập đột biến (Spike Traffic).', 
     'Giải pháp đa tầng: Caching (Redis), Asynchronous Queue (RabbitMQ/Kafka), Rate Limiting tại Gateway, Connection Pooling & Read Replicas.', 
     'CREATE', 'HARD', 'APPROVED', 'MANUAL', gv1, gv1);

    -- Bộ rubric 10 điểm dùng chung cho mỗi môn học
    INSERT INTO rubrics (subject_id, criterion, keywords, max_score, sort_order) VALUES
     (prj, 'Giải thích đúng kiến thức Java Web và cơ chế hoạt động', 'servlet, jsp, session, mvc', 4, 1),
     (prj, 'Phân tích và áp dụng vào tình huống phù hợp', 'ví dụ, ứng dụng, phân tích', 4, 2),
     (prj, 'Trình bày lập luận rõ ràng', 'vì, do, kết luận', 2, 3),
     (dbi, 'Giải thích đúng khái niệm cơ sở dữ liệu', 'khóa, chuẩn hóa, transaction, acid', 4, 1),
     (dbi, 'Phân tích ví dụ và giải pháp', 'ví dụ, tách bảng, nhất quán', 4, 2),
     (dbi, 'Trình bày lập luận rõ ràng', 'vì, do, kết luận', 2, 3),
     (swp, 'Vận dụng quy trình Agile/Scrum & Quản lý mã nguồn với Git', 'agile, scrum, sprint, user story, git, branch, pull request, merge', 4, 1),
     (swp, 'Thiết kế kiến trúc hệ thống và Clean Layered Architecture', 'kiến trúc, layered architecture, controller, service, repository, dto, database', 4, 2),
     (swp, 'Kỹ năng phản biện và phân tích rủi ro kỹ thuật', 'lập luận, giải pháp, tối ưu, rủi ro, bảo mật, kiểm thử', 2, 3);
END $$;
