package com.sastaAgent.DjWale.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiJacksonConfig {
    @Bean
    public com.fasterxml.jackson.databind.ObjectMapper geminiObjectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper();
    }
}
