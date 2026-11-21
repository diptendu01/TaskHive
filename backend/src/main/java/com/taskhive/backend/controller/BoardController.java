package com.taskhive.backend.controller;

import com.taskhive.backend.dto.BoardDtos.BoardDetailDto;
import com.taskhive.backend.dto.BoardDtos.BoardSummaryDto;
import com.taskhive.backend.dto.BoardDtos.CardDto;
import com.taskhive.backend.dto.BoardDtos.CreateBoardRequest;
import com.taskhive.backend.dto.BoardDtos.CreateCardRequest;
import com.taskhive.backend.dto.BoardDtos.CreateListRequest;
import com.taskhive.backend.dto.BoardDtos.MoveCardRequest;
import com.taskhive.backend.dto.BoardDtos.TaskListDto;
import com.taskhive.backend.repository.BoardRepository;
import com.taskhive.backend.repository.CardRepository;
import com.taskhive.backend.repository.TaskListRepository;
import com.taskhive.backend.repository.UserRepository;
import com.taskhive.backend.repository.WorkspaceMemberRepository;
import com.taskhive.backend.repository.WorkspaceRepository;
import com.taskhive.backend.security.Board;
import com.taskhive.backend.security.Card;
import com.taskhive.backend.security.Role;
import com.taskhive.backend.security.TaskList;
import com.taskhive.backend.security.User;
import com.taskhive.backend.security.Workspace;
import com.taskhive.backend.security.WorkspaceMember;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/boards")
public class BoardController {

        private final BoardRepository boardRepository;
        private final TaskListRepository taskListRepository;
        private final CardRepository cardRepository;
        private final WorkspaceRepository workspaceRepository;
        private final WorkspaceMemberRepository workspaceMemberRepository;
        private final UserRepository userRepository;
        private final SimpMessagingTemplate messagingTemplate;

        public BoardController(
                        BoardRepository boardRepository,
                        TaskListRepository taskListRepository,
                        CardRepository cardRepository,
                        WorkspaceRepository workspaceRepository,
                        WorkspaceMemberRepository workspaceMemberRepository,
                        UserRepository userRepository,
                        SimpMessagingTemplate messagingTemplate) {
                this.boardRepository = boardRepository;
                this.taskListRepository = taskListRepository;
                this.cardRepository = cardRepository;
                this.workspaceRepository = workspaceRepository;
                this.workspaceMemberRepository = workspaceMemberRepository;
                this.userRepository = userRepository;
                this.messagingTemplate = messagingTemplate;
        }

        @GetMapping
        @Transactional(readOnly = true)
        public List<BoardSummaryDto> listBoards(Authentication authentication) {
                UUID userId = requireUserId(authentication);
                List<UUID> workspaceIds = workspaceMemberRepository.findByUserId(userId)
                                .stream()
                                .map(WorkspaceMember::getWorkspaceId)
                                .toList();

                if (workspaceIds.isEmpty()) {
                        return List.of();
                }

                return boardRepository.findByWorkspaceIdIn(workspaceIds)
                                .stream()
                                .map(this::toSummaryDto)
                                .toList();
        }

        @PostMapping
        @Transactional
        public ResponseEntity<BoardSummaryDto> createBoard(
                        @Valid @RequestBody CreateBoardRequest request,
                        Authentication authentication) {
                UUID userId = requireUserId(authentication);
                Workspace workspace = workspaceMemberRepository.findByUserId(userId)
                                .stream()
                                .findFirst()
                                .map(WorkspaceMember::getWorkspace)
                                .orElseGet(() -> createDefaultWorkspace(userId));

                Board board = new Board();
                board.setName(request.name().trim());
                board.setSlug("board-" + UUID.randomUUID().toString().substring(0, 8));
                board.setAvatarColor(request.avatarColor() != null && !request.avatarColor().isBlank()
                                ? request.avatarColor().trim()
                                : "#14b8a6");
                board.setWorkspace(workspace);
                board = boardRepository.save(board);

                createDefaultList(board, "To Do", 1000.0);
                createDefaultList(board, "In Progress", 2000.0);
                createDefaultList(board, "Done", 3000.0);

                return ResponseEntity.status(HttpStatus.CREATED).body(toSummaryDto(board));
        }

        @GetMapping("/{boardId}")
        @Transactional(readOnly = true)
        public BoardDetailDto getBoard(@PathVariable UUID boardId, Authentication authentication) {
                UUID userId = requireUserId(authentication);
                Board board = requireAuthorizedBoard(boardId, userId);

                List<TaskList> lists = taskListRepository.findByBoardIdOrderByPositionAsc(boardId);
                List<UUID> listIds = lists.stream().map(TaskList::getId).toList();
                List<Card> cards = listIds.isEmpty()
                                ? List.of()
                                : cardRepository.findByListIdInOrderByPositionAsc(listIds);

                return new BoardDetailDto(
                                board.getId(),
                                board.getName(),
                                board.getSlug(),
                                board.getAvatarColor(),
                                lists.stream().map(this::toListDto).toList(),
                                cards.stream().map(this::toCardDto).toList());
        }

        @PostMapping("/{boardId}/lists")
        @Transactional
        public ResponseEntity<TaskListDto> createList(
                        @PathVariable UUID boardId,
                        @Valid @RequestBody CreateListRequest request,
                        Authentication authentication) {
                UUID userId = requireUserId(authentication);
                Board board = requireAuthorizedBoard(boardId, userId);

                List<TaskList> existing = taskListRepository.findByBoardIdOrderByPositionAsc(boardId);
                double nextPos = existing.isEmpty() ? 1000.0 : existing.get(existing.size() - 1).getPosition() + 1000.0;

                TaskList list = createDefaultList(board, request.title().trim(), nextPos);
                TaskListDto dto = toListDto(list);
                messagingTemplate.convertAndSend(
                                "/topic/board/" + boardId,
                                (Object) Map.of("type", "list:created", "data", Map.of("list", dto)));
                return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        }

