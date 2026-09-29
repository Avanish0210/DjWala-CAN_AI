package com.sastaAgent.DjWale.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.sastaAgent.DjWale.config.LiveDjProperties;
import com.sastaAgent.DjWale.tools.MiraPersonaService;
import com.sastaAgent.DjWale.tools.MusicToolService;
import com.sastaAgent.DjWale.websoket.ServerMessage;
import org.springframework.stereotype.Service;

@Service
public class GeminiLiveClient {
    private final ObjectMapper objectMapper;
    private final LiveDjProperties properties;
    private final MiraPersonaService personaService;
    private final MusicToolService musicToolService;
    private final GeminiEventRouter eventRouter;
    private final ConcurrentMap<String, GeminiLiveSession> sessions = new ConcurrentHashMap<>();

    public GeminiLiveClient(
            ObjectMapper objectMapper,
            LiveDjProperties properties,
            MiraPersonaService personaService,
            MusicToolService musicToolService,
            GeminiEventRouter eventRouter
    ) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.personaService = personaService;
        this.musicToolService = musicToolService;
        this.eventRouter = eventRouter;
    }

    public void openSession(String sessionId, GeminiEventRouter.BrowserSink browser) {
        browser.sendEvent(ServerMessage.status("Opening Gemini Live connection…"));
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            browser.sendEvent(ServerMessage.error("GOOGLE_API_KEY is not configured"));
            return;
        }

        GeminiLiveSession session = new GeminiLiveSession(
                objectMapper, eventRouter, browser, () -> sessions.remove(sessionId));
        GeminiLiveSession previous = sessions.put(sessionId, session);
        if (previous != null) {
            previous.close();
        }

        try {
            String key = URLEncoder.encode(properties.getApiKey(), StandardCharsets.UTF_8);
            URI endpoint = URI.create(properties.getEndpoint() + "?key=" + key);
            session.connect(endpoint, createSetupMessage());
        } catch (RuntimeException exception) {
            sessions.remove(sessionId, session);
            session.close();
            browser.sendEvent(ServerMessage.error("Could not configure Gemini Live connection"));
        }
    }

    public void sendAudio(String sessionId, byte[] pcm16k) {
        GeminiLiveSession session = sessions.get(sessionId);
        if (session != null) {
            session.sendAudio(pcm16k);
        }
    }

    public void closeSession(String sessionId) {
        GeminiLiveSession session = sessions.remove(sessionId);
        if (session != null) {
            session.close();
        }
    }

    private ObjectNode createSetupMessage() {
        ObjectNode setup = objectMapper.createObjectNode()
                .put("model", properties.getModel().startsWith("models/")
                        ? properties.getModel() : "models/" + properties.getModel());
        ObjectNode generationConfig = setup.putObject("generationConfig");
        generationConfig.putArray("responseModalities").add("AUDIO");
        generationConfig.set("speechConfig", objectMapper.createObjectNode()
            .set("voiceConfig", objectMapper.createObjectNode()
                .set("prebuiltVoiceConfig", objectMapper.createObjectNode()
                    .put("voiceName", properties.getVoice()))));
        setup.set("systemInstruction", objectMapper.createObjectNode()
            .set("parts", objectMapper.createArrayNode()
                .add(objectMapper.createObjectNode().put("text", personaService.getInstruction()))));
        setup.set("inputAudioTranscription", objectMapper.createObjectNode());
        setup.set("outputAudioTranscription", objectMapper.createObjectNode());
        ArrayNode tools = objectMapper.createArrayNode().add(musicToolService.createToolDefinition());
        setup.set("tools", tools);
        return objectMapper.createObjectNode().set("setup", setup);
    }
}
