package com.aives.service;

import com.aives.model.entity.InterviewTurn;
import com.aives.model.entity.Rubric;

import java.util.List;

public interface FollowUpGenerator {
    String generate(String mainQuestionText, List<Rubric> rubrics, List<InterviewTurn> thread);
}
