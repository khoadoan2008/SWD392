package com.aives.service;

import com.aives.dto.response.StudentImportResult;
import com.aives.model.entity.Subject;
import com.aives.model.entity.User;
import com.aives.model.entity.Role;
import com.aives.repository.RoleRepository;
import com.aives.repository.SubjectRepository;
import com.aives.repository.UserRepository;
import com.aives.security.Pbkdf2CompatiblePasswordEncoder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentImportService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SubjectRepository subjectRepository;
    private final Pbkdf2CompatiblePasswordEncoder passwordEncoder;

    /**
     * Tạo file Excel mẫu (.xlsx) chuẩn cho Admin/Giảng viên tải về
     */
    public byte[] generateExcelTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Danh sách sinh viên");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Data Styles
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle centerDataStyle = workbook.createCellStyle();
            centerDataStyle.cloneStyleFrom(dataStyle);
            centerDataStyle.setAlignment(HorizontalAlignment.CENTER);

            // Row 0: Headers
            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(26);

            String[] headers = {
                    "STT",
                    "Mã số sinh viên (MSSV) *",
                    "Họ và tên *",
                    "Email trường *"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Sample Data Rows
            String[][] sampleData = {
                    {"1", "SE170001", "Nguyễn Văn An", "anvhse170001@fpt.edu.vn"},
                    {"2", "SE170002", "Trần Thị Bình", "binhttse170002@fpt.edu.vn"},
                    {"3", "SE170003", "Lê Hoàng Cường", "cuonglhse170003@fpt.edu.vn"}
            };

            for (int r = 0; r < sampleData.length; r++) {
                Row row = sheet.createRow(r + 1);
                row.setHeightInPoints(20);
                for (int c = 0; c < sampleData[r].length; c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(sampleData[r][c]);
                    if (c == 0 || c == 1) {
                        cell.setCellStyle(centerDataStyle);
                    } else {
                        cell.setCellStyle(dataStyle);
                    }
                }
            }

            // Auto-size columns with padding
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i) + 1200, 4500));
            }

            // Sheet 2: Hướng dẫn
            Sheet guideSheet = workbook.createSheet("Hướng dẫn");
            Row gRow0 = guideSheet.createRow(0);
            Cell gCell0 = gRow0.createCell(0);
            gCell0.setCellValue("HƯỚNG DẪN IMPORT DANH SÁCH SINH VIÊN");

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 13);
            titleStyle.setFont(titleFont);
            gCell0.setCellStyle(titleStyle);

            String[] instructions = {
                    "1. Cột 'Mã số sinh viên (MSSV)': Bắt buộc, không được để trống. Dùng làm Mã sinh viên và Tên đăng nhập (username).",
                    "2. Cột 'Họ và tên': Bắt buộc, không được để trống.",
                    "3. Cột 'Email trường': Định dạng email hợp lệ, ví dụ: student@fpt.edu.vn.",
                    "4. Mật khẩu khởi tạo: Nếu không nhập mật khẩu tuỳ chọn khi import, hệ thống mặc định tạo mật khẩu theo MSSV (hoặc MSSV + 123456 nếu MSSV dưới 6 ký tự).",
                    "5. Tài khoản sinh viên được tạo có Role là STUDENT và active = true.",
                    "6. Cơ chế kiểm tra trùng lặp: Hệ thống tự động kiểm tra trùng username, MSSV, email cả trong nội bộ file và trong cơ sở dữ liệu.",
                    "7. Hệ thống hỗ trợ định dạng file Excel (.xlsx, .xls) và file CSV (.csv)."
            };

            for (int i = 0; i < instructions.length; i++) {
                Row row = guideSheet.createRow(i + 2);
                row.createCell(0).setCellValue(instructions[i]);
            }
            guideSheet.autoSizeColumn(0);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Import sinh viên từ file Excel / CSV
     *
     * @param file                    File Excel (.xlsx, .xls) hoặc CSV (.csv)
     * @param subjectId               (Tùy chọn) ID môn thi để gán thí sinh vào
     * @param customDefaultPassword   (Tùy chọn) Mật khẩu khởi tạo chung
     * @param updateExistingIfDuplicate Nếu true: sinh viên đã tồn tại sẽ được cập nhật gán môn thi (không báo lỗi)
     */
    @Transactional
    public StudentImportResult importStudents(
            MultipartFile file,
            Integer subjectId,
            String customDefaultPassword,
            boolean updateExistingIfDuplicate) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File tải lên không được để trống.");
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            filename = "";
        }
        String lowerFilename = filename.toLowerCase();

        // 1. Kiểm tra môn thi nếu có truyền subjectId
        final Subject targetSubject = (subjectId != null)
                ? subjectRepository.findById(subjectId)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy môn học ID: " + subjectId))
                : null;

        // 2. Phân tích file thành danh sách RawStudentRow
        List<RawStudentRow> rawRows;
        try {
            if (lowerFilename.endsWith(".xlsx") || lowerFilename.endsWith(".xls")) {
                rawRows = parseExcel(file);
            } else if (lowerFilename.endsWith(".csv")) {
                rawRows = parseCsv(file);
            } else {
                // Thử đọc qua POI trước, nếu thất bại thử CSV
                try {
                    rawRows = parseExcel(file);
                } catch (Exception e) {
                    rawRows = parseCsv(file);
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi đọc file import: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Không thể đọc nội dung file: " + e.getMessage());
        }

        if (rawRows.isEmpty()) {
            throw new IllegalArgumentException("File không chứa dữ liệu dòng sinh viên nào.");
        }

        // 3. Tiến hành kiểm tra và import từng dòng
        List<StudentImportResult.ImportRowError> errors = new ArrayList<>();
        List<StudentImportResult.ImportedStudentItem> importedStudents = new ArrayList<>();

        Map<String, Integer> seenCodes = new HashMap<>();
        Map<String, Integer> seenEmails = new HashMap<>();

        int createdCount = 0;
        int updatedCount = 0;

        for (RawStudentRow row : rawRows) {
            int rowNum = row.getRowNum();
            String code = row.getStudentCode() != null ? row.getStudentCode().trim() : "";
            String fullName = row.getFullName() != null ? row.getFullName().trim() : "";
            String email = row.getEmail() != null ? row.getEmail().trim() : "";

            // Kiểm tra các trường bắt buộc
            if (code.isEmpty()) {
                errors.add(new StudentImportResult.ImportRowError(rowNum, code, fullName, "Mã số sinh viên (MSSV) không được để trống."));
                continue;
            }
            if (fullName.isEmpty()) {
                errors.add(new StudentImportResult.ImportRowError(rowNum, code, fullName, "Họ và tên sinh viên không được để trống."));
                continue;
            }
            if (code.length() > 20) {
                errors.add(new StudentImportResult.ImportRowError(rowNum, code, fullName, "Mã số sinh viên tối đa 20 ký tự."));
                continue;
            }
            if (fullName.length() > 100) {
                errors.add(new StudentImportResult.ImportRowError(rowNum, code, fullName, "Họ và tên tối đa 100 ký tự."));
                continue;
            }
            if (!email.isEmpty()) {
                if (!EMAIL_PATTERN.matcher(email).matches()) {
                    errors.add(new StudentImportResult.ImportRowError(rowNum, code, fullName, "Email '" + email + "' không đúng định dạng."));
                    continue;
                }
                if (email.length() > 100) {
                    errors.add(new StudentImportResult.ImportRowError(rowNum, code, fullName, "Email tối đa 100 ký tự."));
                    continue;
                }
            }

            // Kiểm tra trùng lặp nội bộ trong file
            String codeLower = code.toLowerCase();
            if (seenCodes.containsKey(codeLower)) {
                errors.add(new StudentImportResult.ImportRowError(
                        rowNum, code, fullName,
                        "Mã sinh viên '" + code + "' bị trùng lặp với dòng " + seenCodes.get(codeLower) + " trong cùng file."));
                continue;
            }
            seenCodes.put(codeLower, rowNum);

            if (!email.isEmpty()) {
                String emailLower = email.toLowerCase();
                if (seenEmails.containsKey(emailLower)) {
                    errors.add(new StudentImportResult.ImportRowError(
                            rowNum, code, fullName,
                            "Email '" + email + "' bị trùng lặp với dòng " + seenEmails.get(emailLower) + " trong cùng file."));
                    continue;
                }
                seenEmails.put(emailLower, rowNum);
            }

            // Kiểm tra trùng lặp trong Database
            Optional<User> existingByCode = userRepository.findByStudentCodeIgnoreCase(code);
            Optional<User> existingByUsername = userRepository.findByUsernameIgnoreCase(code);
            Optional<User> existingByEmail = !email.isEmpty() ? userRepository.findByEmailIgnoreCase(email) : Optional.empty();

            boolean isExisting = existingByCode.isPresent() || existingByUsername.isPresent() || existingByEmail.isPresent();

            if (isExisting) {
                if (updateExistingIfDuplicate) {
                    // Cập nhật gán thêm môn thi nếu có
                    User existingUser = existingByCode.orElse(existingByUsername.orElse(existingByEmail.orElse(null)));
                    if (existingUser != null) {
                        boolean modified = false;
                        if (targetSubject != null) {
                            if (existingUser.getAssignedSubjects() == null) {
                                existingUser.setAssignedSubjects(new HashSet<>());
                            }
                            boolean alreadyHas = existingUser.getAssignedSubjects().stream()
                                    .anyMatch(s -> s.getId().equals(targetSubject.getId()));
                            if (!alreadyHas) {
                                existingUser.getAssignedSubjects().add(targetSubject);
                                modified = true;
                            }
                        }
                        if (modified) {
                            userRepository.save(existingUser);
                        }

                        List<String> subNames = existingUser.getAssignedSubjects() != null
                                ? existingUser.getAssignedSubjects().stream().map(Subject::getCode).toList()
                                : List.of();

                        importedStudents.add(StudentImportResult.ImportedStudentItem.builder()
                                .id(existingUser.getId())
                                .username(existingUser.getUsername())
                                .studentCode(existingUser.getStudentCode())
                                .fullName(existingUser.getFullName())
                                .email(existingUser.getEmail())
                                .initialPassword("[ĐÃ TỒN TẠI - GIỮ NGUYÊN]")
                                .status("UPDATED")
                                .assignedSubjects(subNames)
                                .build());
                        updatedCount++;
                    }
                } else {
                    // Báo lỗi trùng lặp chi tiết
                    String reason;
                    if (existingByCode.isPresent()) {
                        reason = "Mã số sinh viên '" + code + "' đã tồn tại trong hệ thống.";
                    } else if (existingByUsername.isPresent()) {
                        reason = "Tên đăng nhập '" + code + "' đã tồn tại trong hệ thống.";
                    } else {
                        reason = "Email '" + email + "' đã tồn tại trong hệ thống.";
                    }
                    errors.add(new StudentImportResult.ImportRowError(rowNum, code, fullName, reason));
                }
                continue;
            }

            // Tạo tài khoản sinh viên mới
            String rawPassword;
            if (customDefaultPassword != null && !customDefaultPassword.trim().isEmpty()) {
                rawPassword = customDefaultPassword.trim();
            } else if (code.length() >= 6) {
                rawPassword = code;
            } else {
                rawPassword = code + "123456";
            }

            String passwordHash = passwordEncoder.encode(rawPassword);

            Set<Subject> subjects = new HashSet<>();
            if (targetSubject != null) {
                subjects.add(targetSubject);
            }

            User newStudent = User.builder()
                    .username(code)
                    .studentCode(code)
                    .fullName(fullName)
                    .email(email.isEmpty() ? null : email)
                    .role(roleRepository.findByNameIgnoreCase(Role.STUDENT_NAME).orElseGet(() -> roleRepository.save(Role.STUDENT)))
                    .active(true)
                    .passwordHash(passwordHash)
                    .assignedSubjects(subjects)
                    .build();

            User saved = userRepository.save(newStudent);

            List<String> subNames = targetSubject != null ? List.of(targetSubject.getCode()) : List.of();

            importedStudents.add(StudentImportResult.ImportedStudentItem.builder()
                    .id(saved.getId())
                    .username(saved.getUsername())
                    .studentCode(saved.getStudentCode())
                    .fullName(saved.getFullName())
                    .email(saved.getEmail())
                    .initialPassword(rawPassword)
                    .status("CREATED")
                    .assignedSubjects(subNames)
                    .build());
            createdCount++;
        }

        String assignedSubjectDesc = targetSubject != null
                ? (targetSubject.getCode() + " - " + targetSubject.getName())
                : null;

        return StudentImportResult.builder()
                .totalRows(rawRows.size())
                .successCount(createdCount + updatedCount)
                .createdCount(createdCount)
                .updatedCount(updatedCount)
                .errorCount(errors.size())
                .assignedSubject(assignedSubjectDesc)
                .errors(errors)
                .importedStudents(importedStudents)
                .build();
    }

    /**
     * Đọc các dòng từ file Excel (.xlsx / .xls) với cơ chế tự động nhận diện vị trí cột
     */
    private List<RawStudentRow> parseExcel(MultipartFile file) throws IOException {
        List<RawStudentRow> result = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return result;
            }

            int headerRowIdx = -1;
            int colStt = 0;
            int colCode = 1;
            int colName = 2;
            int colEmail = 3;

            // Tìm hàng tiêu đề thông minh
            int maxScanRow = Math.min(15, sheet.getLastRowNum());
            for (int r = 0; r <= maxScanRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                boolean foundCode = false;
                boolean foundName = false;
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    String val = formatter.formatCellValue(row.getCell(c)).trim().toLowerCase();
                    if (val.contains("mssv") || val.contains("mã số sinh viên") || val.contains("mã sinh viên")
                            || val.contains("mã sv") || val.contains("student code") || val.contains("student id")) {
                        foundCode = true;
                    }
                    if (val.contains("họ và tên") || val.contains("họ tên") || val.contains("full name")
                            || val.contains("fullname") || val.contains("tên sinh viên")) {
                        foundName = true;
                    }
                }
                if (foundCode || foundName) {
                    headerRowIdx = r;
                    break;
                }
            }

            if (headerRowIdx != -1) {
                Row hRow = sheet.getRow(headerRowIdx);
                for (int c = 0; c < hRow.getLastCellNum(); c++) {
                    String val = formatter.formatCellValue(hRow.getCell(c)).trim().toLowerCase();
                    if (val.contains("stt") || val.contains("số tt") || val.equals("no")) {
                        colStt = c;
                    } else if (val.contains("mssv") || val.contains("mã số") || val.contains("mã sinh viên")
                            || val.contains("mã sv") || val.contains("student code") || val.contains("student id")) {
                        colCode = c;
                    } else if (val.contains("họ") || val.contains("tên") || val.contains("name")) {
                        colName = c;
                    } else if (val.contains("email") || val.contains("mail") || val.contains("thư điện tử")) {
                        colEmail = c;
                    }
                }
            }

            int startRow = (headerRowIdx != -1) ? (headerRowIdx + 1) : 0;
            for (int r = startRow; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String stt = formatter.formatCellValue(row.getCell(colStt)).trim();
                String code = formatter.formatCellValue(row.getCell(colCode)).trim();
                String name = formatter.formatCellValue(row.getCell(colName)).trim();
                String email = formatter.formatCellValue(row.getCell(colEmail)).trim();

                // Bỏ qua dòng trống hoàn toàn
                if (stt.isEmpty() && code.isEmpty() && name.isEmpty() && email.isEmpty()) {
                    continue;
                }

                // Nếu không có header detected và dòng đầu chứa chữ MSSV/Họ tên thì bỏ qua dòng đó
                if (headerRowIdx == -1 && r == 0 && (code.equalsIgnoreCase("mssv") || name.toLowerCase().contains("tên"))) {
                    continue;
                }

                result.add(new RawStudentRow(r + 1, stt, code, name, email));
            }
        }
        return result;
    }

    /**
     * Đọc các dòng từ file CSV với tự động phát hiện dấu phân cách (, ; \t) và UTF-8 BOM
     */
    private List<RawStudentRow> parseCsv(MultipartFile file) throws IOException {
        List<RawStudentRow> result = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (lines.isEmpty() && line.startsWith("\uFEFF")) {
                    line = line.substring(1); // Xoá UTF-8 BOM
                }
                if (!line.trim().isEmpty()) {
                    lines.add(line);
                }
            }

            if (lines.isEmpty()) {
                return result;
            }

            // Phát hiện delimiter từ dòng đầu
            String firstLine = lines.get(0);
            char delimiter = detectCsvDelimiter(firstLine);

            int headerIdx = -1;
            int colStt = 0;
            int colCode = 1;
            int colName = 2;
            int colEmail = 3;

            for (int i = 0; i < Math.min(10, lines.size()); i++) {
                List<String> tokens = splitCsvLine(lines.get(i), delimiter);
                boolean foundCode = false;
                boolean foundName = false;
                for (String t : tokens) {
                    String low = t.trim().toLowerCase();
                    if (low.contains("mssv") || low.contains("mã sinh viên") || low.contains("student code") || low.contains("mã sv")) {
                        foundCode = true;
                    }
                    if (low.contains("họ và tên") || low.contains("họ tên") || low.contains("name")) {
                        foundName = true;
                    }
                }
                if (foundCode || foundName) {
                    headerIdx = i;
                    for (int c = 0; c < tokens.size(); c++) {
                        String low = tokens.get(c).trim().toLowerCase();
                        if (low.contains("stt") || low.contains("số tt") || low.equals("no")) {
                            colStt = c;
                        } else if (low.contains("mssv") || low.contains("mã sinh viên") || low.contains("student code") || low.contains("mã sv")) {
                            colCode = c;
                        } else if (low.contains("họ") || low.contains("tên") || low.contains("name")) {
                            colName = c;
                        } else if (low.contains("email") || low.contains("mail")) {
                            colEmail = c;
                        }
                    }
                    break;
                }
            }

            int startIdx = (headerIdx != -1) ? (headerIdx + 1) : 0;
            for (int i = startIdx; i < lines.size(); i++) {
                List<String> tokens = splitCsvLine(lines.get(i), delimiter);
                String stt = colStt < tokens.size() ? tokens.get(colStt).trim() : "";
                String code = colCode < tokens.size() ? tokens.get(colCode).trim() : "";
                String name = colName < tokens.size() ? tokens.get(colName).trim() : "";
                String email = colEmail < tokens.size() ? tokens.get(colEmail).trim() : "";

                if (stt.isEmpty() && code.isEmpty() && name.isEmpty() && email.isEmpty()) {
                    continue;
                }

                if (headerIdx == -1 && i == 0 && (code.equalsIgnoreCase("mssv") || name.toLowerCase().contains("tên"))) {
                    continue;
                }

                result.add(new RawStudentRow(i + 1, stt, code, name, email));
            }
        }
        return result;
    }

    private char detectCsvDelimiter(String line) {
        int commas = countChars(line, ',');
        int semicolons = countChars(line, ';');
        int tabs = countChars(line, '\t');

        if (semicolons > commas && semicolons > tabs) return ';';
        if (tabs > commas && tabs > semicolons) return '\t';
        return ',';
    }

    private int countChars(String str, char ch) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ch) count++;
        }
        return count;
    }

    private List<String> splitCsvLine(String line, char delimiter) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == delimiter && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());
        return tokens;
    }

    @Getter
    @AllArgsConstructor
    private static class RawStudentRow {
        private final int rowNum;
        private final String stt;
        private final String studentCode;
        private final String fullName;
        private final String email;
    }
}
