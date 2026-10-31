package com.aives.controller;

import com.aives.model.entity.Subject;
import com.aives.model.entity.User;
import com.aives.model.entity.Role;
import com.aives.repository.SubjectRepository;
import com.aives.repository.UserRepository;
import com.aives.security.JwtTokenProvider;
import com.aives.security.Pbkdf2CompatiblePasswordEncoder;
import com.aives.security.UserPrincipal;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StudentImportWorkflowTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private com.aives.repository.RoleRepository roleRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private Pbkdf2CompatiblePasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User admin;
    private User lecturer1;
    private User lecturer2;
    private Subject subject;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from interview_turns");
        jdbcTemplate.update("delete from interview_sessions");
        jdbcTemplate.update("delete from questions");
        jdbcTemplate.update("delete from rubrics");
        jdbcTemplate.update("delete from lecturer_subjects");
        userRepository.deleteAll();
        subjectRepository.deleteAll();

        admin = createUser("admin", Role.ADMIN, null);
        lecturer1 = createUser("lecturer1", Role.LECTURER, null);
        lecturer2 = createUser("lecturer2", Role.LECTURER, null);

        subject = Subject.builder()
                .code("PRN231")
                .name("Building Cross-Platform Apps with .NET")
                .active(true)
                .build();
        subject = subjectRepository.saveAndFlush(subject);

        // Assign subject to lecturer1 only
        lecturer1.getAssignedSubjects().add(subject);
        lecturer1 = userRepository.saveAndFlush(lecturer1);
    }

    private User createUser(String username, Role role, String studentCode) {
        User u = User.builder()
                .username(username)
                .fullName("Full Name " + username)
                .passwordHash(passwordEncoder.encode("Demo@123"))
                .role(roleRepository.findByNameIgnoreCase(role.getName()).orElseGet(() -> roleRepository.save(role)))
                .studentCode(studentCode)
                .email(username + "@fpt.edu.vn")
                .active(true)
                .build();
        return userRepository.saveAndFlush(u);
    }

    private String getBearer(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        return "Bearer " + jwtTokenProvider.generateToken(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private byte[] createTestExcel(String[][] rows) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Danh sách");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("STT");
            header.createCell(1).setCellValue("Mã số sinh viên (MSSV)");
            header.createCell(2).setCellValue("Họ và tên");
            header.createCell(3).setCellValue("Email trường");

            for (int r = 0; r < rows.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < rows[r].length; c++) {
                    row.createCell(c).setCellValue(rows[r][c]);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    @Test
    @DisplayName("Admin tải file Excel mẫu thành công")
    void testDownloadTemplate_Admin() throws Exception {
        byte[] content = mvc.perform(get("/api/admin/users/import-template")
                        .header("Authorization", getBearer(admin)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("mau_import_sinh_vien.xlsx")))
                .andReturn().getResponse().getContentAsByteArray();

        assertNotNull(content);
        assertTrue(content.length > 0);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            assertEquals("Danh sách sinh viên", wb.getSheetAt(0).getSheetName());
            assertEquals("Hướng dẫn", wb.getSheetAt(1).getSheetName());
            Row header = wb.getSheetAt(0).getRow(0);
            assertEquals("STT", header.getCell(0).getStringCellValue());
            assertTrue(header.getCell(1).getStringCellValue().contains("MSSV"));
        }
    }

    @Test
    @DisplayName("Giảng viên tải file Excel mẫu thành công")
    void testDownloadTemplate_Lecturer() throws Exception {
        mvc.perform(get("/api/lecturer/students/import-template")
                        .header("Authorization", getBearer(lecturer1)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("mau_import_sinh_vien.xlsx")));
    }

    @Test
    @DisplayName("Admin import thành công danh sách sinh viên từ Excel và gán môn thi")
    void testImportExcel_Success() throws Exception {
        String[][] data = {
                {"1", "SE170010", "Nguyễn Văn Alpha", "alphanv@fpt.edu.vn"},
                {"2", "SE170011", "Trần Thị Beta", "betatt@fpt.edu.vn"}
        };
        byte[] excelBytes = createTestExcel(data);
        MockMultipartFile file = new MockMultipartFile(
                "file", "danh_sach_lop.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                excelBytes);

        mvc.perform(multipart("/api/admin/users/import")
                        .file(file)
                        .param("subjectId", subject.getId().toString())
                        .header("Authorization", getBearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRows").value(2))
                .andExpect(jsonPath("$.data.successCount").value(2))
                .andExpect(jsonPath("$.data.createdCount").value(2))
                .andExpect(jsonPath("$.data.errorCount").value(0))
                .andExpect(jsonPath("$.data.assignedSubject").value(containsString("PRN231")));

        // Verify database state
        User u1 = userRepository.findByStudentCodeIgnoreCase("SE170010").orElse(null);
        assertNotNull(u1);
        assertEquals("SE170010", u1.getUsername());
        assertEquals("Nguyễn Văn Alpha", u1.getFullName());
        assertEquals(Role.STUDENT, u1.getRole());
        assertTrue(u1.isActive());
        // Default password should be studentCode SE170010
        assertTrue(passwordEncoder.matches("SE170010", u1.getPasswordHash()));
        // Assigned subject
        assertEquals(1, u1.getAssignedSubjects().size());
        assertTrue(u1.getAssignedSubjects().stream().anyMatch(s -> s.getCode().equals("PRN231")));
    }

    @Test
    @DisplayName("Import phát hiện trùng lặp trong file và trong database khi updateExisting = false")
    void testImportExcel_DuplicateDetection() throws Exception {
        // Pre-create student SE170099 in DB
        createUser("SE170099", Role.STUDENT, "SE170099");

        String[][] data = {
                {"1", "SE170001", "Sinh Viên Mới 1", "new1@fpt.edu.vn"},
                {"2", "SE170099", "Sinh Viên Đã Có Sẵn", "dup@fpt.edu.vn"},      // Trùng DB
                {"3", "SE170001", "Sinh Viên Mới Trùng Lại", "new1_dup@fpt.edu.vn"} // Trùng dòng 1 trong file
        };
        byte[] excelBytes = createTestExcel(data);
        MockMultipartFile file = new MockMultipartFile("file", "students.xlsx", "application/vnd.ms-excel", excelBytes);

        mvc.perform(multipart("/api/admin/users/import")
                        .file(file)
                        .param("updateExisting", "false")
                        .header("Authorization", getBearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRows").value(3))
                .andExpect(jsonPath("$.data.createdCount").value(1))
                .andExpect(jsonPath("$.data.errorCount").value(2))
                .andExpect(jsonPath("$.data.errors[0].rowNumber").value(3)) // Dòng 3 trong Excel (index 1 data)
                .andExpect(jsonPath("$.data.errors[0].errorMessage").value(containsString("đã tồn tại trong hệ thống")))
                .andExpect(jsonPath("$.data.errors[1].rowNumber").value(4)) // Dòng 4 trong Excel
                .andExpect(jsonPath("$.data.errors[1].errorMessage").value(containsString("bị trùng lặp với dòng 2")));
    }

    @Test
    @DisplayName("Import với updateExisting = true cập nhật gán môn thi cho sinh viên đã có tài khoản")
    void testImportExcel_UpdateExistingTrue() throws Exception {
        // Sinh viên đã có từ trước, chưa được gán môn
        User preExisting = createUser("SE170088", Role.STUDENT, "SE170088");
        assertTrue(preExisting.getAssignedSubjects().isEmpty());

        String[][] data = {
                {"1", "SE170088", "Sinh Viên Cũ Cần Thi", "SE170088@fpt.edu.vn"}
        };
        byte[] excelBytes = createTestExcel(data);
        MockMultipartFile file = new MockMultipartFile("file", "students.xlsx", "application/vnd.ms-excel", excelBytes);

        mvc.perform(multipart("/api/admin/users/import")
                        .file(file)
                        .param("subjectId", subject.getId().toString())
                        .param("updateExisting", "true")
                        .header("Authorization", getBearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.updatedCount").value(1))
                .andExpect(jsonPath("$.data.createdCount").value(0))
                .andExpect(jsonPath("$.data.errorCount").value(0))
                .andExpect(jsonPath("$.data.importedStudents[0].status").value("UPDATED"));

        // Verify student now has subject
        User reloaded = userRepository.findByStudentCodeIgnoreCase("SE170088").orElseThrow();
        assertEquals(1, reloaded.getAssignedSubjects().size());
        assertTrue(reloaded.getAssignedSubjects().stream().anyMatch(s -> s.getCode().equals("PRN231")));
    }

    @Test
    @DisplayName("Import thành công từ file CSV UTF-8 với dấu phẩy hoặc chấm phẩy")
    void testImportCsv_Success() throws Exception {
        String csvContent = "STT,Mã số sinh viên (MSSV),Họ và tên,Email trường\n" +
                "1,SE170020,Phạm Hữu Đạt,datphse170020@fpt.edu.vn\n" +
                "2,SE170021,Vũ Minh Quân,quanvmse170021@fpt.edu.vn\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "students.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/api/admin/users/import")
                        .file(file)
                        .param("defaultPassword", "CustomPass@123")
                        .header("Authorization", getBearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createdCount").value(2))
                .andExpect(jsonPath("$.data.errorCount").value(0));

        User student = userRepository.findByStudentCodeIgnoreCase("SE170020").orElseThrow();
        assertEquals("Phạm Hữu Đạt", student.getFullName());
        assertTrue(passwordEncoder.matches("CustomPass@123", student.getPasswordHash()));
    }

    @Test
    @DisplayName("Giảng viên import vào môn mình phụ trách thành công; vào môn khác bị từ chối 403")
    void testLecturerImport_Permissions() throws Exception {
        String[][] data = {
                {"1", "SE170030", "Lê Văn Tiến", "tienlv@fpt.edu.vn"}
        };
        byte[] excelBytes = createTestExcel(data);
        MockMultipartFile file = new MockMultipartFile("file", "class.xlsx", "application/vnd.ms-excel", excelBytes);

        // lecturer1 phụ trách PRN231 -> thành công 200
        mvc.perform(multipart("/api/lecturer/students/import")
                        .file(file)
                        .param("subjectId", subject.getId().toString())
                        .header("Authorization", getBearer(lecturer1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.createdCount").value(1));

        // lecturer2 KHÔNG phụ trách PRN231 -> bị từ chối 403 Forbidden
        mvc.perform(multipart("/api/lecturer/students/import")
                        .file(file)
                        .param("subjectId", subject.getId().toString())
                        .header("Authorization", getBearer(lecturer2)))
                .andExpect(status().isForbidden());
    }
}
