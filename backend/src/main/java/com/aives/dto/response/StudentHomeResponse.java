package com.aives.dto.response;

import java.util.List;

public record StudentHomeResponse(
        List<SubjectResponse> subjects,
        List<InterviewSessionResponse> history
) {}
