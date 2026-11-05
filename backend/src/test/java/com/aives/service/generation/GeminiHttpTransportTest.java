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
        assertEquals(10000,connection.getConnectTimeout()); assertEquals(90000,connection.getReadTimeout());
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
    @Test public void overloadedModelIsRetriedThenFallsBackToSecondModel() throws Exception {
        java.util.List<String> paths = new java.util.ArrayList<>();
        java.util.List<Long> waits = new java.util.ArrayList<>();
        GeminiHttpTransport transport = new GeminiHttpTransport("gemini-3.8-flash", "gemini-3.5-flash", url -> {
            paths.add(url.getPath());
            return url.getPath().contains("gemini-3.5-flash") ? fake(200, "{\"ok\":true}") : fake(503, "busy");
        }, waits::add);
        assertEquals("{\"ok\":true}", transport.post("{}", "fake"));
        assertEquals(java.util.List.of(
                "/v1beta/models/gemini-3.8-flash:generateContent",
                "/v1beta/models/gemini-3.8-flash:generateContent",
                "/v1beta/models/gemini-3.5-flash:generateContent"), paths);
        assertEquals(java.util.List.of(GeminiHttpTransport.RETRY_DELAY_MS), waits);
    }
    @Test public void transientErrorRecoversOnRetryWithoutFallback() throws Exception {
        int[] calls = {0};
        GeminiHttpTransport transport = new GeminiHttpTransport("gemini-3.8-flash", "gemini-3.5-flash",
                url -> ++calls[0] == 1 ? fake(500, "x") : fake(200, "{}"), millis -> { });
        assertEquals("{}", transport.post("{}", "fake"));
        assertEquals(2, calls[0]);
    }
    @Test public void allModelsOverloadedGivesClearMessage() {
        GeminiHttpTransport transport = new GeminiHttpTransport("gemini-3.8-flash", "gemini-3.5-flash",
                url -> fake(503, "busy"), millis -> { });
        GenerationException error = assertThrows(GenerationException.class, () -> transport.post("{}", "fake"));
        assertTrue(error.getMessage().contains("quá tải"));
        assertFalse(error.getMessage().contains("busy"));
    }
    @Test public void clientErrorsAreNotRetried() {
        int[] calls = {0};
        GeminiHttpTransport transport = new GeminiHttpTransport("gemini-3.8-flash", "gemini-3.5-flash",
                url -> { calls[0]++; return fake(429, "quota"); }, millis -> { });
        assertThrows(GenerationException.class, () -> transport.post("{}", "fake"));
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
        @Override public int getResponseCode() { return code; }
        @Override public OutputStream getOutputStream() { return sent; }
        @Override public InputStream getInputStream() throws IOException { inputRead=true; return new ByteArrayInputStream(reply); }
        @Override public InputStream getErrorStream() { throw new AssertionError("Provider errors must not be read/logged"); }
    }
}
