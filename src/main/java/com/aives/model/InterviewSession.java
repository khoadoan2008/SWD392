package com.aives.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class InterviewSession {

    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String COMPLETED = "COMPLETED";
    public static final String ABORTED = "ABORTED";

    private int id;
    private int studentId;
    private String studentName;
    private String studentCode;
    private int subjectId;
    private String subjectCode;
    private String status;
    private int answerTimeLimitSec;
    private int maxFollowups;
    private Timestamp startedAt;
    private Timestamp endedAt;
    private List<InterviewTurn> turns = new ArrayList<>();

    // ---- Số liệu tổng hợp cho màn hình kết quả / transcript ----
    public int getMainCount() {
        int n = 0;
        for (InterviewTurn t : turns) {
            if (!t.isFollowUp()) {
                n++;
            }
        }
        return n;
    }

    public int getFollowUpCount() {
        return turns.size() - getMainCount();
    }

    public int getAnsweredCount() {
        int n = 0;
        for (InterviewTurn t : turns) {
            if (t.getAnsweredAt() != null) {
                n++;
            }
        }
        return n;
    }

    public int getTotalResponseSec() {
        int sum = 0;
        for (InterviewTurn t : turns) {
            if (t.getResponseTimeSec() != null) {
                sum += t.getResponseTimeSec();
            }
        }
        return sum;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getStudentCode() { return studentCode; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }
    public int getSubjectId() { return subjectId; }
    public void setSubjectId(int subjectId) { this.subjectId = subjectId; }
    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getAnswerTimeLimitSec() { return answerTimeLimitSec; }
    public void setAnswerTimeLimitSec(int answerTimeLimitSec) { this.answerTimeLimitSec = answerTimeLimitSec; }
    public int getMaxFollowups() { return maxFollowups; }
    public void setMaxFollowups(int maxFollowups) { this.maxFollowups = maxFollowups; }
    public Timestamp getStartedAt() { return startedAt; }
    public void setStartedAt(Timestamp startedAt) { this.startedAt = startedAt; }
    public Timestamp getEndedAt() { return endedAt; }
    public void setEndedAt(Timestamp endedAt) { this.endedAt = endedAt; }
    public List<InterviewTurn> getTurns() { return turns; }
    public void setTurns(List<InterviewTurn> turns) { this.turns = turns; }
}
