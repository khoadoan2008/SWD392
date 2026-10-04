package com.aives.service;

import com.aives.model.BloomLevel;
import com.aives.model.Question;
import java.util.List;

/**
 * Sinh câu hỏi tự động cho ngân hàng câu hỏi (Feature 1).
 * Bản demo dùng template; bản thật sẽ gọi LLM + RAG trên giáo trình/slide của môn.
 * Câu hỏi sinh ra luôn ở trạng thái PENDING_REVIEW, giảng viên phải duyệt.
 */
public interface QuestionGenerator {

    List<Question> generate(int subjectId, String topic, List<BloomLevel> levels);
}
