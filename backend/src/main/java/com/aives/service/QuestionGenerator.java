package com.aives.service;

import com.aives.model.entity.Question;
import com.aives.model.entity.Subject;
import com.aives.model.enums.BloomLevel;

import java.util.List;

public interface QuestionGenerator {
    List<Question> generate(Subject subject, String topic, List<BloomLevel> levels);
}
