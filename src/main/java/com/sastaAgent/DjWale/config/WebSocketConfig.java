package com.sastaAgent.DjWale.config;

import com.sastaAgent.DjWale.websoket.LiveWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final LiveWebSocketHandler handler;
    private final LiveDjProperties properties;

    public WebSocketConfig(LiveWebSocketHandler handler, LiveDjProperties properties) {
        this.handler = handler;
        this.properties = properties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws")
                .setAllowedOrigins(properties.getAllowedOrigins().toArray(String[]::new));
    }
}
