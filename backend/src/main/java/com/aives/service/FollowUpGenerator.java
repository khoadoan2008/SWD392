package com.aives.service;

import com.aives.model.entity.InterviewTurn;
import com.aives.model.entity.Rubric;

import java.util.List;

public interface FollowUpGenerator {
    String generate(String mainQuestionText, List<Rubric> rubrics, List<InterviewTurn> thread);

    default String generate(String mainQuestionText, List<Rubric> rubrics, List<InterviewTurn> thread, String language) {
        return generate(mainQuestionText, rubrics, thread);
    }

    static boolean isEnglish(String language) {
        return language != null && language.toLowerCase().startsWith("en");
    }
}
