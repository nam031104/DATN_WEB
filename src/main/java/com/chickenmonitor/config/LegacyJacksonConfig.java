package com.chickenmonitor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LegacyJacksonConfig {

    @Bean
    public ObjectMapper mqttObjectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}