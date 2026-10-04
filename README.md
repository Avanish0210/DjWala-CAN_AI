# DjWale

DjWale is a real-time AI DJ application built with Java and Spring Boot. It combines a browser-based microphone experience with Gemini Live audio streaming, a custom DJ persona, and music playback controls. The app is designed to feel like a late-night radio host that listens to the user, responds in voice, and controls playlist/track actions in real time.

## Overview

This project turns a browser into a live AI music companion:

- the user clicks the microphone button
- browser audio is captured from the microphone
- audio is streamed to Gemini Live over WebSocket
- the Gemini model responds with voice output and transcript events
- the app plays the returned audio back to the user
- the AI can call music tools such as playlist, track, skip, and pause
- local music assets are played in-browser while the DJ voice remains live

The project is branded around a persona called CAN, also referenced in the project as Mira in the frontend and persona resource file.

---

## Features

### Real-time voice interaction
- Browser mic input with WebAudio and PCM capture
- WebSocket connection to Gemini Live for low-latency streaming
- Audio playback of Gemini-generated voice response
- Transcript streaming for both user and AI messages
- Interruption handling with barge-in logic while the DJ is speaking

### AI DJ persona
- Custom personality loaded from `mira_persona.txt`
- Late-night, soft, minimal, conversational DJ style
- System instruction prompts the AI to act as a live radio host
- Persona encourages short, natural responses and music-aware behavior

### Music control tools
The app exposes Gemini function-calling tools for music operations:

- `play_playlist` — start a vibe or mood-based playlist
- `play_track` — play a track by title
- `skip` — move to the next track
- `pause` — pause or resume music playback

These tool calls are routed through `MusicToolService` and translated into browser playback actions.

### Browser UI
- Minimal single-page interface served from `src/main/resources/static`
- Animated DJ orb with listening, thinking, talking, and idle states
- Live status messages and transcript panel
- Embedded audio player for local music files
- Track queue logic with automatic looping and “now playing” display

### Backend services
- Spring Boot application startup with configuration binding
- WebSocket endpoint at `/ws`
- Gemini session lifecycle management
- CORS/origin configuration for local browser access
- JSON message handling with Jackson

---

## Tech Stack

### Backend
- Java 21
- Spring Boot 4.1.1
- Spring Web MVC
- Spring WebSocket
- Jackson Databind
- Maven

### Frontend
- Vanilla HTML, CSS, and JavaScript
- Web Audio API
- MediaStream audio capture
- Browser WebSocket client
- HTML5 audio playback

### AI and real-time streaming
- Google Gemini Live API
- WebSocket endpoint for bidirectional audio and tool calls
- Real-time audio generation with text and voice modalities

### Project configuration
- Spring Boot `@ConfigurationProperties`
- environment-based config for API key and runtime settings
- local asset-based track library

---

## Architecture

The project is organized around a small real-time streaming architecture:

- `DjWaleApplication` boots the Spring app
- `WebSocketConfig` registers the live socket endpoint
- `LiveWebSocketHandler` handles browser socket connections and binary/audio messages
- `GeminiLiveClient` creates and manages Gemini Live sessions
- `GeminiEventRouter` handles model responses, transcripts, audio chunks, and tool calls
- `MusicToolService` defines executable music actions
- `MiraPersonaService` loads and injects the DJ persona instructions
- static front-end files serve the UI and local audio assets

---

## Project Structure

```text
DjWale/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── sastaAgent/
│   │   │           └── DjWale/
│   │   │               ├── config/
│   │   │               ├── gemini/
│   │   │               ├── tools/
│   │   │               ├── websoket/
│   │   │               └── DjWaleApplication.java
│   │   ├── resources/
│   │   │   ├── application.properties
│   │   │   ├── application.yml
│   │   │   ├── mira_persona.txt
│   │   │   ├── static/
│   │   │   │   ├── index.html
│   │   │   │   ├── main.js
│   │   │   │   ├── pcm-processor.js
│   │   │   │   └── assets/
│   │   │   │       ├── tracks.json
│   │   │   │       └── tracks/
│   │   │   └── templates/
│   │   └── test/
│   │       └── java/
│   │           └── com/
│   │               └── sastaAgent/
│   │                   └── DjWale/
│   └── target/
├── pom.xml
├── mvnw
├── mvnw.cmd
├── HELP.md
└── README.md
```

---

## Requirements and Specifications

### Runtime
- Java 21
- Maven 3.x
- A browser with microphone access
- Internet access for Gemini Live API access

### Environment variables
The app reads configuration from `application.properties` and environment variables.

```bash
GOOGLE_API_KEY=your_google_api_key
LIVE_MODEL=gemini-3.8-live
LIVE_VOICE=Aoede
GEMINI_LIVE_ENDPOINT=wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent
```

### Allowed origins
The app is configured to allow local browser origins:

- `http://localhost:8080`
- `http://127.0.0.1:8080`

---

## Running the Project

### 1. Clone the repository

```bash
git clone <repository-url>
cd DjWale
```

### 2. Configure environment variables

Set your Gemini API key before running the app:

```bash
export GOOGLE_API_KEY="your_api_key_here"
```

On Windows PowerShell:

```powershell
$env:GOOGLE_API_KEY="your_api_key_here"
```

### 3. Build the app

```bash
./mvnw clean package
```

### 4. Run the Spring Boot application

```bash
./mvnw spring-boot:run
```

Then open:

```text
http://localhost:8080
```

### 5. Use the app
1. Click the microphone button
2. Allow microphone access in the browser
3. Talk to the AI DJ
4. Ask for a vibe, a track, or a playlist
5. The AI can respond with voice and trigger music actions in real time

---

## How It Works

1. The browser opens a WebSocket to `/ws`
2. `LiveWebSocketHandler` attaches a session and opens a Gemini Live session
3. `GeminiLiveClient` sends a setup message with:
   - model name
   - voice configuration
   - system prompt
   - function declarations for music tools
4. The browser streams microphone PCM data to the server
5. Gemini responds with generated speech and optional tool calls
6. The app converts audio chunks to browser playback and routes tool calls to music actions
7. The UI updates transcript and playback state in real time

---

## Notes and Current Scope

This version is a focused prototype and demonstrates the live AI DJ concept rather than a full production music platform. It includes:

- live browser audio input
- voice-based AI DJ interaction
- local music playback controls
- custom persona-based behavior
- real-time tool-call integration with Gemini

It is well suited for experimentation, demos, and further extension with:

- richer music catalog management
- user account or queue persistence
- additional AI tools and commands
- more polished UI/UX
- deployment to cloud hosting

---

## License

This project does not currently declare a specific license in `pom.xml`.

---

## Summary

DjWale is a Java + Spring Boot voice-driven DJ assistant that brings together Gemini Live, WebSocket audio streaming, browser mic access, and music playback orchestration in one compact runtime. It is designed as a real-time AI radio experience with a custom DJ personality and music control capabilities.
