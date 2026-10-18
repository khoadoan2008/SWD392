package com.aives.service.generation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/** Strict JSON with bounded size/depth and duplicate-property rejection. */
public final class JsonSupport {
    private JsonSupport() { }
    public static JsonElement parse(String json) throws GenerationException {
        if (json == null || json.length() > 2 * 1024 * 1024) throw invalid();
        try (JsonReader reader = new JsonReader(new StringReader(json))) {
            reader.setStrictness(Strictness.STRICT);
            JsonElement value = read(reader, 0, new int[]{0});
            if (reader.peek() != JsonToken.END_DOCUMENT) throw invalid();
            return value;
        } catch (IOException | RuntimeException e) {
            throw new GenerationException("AI trả JSON không hợp lệ; chưa lưu câu hỏi nào.", e);
        }
    }
    private static JsonElement read(JsonReader reader, int depth, int[] count) throws IOException, GenerationException {
        if (depth > 20 || ++count[0] > 20000) throw invalid();
        switch (reader.peek()) {
            case BEGIN_OBJECT:
                reader.beginObject();
                JsonObject object = new JsonObject();
                Set<String> names = new HashSet<>();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (!names.add(name)) throw invalid();
                    object.add(name, read(reader, depth + 1, count));
                }
                reader.endObject(); return object;
            case BEGIN_ARRAY:
                reader.beginArray();
                JsonArray array = new JsonArray();
                while (reader.hasNext()) array.add(read(reader, depth + 1, count));
                reader.endArray(); return array;
            case STRING: return new JsonPrimitive(reader.nextString());
            case NUMBER:
                String number = reader.nextString();
                if (number.length() > 80) throw invalid();
                return new JsonPrimitive(new BigDecimal(number));
            case BOOLEAN: return new JsonPrimitive(reader.nextBoolean());
            case NULL: reader.nextNull(); return JsonNull.INSTANCE;
            default: throw invalid();
        }
    }
    private static GenerationException invalid() {
        return new GenerationException("AI trả JSON không hợp lệ; chưa lưu câu hỏi nào.");
    }
}
