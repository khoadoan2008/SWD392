package com.aives.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Question {

    private int id;
    private int subjectId;
    private String subjectCode;
    private String topic;
    private String content;
    private String referenceAnswer;
    private BloomLevel bloomLevel;
    private QuestionStatus status;
    private QuestionSource source;
    private int createdBy;
    private String createdByName;
    private Integer reviewedBy;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private List<Rubric> rubrics = new ArrayList<>();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getSubjectId() { return subjectId; }
    public void setSubjectId(int subjectId) { this.subjectId = subjectId; }
    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getReferenceAnswer() { return referenceAnswer; }
    public void setReferenceAnswer(String referenceAnswer) { this.referenceAnswer = referenceAnswer; }
    public BloomLevel getBloomLevel() { return bloomLevel; }
    public void setBloomLevel(BloomLevel bloomLevel) { this.bloomLevel = bloomLevel; }
    public QuestionStatus getStatus() { return status; }
    public void setStatus(QuestionStatus status) { this.status = status; }
    public QuestionSource getSource() { return source; }
    public void setSource(QuestionSource source) { this.source = source; }
    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }
    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }
    public Integer getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Integer reviewedBy) { this.reviewedBy = reviewedBy; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
    public List<Rubric> getRubrics() { return rubrics; }
    public void setRubrics(List<Rubric> rubrics) { this.rubrics = rubrics; }
}
