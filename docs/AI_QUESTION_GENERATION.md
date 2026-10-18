# Material-grounded Gemini question generation on the updated team project

## Integration base

Branch feature/be-ai-question-generation includes the leader's main commit 7b32f66 (2026-10-18),
including Swagger UI, the Maven wrapper and the latest PostgreSQL settings. It retains the leader's
router, dashboard, admin/student pages, question list/edit/review screens, JPA entities and repositories.

## Database decision

This integration keeps the leader's PostgreSQL driver, connection defaults, entity mappings and
database scripts, including ddl-auto=update and open-in-view=false from main. The team must agree
separately if its final database is to change to SQL Server. No new database script is required.

IMPORTANT: the existing database/aives_schema.sql contains destructive DROP statements. Do not
run it again against an existing database containing data. Use a separate disposable database for setup.

## Configuration and run

1. Copy config.example.properties to config.local.properties at the repository root if missing.
   Paste your Gemini key after GEMINI_API_KEY=;
   no quotes. Set GEMINI_MODEL if needed (default gemini-3.8-flash). Restart the backend after edits.
2. The application reads local configuration from either the repository root or backend directory.
   API keys never go in React/VITE_ settings, Java source, screenshots, chat, commits or PRs.
3. PostgreSQL is still configured by the team's application.yml. Set SPRING_DATASOURCE_URL,
   SPRING_DATASOURCE_USERNAME and SPRING_DATASOURCE_PASSWORD on your own machine if needed.
   Main also supports DB_URL / DB_USERNAME / DB_PASSWORD. Check any existing local configuration
   before running: these now override PostgreSQL defaults. Remove obsolete SQL Server settings.
4. Use Java 21 and Maven: mvn -f backend/pom.xml spring-boot:run.
5. In another terminal: npm --prefix frontend ci, then npm --prefix frontend run dev -- --host 127.0.0.1.
6. Open http://127.0.0.1:5173, log in with an existing assigned lecturer account, and open the existing
   question generation page at /lecturer/questions/generate. Existing PostgreSQL seed account passwords
   are those in the team's seed script, NOT the Demo@123 accounts of the previous MSSQL demo.

Do not commit machine-specific database passwords. No new SQL login, SA change or firewall change
is needed for this integration. Missing/invalid Gemini key returns an error, never fake success.

## Workflow and API

Assigned lecturer -> subject/topic/Bloom selection -> PDF/DOCX/TXT upload -> explicit Google transfer
consent -> local text extraction -> bounded material context -> real Gemini generateContent request
-> strict local JSON/business validation -> one database transaction saving questions and rubrics
as AI / PENDING_REVIEW -> existing question review page and status endpoint.

The existing QuestionGenerator/TemplateQuestionGenerator contract is retained for the old JSON
endpoint. The new MaterialQuestionGenerator contract requires a GenerationRequest containing course
text; LlmQuestionGenerator implements it. Distinct contracts avoid ambiguous Spring beans and ensure
the old template path cannot accidentally trigger a paid request. The UI now calls only the material path.

| Method | Route | Contract |
| --- | --- | --- |
| GET | /api/lecturer/questions/generation-subjects | Fresh active assigned subjects; admin sees all active subjects |
| POST | /api/lecturer/questions/generate | multipart/form-data; subjectId, topic, repeated levels, materialFile, allowAiTransfer=true; UUID Idempotency-Key header |
| POST | /api/lecturer/questions/generate | application/json; legacy template path retained, response clearly says no AI call |
| PATCH | /api/lecturer/questions/{id}/status?to=APPROVED | Existing leader route; AI rubric must total 10 before approval |

No App.jsx/router replacement or alternate duplicate Question/Subject/Rubric entity was added.
The provider call is outside the database transaction. A separate DraftPersistenceService saves the
whole batch with cascaded rubric rows atomically. Access is checked both before calling Gemini and
again before saving, using current database assignments rather than detached JWT collections.

JSON validation: exact shape/count/Bloom distribution; nonempty unique content and suggested answer;
1-10 criteria/question; positive scores with at most two decimals and exact sum 10; keyword limits.
Provider refusals, incomplete candidates, non-JSON content and overlong responses are rejected.
Errors do not expose provider response bodies, credentials or course contents. No automatic retries.

Limits: file <=10 MiB; PDF <=100 pages; extracted text <=100,000 characters; DOCX decompression
bounded to 20 MiB/500 entries; API 1-12 questions/request; current UI selects one question per selected
Bloom level (1-6 questions). Context <=12,000 characters selected lexically, not embedding/vector RAG.
Scanned PDFs need external OCR. Files are not stored in a permanent material library. No per-question
citations are saved. Teachers must review AI accuracy and own the rights to uploaded material.

Duplicate/in-flight requests: one active request/user, max four system-wide; UUID retained 10 minutes
in process memory. This is not a persistent cross-server exactly-once guarantee. If the browser times
out, check the question list before making another intentional request. Google quota/charges may apply.

## Verification

