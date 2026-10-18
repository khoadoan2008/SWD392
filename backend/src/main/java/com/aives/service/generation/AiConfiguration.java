package com.aives.service.generation;


public final class AiConfiguration {
    public static final String DEFAULT_MODEL = "gemini-3.8-flash";
    private final String apiKey;
    private final String model;
    public AiConfiguration(String apiKey, String model) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.trim().isEmpty() ? DEFAULT_MODEL : model.trim();
        if (!this.model.matches("gemini-[A-Za-z0-9._-]{1,90}")) throw new IllegalArgumentException("GEMINI_MODEL phải là tên model Gemini, không phải URL.");
    }

    public boolean isConfigured() { return !apiKey.isEmpty(); }
    public String getApiKey() { return apiKey; }
    public String getModel() { return model; }
}
