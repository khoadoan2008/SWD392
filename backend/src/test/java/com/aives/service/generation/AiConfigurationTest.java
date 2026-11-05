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
    @Test public void fallbackModelDefaultsAndCanBeDisabled() {
        assertEquals("gemini-3.5-flash", new AiConfiguration("fake", null).getFallbackModel());
        assertEquals("gemini-3.7-flash", new AiConfiguration("fake", null, "gemini-3.7-flash").getFallbackModel());
        assertNull(new AiConfiguration("fake", null, "none").getFallbackModel());
        assertNull(new AiConfiguration("fake", "gemini-3.5-flash", "gemini-3.5-flash").getFallbackModel());
        assertThrows(IllegalArgumentException.class, () -> new AiConfiguration("fake", null, "https://evil.test"));
    }
    @Test public void acceptsExplicitGeminiModel() {
        assertEquals("gemini-3.8-flash",new AiConfiguration("fake","gemini-3.8-flash").getModel());
    }
}
