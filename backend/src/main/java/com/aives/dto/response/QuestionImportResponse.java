package com.aives.dto.response;

import java.util.List;

public record QuestionImportResponse(String fileHash, int totalRows, int validRows, int invalidRows,
                                     int importedCount, long durationMs, List<Row> rows) {
    public record Row(int rowNumber, String subjectCode, String content, String bloomLevel,
                      String difficulty, String notes, List<String> errors) { }
}
