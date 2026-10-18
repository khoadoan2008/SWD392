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

/** An in-memory URL connection; these tests make no network requests. */
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
