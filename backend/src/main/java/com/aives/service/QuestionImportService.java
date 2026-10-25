package com.aives.service;

import com.aives.dto.response.QuestionImportResponse;
import com.aives.model.entity.Question;
import com.aives.model.entity.Subject;
import com.aives.model.enums.*;
import com.aives.repository.QuestionRepository;
import com.aives.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Application service in the existing controller -> service -> repository architecture. */
@Service
@RequiredArgsConstructor
public class QuestionImportService {
    public static final String[] HEADERS = {"Mã môn", "Nội dung câu hỏi", "Thang Bloom", "Độ khó", "Ghi chú/Gợi ý"};
    public static final int MAX_ROWS = 1000;
    private static final int MAX_BYTES = 10 * 1024 * 1024;
    private static final Set<String> BLOOMS = Set.of("REMEMBER", "UNDERSTAND", "APPLY", "ANALYZE");
    private final QuestionRepository questions;
    private final SubjectRepository subjects;
    private final QuestionAccessService access;
    private final QuestionApprovalPolicy approval;

    @Transactional
    public QuestionImportResponse process(int actorId, MultipartFile file, boolean confirm,
                                         QuestionStatus targetStatus, String previewHash) {
        long started = System.nanoTime();
        var actor = access.actor(actorId);
        if (targetStatus != QuestionStatus.PENDING_REVIEW && targetStatus != QuestionStatus.APPROVED)
            throw new IllegalArgumentException("Import chỉ hỗ trợ PENDING_REVIEW hoặc APPROVED.");
        byte[] bytes = readUpload(file);
        String hash = hash(bytes);
        if (confirm && (previewHash == null || !MessageDigest.isEqual(hash.getBytes(StandardCharsets.US_ASCII),
                previewHash.getBytes(StandardCharsets.US_ASCII))))
            throw new IllegalArgumentException("Tệp đã thay đổi hoặc chưa xem trước. Vui lòng xem trước lại.");

        Map<String, Subject> allowed = new HashMap<>();
        for (Subject subject : access.manageable(actorId)) allowed.put(subject.getCode().toUpperCase(Locale.ROOT), subject);
        List<QuestionImportResponse.Row> parsed = parse(bytes);
        // Lock subjects in a stable order: concurrent imports cannot both insert the same batch.
        if (confirm) parsed.stream().map(QuestionImportResponse.Row::subjectCode).distinct()
                .map(allowed::get).filter(Objects::nonNull).map(Subject::getId).distinct().sorted()
                .forEach(id -> subjects.lockById(id).orElseThrow(() -> new IllegalArgumentException("Môn học không tồn tại.")));

        Map<Integer, Set<String>> existing = new HashMap<>();
        Map<Integer, String> approvalErrors = new HashMap<>();
        Set<String> seen = new HashSet<>();
        List<QuestionImportResponse.Row> checked = new ArrayList<>();
        for (var row : parsed) {
            List<String> errors = new ArrayList<>(row.errors());
            Subject subject = allowed.get(row.subjectCode());
            if (subject == null) errors.add("Mã môn không tồn tại, ngừng hoạt động hoặc bạn chưa được phân công.");
            else {
                // Reload active state and current assignments at commit, not from the preview response.
                if (confirm) access.requireSubject(actorId, subject.getId());
                Set<String> contents = existing.computeIfAbsent(subject.getId(), id -> {
                    Set<String> values = new HashSet<>();
                    questions.findContentsBySubjectId(id).forEach(value -> values.add(normalizeContent(value)));
                    return values;
                });
                String normalized = normalizeContent(row.content());
                if (!normalized.isEmpty() && contents.contains(normalized)) errors.add("Câu hỏi đã tồn tại trong môn học.");
                if (!normalized.isEmpty() && !seen.add(subject.getId() + ":" + normalized)) errors.add("Câu hỏi trùng trong tệp.");
                if (targetStatus == QuestionStatus.APPROVED) {
                    String approvalError = approvalErrors.computeIfAbsent(subject.getId(), id -> {
                        try { approval.requireApprovable(id, QuestionSource.IMPORT); return ""; }
                        catch (IllegalArgumentException ex) { return ex.getMessage(); }
                    });
                    if (!approvalError.isEmpty()) errors.add(approvalError);
                }
            }
            checked.add(new QuestionImportResponse.Row(row.rowNumber(), row.subjectCode(), row.content(),
                    row.bloomLevel(), row.difficulty(), row.notes(), List.copyOf(errors)));
        }
        int invalid = (int) checked.stream().filter(row -> !row.errors().isEmpty()).count();
        int imported = 0;
        if (confirm && invalid == 0) {
            List<Question> batch = checked.stream().map(row -> Question.builder()
                    .subject(allowed.get(row.subjectCode())).content(row.content())
                    .bloomLevel(BloomLevel.valueOf(row.bloomLevel())).difficulty(DifficultyLevel.valueOf(row.difficulty()))
                    .notes(row.notes()).status(targetStatus).source(QuestionSource.IMPORT).createdBy(actor)
                    .reviewedBy(targetStatus == QuestionStatus.APPROVED ? actor : null).build()).toList();
            questions.saveAllAndFlush(batch);
            imported = batch.size();
        }
        return new QuestionImportResponse(hash, checked.size(), checked.size() - invalid, invalid, imported,
                (System.nanoTime() - started) / 1_000_000, checked);
    }

