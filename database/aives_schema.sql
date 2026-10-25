-- ============================================================
-- AIVES (AI Viva Examination System) - Database Schema DDL
-- Target DBMS: PostgreSQL 15+
-- ============================================================

CREATE TABLE roles (
    id          INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(20)  NOT NULL UNIQUE, -- 'ADMIN', 'LECTURER', 'STUDENT'
    description VARCHAR(200)
);

CREATE TABLE users (
    id            INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    role_id       INT          NOT NULL REFERENCES roles(id),
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(100) NOT NULL UNIQUE,
    student_code  VARCHAR(20),                -- Chỉ có giá trị đối với sinh viên
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX ix_users_role ON users(role_id);
CREATE INDEX ix_users_student_code ON users(student_code);

CREATE TABLE subjects (
    id          INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code        VARCHAR(20)  NOT NULL UNIQUE,
    name        VARCHAR(150) NOT NULL,
    description TEXT,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Phân công giảng viên phụ trách môn học (N-N)
CREATE TABLE lecturer_subjects (
    lecturer_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject_id  INT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (lecturer_id, subject_id)
);

CREATE TABLE system_configs (
    config_key   VARCHAR(50)  PRIMARY KEY,
    config_value VARCHAR(255) NOT NULL,
    description  VARCHAR(255)
);

-- ---------------- Feature 1: Ngân hàng câu hỏi & Rubric ----------------
CREATE TABLE questions (
    id            INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subject_id    INT          NOT NULL REFERENCES subjects(id),
    created_by_id INT          NOT NULL REFERENCES users(id),
    content       TEXT         NOT NULL,
    sample_answer TEXT,
    bloom_level   VARCHAR(20)  NOT NULL CHECK (bloom_level IN ('REMEMBER', 'UNDERSTAND', 'APPLY', 'ANALYZE')),
    difficulty    VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM' CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    source        VARCHAR(20)  NOT NULL DEFAULT 'MANUAL' CHECK (source IN ('MANUAL', 'AI_GENERATED', 'EXCEL_IMPORT')),
    status        VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'
                    CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'APPROVED', 'REJECTED')),
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP
);
CREATE INDEX ix_questions_subject_status ON questions(subject_id, status);

-- Tiêu chí chấm điểm (Rubric) gắn theo môn học
CREATE TABLE rubrics (
    id           INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subject_id   INT           NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    criterion    VARCHAR(500)  NOT NULL,
    keywords     VARCHAR(500), -- Danh sách từ khóa bắt buộc/gợi ý, ngăn cách bởi dấu phẩy
    max_score    NUMERIC(5,2)  NOT NULL CHECK (max_score > 0),
    sort_order   INT           NOT NULL DEFAULT 0
);

-- ---------------- Feature 3: Phỏng vấn AI & Chấm điểm ----------------
CREATE TABLE interview_sessions (
    id                     INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    student_id             INT          NOT NULL REFERENCES users(id),
    subject_id             INT          NOT NULL REFERENCES subjects(id),
    status                 VARCHAR(20)  NOT NULL DEFAULT 'IN_PROGRESS'
                             CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'ABORTED')),
    answer_time_limit_sec  INT          NOT NULL,   -- snapshot cấu hình tại thời điểm thi
    max_followups          INT          NOT NULL,
    started_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at               TIMESTAMP,
    ai_suggested_score     NUMERIC(5,2),            -- Điểm tổng do AI đề xuất
    ai_feedback            TEXT,                    -- Đánh giá tổng quan của AI
    ai_grading_source      VARCHAR(20),             -- GEMINI | RULE_BASED | NO_RUBRIC (NULL = AI đang chấm)
    final_score            NUMERIC(5,2),            -- Điểm chốt chính thức của GV
    lecturer_feedback      TEXT,                    -- Nhận xét của giảng viên
    graded_by              INT          REFERENCES users(id) ON DELETE SET NULL,
    graded_at              TIMESTAMP
);

-- Bảng điểm chi tiết theo từng tiêu chí Rubric
CREATE TABLE session_rubric_scores (
    id          INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    session_id  INT          NOT NULL REFERENCES interview_sessions(id) ON DELETE CASCADE,
    rubric_id   INT          NOT NULL REFERENCES rubrics(id) ON DELETE CASCADE,
    ai_score    NUMERIC(5,2),
    final_score NUMERIC(5,2),
    comment     TEXT
);
CREATE INDEX ix_session_rubric_scores_session ON session_rubric_scores(session_id);

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
