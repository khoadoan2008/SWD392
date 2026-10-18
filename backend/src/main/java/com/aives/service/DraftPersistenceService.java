package com.aives.service;

import com.aives.dto.response.QuestionView;
import com.aives.model.entity.Question;
import com.aives.model.entity.Rubric;
import com.aives.model.entity.Subject;
import com.aives.model.entity.User;
import com.aives.model.enums.QuestionSource;
import com.aives.model.enums.QuestionStatus;
import com.aives.repository.QuestionRepository;
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
    private final QuestionAccessService access;

    // One separate transaction after the network request: the whole batch succeeds or rolls back.
    @Transactional
    public List<QuestionView> saveAll(int actorId, GenerationRequest request, List<Question> generated) {
        Subject subject = access.requireSubject(actorId, request.getSubjectId());
        User actor = access.actor(actorId);
        List<Question> entities = new ArrayList<>();
        for (Question row : generated) {
            Question question = new Question();
            question.setSubject(subject); question.setCreatedBy(actor);
            question.setTopic(request.getTopic()); question.setContent(row.getContent());
            question.setReferenceAnswer(row.getReferenceAnswer()); question.setBloomLevel(row.getBloomLevel());
            question.setSource(QuestionSource.AI); question.setStatus(QuestionStatus.PENDING_REVIEW);
            for (Rubric rubric : row.getRubrics()) {
                Rubric criterion = new Rubric(); criterion.setQuestion(question);
                criterion.setCriterion(rubric.getCriterion()); criterion.setKeywords(rubric.getKeywords());
                criterion.setMaxScore(rubric.getMaxScore()); criterion.setSortOrder(question.getRubrics().size() + 1);
                question.getRubrics().add(criterion);
            }
            entities.add(question);
        }
        return questions.saveAllAndFlush(entities).stream().map(QuestionView::of).toList();
    }
}
