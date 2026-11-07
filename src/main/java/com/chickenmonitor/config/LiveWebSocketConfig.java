package com.chickenmonitor.config;

import com.chickenmonitor.ingestion.LiveUpdateHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class LiveWebSocketConfig implements WebSocketConfigurer {

    private final LiveUpdateHandler liveUpdateHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(liveUpdateHandler, "/ws/live");
    }
}