package com.chickenmonitor.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LiveUpdateHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Set<WebSocketSession>> sessionsByUsername = new ConcurrentHashMap<>();

    public LiveUpdateHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (session.getPrincipal() == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }
        sessionsByUsername.computeIfAbsent(session.getPrincipal().getName(), ignored -> ConcurrentHashMap.newKeySet())
                .add(session);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                java.util.Map.of("type", "connected"))));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        if (session.getPrincipal() == null) return;
        Set<WebSocketSession> sessions = sessionsByUsername.get(session.getPrincipal().getName());
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) sessionsByUsername.remove(session.getPrincipal().getName(), sessions);
        }
    }

    public void sendToUser(String username, Object payload) {
        Set<WebSocketSession> sessions = sessionsByUsername.get(username);
        if (sessions == null || sessions.isEmpty()) return;
        try {
            String json = objectMapper.writeValueAsString(payload);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(json));
                    }
                }
            }
        } catch (Exception ignored) {
            sessions.removeIf(session -> !session.isOpen());
        }
    }
}