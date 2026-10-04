package com.aives.service;

import com.aives.model.InterviewTurn;
import com.aives.model.Rubric;
import java.util.List;

/**
 * Sinh câu hỏi xoáy (adaptive follow-up) dựa trên câu trả lời của sinh viên.
 * Bản demo dùng luật đơn giản (RuleBasedFollowUpGenerator); sau này thay bằng
 * implementation gọi LLM mà không phải sửa InterviewService.
 */
public interface FollowUpGenerator {

    /**
     * @param mainQuestionText nội dung câu hỏi chính
     * @param rubrics          tiêu chí chấm của câu hỏi chính (có thể rỗng)
     * @param thread           câu chính + các câu xoáy đã hỏi, theo thứ tự, đều đã có transcript
     * @return câu hỏi xoáy tiếp theo, hoặc null nếu câu trả lời đã đủ ý
     */
    String generate(String mainQuestionText, List<Rubric> rubrics, List<InterviewTurn> thread);
}
