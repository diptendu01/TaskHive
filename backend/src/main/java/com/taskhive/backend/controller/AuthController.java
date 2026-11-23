package com.taskhive.backend.controller;

import com.taskhive.backend.dto.AuthDtos.AuthResponse;
import com.taskhive.backend.dto.AuthDtos.LoginRequest;
import com.taskhive.backend.dto.AuthDtos.RegisterRequest;
import com.taskhive.backend.dto.AuthDtos.UserSummary;
import com.taskhive.backend.repository.BoardRepository;
import com.taskhive.backend.repository.CardRepository;
import com.taskhive.backend.repository.TaskListRepository;
import com.taskhive.backend.repository.UserRepository;
import com.taskhive.backend.repository.WorkspaceMemberRepository;
import com.taskhive.backend.repository.WorkspaceRepository;
import com.taskhive.backend.security.Board;
import com.taskhive.backend.security.Card;
import com.taskhive.backend.security.JwtService;
import com.taskhive.backend.security.Role;
import com.taskhive.backend.security.TaskList;
import com.taskhive.backend.security.User;
import com.taskhive.backend.security.Workspace;
import com.taskhive.backend.security.WorkspaceMember;
import com.taskhive.backend.service.FractionalIndexService;
import com.taskhive.backend.service.LoginRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final BoardRepository boardRepository;
    private final TaskListRepository taskListRepository;
    private final CardRepository cardRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginRateLimiter loginRateLimiter;

    public AuthController(
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            BoardRepository boardRepository,
            TaskListRepository taskListRepository,
            CardRepository cardRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            LoginRateLimiter loginRateLimiter
    ) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.boardRepository = boardRepository;
        this.taskListRepository = taskListRepository;
        this.cardRepository = cardRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(normalizedEmail);
        user.setAvatarInitials(computeInitials(request.name()));
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setGlobalRole(Role.MEMBER);
        user = userRepository.save(user);

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

        Board board = new Board();
        board.setName("Getting Started");
        board.setSlug("getting-started-" + UUID.randomUUID().toString().substring(0, 6));
        board.setAvatarColor("#14b8a6");
        board.setWorkspace(workspace);
        board = boardRepository.save(board);

        TaskList todo = buildList(board, "To Do", FractionalIndexService.DEFAULT_GAP);
        TaskList inProgress = buildList(board, "In Progress", FractionalIndexService.DEFAULT_GAP * 2);
        TaskList done = buildList(board, "Done", FractionalIndexService.DEFAULT_GAP * 3);
        List<TaskList> savedLists = taskListRepository.saveAll(List.of(todo, inProgress, done));

        Card welcomeCard = new Card();
        welcomeCard.setTitle("Welcome to TaskHive! Drag me to In Progress");
        welcomeCard.setDescription("You can create lists, edit cards, invite collaborators, and reorder tasks in real time.");
        welcomeCard.setPosition(FractionalIndexService.DEFAULT_GAP);
        welcomeCard.setList(savedLists.get(0));
        cardRepository.save(welcomeCard);

        String token = jwtService.generateToken(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(token, toSummary(user)));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        String clientIp = httpRequest.getRemoteAddr();
        loginRateLimiter.checkAllowed(clientIp, normalizedEmail);

        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginRateLimiter.recordFailure(clientIp, normalizedEmail);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        loginRateLimiter.clear(clientIp, normalizedEmail);
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token, toSummary(user)));
    }

    private TaskList buildList(Board board, String title, double position) {
        TaskList list = new TaskList();
        list.setTitle(title);
        list.setPosition(position);
        list.setBoard(board);
        return list;
    }

    private UserSummary toSummary(User user) {
        return new UserSummary(user.getId(), user.getName(), user.getEmail(), user.getGlobalRole().name());
    }

    private String computeInitials(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            return "U";
        }
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
        }
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase(Locale.ROOT);
    }
}
