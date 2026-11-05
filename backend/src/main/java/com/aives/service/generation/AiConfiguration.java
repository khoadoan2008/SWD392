package com.aives.service.generation;

import java.util.ArrayList;
import java.util.List;

public final class AiConfiguration {
    public static final String DEFAULT_MODEL = "gemini-3.8-flash";
    public static final String DEFAULT_FALLBACK_MODELS = "gemini-3.5-flash,gemini-3.6-flash,gemini-3.5-flash-lite";
    private static final String MODEL_PATTERN = "gemini-[A-Za-z0-9._-]{1,90}";
    private final String apiKey;
    private final String model;
    private final List<String> fallbackModels;

    public AiConfiguration(String apiKey, String model) { this(apiKey, model, DEFAULT_FALLBACK_MODELS); }
    public AiConfiguration(String apiKey, String model, String fallbackModels) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.trim().isEmpty() ? DEFAULT_MODEL : model.trim();
        if (!this.model.matches(MODEL_PATTERN)) throw new IllegalArgumentException("GEMINI_MODEL phải là tên model Gemini, không phải URL.");
        String raw = fallbackModels == null ? DEFAULT_FALLBACK_MODELS : fallbackModels.trim();
        List<String> list = new ArrayList<>();
        if (!raw.equalsIgnoreCase("none")) {
            for (String part : raw.split(",")) {
                String name = part.trim();
                if (name.isEmpty()) continue;
                if (!name.matches(MODEL_PATTERN)) throw new IllegalArgumentException("GEMINI_FALLBACK_MODEL phải là tên model Gemini, không phải URL.");
                if (!name.equals(this.model) && !list.contains(name)) list.add(name);
            }
        }
        this.fallbackModels = List.copyOf(list);
    }

    public boolean isConfigured() { return !apiKey.isEmpty(); }
    public String getApiKey() { return apiKey; }
    public String getModel() { return model; }
    public List<String> getFallbackModels() { return fallbackModels; }
}
