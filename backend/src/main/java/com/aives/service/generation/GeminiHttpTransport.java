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

public final class GeminiHttpTransport implements AiTransport {
    interface ConnectionFactory { HttpURLConnection open(URL url) throws IOException; }
    interface Sleeper { void sleep(long millis) throws InterruptedException; }

    static final int ATTEMPTS_PER_MODEL = 2;
    static final long RETRY_DELAY_MS = 2000;

    private final List<String> models;
    private final ConnectionFactory connections;
    private final Sleeper sleeper;

    public GeminiHttpTransport(String model) { this(model, (String) null); }
    public GeminiHttpTransport(String model, String fallbackModel) {
        this(model, fallbackModel, url -> (HttpURLConnection) url.openConnection(), Thread::sleep);
    }
    GeminiHttpTransport(String model, ConnectionFactory connections) { this(model, null, connections, millis -> { }); }
    GeminiHttpTransport(String model, String fallbackModel, ConnectionFactory connections, Sleeper sleeper) {
        List<String> list = new ArrayList<>();
        list.add(new AiConfiguration("", model).getModel());
        if (fallbackModel != null && !fallbackModel.isBlank()) {
            String fallback = new AiConfiguration("", fallbackModel).getModel();
            if (!list.contains(fallback)) list.add(fallback);
        }
        this.models = List.copyOf(list);
        this.connections = connections;
        this.sleeper = sleeper;
    }

    List<String> models() { return models; }

    @Override public String post(String jsonBody, String apiKey) throws GenerationException {
        TransientStatus last = null;
        for (String model : models) {
            for (int attempt = 1; attempt <= ATTEMPTS_PER_MODEL; attempt++) {
                try {
                    return postOnce(model, jsonBody, apiKey);
                } catch (TransientStatus busy) {
                    last = busy;
                    if (attempt < ATTEMPTS_PER_MODEL) pause(RETRY_DELAY_MS * attempt);
                }
            }
        }
        if (last != null && last.status == 503) {
            throw new GenerationException("Gemini đang quá tải (HTTP 503). Hệ thống đã tự thử lại nhưng chưa được; chưa lưu câu hỏi nào. Vui lòng thử lại sau ít phút.");
        }
        throw new GenerationException("Dịch vụ Gemini đang lỗi (HTTP " + (last == null ? 500 : last.status) + "). Chưa lưu câu hỏi nào, vui lòng thử lại sau.");
    }

    private void pause(long millis) throws GenerationException {
        try { sleeper.sleep(millis); }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GenerationException("Yêu cầu tới Gemini bị huỷ.", e);
        }
    }

    private String postOnce(String model, String jsonBody, String apiKey) throws GenerationException, TransientStatus {
        HttpURLConnection connection = null;
        try {
            URL endpoint = new URL("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent");
            connection = connections.open(endpoint);
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(90000);
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
                if (status == 404) throw new GenerationException("Không tìm thấy model Gemini. Kiểm tra GEMINI_MODEL và quyền của key.");
                if (status >= 500) throw new TransientStatus(status);
                throw new GenerationException("Gemini không chấp nhận yêu cầu (HTTP " + status + "). Kiểm tra key, model và cấu hình.");
            }
            try (InputStream in = connection.getInputStream()) {
                return new String(MaterialTextExtractor.readBounded(in, 2 * 1024 * 1024), StandardCharsets.UTF_8);
            }
        } catch (SocketTimeoutException e) {
            throw new GenerationException("Gemini phản hồi quá lâu; chưa lưu bản nháp. Hãy thử lại.", e);
        } catch (IOException | IllegalArgumentException e) {
            throw new GenerationException("Không kết nối được Gemini; chưa lưu bản nháp.", e);
        } finally { if (connection != null) connection.disconnect(); }
    }

    private static final class TransientStatus extends Exception {
        final int status;
        TransientStatus(int status) { super(null, null, false, false); this.status = status; }
    }
}
