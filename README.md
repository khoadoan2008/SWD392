# AIVES – AI Adaptive Oral Examination

Hệ thống vấn đáp thông minh có hỗ trợ AI (SWD392). Bản này gồm **Feature 1** (ngân hàng câu hỏi & rubric),
**Feature 3** (phỏng vấn AI với hỏi xoáy) và **Feature 7** (quản trị hệ thống, đăng nhập / phân quyền).

**Công nghệ:** Java 8 · Servlet 4 / JSP + JSTL · PostgreSQL · Tomcat 9 · Maven

## Chạy nhanh

1. Tạo database và nạp dữ liệu:
   ```bash
   createdb -U postgres -E UTF8 -T template0 aives
   psql -U postgres -d aives -f database/aives_schema.sql
   psql -U postgres -d aives -f database/aives_demo_data.sql
   ```
2. Sửa mật khẩu PostgreSQL trong `src/main/resources/db.properties`.
3. `mvn package` → deploy `target/aives_system.war` lên **Tomcat 9** (không dùng Tomcat 10+).
4. Mở `http://localhost:8080/aives_system` — tài khoản demo: `admin`/`admin123`, `lecturer1`, `student1`… / `123456`.

## Tài liệu

| File | Nội dung |
|---|---|
| [docs/CRUD_DESIGN.md](docs/CRUD_DESIGN.md) | Thiết kế CRUD, phân quyền, luồng hỏi xoáy |
| [docs/DATABASE_DESIGN.md](docs/DATABASE_DESIGN.md) | ERD, từ điển dữ liệu PostgreSQL |
| [docs/UI_UX_DESIGN.md](docs/UI_UX_DESIGN.md) | Design system, bản đồ màn hình |
| [docs/TEST_GUIDE.md](docs/TEST_GUIDE.md) | Hướng dẫn test từng bước |
| [docs/TEST_REPORT.md](docs/TEST_REPORT.md) | Kết quả kiểm thử |

## Cấu trúc

```
src/main/java/com/aives/
  controller/   auth · admin · lecturer · student   (Servlet)
  dao/          truy vấn JDBC
  model/        entity + enum
  service/      InterviewService, FollowUpGenerator, QuestionGenerator (điểm gắn AI thật)
  filter/       EncodingFilter, AuthFilter (phân quyền)
src/main/webapp/WEB-INF/views/   JSP theo vai trò
database/       schema + dữ liệu demo
```
