package com.aives.service.generation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Native Gemini generateContent transport. API key is a header, never a URL parameter. */
public final class GeminiHttpTransport implements AiTransport {
    interface ConnectionFactory { HttpURLConnection open(URL url) throws IOException; }
    private final String model;
    private final ConnectionFactory connections;

    public GeminiHttpTransport(String model) { this(model, url -> (HttpURLConnection) url.openConnection()); }
    GeminiHttpTransport(String model, ConnectionFactory connections) {
        this.model = new AiConfiguration("", model).getModel();
        this.connections = connections;
    }

    @Override public String post(String jsonBody, String apiKey) throws GenerationException {
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
                // Never read/log/render provider error bodies: they may echo keys or document text.
                if (status == 401 || status == 403) throw new GenerationException("Gemini key không hợp lệ hoặc chưa có quyền truy cập API/model.");
                if (status == 429) throw new GenerationException("Gemini hết hạn mức hoặc quá nhiều yêu cầu. Kiểm tra quota và thử lại sau.");
                if (status == 404) throw new GenerationException("Không tìm thấy model Gemini. Kiểm tra GEMINI_MODEL và quyền của key.");
                if (status >= 500) throw new GenerationException("Dịch vụ Gemini đang lỗi. Thử lại sau.");
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
}
