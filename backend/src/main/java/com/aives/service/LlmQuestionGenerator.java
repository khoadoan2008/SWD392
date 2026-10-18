package com.aives.service;

import com.aives.model.enums.BloomLevel;
import com.aives.model.entity.Question;
import com.aives.service.generation.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import java.util.List;

public final class LlmQuestionGenerator implements MaterialQuestionGenerator {
    private final AiConfiguration configuration;
    private final AiTransport transport;
    public LlmQuestionGenerator(AiConfiguration configuration) { this(configuration, new GeminiHttpTransport(configuration.getModel())); }
    public LlmQuestionGenerator(AiConfiguration configuration, AiTransport transport) {
        this.configuration=configuration; this.transport=transport;
    }
    @Override
    public List<Question> generate(GenerationRequest request) throws GenerationException {
        if (!configuration.isConfigured()) throw new GenerationException("Chưa cấu hình GEMINI_API_KEY trên máy chủ. Không có câu hỏi nào được tạo.");
        String raw = transport.post(new QuestionPromptBuilder().build(request), configuration.getApiKey());
        try {
            JsonElement parsed = JsonSupport.parse(raw);
            JsonObject envelope = parsed.getAsJsonObject();
            if (envelope.has("promptFeedback") && envelope.getAsJsonObject("promptFeedback").has("blockReason")) {
                String reason = envelope.getAsJsonObject("promptFeedback").get("blockReason").getAsString();
                if (!"BLOCK_REASON_UNSPECIFIED".equals(reason)) {
                    throw new GenerationException("Gemini từ chối xử lý học liệu này. Chưa lưu câu hỏi nào.");
                }
            }
            JsonArray candidates = envelope.getAsJsonArray("candidates");
            if (candidates == null || candidates.size() != 1) throw new GenerationException("Gemini không trả đúng một kết quả. Chưa lưu câu hỏi nào.");
            JsonObject candidate = candidates.get(0).getAsJsonObject();
            if (!"STOP".equals(candidate.get("finishReason").getAsString())) {
                throw new GenerationException("Gemini trả kết quả bị chặn hoặc chưa hoàn chỉnh. Hãy giảm số câu/kiểm tra học liệu và thử lại.");
            }
            JsonArray parts = candidate.getAsJsonObject("content").getAsJsonArray("parts");
            StringBuilder content = new StringBuilder();
            for (JsonElement element : parts) {
                JsonObject part = element.getAsJsonObject();
                if (part.has("thought") && part.get("thought").getAsBoolean()) continue;
                if (!part.has("text") || !part.get("text").isJsonPrimitive()
                        || !part.get("text").getAsJsonPrimitive().isString()) {
                    throw new GenerationException("Gemini trả nội dung không phải văn bản JSON. Chưa lưu câu hỏi nào.");
                }
                content.append(part.get("text").getAsString());
            }
            return new GeneratedQuestionParser().parse(content.toString(), request);
        } catch (RuntimeException e) {
            throw new GenerationException("Phản hồi dịch vụ AI sai cấu trúc; chưa lưu câu hỏi nào.", e);
        }
    }
}
