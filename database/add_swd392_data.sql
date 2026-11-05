-- =============================================================
-- AIVES - Bổ sung dữ liệu môn SWD392 (Software Architecture and Design)
-- Bao gồm: Môn học, phân công giảng viên, câu hỏi đầy đủ Bloom levels,
-- đáp án chuẩn tham chiếu và bộ tiêu chí Rubric 10 điểm.
-- =============================================================

SET client_encoding = 'UTF8';

-- 1. Thêm môn học SWD392 nếu chưa tồn tại
INSERT INTO subjects (code, name, description)
VALUES ('SWD392', 'Software Architecture and Design', 'Kiến trúc và Thiết kế phần mềm, Clean Architecture, Design Patterns, NFRs, Microservices')
ON CONFLICT (code) DO UPDATE 
SET name = EXCLUDED.name, description = EXCLUDED.description;

-- 2. Phân công giảng viên cho môn SWD392 (lecturer1 & lecturer2)
INSERT INTO lecturer_subjects (lecturer_id, subject_id)
SELECT u.id, s.id
FROM users u, subjects s
WHERE u.username IN ('lecturer1', 'lecturer2')
  AND s.code = 'SWD392'
ON CONFLICT DO NOTHING;

-- 3. Tạo Rubrics và Câu hỏi cho SWD392
DO $$
DECLARE
    swd INT := (SELECT id FROM subjects WHERE code = 'SWD392');
    gv1 INT := (SELECT id FROM users WHERE username = 'lecturer1');
    gv2 INT := (SELECT id FROM users WHERE username = 'lecturer2');
