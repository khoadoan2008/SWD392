package com.aives.dto.response;

public record InterviewTurnDataResponse(
        InterviewSessionResponse session,
        InterviewSessionResponse.TurnSummary currentTurn,
        InterviewSessionResponse.TurnSummary prevTurn,
        Long remainingSec,
        String speechLang
) {}