    private byte[] readUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Hãy chọn tệp Excel có dữ liệu.");
        if (file.getOriginalFilename() == null || !file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".xlsx"))
            throw new IllegalArgumentException("Chỉ hỗ trợ tệp .xlsx theo mẫu.");
        if (file.getSize() > MAX_BYTES) throw new IllegalArgumentException("Tệp Excel tối đa 10 MB.");
        try (InputStream in = file.getInputStream()) {
            byte[] bytes = in.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) throw new IllegalArgumentException("Tệp Excel tối đa 10 MB.");
            // Bound decompression before POI allocates workbook XML/shared strings.
            long expanded = 0; int entries = 0;
            try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
                ZipEntry entry; byte[] buffer = new byte[8192];
                while ((entry = zip.getNextEntry()) != null) {
                    if (++entries > 1000) throw new IllegalArgumentException("Tệp Excel có quá nhiều thành phần.");
                    if (entry.getName().toLowerCase(Locale.ROOT).contains("vbaproject"))
                        throw new IllegalArgumentException("Không hỗ trợ tệp có macro.");
                    int n;
                    while ((n = zip.read(buffer)) != -1) {
                        expanded += n;
                        if (expanded > 20L * 1024 * 1024) throw new IllegalArgumentException("Tệp Excel giải nén quá lớn.");
                    }
                }
            }
            if (entries == 0) throw new IllegalArgumentException("Nội dung không phải tệp .xlsx hợp lệ.");
            return bytes;
        } catch (IOException ex) { throw new IllegalArgumentException("Không đọc được tệp Excel."); }
    }

    private List<QuestionImportResponse.Row> parse(byte[] bytes) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            if (workbook.getNumberOfSheets() != 1) throw new IllegalArgumentException("Tệp phải có đúng một sheet theo mẫu.");
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getLastRowNum() > MAX_ROWS) throw new IllegalArgumentException("Mỗi lần tối đa 1000 dòng dữ liệu.");
            Row header = sheet.getRow(0);
            if (header == null) throw new IllegalArgumentException("Thiếu dòng tiêu đề theo mẫu.");
            for (int c = 0; c < HEADERS.length; c++)
                if (!normalizeContent(HEADERS[c]).equals(normalizeContent(text(header.getCell(c)))))
                    throw new IllegalArgumentException("Sai tiêu đề cột " + (c + 1) + ". Cần: " + HEADERS[c]);
            List<QuestionImportResponse.Row> rows = new ArrayList<>();
            for (int index = 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null) continue;
                List<String> errors = new ArrayList<>(); String[] values = new String[5];
                boolean populated = false;
                for (int c = 0; c < Math.max(5, row.getLastCellNum()); c++) {
                    Cell cell = row.getCell(c);
                    if (cell == null || cell.getCellType() == CellType.BLANK) { if (c < 5) values[c] = ""; continue; }
                    if (cell.getCellType() == CellType.STRING && cell.getStringCellValue().isBlank()) { if (c < 5) values[c] = ""; continue; }
                    populated = true;
                    if (c >= 5) { errors.add("Có dữ liệu ngoài 5 cột của mẫu."); break; }
                    if (cell.getCellType() != CellType.STRING) {
                        errors.add("Cột " + HEADERS[c] + " phải là văn bản, không dùng công thức."); values[c] = "";
                    } else values[c] = cell.getStringCellValue().strip();
                }
                if (!populated) continue;
                for (int c = 0; c < 5; c++) if (values[c] == null) values[c] = "";
                values[0] = values[0].toUpperCase(Locale.ROOT);
                values[2] = values[2].toUpperCase(Locale.ROOT); values[3] = values[3].toUpperCase(Locale.ROOT);
                if (values[0].isEmpty()) errors.add("Mã môn không được trống.");
                if (values[1].isEmpty() || values[1].length() > 10000) errors.add("Nội dung câu hỏi cần từ 1 đến 10000 ký tự.");
                if (!BLOOMS.contains(values[2])) errors.add("Bloom chỉ nhận REMEMBER, UNDERSTAND, APPLY, ANALYZE.");
                if (!Set.of("EASY", "MEDIUM", "HARD").contains(values[3])) errors.add("Độ khó chỉ nhận EASY, MEDIUM, HARD.");
                if (values[4].length() > 2000) errors.add("Ghi chú tối đa 2000 ký tự.");
                rows.add(new QuestionImportResponse.Row(index + 1, values[0], values[1], values[2], values[3], values[4], errors));
            }
            if (rows.isEmpty()) throw new IllegalArgumentException("Tệp không có dòng câu hỏi nào.");
            return rows;
        } catch (IllegalArgumentException ex) { throw ex; }
        catch (Exception ex) { throw new IllegalArgumentException("Tệp .xlsx bị hỏng, được mã hóa hoặc không đúng định dạng."); }
    }

    private String text(Cell cell) {
        return cell != null && cell.getCellType() == CellType.STRING ? cell.getStringCellValue().strip() : "";
    }

    public static String normalizeContent(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFC)
                .strip().replaceAll("[\\s\\p{Z}]+", " ").toLowerCase(Locale.ROOT);
    }

    private String hash(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }

    /** Runtime template export, using the same POI dependency as the importer. */
    public byte[] template() {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Cau hoi"); sheet.createFreezePane(0, 1);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex()); headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font font = workbook.createFont(); font.setBold(true); font.setColor(IndexedColors.WHITE.getIndex()); headerStyle.setFont(font);
            Row header = sheet.createRow(0); header.setHeightInPoints(30);
            int[] widths = {18, 80, 23, 18, 65};
            for (int c = 0; c < 5; c++) { Cell cell = header.createCell(c); cell.setCellValue(HEADERS[c]); cell.setCellStyle(headerStyle); sheet.setColumnWidth(c, widths[c] * 256); }
            DataValidationHelper helper = sheet.getDataValidationHelper();
            for (int c : new int[]{2, 3}) {
                String[] values = c == 2 ? new String[]{"REMEMBER", "UNDERSTAND", "APPLY", "ANALYZE"} : new String[]{"EASY", "MEDIUM", "HARD"};
                DataValidation validation = helper.createValidation(helper.createExplicitListConstraint(values), new CellRangeAddressList(1, MAX_ROWS, c, c));
                validation.setShowErrorBox(true); validation.createErrorBox("Giá trị không hợp lệ", "Hãy chọn giá trị trong danh sách."); sheet.addValidationData(validation);
            }
            workbook.write(out); return out.toByteArray();
        } catch (IOException ex) { throw new IllegalStateException("Không tạo được file mẫu.", ex); }
    }
}
