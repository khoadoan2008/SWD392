-- =============================================================
-- AIVES - AI Interactive Viva Examination System
-- Schema cho Feature 1 (Ngân hàng câu hỏi & rubric),
--            Feature 3 (Phỏng vấn AI),
--            Feature 7 (Quản trị hệ thống)
-- DBMS: PostgreSQL (>= 12)
--
-- Cách chạy:
--   createdb -U postgres aives
--   psql -U postgres -d aives -f database/aives_schema.sql
-- Tài khoản demo được tạo tự động khi app khởi động (AppInitListener)
-- =============================================================

-- Đảm bảo psql đọc file dạng UTF-8 (tránh lỗi font tiếng Việt trên Windows)
SET client_encoding = 'UTF8';

DROP TABLE IF EXISTS interview_turns, interview_sessions, rubrics, questions,
                     lecturer_subjects, subjects, system_configs, users CASCADE;

-- ---------------- Feature 7: Quản trị hệ thống ----------------
CREATE TABLE users (
    id             INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username       VARCHAR(50)  NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,           -- PBKDF2: iterations:salt:hash
    full_name      VARCHAR(100) NOT NULL,
    email          VARCHAR(100),
    role           VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'LECTURER', 'STUDENT')),
    student_code   VARCHAR(20),                     -- MSSV, chỉ dùng cho STUDENT
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE subjects (
    id           INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code         VARCHAR(20)  NOT NULL UNIQUE,
    name         VARCHAR(200) NOT NULL,
    description  TEXT,
    active       BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Phân quyền giảng viên <-> môn học
CREATE TABLE lecturer_subjects (
    lecturer_id  INT NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
    subject_id   INT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    PRIMARY KEY (lecturer_id, subject_id)
);

-- Cấu hình hệ thống dạng key/value (ngôn ngữ STT/TTS, giới hạn mặc định...)
CREATE TABLE system_configs (
    config_key    VARCHAR(50)  PRIMARY KEY,
    config_value  VARCHAR(255) NOT NULL,
    description   VARCHAR(255)
);

-- ---------------- Feature 1: Ngân hàng câu hỏi & rubric ----------------
CREATE TABLE questions (
    id                INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subject_id        INT          NOT NULL REFERENCES subjects(id),
    topic             VARCHAR(200),                 -- chương / chủ đề
    content           TEXT         NOT NULL,
    reference_answer  TEXT,                         -- đáp án tham khảo
    bloom_level       VARCHAR(20)  NOT NULL CHECK (bloom_level IN
                        ('REMEMBER', 'UNDERSTAND', 'APPLY', 'ANALYZE', 'EVALUATE', 'CREATE')),
    status            VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' CHECK (status IN
                        ('DRAFT', 'PENDING_REVIEW', 'APPROVED', 'REJECTED')),
    source            VARCHAR(10)  NOT NULL DEFAULT 'MANUAL' CHECK (source IN ('MANUAL', 'IMPORT', 'AI')),
    created_by        INT          NOT NULL REFERENCES users(id),
    reviewed_by       INT          REFERENCES users(id),
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX ix_questions_subject_status ON questions(subject_id, status);

-- Rubric: tiêu chí + thang điểm cho từng câu hỏi
CREATE TABLE rubrics (
    id           INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    question_id  INT           NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    criterion    VARCHAR(500)  NOT NULL,
    keywords     VARCHAR(500),                      -- từ khoá, phân tách bằng dấu phẩy (AI dùng để hỏi xoáy)
    max_score    NUMERIC(5,2)  NOT NULL CHECK (max_score > 0),
    sort_order   INT           NOT NULL DEFAULT 0
);

-- ---------------- Feature 3: Phỏng vấn AI ----------------
CREATE TABLE interview_sessions (
    id                     INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    student_id             INT         NOT NULL REFERENCES users(id),
    subject_id             INT         NOT NULL REFERENCES subjects(id),
    status                 VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS'
                             CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'ABORTED')),
    answer_time_limit_sec  INT         NOT NULL,   -- snapshot cấu hình tại thời điểm thi
    max_followups          INT         NOT NULL,
    started_at             TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at               TIMESTAMP
);

-- Mỗi lượt hỏi-đáp. Câu chính: followup_index = 0; câu hỏi xoáy: 1..max_followups
-- Thứ tự hỏi: ORDER BY main_index, followup_index
CREATE TABLE interview_turns (
    id                 INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    session_id         INT          NOT NULL REFERENCES interview_sessions(id) ON DELETE CASCADE,
    question_id        INT          REFERENCES questions(id) ON DELETE SET NULL,
    parent_turn_id     INT          REFERENCES interview_turns(id) ON DELETE CASCADE,
    turn_type          VARCHAR(10)  NOT NULL CHECK (turn_type IN ('MAIN', 'FOLLOW_UP')),
    main_index         INT          NOT NULL,
    followup_index     INT          NOT NULL DEFAULT 0,
    question_text      TEXT         NOT NULL,      -- snapshot nội dung câu hỏi
    transcript         TEXT,                       -- văn bản STT câu trả lời
    response_time_sec  INT,
    asked_at           TIMESTAMP,
    answered_at        TIMESTAMP
);
CREATE INDEX ix_turns_session ON interview_turns(session_id, main_index, followup_index);

-- ---------------- Seed ----------------
INSERT INTO system_configs (config_key, config_value, description) VALUES
 ('speech.language',         'vi-VN', 'Ngôn ngữ STT/TTS (vi-VN hoặc en-US)'),
 ('interview.answer_time',   '120',   'Thời gian trả lời mặc định mỗi câu (giây)'),
 ('interview.max_followups', '2',     'Số câu hỏi xoáy tối đa cho mỗi câu chính'),
 ('interview.main_questions','3',     'Số câu hỏi chính mỗi lượt phỏng vấn');

INSERT INTO subjects (code, name, description) VALUES
 ('PRJ301', 'Java Web Application Development', 'Servlet, JSP, MVC'),
 ('DBI202', 'Database Systems', 'Cơ sở dữ liệu quan hệ, SQL');
