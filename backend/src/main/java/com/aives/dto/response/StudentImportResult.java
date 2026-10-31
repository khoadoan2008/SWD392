package com.aives.dto.response;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentImportResult {

    private int totalRows;
    private int successCount;
    private int createdCount;
    private int updatedCount;
    private int errorCount;
    private String assignedSubject;

    @Builder.Default
    private List<ImportRowError> errors = new ArrayList<>();

    @Builder.Default
    private List<ImportedStudentItem> importedStudents = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ImportRowError {
        private int rowNumber;
        private String studentCode;
        private String fullName;
        private String errorMessage;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ImportedStudentItem {
        private Integer id;
        private String username;
        private String studentCode;
        private String fullName;
        private String email;
        private String initialPassword;
        private String status; // "CREATED", "UPDATED"
        private List<String> assignedSubjects;
    }
}