BEGIN
    -- Xóa rubric cũ của SWD392 nếu có để tạo mới chuẩn 10 điểm
    DELETE FROM rubrics WHERE subject_id = swd;

    -- Bộ tiêu chí Rubric 10 điểm chuẩn cho SWD392
    INSERT INTO rubrics (subject_id, criterion, keywords, max_score, sort_order) VALUES
     (swd, 'Nắm vững và giải thích chính xác các phong cách kiến trúc (Layered, Microservices, Event-Driven) và Design Patterns', 
           'kiến trúc, pattern, layered, microservices, clean architecture, coupling, cohesion, factory, singleton, observer, strategy', 4.00, 1),
     (swd, 'Phân tích tình huống, thiết kế giải pháp và bảo đảm các yêu cầu phi chức năng (NFRs: Performance, Scalability, Security, Maintainability)', 
           'hiệu năng, bảo mật, mở rộng, scalability, latency, trade-off, caching, phân tán, chịu lỗi, nfr', 4.00, 2),
     (swd, 'Kỹ năng lập luận logic, phản biện thiết kế và bảo vệ phương án kiến trúc rõ ràng', 
           'vì, do đó, ưu điểm, nhược điểm, kết luận, phân tích, đề xuất, so sánh', 2.00, 3);

    -- Xóa các câu hỏi cũ của SWD392 trước khi nạp bộ câu hỏi chuẩn
    DELETE FROM questions WHERE subject_id = swd;

    -- Câu hỏi 1: REMEMBER (EASY) - Thuộc tính chất lượng phần mềm (NFRs)
    INSERT INTO questions (subject_id, topic, content, reference_answer, bloom_level, difficulty, status, source, created_by, reviewed_by)
    VALUES (swd, 'Quality Attributes & NFRs',
            'Trong kiến trúc phần mềm, hãy nêu định nghĩa về Yêu cầu phi chức năng (NFRs) và liệt kê 4 thuộc tính chất lượng (Quality Attributes) quan trọng nhất của một hệ thống web hiện đại.',
            'Yêu cầu phi chức năng (NFRs - Non-Functional Requirements) xác định các tiêu chí chất lượng và ràng buộc hoạt động của hệ thống thay vì các hành vi cụ thể (chức năng). 4 thuộc tính chất lượng phổ biến gồm: 1) Performance (Hiệu năng/Thời gian đáp ứng); 2) Scalability (Khả năng mở rộng tải); 3) Availability/Reliability (Tính sẵn sàng và độ tin cậy); 4) Security (Tính an toàn và bảo mật dữ liệu).',
            'REMEMBER', 'EASY', 'APPROVED', 'MANUAL', gv1, gv1);

    -- Câu hỏi 2: UNDERSTAND (MEDIUM) - Khái niệm Coupling và Cohesion
    INSERT INTO questions (subject_id, topic, content, reference_answer, bloom_level, difficulty, status, source, created_by, reviewed_by)
    VALUES (swd, 'Coupling & Cohesion',
            'Hãy giải thích nguyên lý "High Cohesion, Low Coupling" trong thiết kế phần mềm. Cho một ví dụ cụ thể minh họa một module vi phạm nguyên lý này.',
            'Cohesion (Tính gắn kết) thể hiện mức độ liên quan chặt chẽ của các trách nhiệm bên trong một module. Coupling (Độ phụ thuộc) thể hiện mức độ phụ thuộc qua lại giữa các module khác nhau. "High Cohesion, Low Coupling" giúp module độc lập, dễ bảo trì, dễ kiểm thử và tái sử dụng. Ví dụ vi phạm: Một class OrderService vừa xử lý logic thanh toán, vừa tạo file PDF hóa đơn, vừa gửi email trực tiếp qua SMTP và viết câu lệnh raw SQL vào CSDL - vừa Low Cohesion vừa High Coupling với hạ tầng.',
            'UNDERSTAND', 'MEDIUM', 'APPROVED', 'MANUAL', gv1, gv1);

    -- Câu hỏi 3: APPLY (MEDIUM) - Áp dụng Design Pattern
    INSERT INTO questions (subject_id, topic, content, reference_answer, bloom_level, difficulty, status, source, created_by, reviewed_by)
    VALUES (swd, 'Design Patterns',
            'Khi hệ thống cần hỗ trợ nhiều phương thức thanh toán khác nhau (VNPAY, MoMo, PayPal, Thẻ tín dụng) và cho phép người dùng chọn tại thời điểm thanh toán, bạn nên áp dụng Design Pattern nào? Hãy trình bày cách thiết kế lớp và giao diện.',
            'Nên áp dụng Strategy Pattern. Thiết kế: 1) Interface chung PaymentStrategy với phương thức pay(amount); 2) Các Concrete Strategy triển khai cụ thể: VnPayStrategy, MoMoStrategy, PayPalStrategy; 3) Lớp Context PaymentContext hoặc PaymentService giữ một tham chiếu đến PaymentStrategy và gọi strategy.pay(). Có thể kết hợp Factory Pattern để khởi tạo Strategy tương ứng dựa trên lựa chọn của người dùng, tuân thủ nguyên lý Open/Closed Principle (OCP).',
            'APPLY', 'MEDIUM', 'APPROVED', 'MANUAL', gv1, gv1);

    -- Câu hỏi 4: ANALYZE (HARD) - Phân tích Clean Architecture & Dependency Inversion
    INSERT INTO questions (subject_id, topic, content, reference_answer, bloom_level, difficulty, status, source, created_by, reviewed_by)
    VALUES (swd, 'Clean Architecture',
            'Phân tích quy tắc Dependency Rule trong Clean Architecture (hoặc Onion/Hexagonal Architecture). Tại sao tầng nghiệp vụ cốt lõi (Domain/Core) không được phụ thuộc trực tiếp vào tầng cơ sở dữ liệu (Database/Frameworks)?',
            'Dependency Rule quy định: Các phụ thuộc mã nguồn chỉ được phép trỏ từ ngoài vào trong. Tầng Domain Core nằm ở trung tâm và không phụ thuộc vào bất kỳ framework, UI hay Database nào. Nhờ áp dụng Dependency Inversion Principle (DIP), tầng Core định nghĩa các Interface Repository, và tầng Infrastructure bên ngoài sẽ implements các Interface đó. Điều này giúp hệ thống độc lập công nghệ, có thể đổi CSDL từ MySQL sang PostgreSQL hoặc Mock DB để kiểm thử đơn vị một cách dễ dàng mà không làm sửa đổi logic nghiệp vụ.',
            'ANALYZE', 'HARD', 'APPROVED', 'MANUAL', gv1, gv1);

    -- Câu hỏi 5: EVALUATE (HARD) - Đánh giá Trade-off giữa Monolith và Microservices
    INSERT INTO questions (subject_id, topic, content, reference_answer, bloom_level, difficulty, status, source, created_by, reviewed_by)
    VALUES (swd, 'Architectural Styles Trade-offs',
            'Hãy đánh giá các yếu tố đánh đổi (Trade-offs) giữa kiến trúc Monolithic và Microservices. Trong trường hợp một startup xây dựng sản phẩm MVP với nhóm 4 lập trình viên, bạn khuyến nghị lựa chọn phong cách nào? Vì sao?',
            'Trade-offs: Monolith dễ phát triển, kiểm thử, triển khai ban đầu và đơn giản trong quản lý transaction ACID; nhưng khó mở rộng độc lập khi hệ thống lớn. Microservices cho phép scale độc lập từng service, triển khai CI/CD linh hoạt và chọn công nghệ đa dạng; nhưng phát sinh chi phí vận hành mạng, phân tán dữ liệu, transaction phân tán (Saga) và độ phức tạp cao trong giám sát/logging. Với startup 4 người làm MVP, khuyến nghị chọn Modular Monolith hoặc Clean Layered Architecture để tối ưu tốc độ phát triển (Time-to-market), tránh gánh nặng DevOps và độ trễ mạng ban đầu.',
            'EVALUATE', 'HARD', 'APPROVED', 'MANUAL', gv1, gv1);

    -- Câu hỏi 6: CREATE (HARD) - Thiết kế kiến trúc chịu tải cho hệ thống thi vấn đáp AIVES
    INSERT INTO questions (subject_id, topic, content, reference_answer, bloom_level, difficulty, status, source, created_by, reviewed_by)
    VALUES (swd, 'System Architecture Design',
            'Hãy đề xuất giải pháp thiết kế kiến trúc tổng thể cho hệ thống thi vấn đáp trực tuyến AIVES khi số lượng thí sinh đồng thời (Concurrent Users) tăng đột biến lên 5.000 sinh viên. Hãy mô tả giải pháp từ tầng Client, Gateway, Caching, Xử lý AI đến Cơ sở dữ liệu.',
            'Đề xuất kiến trúc đa tầng chịu tải cao: 1) Client: Single Page Application (React) nén build tĩnh qua CDN; 2) API Gateway / Reverse Proxy: Nginx cân bằng tải (Round Robin) kèm Rate Limiting chống DDoS; 3) Application Tier: Chạy cụm Spring Boot instances stateless; 4) Caching Tier: Redis lưu trữ cache session phòng thi và token JWT; 5) Asynchronous Queue: Dùng Kafka hoặc RabbitMQ làm hàng đợi cho tác vụ nặng như trích xuất văn bản tài liệu và phân tích âm thanh, tránh nghẽn thread HTTP; 6) Database Tier: PostgreSQL với mô hình Primary-Replica (Read-Write Splitting), đánh Index B-Tree chuẩn trên các trường session và turn; 7) AI Integration: Cơ chế Circuit Breaker (Resilience4j) với Fallback Model để tự động dự phòng khi Gemini API bị nghẽn.',
            'CREATE', 'HARD', 'APPROVED', 'MANUAL', gv1, gv1);

END $$;