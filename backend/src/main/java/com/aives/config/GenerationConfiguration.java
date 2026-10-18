package com.aives.config;

import com.aives.service.LlmQuestionGenerator;
import com.aives.service.MaterialQuestionGenerator;
import com.aives.service.generation.AiConfiguration;
import com.aives.service.generation.MaterialTextExtractor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class GenerationConfiguration {
    @Bean AiConfiguration aiConfiguration(Environment environment) {
        return new AiConfiguration(environment.getProperty("GEMINI_API_KEY"), environment.getProperty("GEMINI_MODEL"));
    }
    @Bean MaterialQuestionGenerator materialQuestionGenerator(AiConfiguration configuration) {
        return new LlmQuestionGenerator(configuration);
    }
    @Bean MaterialTextExtractor materialTextExtractor() { return new MaterialTextExtractor(); }
}
