package com.aives.service.generation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Bounded lexical passage selection; not an embedding/vector database implementation. */
public final class GroundingContextSelector {
    public static final int MAX_CONTEXT_CHARS = 12000;
    public String select(String text, String topic) {
        if (text.length() <= MAX_CONTEXT_CHARS) return text;
        Set<String> terms = new HashSet<>();
        for (String term : topic.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (term.length() >= 3) terms.add(term);
        }
        List<Passage> candidates = new ArrayList<>();
        for (int start = 0, number = 1; start < text.length(); start += 1650, number++) {
            String body = text.substring(start, Math.min(start + 1800, text.length()));
            String lower = body.toLowerCase(Locale.ROOT);
            int score = 0;
            for (String term : terms) if (lower.contains(term)) score++;
            candidates.add(new Passage(number, score, body));
        }
        candidates.sort(Comparator.comparingInt((Passage p) -> p.score).reversed().thenComparingInt(p -> p.number));
        List<Passage> selected = new ArrayList<>(candidates.subList(0, Math.min(6, candidates.size())));
        selected.sort(Comparator.comparingInt(p -> p.number));
        StringBuilder result = new StringBuilder();
        for (Passage p : selected) result.append("\n[Đoạn ").append(p.number).append("]\n").append(p.body).append('\n');
        return result.toString();
    }
    private static final class Passage {
        final int number, score;
        final String body;
        Passage(int number, int score, String body) { this.number=number; this.score=score; this.body=body; }
    }
}
