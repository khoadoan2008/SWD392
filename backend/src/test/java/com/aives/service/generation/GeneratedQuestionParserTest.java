package com.aives.service.generation;

import com.aives.model.enums.BloomLevel;
import com.aives.model.entity.Question;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class GeneratedQuestionParserTest {
    private final GeneratedQuestionParser parser = new GeneratedQuestionParser();
    @Test public void parsesQuestionAnswerAndRubrics() throws Exception {
        List<Question> rows=parser.parse(GenerationFixtures.valid(),GenerationFixtures.request(BloomLevel.ANALYZE));
        assertEquals(1,rows.size()); assertEquals(2,rows.get(0).getRubrics().size());
        assertNotNull(rows.get(0).getReferenceAnswer());
        assertEquals(0,rows.get(0).getRubrics().get(0).getMaxScore().compareTo(new BigDecimal("4")));
    }
    @Test public void rejectsWrongQuestionCount() throws Exception {
        reject("{\"questions\":[]}");
    }
    @Test public void rejectsWrongBloomDistribution() throws Exception {
        reject(GenerationFixtures.valid().replace("ANALYZE","APPLY"));
    }
    @Test public void rejectsUnknownBloom() throws Exception {
        reject(GenerationFixtures.valid().replace("ANALYZE","INVALID"));
    }
    @Test public void rejectsEmptyAnswer() throws Exception {
        reject(GenerationFixtures.valid().replace("TCP retransmits missing data; UDP does not."," "));
    }
    @Test public void rejectsInvalidRubricTotals() throws Exception {
        reject(GenerationFixtures.valid().replace("\"maxScore\":6","\"maxScore\":5"));
    }
    @Test public void rejectsStringScoreAndNonPositiveScore() throws Exception {
        reject(GenerationFixtures.valid().replace("\"maxScore\":4","\"maxScore\":\"4\""));
        reject(GenerationFixtures.valid().replace("\"maxScore\":4","\"maxScore\":0"));
    }
    @Test public void rejectsOverPreciseScoresEvenIfTotalIsTen() throws Exception {
        reject(GenerationFixtures.valid().replace("\"maxScore\":4","\"maxScore\":4.001").replace("\"maxScore\":6","\"maxScore\":5.999"));
    }
    @Test public void rejectsAiControlledOwnershipFields() throws Exception {
        reject(GenerationFixtures.valid().replace("\"content\":","\"createdBy\":99,\"content\":"));
    }
    @Test public void rejectsDuplicateQuestions() throws Exception {
        String q=GenerationFixtures.question("Same question", "ANALYZE");
        assertThrows(GenerationException.class,()->parser.parse(GenerationFixtures.batch(q,q),GenerationFixtures.request(BloomLevel.ANALYZE,BloomLevel.ANALYZE)));
    }
    @Test public void rejectsMarkdownTrailingGarbageAndDuplicateKeys() throws Exception {
        reject("```json\n"+GenerationFixtures.valid()+"\n```");
        reject(GenerationFixtures.valid()+" garbage");
        reject(GenerationFixtures.valid().replace("\"bloomLevel\":\"ANALYZE\"","\"bloomLevel\":\"APPLY\",\"bloomLevel\":\"ANALYZE\""));
    }
    @Test public void rejectsTooDeepJson() {
        String json=String.join("",Collections.nCopies(25,"["))+"0"+String.join("",Collections.nCopies(25,"]"));
        assertThrows(GenerationException.class,()->JsonSupport.parse(json));
    }
    @Test public void validatesGenerationRequestBeforeCallingAi() {
        assertThrows(GenerationException.class,()->new GenerationRequest(1,"Subject","Topic",Collections.emptyList(),GenerationFixtures.MATERIAL));
        assertThrows(GenerationException.class,()->new GenerationRequest(1,"Subject","Topic",Arrays.asList(BloomLevel.APPLY),"tiny"));
        assertThrows(GenerationException.class,()->new GenerationRequest(1,"Subject","Topic",Collections.nCopies(13,BloomLevel.APPLY),GenerationFixtures.MATERIAL));
    }
    @Test public void acceptsJava21ImmutableLevelLists() throws Exception {
        GenerationRequest request=new GenerationRequest(1,"Subject","Topic",List.of(BloomLevel.APPLY),GenerationFixtures.MATERIAL);
        assertEquals(List.of(BloomLevel.APPLY),request.getLevels());
    }
    private void reject(String json) throws Exception {
        GenerationRequest request=GenerationFixtures.request(BloomLevel.ANALYZE);
        assertThrows(GenerationException.class,()->parser.parse(json,request));
    }
}
