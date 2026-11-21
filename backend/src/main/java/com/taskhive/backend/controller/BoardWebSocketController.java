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
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Controller
public class BoardWebSocketController {

    private final CardRepository cardRepository;
    private final TaskListRepository taskListRepository;
    private final BoardRepository boardRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public BoardWebSocketController(
            CardRepository cardRepository,
            TaskListRepository taskListRepository,
            BoardRepository boardRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.cardRepository = cardRepository;
        this.taskListRepository = taskListRepository;
        this.boardRepository = boardRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/card/move")
    @Transactional
    public void handleCardMove(MoveCardRequest request, Principal principal) {
        if (!(principal instanceof Authentication auth) || !(auth.getPrincipal() instanceof UUID userId)) {
            return;
        }
        if (request == null || request.cardId() == null || request.newListId() == null) {
            return;
        }

        Card card = cardRepository.findById(request.cardId()).orElse(null);
        TaskList targetList = taskListRepository.findById(request.newListId()).orElse(null);
        if (card == null || targetList == null) {
            return;
        }

        UUID sourceListId = card.getListId() != null
                ? card.getListId()
                : (card.getList() != null ? card.getList().getId() : null);
        TaskList sourceList = sourceListId != null
                ? taskListRepository.findById(sourceListId).orElse(null)
                : null;
        if (sourceList == null || !Objects.equals(sourceList.getBoardId(), targetList.getBoardId())) {
            return;
        }

        UUID boardId = targetList.getBoardId();
        Board board = boardId != null ? boardRepository.findById(boardId).orElse(null) : null;
        if (board == null
                || !workspaceMemberRepository.existsByWorkspaceIdAndUserId(board.getWorkspaceId(), userId)) {
            return;
        }

        card.setList(targetList);
        card.setPosition(request.newPosition());
        card = cardRepository.save(card);

        CardDto updatedCard = new CardDto(
                card.getId(),
                card.getTitle(),
                card.getDescription(),
                card.getPosition(),
                targetList.getId()
        );

        messagingTemplate.convertAndSend(
                "/topic/board/" + boardId,
                (Object) Map.of(
                        "type", "card:moved",
                        "data", Map.of("cardId", card.getId(), "newCard", updatedCard)
                )
        );
    }
}