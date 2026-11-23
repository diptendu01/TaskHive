package com.taskhive.backend.service;

import com.taskhive.backend.dto.BoardDtos.CardDto;
import com.taskhive.backend.dto.BoardDtos.MoveCardRequest;
import com.taskhive.backend.repository.BoardRepository;
import com.taskhive.backend.repository.CardRepository;
import com.taskhive.backend.repository.TaskListRepository;
import com.taskhive.backend.repository.WorkspaceMemberRepository;
import com.taskhive.backend.security.Board;
import com.taskhive.backend.security.Card;
import com.taskhive.backend.security.TaskList;
import com.taskhive.backend.service.FractionalIndexService.PositionResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Isolates the @Transactional card move boundary so JPA optimistic-locking (@Version)
 * flush/commit exceptions surface to callers (including WebSocket controllers).
 */
@Service
public class CardMoveService {

    private final CardRepository cardRepository;
    private final TaskListRepository taskListRepository;
    private final BoardRepository boardRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final FractionalIndexService fractionalIndexService;
    private final BoardEventPublisher boardEventPublisher;

    public CardMoveService(
            CardRepository cardRepository,
            TaskListRepository taskListRepository,
            BoardRepository boardRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            FractionalIndexService fractionalIndexService,
            BoardEventPublisher boardEventPublisher
    ) {
        this.cardRepository = cardRepository;
        this.taskListRepository = taskListRepository;
        this.boardRepository = boardRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.fractionalIndexService = fractionalIndexService;
        this.boardEventPublisher = boardEventPublisher;
    }

    @Transactional
    public CardDto moveCard(UUID expectedBoardId, MoveCardRequest request, UUID userId) {
        if (request == null || request.cardId() == null || request.newListId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid move request payload.");
        }

        Card card = cardRepository.findById(request.cardId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found."));
        TaskList targetList = taskListRepository.findById(request.newListId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target list not found."));

        UUID sourceListId = card.getListId() != null
                ? card.getListId()
                : (card.getList() != null ? card.getList().getId() : null);
        TaskList sourceList = sourceListId != null
                ? taskListRepository.findById(sourceListId).orElse(null)
                : null;
        if (sourceList == null || !Objects.equals(sourceList.getBoardId(), targetList.getBoardId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cannot move a card across different boards.");
        }

        UUID boardId = targetList.getBoardId();
        if (expectedBoardId != null && !Objects.equals(expectedBoardId, boardId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Card does not belong to this board.");
        }

        Board board = boardId != null ? boardRepository.findById(boardId).orElse(null) : null;
        if (board == null || !workspaceMemberRepository.existsByWorkspaceIdAndUserId(board.getWorkspaceId(), userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to move cards on this board.");
        }

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
            return toCardDto(card, targetList.getId());
        }

        card = cardRepository.save(card);
        CardDto dto = toCardDto(card, targetList.getId());
        boardEventPublisher.publishAfterCommit(
                boardId,
                "card:moved",
                Map.of("cardId", card.getId(), "newCard", dto)
        );
        return dto;
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
