package com.aives.service.generation;

import com.aives.model.enums.BloomLevel;
import com.aives.model.entity.Question;
import com.aives.model.entity.Rubric;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Local business validation remains mandatory even when the provider supports JSON Schema. */
public final class GeneratedQuestionParser {
    public List<Question> parse(String json, GenerationRequest request) throws GenerationException {
        JsonObject root = object(JsonSupport.parse(json));
        fields(root, "questions");
        JsonArray rows = array(root.get("questions"));
        if (rows.size() != request.getLevels().size()) throw invalid("AI trả sai số lượng câu hỏi.");
        Map<BloomLevel,Integer> expected = counts(request.getLevels());
        List<BloomLevel> actual = new ArrayList<>();
        List<Question> result = new ArrayList<>();
        Set<String> unique = new HashSet<>();
        for (JsonElement row : rows) {
            JsonObject node = object(row);
            fields(node, "content", "referenceAnswer", "bloomLevel", "rubrics");
            Question q = new Question();
            q.setContent(text(node, "content", 6000));
            q.setReferenceAnswer(text(node, "referenceAnswer", 12000));
            if (!unique.add(q.getContent().toLowerCase(Locale.ROOT).replaceAll("\\s+", " "))) {
                throw invalid("AI trả câu hỏi trùng nhau.");
            }
            try { q.setBloomLevel(BloomLevel.valueOf(text(node, "bloomLevel", 20))); }
            catch (IllegalArgumentException e) { throw invalid("Mức Bloom do AI trả về không hợp lệ."); }
            actual.add(q.getBloomLevel());
            JsonArray criteria = array(node.get("rubrics"));
            if (criteria.size() < 1 || criteria.size() > 10) throw invalid("Mỗi câu cần từ 1 đến 10 tiêu chí rubric.");
            BigDecimal total = BigDecimal.ZERO;
            List<Rubric> rubrics = new ArrayList<>();
            for (JsonElement criterion : criteria) {
                JsonObject rnode = object(criterion);
                fields(rnode, "criterion", "maxScore", "keywords");
                Rubric r = new Rubric();
                r.setCriterion(text(rnode, "criterion", 500));
                r.setKeywords(text(rnode, "keywords", 500));
                JsonElement score = rnode.get("maxScore");
                if (!score.isJsonPrimitive() || !score.getAsJsonPrimitive().isNumber()) throw invalid("Điểm rubric phải là số.");
                BigDecimal value = score.getAsBigDecimal();
                if (value.signum() <= 0 || value.compareTo(BigDecimal.TEN) > 0
                        || value.stripTrailingZeros().scale() > 2) throw invalid("Điểm rubric cần > 0, tối đa 10 và tối đa 2 số thập phân.");
                r.setMaxScore(value);
                r.setSortOrder(rubrics.size() + 1);
                total = total.add(value);
                rubrics.add(r);
            }
            if (total.compareTo(BigDecimal.TEN) != 0) throw invalid("Tổng điểm rubric mỗi câu AI phải bằng 10.");
            q.setRubrics(rubrics);
            result.add(q);
        }
        if (!counts(actual).equals(expected)) throw invalid("AI trả sai phân bổ mức Bloom đã chọn.");
        return result;
    }
    private static Map<BloomLevel,Integer> counts(List<BloomLevel> levels) {
        Map<BloomLevel,Integer> result = new EnumMap<>(BloomLevel.class);
        for (BloomLevel level : levels) result.put(level, result.getOrDefault(level, 0) + 1);
        return result;
    }
    private static JsonObject object(JsonElement element) throws GenerationException {
        if (element == null || !element.isJsonObject()) throw invalid("Sai cấu trúc JSON của AI.");
        return element.getAsJsonObject();
    }
    private static JsonArray array(JsonElement element) throws GenerationException {
        if (element == null || !element.isJsonArray()) throw invalid("Sai cấu trúc danh sách AI.");
        return element.getAsJsonArray();
    }
    private static void fields(JsonObject object, String... names) throws GenerationException {
        if (!object.keySet().equals(new HashSet<>(Arrays.asList(names)))) throw invalid("AI trả thiếu hoặc thừa trường dữ liệu.");
    }
    private static String text(JsonObject object, String name, int max) throws GenerationException {
        JsonElement value = object.get(name);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw invalid("Trường " + name + " không phải văn bản.");
        String result = value.getAsString().trim();
        if (result.isEmpty() || result.length() > max) throw invalid("Trường " + name + " rỗng hoặc quá dài.");
        return result;
    }
    private static GenerationException invalid(String message) { return new GenerationException(message + " Chưa lưu câu hỏi nào."); }
}
