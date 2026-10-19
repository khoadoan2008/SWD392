package com.aives.service.generation;

/** Tests inject a fake transport: no paid calls and no document upload during automated tests. */
public interface AiTransport {
    String post(String jsonBody, String apiKey) throws GenerationException;
}
