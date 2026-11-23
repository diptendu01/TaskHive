package com.taskhive.backend.controller;

import com.taskhive.backend.dto.BoardDtos.MoveCardRequest;
import com.taskhive.backend.service.CardMoveService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@Controller
public class BoardWebSocketController {

    private final CardMoveService cardMoveService;
    private final SimpMessagingTemplate messagingTemplate;

    public BoardWebSocketController(
            CardMoveService cardMoveService,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.cardMoveService = cardMoveService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/card/move")
    public void handleCardMove(MoveCardRequest request, Principal principal) {
        if (!(principal instanceof Authentication auth) || !(auth.getPrincipal() instanceof UUID userId)) {
            return;
        }

        UUID cardId = request != null ? request.cardId() : null;
        try {
            cardMoveService.moveCard(null, request, userId);
        } catch (ObjectOptimisticLockingFailureException ex) {
            sendError(principal, cardId, "Card was moved by another user. Syncing latest state.");
        } catch (ResponseStatusException ex) {
            String reason = ex.getReason() != null ? ex.getReason() : "Unable to move card.";
            sendError(principal, cardId, reason);
        }
    }

    private void sendError(Principal principal, UUID cardId, String message) {
        Map<String, Object> errorPayload = cardId != null
                ? Map.of("type", "card:move:error", "cardId", cardId, "message", message)
                : Map.of("type", "card:move:error", "message", message);
        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/errors",
                (Object) errorPayload
        );
    }
}