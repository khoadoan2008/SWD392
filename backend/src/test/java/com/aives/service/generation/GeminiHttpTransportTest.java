package com.aives.service.generation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class GeminiHttpTransportTest {
    @Test public void sendsKeyInHeaderOnlyToGoogleWithoutRedirects() throws Exception {
        FakeConnection connection=new FakeConnection(200,"{\"candidates\":[]}");
        final URL[] destination={null};
        GeminiHttpTransport transport=new GeminiHttpTransport("gemini-2.5-flash",url->{destination[0]=url;return connection;});
        assertEquals("{\"candidates\":[]}",transport.post("{\"contents\":[]}","fake-secret"));
        assertEquals("https",destination[0].getProtocol());
        assertEquals("generativelanguage.googleapis.com",destination[0].getHost());
        assertEquals("/v1beta/models/gemini-2.5-flash:generateContent",destination[0].getPath());
        assertNull(destination[0].getQuery());
        assertEquals("fake-secret",connection.getRequestProperty("x-goog-api-key"));
        assertNull(connection.getRequestProperty("Authorization"));
        assertFalse(connection.getInstanceFollowRedirects());
        assertEquals("POST",connection.getRequestMethod());
        assertEquals(10000,connection.getConnectTimeout()); assertEquals(60000,connection.getReadTimeout());
        assertFalse(new String(connection.sent.toByteArray(),StandardCharsets.UTF_8).contains("fake-secret"));
        assertTrue(connection.disconnected);
    }
    @Test public void errorsDoNotReadOrExposeProviderBody() throws Exception {
        for(int status:new int[]{301,400,401,403,404,429,500,503}) {
            FakeConnection connection=new FakeConnection(status,"sensitive provider error");
            GenerationException error=assertThrows(GenerationException.class,()->new GeminiHttpTransport(null,url->connection).post("{}","fake"));
            assertFalse(error.getMessage().contains("sensitive"));
            assertFalse(connection.inputRead); assertTrue(connection.disconnected);
        }
    }
    @Test public void timeoutDoesNotRetry() throws Exception {
        final int[] attempts={0};
        GenerationException error=assertThrows(GenerationException.class,()->new GeminiHttpTransport(null,url->{attempts[0]++;throw new SocketTimeoutException();}).post("{}","fake"));
        assertEquals(1,attempts[0]); assertTrue(error.getMessage().contains("quá lâu"));
    }
    @Test public void rejectsOversizedResponse() throws Exception {
        FakeConnection connection=new FakeConnection(200,String.join("",java.util.Collections.nCopies(2*1024*1024+1,"x")));
        assertThrows(GenerationException.class,()->new GeminiHttpTransport(null,url->connection).post("{}","fake"));
        assertTrue(connection.disconnected);
    }
    private static final int TIMEOUT = -1;
    private static final List<String> FALLBACKS = List.of("gemini-3.5-flash", "gemini-3.6-flash");
    private static GeminiHttpTransport transport(GeminiHttpTransport.ConnectionFactory factory, List<Long> waits, long[] clock) {
        return new GeminiHttpTransport("gemini-3.8-flash", FALLBACKS, factory, waits::add, () -> clock[0], 2, 100_000);
    }
    @Test public void overloadedModelFallsBackToNextModelImmediately() throws Exception {
        List<String> paths = new ArrayList<>(); List<Long> waits = new ArrayList<>();
        GeminiHttpTransport t = transport(url -> {
            paths.add(url.getPath());
            return url.getPath().contains("gemini-3.5-flash") ? fake(200, "{\"ok\":true}") : fake(503, "busy");
        }, waits, new long[]{0});
        assertEquals("{\"ok\":true}", t.post("{}", "fake"));
        assertEquals(List.of("/v1beta/models/gemini-3.8-flash:generateContent", "/v1beta/models/gemini-3.5-flash:generateContent"), paths);
        assertTrue(waits.isEmpty());
    }
    @Test public void slowModelFallsBackToNextModel() throws Exception {
        List<String> paths = new ArrayList<>();
        GeminiHttpTransport t = transport(url -> {
            paths.add(url.getPath());
            if (url.getPath().contains("gemini-3.8-flash")) throw new SocketTimeoutException();
            return fake(200, "{}");
        }, new ArrayList<>(), new long[]{0});
        assertEquals("{}", t.post("{}", "fake"));
        assertEquals(2, paths.size());
    }
    @Test public void secondPassRetriesAfterShortDelay() throws Exception {
        int[] calls = {0}; List<Long> waits = new ArrayList<>();
        GeminiHttpTransport t = transport(url -> ++calls[0] <= 3 ? fake(503, "busy") : fake(200, "{}"), waits, new long[]{0});
        assertEquals("{}", t.post("{}", "fake"));
        assertEquals(4, calls[0]);
        assertEquals(List.of(GeminiHttpTransport.RETRY_DELAY_MS), waits);
    }
    @Test public void totalTimeBudgetLimitsAttemptsAndTimeouts() {
        long[] clock = {0}; List<FakeConnection> opened = new ArrayList<>();
        GeminiHttpTransport t = transport(url -> {
            FakeConnection c = fake(TIMEOUT, ""); opened.add(c);
            clock[0] += opened.size() == 1 ? 60_000 : 40_000;
            return c;
        }, new ArrayList<>(), clock);
        GenerationException error = assertThrows(GenerationException.class, () -> t.post("{}", "fake"));
        assertTrue(error.getMessage().contains("quá lâu"));
        assertEquals(2, opened.size());
        assertEquals(60_000, opened.get(0).getReadTimeout());
        assertEquals(40_000, opened.get(1).getReadTimeout());
    }
    @Test public void allModelsOverloadedGivesClearMessage() {
        GeminiHttpTransport t = transport(url -> fake(503, "busy"), new ArrayList<>(), new long[]{0});
        GenerationException error = assertThrows(GenerationException.class, () -> t.post("{}", "fake"));
        assertTrue(error.getMessage().contains("quá tải"));
        assertFalse(error.getMessage().contains("busy"));
    }
    @Test public void retiredModelIsSkippedAndAllMissingIsReported() throws Exception {
        GeminiHttpTransport t = transport(url -> url.getPath().contains("gemini-3.6-flash") ? fake(200, "{}") : fake(404, "gone"),
                new ArrayList<>(), new long[]{0});
        assertEquals("{}", t.post("{}", "fake"));
        GeminiHttpTransport missing = transport(url -> fake(404, "gone"), new ArrayList<>(), new long[]{0});
        assertTrue(assertThrows(GenerationException.class, () -> missing.post("{}", "fake")).getMessage().contains("Không tìm thấy model"));
    }
    @Test public void interactiveModeIsSinglePassWithShortBudget() {
        List<FakeConnection> opened = new ArrayList<>(); List<Long> waits = new ArrayList<>();
        GeminiHttpTransport t = new GeminiHttpTransport("gemini-3.8-flash", FALLBACKS,
                url -> { FakeConnection c = fake(503, "busy"); opened.add(c); return c; }, waits::add,
                () -> 0L, 1, GeminiHttpTransport.INTERACTIVE_BUDGET_MS);
        assertThrows(GenerationException.class, () -> t.post("{}", "fake"));
        assertEquals(3, opened.size());
        assertTrue(waits.isEmpty());
        assertEquals(20_000, opened.get(0).getReadTimeout());
    }
    @Test public void clientErrorsAreNotRetried() {
        int[] calls = {0};
        GeminiHttpTransport t = transport(url -> { calls[0]++; return fake(429, "quota"); }, new ArrayList<>(), new long[]{0});
        assertThrows(GenerationException.class, () -> t.post("{}", "fake"));
        assertEquals(1, calls[0]);
    }
    private static FakeConnection fake(int status, String body) {
        try { return new FakeConnection(status, body); } catch (Exception e) { throw new IllegalStateException(e); }
    }
    @Test public void rejectsModelPathInjectionBeforeOpeningConnection() {
        assertThrows(IllegalArgumentException.class,()->new GeminiHttpTransport("gemini-test/evil",url->{fail("Do not open");return null;}));
    }

    private static final class FakeConnection extends HttpURLConnection {
        final ByteArrayOutputStream sent=new ByteArrayOutputStream();
        final int code;
        final byte[] reply;
        boolean disconnected, inputRead;
        FakeConnection(int code,String reply) throws Exception {
            super(new URL("https://generativelanguage.googleapis.com/"));
            this.code=code; this.reply=reply.getBytes(StandardCharsets.UTF_8);
        }
        @Override public void connect() { }
        @Override public boolean usingProxy() { return false; }
        @Override public void disconnect() { disconnected=true; }
        @Override public int getResponseCode() throws IOException { if (code == TIMEOUT) throw new SocketTimeoutException(); return code; }
        @Override public OutputStream getOutputStream() { return sent; }
        @Override public InputStream getInputStream() throws IOException { inputRead=true; return new ByteArrayInputStream(reply); }
        @Override public InputStream getErrorStream() { throw new AssertionError("Provider errors must not be read/logged"); }
    }
}
