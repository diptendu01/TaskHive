package com.taskhive.backend.config;

import com.taskhive.backend.repository.BoardRepository;
import com.taskhive.backend.repository.WorkspaceMemberRepository;
import com.taskhive.backend.security.Board;
import com.taskhive.backend.security.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final String BOARD_TOPIC_PREFIX = "/topic/board/";
    private static final String SESSION_EXP_ATTR = "jwtExpiresAt";

    private final JwtService jwtService;
    private final BoardRepository boardRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final String frontendUrl;
    private final String appEnv;

    public WebSocketConfig(
            JwtService jwtService,
            BoardRepository boardRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl,
            @Value("${app.env:dev}") String appEnv
    ) {
        this.jwtService = jwtService;
        this.boardRepository = boardRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.frontendUrl = frontendUrl;
        this.appEnv = appEnv;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] allowedOrigins = "prod".equalsIgnoreCase(appEnv)
                ? new String[]{frontendUrl}
                : new String[]{frontendUrl, "http://localhost:5173", "http://localhost:3000"};

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null || accessor.getCommand() == null) {
                    return message;
                }

                StompCommand command = accessor.getCommand();
                if (StompCommand.CONNECT.equals(command)) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (authHeader == null) {
                        authHeader = accessor.getFirstNativeHeader("authorization");
                    }
                    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                        throw new MessageDeliveryException("Missing Bearer token on STOMP CONNECT");
                    }

                    String token = authHeader.substring(7);
                    if (!jwtService.isValid(token)) {
                        throw new MessageDeliveryException("Invalid or expired JWT token");
                    }

                    Claims claims = jwtService.parseClaims(token);
                    UUID userId = UUID.fromString(claims.getSubject());
                    String role = claims.get("role", String.class);
                    List<SimpleGrantedAuthority> authorities = role != null
                            ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                            : List.of();

                    Authentication auth =
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    accessor.setUser(auth);

                    Date expiration = claims.getExpiration();
                    Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
                    if (expiration != null && sessionAttrs != null) {
                        sessionAttrs.put(SESSION_EXP_ATTR, expiration.toInstant());
                    }
                } else if (StompCommand.SUBSCRIBE.equals(command) || StompCommand.SEND.equals(command)) {
                    requireSessionNotExpired(accessor);

                    if (StompCommand.SUBSCRIBE.equals(command)) {
                        String destination = accessor.getDestination();
                        if (destination != null && destination.startsWith(BOARD_TOPIC_PREFIX)) {
                            UUID userId = requireAuthenticatedUserId(accessor.getUser());
                            String rawBoardId = destination.substring(BOARD_TOPIC_PREFIX.length());
                            UUID boardId;
                            try {
                                boardId = UUID.fromString(rawBoardId);
                            } catch (IllegalArgumentException ex) {
                                throw new MessageDeliveryException("Invalid board topic destination");
                            }

                            Board board = boardRepository.findById(boardId)
                                    .orElseThrow(() -> new MessageDeliveryException("Board not found"));
                            boolean allowed = workspaceMemberRepository
                                    .existsByWorkspaceIdAndUserId(board.getWorkspaceId(), userId);
                            if (!allowed) {
                                throw new MessageDeliveryException("Forbidden board subscription");
                            }
                        }
                    }
                }

                return message;
            }
        });
    }

    private void requireSessionNotExpired(StompHeaderAccessor accessor) {
        Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
        if (sessionAttrs != null && sessionAttrs.get(SESSION_EXP_ATTR) instanceof Instant expiresAt) {
            if (Instant.now().isAfter(expiresAt)) {
                throw new MessageDeliveryException("JWT session expired. Please reconnect.");
            }
        }
    }

    private UUID requireAuthenticatedUserId(Principal principal) {
        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof UUID userId) {
            return userId;
        }
        throw new MessageDeliveryException("Authentication required");
    }
}
