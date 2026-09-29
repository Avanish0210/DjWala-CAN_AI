package com.sastaAgent.DjWale.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Base64;

import com.sastaAgent.DjWale.tools.MusicToolService;
import com.sastaAgent.DjWale.websoket.ServerMessage;
import org.springframework.stereotype.Component;

@Component
public class GeminiEventRouter {
    private final ObjectMapper objectMapper;
    private final MusicToolService musicToolService;

    public GeminiEventRouter(ObjectMapper objectMapper, MusicToolService musicToolService) {
        this.objectMapper = objectMapper;
        this.musicToolService = musicToolService;
    }

    public void route(JsonNode message, BrowserSink browser, GeminiSender gemini) throws Exception {
        JsonNode error = message.path("error");
        if (!error.isMissingNode() && !error.isNull()) {
            String status = error.path("status").asText("");
            String detail = error.path("message").asText(error.asText(""));
            String prefix = status.isBlank() ? "Gemini Live error" : "Gemini Live error (" + status + ")";
            browser.sendEvent(ServerMessage.error(detail.isBlank() ? prefix : prefix + ": " + detail));
            return;
        }

        if (message.has("setupComplete")) {
            browser.sendEvent(ServerMessage.ready("CAN connected. Listening…"));
        }

        JsonNode serverContent = message.path("serverContent");
        routeTranscript(serverContent.path("inputTranscription"), "user", browser);
        routeTranscript(serverContent.path("outputTranscription"), "CAN", browser);

        JsonNode parts = serverContent.path("modelTurn").path("parts");
        if (parts.isArray()) {
            for (JsonNode part : parts) {
                JsonNode inlineData = part.path("inlineData");
                if (inlineData.hasNonNull("data")) {
                    browser.sendAudio(Base64.getDecoder().decode(inlineData.path("data").asText()));
                }
            }
        }

        if (serverContent.path("interrupted").asBoolean(false)) {
            browser.sendEvent(ServerMessage.interrupted());
        }

        JsonNode calls = message.path("toolCall").path("functionCalls");
        if (calls.isArray() && !calls.isEmpty()) {
            routeToolCalls(calls, browser, gemini);
        }
    }

    private void routeTranscript(JsonNode transcription, String role, BrowserSink browser) {
        String text = transcription.path("text").asText("");
        if (!text.isBlank()) {
            browser.sendEvent(ServerMessage.transcript(role, text));
        }
    }

    private void routeToolCalls(JsonNode calls, BrowserSink browser, GeminiSender gemini)
            throws Exception {
        ArrayNode responses = objectMapper.createArrayNode();
        for (JsonNode call : calls) {
            MusicToolService.ToolExecution result = musicToolService.execute(
                    call.path("name").asText(""), call.path("args"), call.path("id").asText(""));
            if (result.browserEvent() != null) {
                browser.sendEvent(result.browserEvent());
            }
            responses.add(objectMapper.createObjectNode()
                    .put("id", result.callId())
                    .put("name", result.name())
                    .set("response", result.response()));
        }
        gemini.send(objectMapper.createObjectNode()
                .set("toolResponse", objectMapper.createObjectNode()
                        .set("functionResponses", responses)));
    }

    public interface BrowserSink {
        void sendAudio(byte[] pcm24k);

        void sendEvent(ServerMessage event);
    }

    @FunctionalInterface
    public interface GeminiSender {
        void send(ObjectNode message);
    }
}
