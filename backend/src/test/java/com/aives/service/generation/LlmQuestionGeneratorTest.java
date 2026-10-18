package com.aives.service.generation;

import com.aives.model.enums.BloomLevel;
import com.aives.service.LlmQuestionGenerator;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LlmQuestionGeneratorTest {
    @Test public void sendsStructuredSchemaAndParsesFakeProviderResponse() throws Exception {
        final String[] sent={null};
        LlmQuestionGenerator generator=new LlmQuestionGenerator(new AiConfiguration("fake-test-key","gemini-2.5-flash"),(body,key)->{
            sent[0]=body; assertEquals("fake-test-key",key); return GenerationFixtures.envelope(GenerationFixtures.valid());
        });
        assertEquals(1,generator.generate(GenerationFixtures.request(BloomLevel.ANALYZE)).size());
        JsonObject request=JsonSupport.parse(sent[0]).getAsJsonObject();
        assertFalse(request.has("model")); // Native generateContent selects the model in the endpoint, not the JSON body.
        JsonObject format=request.getAsJsonObject("generationConfig").getAsJsonObject("responseFormat").getAsJsonObject("text");
        assertEquals("APPLICATION_JSON",format.get("mimeType").getAsString());
        assertFalse(format.getAsJsonObject("schema").get("additionalProperties").getAsBoolean());
        assertTrue(request.has("systemInstruction"));
        assertEquals("user",request.getAsJsonArray("contents").get(0).getAsJsonObject().get("role").getAsString());
        assertFalse(request.get("store").getAsBoolean());
        assertFalse(sent[0].contains("fake-test-key"));
        assertTrue(sent[0].contains("materialExcerpt"));
    }
    @Test public void missingApiKeyNeverInvokesTransport() throws Exception {
        LlmQuestionGenerator generator=new LlmQuestionGenerator(new AiConfiguration("",null),(body,key)->{ fail("Must not call any provider"); return ""; });
        assertThrows(GenerationException.class,()->generator.generate(GenerationFixtures.request(BloomLevel.ANALYZE)));
    }
    @Test public void refusalIsNotSavedOrTreatedAsGeneratedQuestions() throws Exception {
        String response="{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}";
        rejectEnvelope(response);
    }
    @Test public void incompleteGenerationIsRejected() throws Exception {
        rejectEnvelope(GenerationFixtures.envelope(GenerationFixtures.valid()).replace("\"STOP\"","\"MAX_TOKENS\""));
    }
    @Test public void malformedEnvelopeIsRejected() throws Exception { rejectEnvelope("{\"candidates\":[]}"); }
    @Test public void safetyFinishReasonIsRejected() throws Exception {
        rejectEnvelope(GenerationFixtures.envelope(GenerationFixtures.valid()).replace("\"STOP\"","\"SAFETY\""));
    }
    @Test public void thoughtPartsAreExcludedFromAnswer() throws Exception {
        JsonObject envelope=JsonSupport.parse(GenerationFixtures.envelope(GenerationFixtures.valid())).getAsJsonObject();
        JsonArray parts=envelope.getAsJsonArray("candidates").get(0).getAsJsonObject().getAsJsonObject("content").getAsJsonArray("parts");
        JsonObject thought=new JsonObject(); thought.addProperty("thought",true); thought.addProperty("text","Not answer JSON"); parts.add(thought);
        LlmQuestionGenerator generator=new LlmQuestionGenerator(new AiConfiguration("fake",null),(body,key)->envelope.toString());
        assertEquals(1,generator.generate(GenerationFixtures.request(BloomLevel.ANALYZE)).size());
    }
    @Test public void concatenatesMultipleAnswerTextParts() throws Exception {
        String batch=GenerationFixtures.valid();
        JsonObject envelope=JsonSupport.parse(GenerationFixtures.envelope(batch)).getAsJsonObject();
        JsonArray parts=new JsonArray();
        for (String text:new String[]{batch.substring(0,40),batch.substring(40)}) {
            JsonObject part=new JsonObject(); part.addProperty("text",text); parts.add(part);
        }
        envelope.getAsJsonArray("candidates").get(0).getAsJsonObject().getAsJsonObject("content").add("parts",parts);
        LlmQuestionGenerator generator=new LlmQuestionGenerator(new AiConfiguration("fake",null),(body,key)->envelope.toString());
        assertEquals(1,generator.generate(GenerationFixtures.request(BloomLevel.ANALYZE)).size());
    }
    @Test public void nonTextPartIsRejected() throws Exception {
        rejectEnvelope("{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":[{\"functionCall\":{}}]}}]}");
    }
    @Test public void multipleCandidatesAreRejected() throws Exception {
        JsonObject envelope=JsonSupport.parse(GenerationFixtures.envelope(GenerationFixtures.valid())).getAsJsonObject();
        envelope.getAsJsonArray("candidates").add(envelope.getAsJsonArray("candidates").get(0).deepCopy());
        rejectEnvelope(envelope.toString());
    }
    @Test public void openAiEnvelopeCannotBeMistakenForGemini() throws Exception { rejectEnvelope("{\"choices\":[{}]}"); }
    @Test public void providerFailureDoesNotFallBackToTemplates() throws Exception {
        LlmQuestionGenerator generator=new LlmQuestionGenerator(new AiConfiguration("fake",null),(body,key)->{throw new GenerationException("Timeout");});
        assertThrows(GenerationException.class,()->generator.generate(GenerationFixtures.request(BloomLevel.ANALYZE)));
    }
    @Test public void materialGeneratorCannotBeUsedAsLegacyTemplateGenerator() {
        LlmQuestionGenerator generator=new LlmQuestionGenerator(new AiConfiguration("fake",null),(body,key)->"{}");
        assertFalse(com.aives.service.QuestionGenerator.class.isInstance(generator));
    }
    @Test public void selectsBoundedTopicRelevantContext() {
        String longText=String.join(" ",java.util.Collections.nCopies(5000,"unrelated"))+" TCP congestion and retransmission are important.";
        String context=new GroundingContextSelector().select(longText,"TCP retransmission");
        assertTrue(context.length()<=GroundingContextSelector.MAX_CONTEXT_CHARS);
        assertTrue(context.contains("TCP congestion"));
    }
    private void rejectEnvelope(String response) throws Exception {
        LlmQuestionGenerator generator=new LlmQuestionGenerator(new AiConfiguration("fake",null),(body,key)->response);
        assertThrows(GenerationException.class,()->generator.generate(GenerationFixtures.request(BloomLevel.ANALYZE)));
    }
}
