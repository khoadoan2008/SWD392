package com.aives.service.generation;

import com.aives.model.enums.BloomLevel;
import java.util.Arrays;

public final class GenerationFixtures {
    private GenerationFixtures() { }
    public static final String MATERIAL = "TCP provides reliable ordered delivery with acknowledgements and retransmission. "
            + "UDP is connectionless and does not guarantee delivery or ordering. "
            + "Use this material to compare transport protocols and their applications.";
    public static GenerationRequest request(BloomLevel... levels) throws GenerationException {
        return new GenerationRequest(1, "Computer Networks", "TCP and UDP", Arrays.asList(levels), MATERIAL);
    }
    public static String question(String content, String bloom) {
        return "{\"content\":\"" + content + "\",\"referenceAnswer\":\"TCP retransmits missing data; UDP does not.\","
                + "\"bloomLevel\":\""+bloom+"\",\"rubrics\":["
                + "{\"criterion\":\"Describe delivery\",\"maxScore\":4,\"keywords\":\"reliable, delivery\"},"
                + "{\"criterion\":\"Explain retransmission\",\"maxScore\":6,\"keywords\":\"acknowledgement, retransmission\"}]}";
    }
    public static String batch(String... questions) { return "{\"questions\":["+String.join(",",questions)+"]}"; }
    public static String valid() { return batch(question("Compare TCP with UDP", "ANALYZE")); }
    public static String envelope(String batch) {
        com.google.gson.JsonObject part = new com.google.gson.JsonObject(); part.addProperty("text",batch);
        com.google.gson.JsonArray parts = new com.google.gson.JsonArray(); parts.add(part);
        com.google.gson.JsonObject content = new com.google.gson.JsonObject(); content.addProperty("role","model"); content.add("parts",parts);
        com.google.gson.JsonObject candidate = new com.google.gson.JsonObject(); candidate.addProperty("finishReason","STOP"); candidate.add("content",content);
        com.google.gson.JsonArray candidates = new com.google.gson.JsonArray(); candidates.add(candidate);
        com.google.gson.JsonObject root = new com.google.gson.JsonObject(); root.add("candidates",candidates);
        return root.toString();
    }
}
