package com.aives.service.impl;

import com.aives.model.entity.Question;
import com.aives.model.entity.Subject;
import com.aives.model.enums.BloomLevel;
import com.aives.model.enums.QuestionSource;
import com.aives.model.enums.QuestionStatus;
import com.aives.service.QuestionGenerator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TemplateQuestionGenerator implements QuestionGenerator {

    @Override
    public List<Question> generate(Subject subject, String topic, List<BloomLevel> levels) {
        List<Question> list = new ArrayList<>();
        for (BloomLevel level : levels) {
            Question q = Question.builder()
                    .subject(subject)
                    .topic(topic)
                    .bloomLevel(level)
                    .status(QuestionStatus.PENDING_REVIEW)
                    .source(QuestionSource.AI)
                    .content(template(level, topic))
                    .referenceAnswer(referenceAnswerTemplate(level, topic))
                    .build();
            list.add(q);
        }
        return list;
    }

    private String template(BloomLevel level, String topic) {
        return switch (level) {
            case REMEMBER -> "Hãy nêu định nghĩa và các khái niệm cốt lõi của " + topic + ".";
            case UNDERSTAND -> "Hãy giải thích bằng lời của bạn cách " + topic + " hoạt động và bản chất kỹ thuật.";
            case APPLY -> "Cho một kịch bản dự án thực tế áp dụng " + topic + " và mô tả các bước bạn sẽ triển khai.";
            case ANALYZE -> "Hãy so sánh, phân tích ưu điểm và nhược điểm của " + topic + " so với các giải pháp thay thế.";
            case EVALUATE -> "Trong trường hợp nào hệ thống KHÔNG nên sử dụng " + topic + "? Hãy đưa ra lập luận và dẫn chứng.";
            case CREATE -> "Hãy thiết kế một kiến trúc hoặc đề xuất giải pháp cải tiến mới tối ưu hoá dựa trên " + topic + ".";
        };
    }

    private String referenceAnswerTemplate(BloomLevel level, String topic) {
        return "1. Định nghĩa chuẩn xác về " + topic + ".\n"
                + "2. Các thành phần chính và cơ chế vận hành.\n"
                + "3. Ưu nhược điểm và ứng dụng thực tiễn phù hợp với mức " + level.getLabel() + ".";
    }
}
