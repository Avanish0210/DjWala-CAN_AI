package com.sastaAgent.DjWale.websoket;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.sastaAgent.DjWale.gemini.GeminiEventRouter;
import com.sastaAgent.DjWale.gemini.GeminiLiveClient;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

@Component
public class LiveWebSocketHandler extends AbstractWebSocketHandler {
    private final GeminiLiveClient geminiLiveClient;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public LiveWebSocketHandler(GeminiLiveClient geminiLiveClient, ObjectMapper objectMapper) {
        this.geminiLiveClient = geminiLiveClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        WebSocketSession safeSession = new ConcurrentWebSocketSessionDecorator(
                session, 10_000, 1_048_576);
        sessions.put(session.getId(), safeSession);
        geminiLiveClient.openSession(session.getId(), new GeminiEventRouter.BrowserSink() {
            @Override
            public void sendAudio(byte[] pcm24k) {
                send(safeSession, new BinaryMessage(pcm24k));
            }

            @Override
            public void sendEvent(ServerMessage event) {
                try {
                    send(safeSession, new TextMessage(objectMapper.writeValueAsString(event)));
                } catch (IOException exception) {
                    geminiLiveClient.closeSession(session.getId());
                }
            }
        });
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        ByteBuffer payload = message.getPayload().asReadOnlyBuffer();
        byte[] pcm16k = new byte[payload.remaining()];
        payload.get(pcm16k);
        geminiLiveClient.sendAudio(session.getId(), pcm16k);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            objectMapper.readValue(message.getPayload(), BrowserMessage.class);
        } catch (IOException ignored) {
            // This browser currently sends audio as binary and has no text commands.
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        close(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        close(session.getId());
    }

    private void send(WebSocketSession session, WebSocketMessage<?> message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(message);
            }
        } catch (IOException exception) {
            close(session.getId());
        }
    }

    private void close(String sessionId) {
        sessions.remove(sessionId);
        geminiLiveClient.closeSession(sessionId);
    }
}