package com.sastaAgent.DjWale.websoket;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServerMessage(
        String type,
        String role,
        String text,
        String action,
        String value,
        String message
) {
    public static ServerMessage transcript(String role, String text) {
        return new ServerMessage("transcript", role, text, null, null, null);
    }

    public static ServerMessage play(String action, String value) {
        return new ServerMessage("play", null, null, action, value, null);
    }

    public static ServerMessage play(String action) {
        return play(action, null);
    }

    public static ServerMessage interrupted() {
        return new ServerMessage("interrupted", null, null, null, null, null);
    }

    public static ServerMessage status(String message) {
        return new ServerMessage("status", null, null, null, null, message);
    }

    public static ServerMessage ready(String message) {
        return new ServerMessage("ready", null, null, null, null, message);
    }

    public static ServerMessage error(String message) {
        return new ServerMessage("error", null, null, null, null, message);
    }
}
