package com.taskhive.backend.config;

import com.taskhive.backend.service.BoardEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Subscribes to Redis Pub/Sub board events and fans them out to local STOMP WebSocket
 * subscribers when app.redis.pubsub.enabled=true.
 */
@Configuration
@ConditionalOnProperty(name = "app.redis.pubsub.enabled", havingValue = "true")
public class RedisPubSubConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisPubSubConfig.class);

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            BoardEventPublisher boardEventPublisher,
            ObjectMapper objectMapper
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setErrorHandler(err -> log.warn("Redis Pub/Sub listener error: {}", err.getMessage()));

        MessageListener listener = (message, pattern) -> {
            try {
                String json = new String(message.getBody(), StandardCharsets.UTF_8);
                Map<String, Object> envelope = objectMapper.readValue(
                        json,
                        new TypeReference<Map<String, Object>>() {}
                );
                Object boardId = envelope.get("boardId");
                Object payload = envelope.get("payload");
                if (boardId != null && payload != null) {
                    boardEventPublisher.deliverLocal(boardId.toString(), payload);
                }
            } catch (Exception ex) {
                log.warn("Failed to relay Redis board event to STOMP: {}", ex.getMessage());
            }
        };

        container.addMessageListener(listener, new ChannelTopic(BoardEventPublisher.BOARD_EVENTS_CHANNEL));
        return container;
    }
}
