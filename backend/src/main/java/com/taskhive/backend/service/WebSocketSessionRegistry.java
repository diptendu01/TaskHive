package com.taskhive.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks active WebSocket sessions by sessionId and authenticated userId so
 * removed workspace members are disconnected immediately.
 */
@Service
public class WebSocketSessionRegistry {

    private static final Logger log = LoggerFactory.getLogger(WebSocketSessionRegistry.class);
    private final ConcurrentHashMap<String, WebSocketSession> sessionsById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, UUID> userBySessionId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Set<String>> sessionIdsByUser = new ConcurrentHashMap<>();

    public void registerTransportSession(WebSocketSession session) {
        sessionsById.put(session.getId(), session);
    }

    public void bindUser(String sessionId, UUID userId) {
        if (sessionId == null || userId == null) {
            return;
        }
        userBySessionId.put(sessionId, userId);
        sessionIdsByUser.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
    }

    public void unregisterTransportSession(WebSocketSession session) {
        String sessionId = session.getId();
        sessionsById.remove(sessionId);
        UUID userId = userBySessionId.remove(sessionId);
        if (userId != null) {
            Set<String> ids = sessionIdsByUser.get(userId);
            if (ids != null) {
                ids.remove(sessionId);
                if (ids.isEmpty()) {
                    sessionIdsByUser.remove(userId, ids);
                }
            }
        }
    }

    public void disconnectUser(UUID userId) {
        Set<String> sessionIds = sessionIdsByUser.remove(userId);
        if (sessionIds == null) {
            return;
        }
        for (String sessionId : sessionIds) {
            userBySessionId.remove(sessionId);
            WebSocketSession session = sessionsById.remove(sessionId);
            if (session != null && session.isOpen()) {
                try {
                    session.close(CloseStatus.POLICY_VIOLATION);
                } catch (IOException ex) {
                    log.debug("Failed to close revoked WebSocket session {}: {}", sessionId, ex.getMessage());
                }
            }
        }
    }
}
