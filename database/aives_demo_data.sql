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

INSERT INTO users (username, password_hash, full_name, email, role, student_code) VALUES
 ('admin',     '65536:kfTNw/rdoXXxzRGQ0Y0Z2g==:gbVuZTCHO759D4Ro6V/opitW/x27fUOAIQ2MGhrAa24=', 'Quản trị hệ thống', 'admin@aives.edu.vn',     'ADMIN',    NULL),
 ('lecturer1', '65536:svZ+c3lZ4+XK6sWTdHNtdQ==:0svGRpt9mLJNAthHviv1HgZn30rF5ANNYsens3GXX4E=', 'Nguyễn Văn Minh',   'minhnv@aives.edu.vn',    'LECTURER', NULL),
 ('lecturer2', '65536:svZ+c3lZ4+XK6sWTdHNtdQ==:0svGRpt9mLJNAthHviv1HgZn30rF5ANNYsens3GXX4E=', 'Trần Thị Lan',      'lantt@aives.edu.vn',     'LECTURER', NULL),
 ('student1',  '65536:svZ+c3lZ4+XK6sWTdHNtdQ==:0svGRpt9mLJNAthHviv1HgZn30rF5ANNYsens3GXX4E=', 'Lê Hoàng An',       'anlh@student.edu.vn',    'STUDENT',  'SE170001'),
 ('student2',  '65536:svZ+c3lZ4+XK6sWTdHNtdQ==:0svGRpt9mLJNAthHviv1HgZn30rF5ANNYsens3GXX4E=', 'Phạm Thu Hà',       'hapt@student.edu.vn',    'STUDENT',  'SE170002'),
 ('student3',  '65536:svZ+c3lZ4+XK6sWTdHNtdQ==:0svGRpt9mLJNAthHviv1HgZn30rF5ANNYsens3GXX4E=', 'Đoàn Minh Khoa',    'khoadm@student.edu.vn',  'STUDENT',  'SE170003');

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

    -- ===== PRJ301: AI sinh, chờ duyệt =====
    INSERT INTO questions (subject_id, topic, content, bloom_level, status, source, created_by) VALUES
     (prj, 'Filter', 'Hãy nêu định nghĩa của Filter trong Java Web.', 'REMEMBER', 'PENDING_REVIEW', 'AI', gv1),
     (prj, 'Filter', 'Cho một ví dụ thực tế áp dụng Filter và mô tả cách bạn triển khai.', 'APPLY', 'PENDING_REVIEW', 'AI', gv1),
     (prj, 'Filter', 'Phân tích ưu điểm và nhược điểm của Filter.', 'ANALYZE', 'PENDING_REVIEW', 'AI', gv1);

    -- ===== PRJ301: nháp =====
    INSERT INTO questions (subject_id, topic, content, bloom_level, status, source, created_by) VALUES
     (prj, 'Chương 6 - JDBC', 'Thiết kế lớp DAO cho bảng sinh viên, giải thích vì sao nên dùng PreparedStatement.', 'CREATE', 'DRAFT', 'MANUAL', gv1);

    -- ===== DBI202 =====
    INSERT INTO questions (subject_id, topic, content, reference_answer, bloom_level, status, source, created_by, reviewed_by)
    VALUES (dbi, 'Chuẩn hoá', 'Giải thích dạng chuẩn 3NF và cho ví dụ một bảng vi phạm 3NF.',
            'Không có phụ thuộc bắc cầu của thuộc tính không khoá vào khoá chính.',
            'UNDERSTAND', 'APPROVED', 'MANUAL', gv2, gv2) RETURNING id INTO q;

    INSERT INTO questions (subject_id, topic, content, reference_answer, bloom_level, status, source, created_by, reviewed_by)
    VALUES (dbi, 'Transaction', 'Trình bày 4 tính chất ACID của transaction.',
            'Atomicity, Consistency, Isolation, Durability.',
            'REMEMBER', 'APPROVED', 'MANUAL', gv2, gv2) RETURNING id INTO q;
    -- Một bộ rubric 10 điểm dùng chung cho mỗi môn, không còn theo từng câu.
    INSERT INTO rubrics (subject_id, criterion, keywords, max_score, sort_order) VALUES
     (prj, 'Giải thích đúng kiến thức Java Web và cơ chế hoạt động', 'servlet, jsp, session, mvc', 4, 1),
     (prj, 'Phân tích và áp dụng vào tình huống phù hợp', 'ví dụ, ứng dụng, phân tích', 4, 2),
     (prj, 'Trình bày lập luận rõ ràng', 'vì, do, kết luận', 2, 3),
     (dbi, 'Giải thích đúng khái niệm cơ sở dữ liệu', 'khóa, chuẩn hóa, transaction, acid', 4, 1),
     (dbi, 'Phân tích ví dụ và giải pháp', 'ví dụ, tách bảng, nhất quán', 4, 2),
     (dbi, 'Trình bày lập luận rõ ràng', 'vì, do, kết luận', 2, 3);
END $$;
