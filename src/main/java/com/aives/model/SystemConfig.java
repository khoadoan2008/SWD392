package com.aives.model;

public class SystemConfig {

    public static final String SPEECH_LANGUAGE = "speech.language";
    public static final String ANSWER_TIME = "interview.answer_time";
    public static final String MAX_FOLLOWUPS = "interview.max_followups";
    public static final String MAIN_QUESTIONS = "interview.main_questions";

    private String key;
    private String value;
    private String description;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
