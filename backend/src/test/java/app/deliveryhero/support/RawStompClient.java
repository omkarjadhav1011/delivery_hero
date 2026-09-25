package app.deliveryhero.support;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.Nullable;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * A STOMP 1.2 client over a plain WebSocket that writes and reads frames by hand, so tests control exactly which
 * frames and heartbeats are sent (API section 8.1).
 */
public final class RawStompClient extends TextWebSocketHandler implements AutoCloseable {

    /** One frame received from the server. */
    public record Frame(String command, Map<String, String> headers, String body) {}

    private final BlockingQueue<Frame> frames = new LinkedBlockingQueue<>();
    private final AtomicInteger heartbeatsReceived = new AtomicInteger();
    private final CompletableFuture<CloseStatus> closed = new CompletableFuture<>();
    private final WebSocketSession session;

    private RawStompClient(URI uri, WebSocketHttpHeaders handshakeHeaders) throws Exception {
        this.session = new StandardWebSocketClient()
                .execute(this, handshakeHeaders, uri)
                .get(5, TimeUnit.SECONDS);
    }

    public static RawStompClient open(int port) throws Exception {
        return open(port, new WebSocketHttpHeaders());
    }

    public static RawStompClient open(int port, WebSocketHttpHeaders handshakeHeaders) throws Exception {
        return new RawStompClient(URI.create("ws://localhost:" + port + "/ws"), handshakeHeaders);
    }

    /** Sends CONNECT with 10-second heartbeats both ways and the given extra headers. */
    public void connect(Map<String, String> extraHeaders) throws IOException {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("accept-version", "1.2");
        headers.put("host", "localhost");
        headers.put("heart-beat", "10000,10000");
        headers.putAll(extraHeaders);
        send("CONNECT", headers);
    }

    public void subscribe(String id, String destination) throws IOException {
        send("SUBSCRIBE", Map.of("id", id, "destination", destination));
    }

    public void sendTo(String destination, String body) throws IOException {
        send("SEND", Map.of("destination", destination, "content-type", "application/json"), body);
    }

    public void send(String command, Map<String, String> headers) throws IOException {
        send(command, headers, "");
    }

    public void send(String command, Map<String, String> headers, String body) throws IOException {
        StringBuilder frame = new StringBuilder(command).append('\n');
        headers.forEach(
                (name, value) -> frame.append(name).append(':').append(value).append('\n'));
        frame.append('\n').append(body).append('\0');
        session.sendMessage(new TextMessage(frame.toString()));
    }

    /** Sends one heartbeat, an end of line (STOMP 1.2). */
    public void heartbeat() throws IOException {
        session.sendMessage(new TextMessage("\n"));
    }

    /** The next frame, or null if none arrives within the timeout. */
    public @Nullable Frame nextFrame(Duration timeout) throws InterruptedException {
        return frames.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    public int heartbeatsReceived() {
        return heartbeatsReceived.get();
    }

    public CompletableFuture<CloseStatus> closed() {
        return closed;
    }

    public boolean isOpen() {
        return session.isOpen();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        if (payload.isBlank()) {
            heartbeatsReceived.incrementAndGet();
            return;
        }
        frames.add(parse(payload));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        closed.complete(status);
    }

    private static Frame parse(String payload) {
        String text = payload.stripLeading();
        int headerEnd = text.indexOf("\n\n");
        List<String> lines = text.substring(0, headerEnd).lines().toList();
        Map<String, String> headers = new LinkedHashMap<>();
        for (String line : lines.subList(1, lines.size())) {
            int colon = line.indexOf(':');
            headers.putIfAbsent(line.substring(0, colon), line.substring(colon + 1));
        }
        String body = text.substring(headerEnd + 2);
        int end = body.indexOf('\0');
        return new Frame(lines.get(0), headers, end >= 0 ? body.substring(0, end) : body);
    }

    @Override
    public void close() throws IOException {
        if (session.isOpen()) {
            session.close();
        }
    }
}
