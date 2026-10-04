package com.aives.service;

import com.aives.model.BloomLevel;
import com.aives.model.Question;
import com.aives.model.QuestionSource;
import com.aives.model.QuestionStatus;
import java.util.ArrayList;
import java.util.List;

/** Stub thay cho AI: sinh câu hỏi theo mẫu cho từng mức Bloom. */
public class TemplateQuestionGenerator implements QuestionGenerator {

    @Override
    public List<Question> generate(int subjectId, String topic, List<BloomLevel> levels) {
        List<Question> list = new ArrayList<>();
        for (BloomLevel level : levels) {
            Question q = new Question();
            q.setSubjectId(subjectId);
            q.setTopic(topic);
            q.setBloomLevel(level);
            q.setStatus(QuestionStatus.PENDING_REVIEW);
            q.setSource(QuestionSource.AI);
            q.setContent(template(level, topic));
            list.add(q);
        }
        return list;
    }

    private String template(BloomLevel level, String topic) {
        switch (level) {
            case REMEMBER:
                return "Hãy nêu định nghĩa của " + topic + ".";
            case UNDERSTAND:
                return "Hãy giải thích bằng lời của bạn " + topic + " hoạt động như thế nào.";
            case APPLY:
                return "Cho một ví dụ thực tế áp dụng " + topic + " và mô tả cách bạn triển khai.";
            case ANALYZE:
                return "Phân tích ưu điểm và nhược điểm của " + topic + ".";
            case EVALUATE:
                return "Trong trường hợp nào bạn KHÔNG nên dùng " + topic + "? Hãy lập luận.";
            default:
                return "Hãy đề xuất một cải tiến hoặc thiết kế mới dựa trên " + topic + ".";
        }
    }
}
