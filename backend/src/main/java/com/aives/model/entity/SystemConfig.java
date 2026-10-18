package com.aives.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "system_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemConfig {

    public static final String SPEECH_LANGUAGE = "speech.language";
    public static final String ANSWER_TIME = "interview.answer_time";
    public static final String MAX_FOLLOWUPS = "interview.max_followups";
    public static final String MAIN_QUESTIONS = "interview.main_questions";

    @Id
    @Column(name = "config_key", nullable = false, length = 50)
    private String configKey;

    @Column(name = "config_value", nullable = false, length = 255)
    private String configValue;

    @Column(length = 255)
    private String description;
}
