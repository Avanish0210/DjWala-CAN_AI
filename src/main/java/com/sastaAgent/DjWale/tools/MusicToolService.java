package com.sastaAgent.DjWale.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;

import com.sastaAgent.DjWale.websoket.ServerMessage;
import org.springframework.stereotype.Service;

@Service
public class MusicToolService {
    private final ObjectMapper objectMapper;

    public MusicToolService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ToolExecution execute(String name, JsonNode args, String callId) {
        JsonNode safeArgs = args == null ? objectMapper.createObjectNode() : args;
        ServerMessage event;
        switch (name) {
            case "play_playlist" -> event = ServerMessage.play("playlist", safeArgs.path("mood").asText(""));
            case "play_track" -> event = ServerMessage.play("track", safeArgs.path("title").asText(""));
            case "skip" -> event = ServerMessage.play("skip");
            case "pause" -> event = ServerMessage.play("pause");
            default -> {
                return new ToolExecution(name, callId, null,
                        objectMapper.createObjectNode().put("result", "unknown tool: " + name));
            }
        }
        return new ToolExecution(name, callId, event,
                objectMapper.createObjectNode().put("result", "ok"));
    }

    public ObjectNode createToolDefinition() {
        ArrayNode declarations = objectMapper.createArrayNode();
        addTool(declarations, "play_playlist", "Start playing music of a given vibe or mood.",
                Map.of("mood", "The vibe to play"), "mood");
        addTool(declarations, "play_track", "Play a specific track by its title.",
                Map.of("title", "The track title"), "title");
        addTool(declarations, "skip", "Skip to the next track.", Map.of());
        addTool(declarations, "pause", "Pause or resume the music.", Map.of());
        return objectMapper.createObjectNode().set("functionDeclarations", declarations);
    }

    private void addTool(
            ArrayNode declarations,
            String name,
            String description,
            Map<String, String> properties,
            String... required
    ) {
        ObjectNode propertySchemas = objectMapper.createObjectNode();
        properties.forEach((property, help) -> propertySchemas.set(property,
                objectMapper.createObjectNode().put("type", "STRING").put("description", help)));

        ObjectNode parameters = objectMapper.createObjectNode()
                .put("type", "OBJECT")
                .set("properties", propertySchemas);
        if (required.length > 0) {
            ArrayNode requiredProperties = parameters.putArray("required");
            for (String property : required) {
                requiredProperties.add(property);
            }
        }
        declarations.add(objectMapper.createObjectNode()
                .put("name", name)
                .put("description", description)
                .set("parameters", parameters));
    }

    public record ToolExecution(
            String name,
            String callId,
            ServerMessage browserEvent,
            ObjectNode response
    ) {
    }
}
