package com.aives.service;

import com.aives.dto.response.QuestionView;
import com.aives.model.entity.Subject;
import com.aives.model.enums.BloomLevel;
import com.aives.service.generation.GenerationException;
import com.aives.service.generation.GenerationRequest;
import com.aives.service.generation.MaterialTextExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiQuestionGenerationService {
    private final QuestionAccessService access;
    private final MaterialTextExtractor extractor;
    private final MaterialQuestionGenerator generator;
    private final DraftPersistenceService persistence;
    private final GenerationGate gate;

    // Deliberately NOT transactional: do not hold a database transaction while waiting for Gemini.
    public List<QuestionView> generate(int actorId, int subjectId, String topic, List<BloomLevel> levels,
            boolean consent, MultipartFile file, String requestId) throws Exception {
        Subject subject = access.requireSubject(actorId, subjectId);
        if (!consent) throw new IllegalArgumentException("Cần đồng ý gửi học liệu đến Google Gemini.");
        if (file.isEmpty() || file.getSize() > MaterialTextExtractor.MAX_FILE_BYTES)
            throw new IllegalArgumentException("Chọn học liệu hợp lệ, tối đa 10 MB.");
        try (AutoCloseable ticket = gate.begin(actorId, requestId)) {
            GenerationRequest request;
            try (var input = file.getInputStream()) {
                request = new GenerationRequest(subjectId, subject.getName(), topic, levels,
                    extractor.extract(file.getOriginalFilename(), input));
            } catch (GenerationException error) { throw new IllegalArgumentException(error.getMessage()); }
            return persistence.saveAll(actorId, request, generator.generate(request));
        }
    }
}
