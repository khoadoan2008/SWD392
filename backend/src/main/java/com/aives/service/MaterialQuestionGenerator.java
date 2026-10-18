package com.aives.service;

import com.aives.model.entity.Question;
import com.aives.service.generation.GenerationException;
import com.aives.service.generation.GenerationRequest;
import java.util.List;

/** Real provider contract: course material is mandatory. The legacy template contract stays separate. */
public interface MaterialQuestionGenerator {
    List<Question> generate(GenerationRequest request) throws GenerationException;
}
