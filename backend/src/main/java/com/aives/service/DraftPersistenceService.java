package com.aives.service;

import com.aives.dto.response.QuestionView;
import com.aives.model.entity.Question;
import com.aives.model.entity.Rubric;
import com.aives.model.entity.Subject;
import com.aives.model.entity.User;
import com.aives.model.enums.QuestionSource;
import com.aives.model.enums.QuestionStatus;
import com.aives.repository.QuestionRepository;
import com.aives.repository.RubricRepository;
import com.aives.service.generation.GenerationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DraftPersistenceService {
    private final QuestionRepository questions;
    private final RubricRepository rubricsRepo;
    private final QuestionAccessService access;

    // One separate transaction after the network request: the whole batch succeeds or rolls back.
    @Transactional
    public List<QuestionView> saveAll(int actorId, GenerationRequest request, List<Question> generated) {
        Subject subject = access.requireSubject(actorId, request.getSubjectId());
        User actor = access.actor(actorId);

        // If subject doesn't have rubrics yet, initialize them from the generated sample rubrics
        if (!generated.isEmpty() && !generated.get(0).getRubrics().isEmpty()) {
            if (rubricsRepo.findBySubjectIdOrderBySortOrderAscIdAsc(subject.getId()).isEmpty()) {
                int sort = 1;
                for (Rubric r : generated.get(0).getRubrics()) {
                    Rubric criterion = new Rubric();
                    criterion.setSubject(subject);
                    criterion.setCriterion(r.getCriterion());
                    criterion.setKeywords(r.getKeywords());
                    criterion.setMaxScore(r.getMaxScore());
                    criterion.setSortOrder(r.getSortOrder() != null ? r.getSortOrder() : sort++);
                    rubricsRepo.save(criterion);
                }
            }
        }

        List<Question> entities = new ArrayList<>();
        for (Question row : generated) {
            Question question = new Question();
            question.setSubject(subject);
            question.setCreatedBy(actor);
            question.setTopic(request.getTopic());
            question.setContent(row.getContent());
            question.setReferenceAnswer(row.getReferenceAnswer());
            question.setBloomLevel(row.getBloomLevel());
            question.setSource(QuestionSource.AI);
            question.setStatus(QuestionStatus.PENDING_REVIEW);
            question.setRubrics(row.getRubrics());
            entities.add(question);
        }
        return questions.saveAllAndFlush(entities).stream().map(QuestionView::of).toList();
    }
}
