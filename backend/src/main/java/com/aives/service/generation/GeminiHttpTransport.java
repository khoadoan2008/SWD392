package com.aives.service.generation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

public final class GeminiHttpTransport implements AiTransport {
    interface ConnectionFactory { HttpURLConnection open(URL url) throws IOException; }
    interface Sleeper { void sleep(long millis) throws InterruptedException; }

    static final long RETRY_DELAY_MS = 2000;
    static final long MIN_ATTEMPT_MS = 5000;
    static final long MAX_ATTEMPT_MS = 60_000;
    // Frontend chờ tối đa 120 giây cho API sinh câu hỏi, nên toàn bộ các lần thử phải xong trước đó.
    static final long DEFAULT_BUDGET_MS = 100_000;
    static final int DEFAULT_PASSES = 2;
    static final long INTERACTIVE_BUDGET_MS = 20_000;

    private final List<String> models;
    private final ConnectionFactory connections;
    private final Sleeper sleeper;
    private final LongSupplier clock;
    private final int passes;
    private final long budgetMs;

    public GeminiHttpTransport(String model) { this(model, List.of()); }
    public GeminiHttpTransport(String model, List<String> fallbackModels) {
        this(model, fallbackModels, url -> (HttpURLConnection) url.openConnection(), Thread::sleep,
                System::currentTimeMillis, DEFAULT_PASSES, DEFAULT_BUDGET_MS);
    }
    GeminiHttpTransport(String model, ConnectionFactory connections) {
        this(model, List.of(), connections, millis -> { }, System::currentTimeMillis, 1, DEFAULT_BUDGET_MS);
    }
    GeminiHttpTransport(String model, List<String> fallbackModels, ConnectionFactory connections, Sleeper sleeper,
                        LongSupplier clock, int passes, long budgetMs) {
        List<String> list = new ArrayList<>();
        list.add(new AiConfiguration("", model).getModel());
        for (String fallback : fallbackModels == null ? List.<String>of() : fallbackModels) {
            String name = new AiConfiguration("", fallback).getModel();
            if (!list.contains(name)) list.add(name);
        }
        this.models = List.copyOf(list);
        this.connections = connections;
        this.sleeper = sleeper;
        this.clock = clock;
        this.passes = passes;
        this.budgetMs = budgetMs;
    }

    // Sinh viên đang chờ trong phòng thi: một vòng, ngân sách ngắn để kịp chuyển sang hỏi xoáy theo luật.
    public static GeminiHttpTransport interactive(String model, List<String> fallbackModels) {
        return new GeminiHttpTransport(model, fallbackModels, url -> (HttpURLConnection) url.openConnection(),
                millis -> { }, System::currentTimeMillis, 1, INTERACTIVE_BUDGET_MS);
    }

    List<String> models() { return models; }

    @Override public String post(String jsonBody, String apiKey) throws GenerationException {
        long deadline = clock.getAsLong() + budgetMs;
        List<Integer> statuses = new ArrayList<>();
        boolean timedOut = false;
        for (int pass = 1; pass <= passes; pass++) {
            if (pass > 1) {
                if (deadline - clock.getAsLong() < RETRY_DELAY_MS + MIN_ATTEMPT_MS) break;
                pause(RETRY_DELAY_MS);
            }
            for (String model : models) {
                long remaining = deadline - clock.getAsLong();
                if (remaining < MIN_ATTEMPT_MS) break;
                try {
                    return postOnce(model, jsonBody, apiKey, (int) Math.min(remaining, MAX_ATTEMPT_MS));
                } catch (TransientStatus failed) {
                    statuses.add(failed.status);
                } catch (SocketTimeoutException slow) {
                    timedOut = true;
                }
            }
            if (!timedOut && !statuses.isEmpty() && statuses.stream().allMatch(s -> s == 404)) break;
        }
        if (!timedOut && !statuses.isEmpty() && statuses.stream().allMatch(s -> s == 404)) {
            throw new GenerationException("Không tìm thấy model Gemini. Kiểm tra GEMINI_MODEL và quyền của key.");
        }
        if (statuses.isEmpty() && timedOut) {
            throw new GenerationException("Gemini phản hồi quá lâu; chưa lưu bản nháp. Hãy thử lại.");
        }
        if (timedOut || statuses.contains(503)) {
            throw new GenerationException("Gemini đang quá tải. Hệ thống đã thử " + models.size()
                    + " model nhưng chưa được; chưa lưu câu hỏi nào. Vui lòng thử lại sau ít phút.");
        }
        int status = statuses.isEmpty() ? 500 : statuses.get(statuses.size() - 1);
        throw new GenerationException("Dịch vụ Gemini đang lỗi (HTTP " + status + "). Chưa lưu câu hỏi nào, vui lòng thử lại sau.");
    }

    private void pause(long millis) throws GenerationException {
        try { sleeper.sleep(millis); }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GenerationException("Yêu cầu tới Gemini bị huỷ.", e);
        }
    }

    private String postOnce(String model, String jsonBody, String apiKey, int readTimeoutMs)
            throws GenerationException, TransientStatus, SocketTimeoutException {
        HttpURLConnection connection = null;
        try {
            URL endpoint = new URL("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent");
            connection = connections.open(endpoint);
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(Math.min(10000, readTimeoutMs));
            connection.setReadTimeout(readTimeoutMs);
            connection.setRequestMethod("POST"); connection.setDoOutput(true);
            connection.setRequestProperty("x-goog-api-key", apiKey);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream out = connection.getOutputStream()) { out.write(bytes); }
            int status = connection.getResponseCode();
            if (status != 200) {
                // Provider error bodies may echo the key or document text, so they are never read.
                if (status == 401 || status == 403) throw new GenerationException("Gemini key không hợp lệ hoặc chưa có quyền truy cập API/model.");
                if (status == 429) throw new GenerationException("Gemini hết hạn mức hoặc quá nhiều yêu cầu. Kiểm tra quota và thử lại sau.");
                if (status == 404) throw new TransientStatus(status);
                if (status >= 500) throw new TransientStatus(status);
                throw new GenerationException("Gemini không chấp nhận yêu cầu (HTTP " + status + "). Kiểm tra key, model và cấu hình.");
            }
            try (InputStream in = connection.getInputStream()) {
                return new String(MaterialTextExtractor.readBounded(in, 2 * 1024 * 1024), StandardCharsets.UTF_8);
            }
        } catch (SocketTimeoutException e) {
            throw e;
        } catch (IOException | IllegalArgumentException e) {
            throw new GenerationException("Không kết nối được Gemini; chưa lưu bản nháp.", e);
        } finally { if (connection != null) connection.disconnect(); }
    }

    private static final class TransientStatus extends Exception {
        final int status;
        TransientStatus(int status) { super(null, null, false, false); this.status = status; }
    }
}
