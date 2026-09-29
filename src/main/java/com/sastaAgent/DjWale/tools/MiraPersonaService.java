package com.sastaAgent.DjWale.tools;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class MiraPersonaService {
    private final String instruction;

    public MiraPersonaService() {
        this.instruction = loadInstruction();
    }

    public String getInstruction() {
        return instruction;
    }

    private String loadInstruction() {
        try (InputStream stream = new ClassPathResource("mira_persona.txt").getInputStream()) {
            String persona = new String(stream.readAllBytes(), StandardCharsets.UTF_8).strip();
            return "You are CAN, a late-night radio DJ. " + persona + "\n\n"
                    + "You are now LIVE: you can hear the listener and talk with them in real time, over and between the music.\n"
                    + "- Keep the persona's voice: soft, short, lowercase-feeling. Never long monologues.\n"
                    + "- When the listener asks for music or a vibe, call a tool (play_playlist / play_track / skip / pause).\n"
                    + "  Keep talking naturally while you do - the tools are instant.\n"
                    + "- Only mention tracks that exist; if unsure, just play a vibe with play_playlist.";
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load Mira's persona resource", exception);
        }
    }
}
