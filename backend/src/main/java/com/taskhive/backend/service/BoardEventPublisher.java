package com.taskhive.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

/**
 * Publishes board real-time events strictly AFTER transaction commit so connected
 * clients never observe uncommitted state. Routes messages through Redis Pub/Sub
 * for multi-instance fan-out, with automatic local STOMP fallback if Redis is down.
 */
@Service
public class BoardEventPublisher {

    public static final String BOARD_EVENTS_CHANNEL = "taskhive:board-events";
    private static final Logger log = LoggerFactory.getLogger(BoardEventPublisher.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    public BoardEventPublisher(
            SimpMessagingTemplate messagingTemplate,
            ObjectMapper objectMapper,
            @Autowired(required = false) StringRedisTemplate redisTemplate
    ) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    public void publishAfterCommit(UUID boardId, String type, Object data) {
        Map<String, Object> payload = Map.of("type", type, "data", data);
        Runnable dispatch = () -> dispatch(boardId, payload);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch.run();
                }
            });
        } else {
            dispatch.run();
        }
    }

    private void dispatch(UUID boardId, Map<String, Object> payload) {
        if (redisTemplate != null) {
            try {
                String envelope = objectMapper.writeValueAsString(
                        Map.of("boardId", boardId.toString(), "payload", payload)
                );
                redisTemplate.convertAndSend(BOARD_EVENTS_CHANNEL, envelope);
                return;
            } catch (Exception ex) {
                log.warn("Redis Pub/Sub publish failed; falling back to local STOMP broker: {}", ex.getMessage());
            }
        }
        deliverLocal(boardId.toString(), payload);
    }

    public void deliverLocal(String boardId, Object payload) {
        messagingTemplate.convertAndSend("/topic/board/" + boardId, payload);
    }
}
