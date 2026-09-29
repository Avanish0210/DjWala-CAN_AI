package com.sastaAgent.DjWale.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sastaAgent.DjWale.websoket.ServerMessage;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class GeminiLiveSession implements WebSocket.Listener, AutoCloseable {
    private static final int MAX_BUFFERED_AUDIO_CHUNKS = 150;

    private final ObjectMapper objectMapper;
    private final GeminiEventRouter eventRouter;
    private final GeminiEventRouter.BrowserSink browser;
    private final Runnable onClose;
    private final ArrayDeque<byte[]> bufferedAudio = new ArrayDeque<>();
    private final Object sendLock = new Object();
    private final StringBuilder incomingText = new StringBuilder();
    private final ByteArrayOutputStream incomingBinary = new ByteArrayOutputStream();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();

    private WebSocket socket;
    private volatile boolean setupComplete;
    private CompletableFuture<Void> sendTail = CompletableFuture.completedFuture(null);

    public GeminiLiveSession(
            ObjectMapper objectMapper,
            GeminiEventRouter eventRouter,
            GeminiEventRouter.BrowserSink browser,
            Runnable onClose
    ) {
        this.objectMapper = objectMapper;
        this.eventRouter = eventRouter;
        this.browser = browser;
        this.onClose = onClose;
    }

    public void connect(URI endpoint, ObjectNode setupMessage) {
        this.setupMessage = setupMessage;
        CompletableFuture.delayedExecutor(15, TimeUnit.SECONDS).execute(() -> {
            if (!setupComplete && !closed.get()) {
                fail("Timed out waiting for Gemini Live setup");
            }
        });
        httpClient.newWebSocketBuilder()
                .buildAsync(endpoint, this)
                .whenComplete((connected, error) -> {
                    if (error != null) {
                        fail("Could not connect to Gemini Live");
                    } else if (closed.get()) {
                        connected.sendClose(WebSocket.NORMAL_CLOSURE, "browser disconnected");
                    }
                });
    }

    private ObjectNode setupMessage;

    public void sendAudio(byte[] pcm16k) {
        synchronized (sendLock) {
            if (closed.get() || pcm16k == null || pcm16k.length == 0) {
                return;
            }
            if (!setupComplete) {
                if (bufferedAudio.size() == MAX_BUFFERED_AUDIO_CHUNKS) {
                    bufferedAudio.removeFirst();
                }
                bufferedAudio.addLast(pcm16k.clone());
                return;
            }
            sendAudioMessage(pcm16k);
        }
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        socket = webSocket;
        browser.sendEvent(ServerMessage.status("Gemini socket open; waiting for setup…"));
        sendJson(setupMessage);
        webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        synchronized (incomingText) {
            incomingText.append(data);
            if (last) {
                String json = incomingText.toString();
                incomingText.setLength(0);
                processIncomingMessage(json);
            }
        }
        webSocket.request(1);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
        synchronized (incomingText) {
            byte[] chunk = new byte[data.remaining()];
            data.get(chunk);
            incomingBinary.write(chunk, 0, chunk.length);
            if (last) {
                String json = incomingBinary.toString(StandardCharsets.UTF_8);
                incomingBinary.reset();
                processIncomingMessage(json);
            }
        }
        webSocket.request(1);
        return CompletableFuture.completedFuture(null);
    }

    private void processIncomingMessage(String json) {
        try {
            JsonNode message = objectMapper.readTree(json);
            if (message.has("setupComplete")) {
                flushBufferedAudio();
            }
            eventRouter.route(message, browser, this::sendJson);
        } catch (Exception exception) {
            fail("Could not process a Gemini Live response");
        }
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        if (closed.compareAndSet(false, true)) {
            String detail = reason == null || reason.isBlank() ? "" : ": " + reason;
            browser.sendEvent(ServerMessage.error(
                    "Gemini Live closed the connection (code " + statusCode + ")" + detail));
            synchronized (sendLock) {
                bufferedAudio.clear();
            }
            onClose.run();
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        fail("Gemini Live connection failed");
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        synchronized (sendLock) {
            bufferedAudio.clear();
        }
        WebSocket current = socket;
        if (current != null) {
            current.sendClose(WebSocket.NORMAL_CLOSURE, "browser disconnected");
        }
        onClose.run();
    }

    private void flushBufferedAudio() {
        synchronized (sendLock) {
            setupComplete = true;
            while (!bufferedAudio.isEmpty() && !closed.get()) {
                sendAudioMessage(bufferedAudio.removeFirst());
            }
        }
    }

    private void sendAudioMessage(byte[] pcm16k) {
        ObjectNode audio = objectMapper.createObjectNode()
                .put("data", Base64.getEncoder().encodeToString(pcm16k))
                .put("mimeType", "audio/pcm;rate=16000");
        sendJson(objectMapper.createObjectNode()
                .set("realtimeInput", objectMapper.createObjectNode().set("audio", audio)));
    }

    private void sendJson(ObjectNode message) {
        WebSocket current = socket;
        if (current == null || closed.get()) {
            return;
        }
        final String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (Exception exception) {
            fail("Could not encode a Gemini Live message");
            return;
        }
        synchronized (sendLock) {
            if (closed.get()) {
                return;
            }
            sendTail = sendTail.thenCompose(ignored -> current.sendText(json, true)
                    .thenApply(webSocket -> null));
            sendTail.whenComplete((ignored, error) -> {
                if (error != null) {
                    fail("Could not send data to Gemini Live");
                } else if (message.has("setup")) {
                    browser.sendEvent(ServerMessage.status(
                            "Gemini setup sent; waiting for acknowledgement…"));
                }
            });
        }
    }

    private void fail(String message) {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        browser.sendEvent(ServerMessage.error(message));
        WebSocket current = socket;
        if (current != null) {
            current.sendClose(WebSocket.NORMAL_CLOSURE, "session failed");
        }
        onClose.run();
    }

    private void closeState() {
        closed.set(true);
        synchronized (sendLock) {
            bufferedAudio.clear();
        }
        onClose.run();
    }
}
