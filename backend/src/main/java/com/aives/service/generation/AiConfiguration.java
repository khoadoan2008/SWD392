package com.aives.service.generation;


public final class AiConfiguration {
    public static final String DEFAULT_MODEL = "gemini-3.8-flash";
    public static final String DEFAULT_FALLBACK_MODEL = "gemini-3.5-flash";
    private static final String MODEL_PATTERN = "gemini-[A-Za-z0-9._-]{1,90}";
    private final String apiKey;
    private final String model;
    private final String fallbackModel;
    public AiConfiguration(String apiKey, String model) { this(apiKey, model, DEFAULT_FALLBACK_MODEL); }
    public AiConfiguration(String apiKey, String model, String fallbackModel) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.trim().isEmpty() ? DEFAULT_MODEL : model.trim();
        if (!this.model.matches(MODEL_PATTERN)) throw new IllegalArgumentException("GEMINI_MODEL phải là tên model Gemini, không phải URL.");
        String fallback = fallbackModel == null ? DEFAULT_FALLBACK_MODEL : fallbackModel.trim();
        if (fallback.equalsIgnoreCase("none")) fallback = "";
        if (!fallback.isEmpty() && !fallback.matches(MODEL_PATTERN)) throw new IllegalArgumentException("GEMINI_FALLBACK_MODEL phải là tên model Gemini, không phải URL.");
        this.fallbackModel = fallback.isEmpty() || fallback.equals(this.model) ? null : fallback;
    }

    public boolean isConfigured() { return !apiKey.isEmpty(); }
    public String getApiKey() { return apiKey; }
    public String getModel() { return model; }
    public String getFallbackModel() { return fallbackModel; }
}
