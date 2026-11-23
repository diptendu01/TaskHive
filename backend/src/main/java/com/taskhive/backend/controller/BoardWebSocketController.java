package com.taskhive.backend.controller;

import com.taskhive.backend.dto.BoardDtos.CardDto;
import com.taskhive.backend.dto.BoardDtos.MoveCardRequest;
import com.taskhive.backend.repository.BoardRepository;
import com.taskhive.backend.repository.CardRepository;
import com.taskhive.backend.repository.TaskListRepository;
import com.taskhive.backend.repository.WorkspaceMemberRepository;
import com.taskhive.backend.security.Board;
import com.taskhive.backend.security.Card;
import com.taskhive.backend.security.TaskList;
import com.taskhive.backend.service.BoardEventPublisher;
import com.taskhive.backend.service.FractionalIndexService;
import com.taskhive.backend.service.FractionalIndexService.PositionResult;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Controller
public class BoardWebSocketController {

    private final CardRepository cardRepository;
    private final TaskListRepository taskListRepository;
    private final BoardRepository boardRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final FractionalIndexService fractionalIndexService;
    private final BoardEventPublisher boardEventPublisher;
    private final SimpMessagingTemplate messagingTemplate;

    public BoardWebSocketController(
            CardRepository cardRepository,
            TaskListRepository taskListRepository,
            BoardRepository boardRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            FractionalIndexService fractionalIndexService,
            BoardEventPublisher boardEventPublisher,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.cardRepository = cardRepository;
        this.taskListRepository = taskListRepository;
        this.boardRepository = boardRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.fractionalIndexService = fractionalIndexService;
        this.boardEventPublisher = boardEventPublisher;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/card/move")
    @Transactional
    public void handleCardMove(MoveCardRequest request, Principal principal) {
        if (!(principal instanceof Authentication auth) || !(auth.getPrincipal() instanceof UUID userId)) {
            return;
        }
        if (request == null || request.cardId() == null || request.newListId() == null) {
            sendError(principal, null, "Invalid move request payload.");
            return;
        }

        Card card = cardRepository.findById(request.cardId()).orElse(null);
        TaskList targetList = taskListRepository.findById(request.newListId()).orElse(null);
        if (card == null || targetList == null) {
            sendError(principal, request.cardId(), "Card or target list was not found.");
            return;
        }

        UUID sourceListId = card.getListId() != null
                ? card.getListId()
                : (card.getList() != null ? card.getList().getId() : null);
        TaskList sourceList = sourceListId != null
                ? taskListRepository.findById(sourceListId).orElse(null)
                : null;
        if (sourceList == null || !Objects.equals(sourceList.getBoardId(), targetList.getBoardId())) {
            sendError(principal, request.cardId(), "Cannot move a card across different boards.");
            return;
        }

        UUID boardId = targetList.getBoardId();
        Board board = boardId != null ? boardRepository.findById(boardId).orElse(null) : null;
        if (board == null
                || !workspaceMemberRepository.existsByWorkspaceIdAndUserId(board.getWorkspaceId(), userId)) {
            sendError(principal, request.cardId(), "You do not have permission to move cards on this board.");
            return;
        }

        try {
            List<Card> targetCards = cardRepository.findByListIdOrderByPositionAsc(targetList.getId());
            card.setList(targetList);

            PositionResult<Card> result = fractionalIndexService.computePosition(
                    targetCards,
                    card,
                    request.beforeCardId(),
                    request.afterCardId(),
                    request.newPosition(),
                    Card::getId,
                    Card::getPosition,
                    Card::setPosition
            );

            if (result.rebalanced()) {
                cardRepository.saveAll(result.orderedItems());
                List<CardDto> rebalancedDtos = result.orderedItems().stream()
                        .map(c -> toCardDto(c, targetList.getId()))
                        .toList();
                boardEventPublisher.publishAfterCommit(
                        boardId,
                        "cards:rebalanced",
                        Map.of("listId", targetList.getId(), "cards", rebalancedDtos)
                );
            } else {
                card = cardRepository.save(card);
                CardDto updatedCard = toCardDto(card, targetList.getId());
                boardEventPublisher.publishAfterCommit(
                        boardId,
                        "card:moved",
                        Map.of("cardId", card.getId(), "newCard", updatedCard)
                );
            }
        } catch (ObjectOptimisticLockingFailureException ex) {
            sendError(principal, request.cardId(), "Card was moved by another user. Syncing latest state.");
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

    private CardDto toCardDto(Card card, UUID listId) {
        return new CardDto(
                card.getId(),
                card.getTitle(),
                card.getDescription(),
                card.getPosition(),
                listId
        );
    }
}