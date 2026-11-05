package com.aives.service.generation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AiConfigurationTest {
    @Test public void defaultsToGeminiAndTrimsKey() {
        AiConfiguration config=new AiConfiguration(" fake-key ",null);
        assertEquals("gemini-3.8-flash",config.getModel());
        assertEquals("fake-key",config.getApiKey()); assertTrue(config.isConfigured());
    }
    @Test public void noKeyIsNotConfigured() { assertFalse(new AiConfiguration(" ",null).isConfigured()); }
    @Test public void rejectsForeignModelsAndUrlInjection() {
        for(String model:new String[]{"gpt-4o-mini","https://evil.test/model","gemini-x?key=leak","gemini-x/../../evil"}) {
            assertThrows(IllegalArgumentException.class,()->new AiConfiguration("fake",model));
        }
    }
    @Test public void fallbackModelsDefaultParseAndCanBeDisabled() {
        assertEquals(java.util.List.of("gemini-3.5-flash", "gemini-3.6-flash", "gemini-3.5-flash-lite"), new AiConfiguration("fake", null).getFallbackModels());
        assertEquals(java.util.List.of("gemini-3.7-flash", "gemini-3.5-flash-lite"),
                new AiConfiguration("fake", null, " gemini-3.7-flash, ,gemini-3.5-flash-lite,gemini-3.7-flash ").getFallbackModels());
        assertTrue(new AiConfiguration("fake", null, "none").getFallbackModels().isEmpty());
        assertEquals(java.util.List.of("gemini-3.6-flash"), new AiConfiguration("fake", "gemini-3.5-flash", "gemini-3.5-flash,gemini-3.6-flash").getFallbackModels());
        assertThrows(IllegalArgumentException.class, () -> new AiConfiguration("fake", null, "gemini-3.5-flash,https://evil.test"));
    }
    @Test public void acceptsExplicitGeminiModel() {
        assertEquals("gemini-3.8-flash",new AiConfiguration("fake","gemini-3.8-flash").getModel());
    }
}