Final local checks on 2026-10-18 after syncing main: 70 Maven tests passed, zero failures/errors
(20 security/controller/JPA integration tests plus 50 unit tests). React production build succeeded.
The executable JAR was packaged from an isolated copy of the same backend sources because Windows
held the running demo JAR open in the original target directory.
Lint exited successfully with warnings in existing unrelated leader pages/AuthContext; no additional
frontend dependency or router changes were needed. git diff --check passed.

Run mvn -f backend/pom.xml clean package; npm --prefix frontend run build; npm --prefix frontend run lint.
Integration tests use isolated H2 in PostgreSQL mode and a fake MaterialQuestionGenerator, not your
database, API key or a paid provider. They cover the real security/controller/JPA wiring, transaction
rollback, subject scope, consent, duplicate request handling and the existing review/list/template routes.
H2 passing is not proof that a live PostgreSQL deployment or real Google request has succeeded.
Initial integration used only a fake provider. A subsequent live test on 2026-10-18 succeeded through
the actual MaterialTextExtractor -> LlmQuestionGenerator -> strict validation pipeline using the local
key and docs/samples/network-material.txt: 819 extracted characters, one UNDERSTAND question,
suggested answer, three rubric criteria with a total of 10 points. This was a standalone Java harness
using the project's compiled classes, NOT an HTTP/UI or persistence test. It did not save to a database.
The first live call exposed the MIME enum mismatch: responseFormat.text.mimeType requires
APPLICATION_JSON, not the old responseMimeType string application/json. The request and regression
test were corrected against Google's public REST discovery schema. Google then returned NOT_FOUND
for gemini-2.5-flash, explicitly stating it is unavailable to new users and recommending gemini-3.8-flash.
The local model, example config and default now use gemini-3.8-flash. No automatic retries/fallback
were added to the application. The key was never printed or placed in a URL, report or Git.
Subsequent live PostgreSQL test on 2026-10-18: the user supplied the local database password, which
was passed only through temporary process environment variables (not saved in source/Git). Spring
Boot started and Hibernate validated the existing aives schema on PostgreSQL 17.11. No schema script
was run and no old rows were modified/deleted. A one-page, selectable-text PDF test fixture was
generated from the public TCP/UDP material, visually checked, and submitted via the actual multipart
HTTP endpoint using lecturer1. Gemini 3.8 Flash returned one UNDERSTAND question with suggested
answer and three criteria totaling 10.00. PostgreSQL independently confirmed question ID 11 with
source AI and status PENDING_REVIEW. Counts changed from 10 questions / 13 rubrics to 11 / 16.
The new question was deliberately left pending, not approved into an exam bank.

Browser login, assigned-subject generation form and question list were checked on the live backend.
The automation file chooser failed to attach a file (the DOM input stayed empty), so the paid PDF
generation was tested through the multipart HTTP API rather than by clicking submit in the browser.
Do not describe this as a completed browser-upload test. Manual file selection/submission in a normal
browser remains to be checked. The generated question can be viewed at /lecturer/questions/11/edit.

The live browser check also exposed an existing issue: lecturer question list/edit/rubric screens
requested /api/admin/subjects and therefore got 403. They now use the existing assigned-subject
endpoint, without relaxing admin access or changing role permissions. React build passed after this fix.

## Shared files to flag to the leader before merge

- backend/pom.xml: PDFBox 3.0.8, POI 5.5.1, Gson 2.13.2, test-only H2; PostgreSQL driver unchanged.
- application.yml: optional local key config import, multipart size bounds, SQL logging disabled;
  latest main datasource/JPA/JWT settings retained.
- SecurityConfig: localhost and 127.0.0.1 CORS, Idempotency-Key header.
- LecturerQuestionController: multipart overload + assigned generation subject endpoint;
  legacy JSON contract retained.
- SubjectRepository: correct assigned-subject JPQL join.
- QuestionService: current assignment lookup, AI rubric total check at approval, and initialization
  of creator/reviewer/rubrics inside the transaction for main's open-in-view=false setting.
- Question entity: exclude lazy user assignment/proxy internals from creator/reviewer JSON while
  keeping existing fields such as id/fullName for the existing frontend.
- frontend questionApi and QuestionGeneratePage: multipart request/file/consent; no npm dependency changes.
- frontend QuestionsPage, QuestionFormPage and RubricsPage: use assigned subjects instead of the
  admin-only subject list so lecturers can view/edit generated results and criteria.
- .gitignore: local credentials/tools excluded. No database script changes.

The remote had only main during the audit, although the team guide asks for PRs targeting develop.
Confirm the intended PR base with the leader; do not create or push a replacement develop automatically.

## Official references consulted

- Spring Boot 3.3 external config: https://docs.spring.io/spring-boot/3.3/reference/features/external-config.html
- Gemini JSON schema: https://ai.google.dev/gemini-api/docs/generate-content/structured-output
- Gemini request/response: https://ai.google.dev/api/generate-content
- Live REST schema: https://generativelanguage.googleapis.com/$discovery/rest?version=v1beta
