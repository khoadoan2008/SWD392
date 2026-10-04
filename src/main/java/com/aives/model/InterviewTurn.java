package com.aives.model;

import java.sql.Timestamp;

public class InterviewTurn {

    public static final String MAIN = "MAIN";
    public static final String FOLLOW_UP = "FOLLOW_UP";

    private int id;
    private int sessionId;
    private Integer questionId;
    private Integer parentTurnId;
    private String turnType;
    private int mainIndex;
    private int followupIndex;
    private String questionText;
    private String transcript;
    private Integer responseTimeSec;
    private Timestamp askedAt;
    private Timestamp answeredAt;

    public boolean isFollowUp() { return FOLLOW_UP.equals(turnType); }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getSessionId() { return sessionId; }
    public void setSessionId(int sessionId) { this.sessionId = sessionId; }
    public Integer getQuestionId() { return questionId; }
    public void setQuestionId(Integer questionId) { this.questionId = questionId; }
    public Integer getParentTurnId() { return parentTurnId; }
    public void setParentTurnId(Integer parentTurnId) { this.parentTurnId = parentTurnId; }
    public String getTurnType() { return turnType; }
    public void setTurnType(String turnType) { this.turnType = turnType; }
    public int getMainIndex() { return mainIndex; }
    public void setMainIndex(int mainIndex) { this.mainIndex = mainIndex; }
    public int getFollowupIndex() { return followupIndex; }
    public void setFollowupIndex(int followupIndex) { this.followupIndex = followupIndex; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getTranscript() { return transcript; }
    public void setTranscript(String transcript) { this.transcript = transcript; }
    public Integer getResponseTimeSec() { return responseTimeSec; }
    public void setResponseTimeSec(Integer responseTimeSec) { this.responseTimeSec = responseTimeSec; }
    public Timestamp getAskedAt() { return askedAt; }
    public void setAskedAt(Timestamp askedAt) { this.askedAt = askedAt; }
    public Timestamp getAnsweredAt() { return answeredAt; }
    public void setAnsweredAt(Timestamp answeredAt) { this.answeredAt = answeredAt; }
}
