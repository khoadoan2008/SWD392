package com.aives.service.generation;

import com.aives.model.enums.BloomLevel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One list entry represents one question, including repeated Bloom levels. */
public final class GenerationRequest {
    public static final int MAX_QUESTIONS = 12;
    public static final int MAX_TEXT_CHARS = 100000;
    private final int subjectId;
    private final String subjectName;
    private final String topic;
    private final List<BloomLevel> levels;
    private final String materialText;

    public GenerationRequest(int subjectId, String subjectName, String topic,
            List<BloomLevel> levels, String materialText) throws GenerationException {
        if (subjectId <= 0 || topic == null || topic.trim().isEmpty() || topic.trim().length() > 200) {
            throw new GenerationException("Chọn môn và nhập chủ đề từ 1 đến 200 ký tự.");
        }
        if (levels == null || levels.isEmpty() || levels.size() > MAX_QUESTIONS || levels.stream().anyMatch(java.util.Objects::isNull)) {
            throw new GenerationException("Chọn số câu và mức Bloom hợp lệ (tối đa 12 câu/lần).");
        }
        if (materialText == null || materialText.trim().length() < 40) {
            throw new GenerationException("Tài liệu cần có ít nhất 40 ký tự văn bản đọc được.");
        }
        if (materialText.length() > MAX_TEXT_CHARS) {
            throw new GenerationException("Văn bản học liệu vượt giới hạn 100.000 ký tự. Hãy chia nhỏ tài liệu.");
        }
        this.subjectId = subjectId;
        this.subjectName = subjectName == null ? "" : subjectName;
        this.topic = topic.trim();
        this.levels = Collections.unmodifiableList(new ArrayList<>(levels));
        this.materialText = materialText.trim();
    }
    public int getSubjectId() { return subjectId; }
    public String getSubjectName() { return subjectName; }
    public String getTopic() { return topic; }
    public List<BloomLevel> getLevels() { return levels; }
    public String getMaterialText() { return materialText; }
}