        @PostMapping("/{boardId}/cards")
        @Transactional
        public ResponseEntity<CardDto> createCard(
                        @PathVariable UUID boardId,
                        @Valid @RequestBody CreateCardRequest request,
                        Authentication authentication) {
                UUID userId = requireUserId(authentication);
                requireAuthorizedBoard(boardId, userId);

                TaskList list = taskListRepository.findById(request.listId())
                                .filter(l -> boardId.equals(l.getBoardId()))
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "List not found on this board"));

                List<Card> existing = cardRepository.findByListIdOrderByPositionAsc(list.getId());
                double nextPos = existing.isEmpty() ? 1000.0 : existing.get(existing.size() - 1).getPosition() + 1000.0;

                Card card = new Card();
                card.setTitle(request.title().trim());
                card.setDescription(request.description());
                card.setPosition(nextPos);
                card.setList(list);
                card = cardRepository.save(card);

                CardDto dto = toCardDto(card);
                messagingTemplate.convertAndSend(
                                "/topic/board/" + boardId,
                                (Object) Map.of("type", "card:created", "data", Map.of("card", dto)));
                return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        }

        @PatchMapping("/{boardId}/cards/{cardId}/move")
        @Transactional
        public CardDto moveCard(
                        @PathVariable UUID boardId,
                        @PathVariable UUID cardId,
                        @Valid @RequestBody MoveCardRequest request,
                        Authentication authentication) {
                UUID userId = requireUserId(authentication);
                requireAuthorizedBoard(boardId, userId);

                Card card = requireCardOnBoard(cardId, boardId);
                TaskList targetList = taskListRepository.findById(request.newListId())
                                .filter(l -> boardId.equals(l.getBoardId()))
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Target list not found"));

                card.setList(targetList);
                card.setPosition(request.newPosition());
                card = cardRepository.save(card);

                CardDto dto = toCardDto(card);
                messagingTemplate.convertAndSend(
                                "/topic/board/" + boardId,
                                (Object) Map.of("type", "card:moved", "data",
                                                Map.of("cardId", card.getId(), "newCard", dto)));
                return dto;
        }

        @DeleteMapping("/{boardId}/cards/{cardId}")
        @Transactional
        public ResponseEntity<Void> deleteCard(
                        @PathVariable UUID boardId,
                        @PathVariable UUID cardId,
                        Authentication authentication) {
                UUID userId = requireUserId(authentication);
                requireAuthorizedBoard(boardId, userId);

                Card card = requireCardOnBoard(cardId, boardId);
                cardRepository.delete(card);
                messagingTemplate.convertAndSend(
                                "/topic/board/" + boardId,
                                (Object) Map.of("type", "card:deleted", "data", Map.of("cardId", cardId)));
                return ResponseEntity.noContent().build();
        }

        private Card requireCardOnBoard(UUID cardId, UUID boardId) {
                Card card = cardRepository.findById(cardId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));
                UUID listId = card.getListId() != null
                                ? card.getListId()
                                : (card.getList() != null ? card.getList().getId() : null);
                boolean belongsToBoard = listId != null && taskListRepository.findById(listId)
                                .map(l -> boardId.equals(l.getBoardId()))
                                .orElse(false);
                if (!belongsToBoard) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found on this board");
                }
                return card;
        }

        private UUID requireUserId(Authentication authentication) {
                if (authentication == null || !(authentication.getPrincipal() instanceof UUID userId)) {
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
                }
                return userId;
        }

        private Board requireAuthorizedBoard(UUID boardId, UUID userId) {
                Board board = boardRepository.findById(boardId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Board not found"));
                if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(board.getWorkspaceId(), userId)) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this board");
                }
                return board;
        }

        private Workspace createDefaultWorkspace(UUID userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                                                "User not found"));
                Workspace workspace = new Workspace();
                workspace.setName(user.getName() + "'s Workspace");
                workspace.setSlug("ws-" + UUID.randomUUID().toString().substring(0, 8));
                workspace.setAvatarColor("#14b8a6");
                workspace = workspaceRepository.save(workspace);

                WorkspaceMember member = new WorkspaceMember();
                member.setWorkspace(workspace);
                member.setUser(user);
                member.setRole(Role.OWNER);
                member.setName(user.getName());
                workspaceMemberRepository.save(member);
                return workspace;
        }

        private TaskList createDefaultList(Board board, String title, double position) {
                TaskList list = new TaskList();
                list.setTitle(title);
                list.setPosition(position);
                list.setBoard(board);
                return taskListRepository.save(list);
        }

        private BoardSummaryDto toSummaryDto(Board board) {
                UUID workspaceId = board.getWorkspaceId() != null
                                ? board.getWorkspaceId()
                                : (board.getWorkspace() != null ? board.getWorkspace().getId() : null);
                return new BoardSummaryDto(
                                board.getId(),
                                board.getName(),
                                board.getSlug(),
                                board.getAvatarColor(),
                                workspaceId);
        }

        private TaskListDto toListDto(TaskList list) {
                UUID boardId = list.getBoardId() != null
                                ? list.getBoardId()
                                : (list.getBoard() != null ? list.getBoard().getId() : null);
                return new TaskListDto(
                                list.getId(),
                                list.getTitle(),
                                list.getPosition(),
                                boardId);
        }

        private CardDto toCardDto(Card card) {
                UUID listId = card.getList() != null
                                ? card.getList().getId()
                                : card.getListId();
                return new CardDto(
                                card.getId(),
                                card.getTitle(),
                                card.getDescription(),
                                card.getPosition(),
                                listId);
        }
}