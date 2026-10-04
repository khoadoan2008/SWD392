package com.aives.model;

import java.math.BigDecimal;

public class Rubric {

    private int id;
    private int questionId;
    private String criterion;
    private String keywords;
    private BigDecimal maxScore;
    private int sortOrder;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getQuestionId() { return questionId; }
    public void setQuestionId(int questionId) { this.questionId = questionId; }
    public String getCriterion() { return criterion; }
    public void setCriterion(String criterion) { this.criterion = criterion; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public BigDecimal getMaxScore() { return maxScore; }
    public void setMaxScore(BigDecimal maxScore) { this.maxScore = maxScore; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
