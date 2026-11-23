package com.taskhive.backend.controller;

import com.taskhive.backend.dto.BoardDtos.InviteMemberRequest;
import com.taskhive.backend.dto.BoardDtos.WorkspaceMemberDto;
import com.taskhive.backend.repository.UserRepository;
import com.taskhive.backend.repository.WorkspaceMemberRepository;
import com.taskhive.backend.repository.WorkspaceRepository;
import com.taskhive.backend.security.Role;
import com.taskhive.backend.security.User;
import com.taskhive.backend.security.Workspace;
import com.taskhive.backend.security.WorkspaceMember;
import com.taskhive.backend.service.WebSocketSessionRegistry;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    private final WebSocketSessionRegistry sessionRegistry;

    public WorkspaceController(
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            UserRepository userRepository,
            WebSocketSessionRegistry sessionRegistry
    ) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userRepository = userRepository;
        this.sessionRegistry = sessionRegistry;
    }

    @GetMapping("/{workspaceId}/members")
    @Transactional(readOnly = true)
    public List<WorkspaceMemberDto> listMembers(
            @PathVariable UUID workspaceId,
            Authentication authentication
    ) {
        UUID userId = requireUserId(authentication);
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this workspace");
        }

        return workspaceMemberRepository.findByWorkspaceId(workspaceId)
                .stream()
                .map(this::toMemberDto)
                .toList();
    }

    @PostMapping("/{workspaceId}/members")
    @Transactional
    public ResponseEntity<WorkspaceMemberDto> inviteMember(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody InviteMemberRequest request,
            Authentication authentication
    ) {
        UUID requesterId = requireUserId(authentication);
        WorkspaceMember requester = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(workspaceId, requesterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this workspace"));

        if (requester.getRole() != Role.OWNER && requester.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only workspace owners or admins can invite members");
        }

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));

        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        User invitee = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (invitee == null || workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, invitee.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unable to invite this user. Please verify the email address and ensure they have a registered account."
            );
        }

        Role targetRole = parseInviteRole(request.role());
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(invitee);
        member.setRole(targetRole);
        member.setName(invitee.getName());
        member = workspaceMemberRepository.save(member);

        return ResponseEntity.status(HttpStatus.CREATED).body(toMemberDto(member));
    }

    @DeleteMapping("/{workspaceId}/members/{memberId}")
    @Transactional
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID workspaceId,
            @PathVariable UUID memberId,
            Authentication authentication
    ) {
        UUID requesterId = requireUserId(authentication);
        WorkspaceMember requester = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(workspaceId, requesterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this workspace"));

        if (requester.getRole() != Role.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only workspace owners can remove members");
        }

        WorkspaceMember target = workspaceMemberRepository.findById(memberId)
                .filter(m -> workspaceId.equals(m.getWorkspaceId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found in this workspace"));

        if (requesterId.equals(target.getUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace owners cannot remove themselves");
        }

        UUID removedUserId = target.getUserId();
        workspaceMemberRepository.delete(target);
        if (removedUserId != null) {
            sessionRegistry.disconnectUser(removedUserId);
        }
        return ResponseEntity.noContent().build();
    }

    private Role parseInviteRole(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            return Role.MEMBER;
        }
        try {
            Role parsed = Role.valueOf(rawRole.trim().toUpperCase(Locale.ROOT));
            return parsed == Role.OWNER ? Role.ADMIN : parsed;
        } catch (IllegalArgumentException ex) {
            return Role.MEMBER;
        }
    }

    private WorkspaceMemberDto toMemberDto(WorkspaceMember member) {
        User user = member.getUser();
        UUID uid = member.getUserId() != null ? member.getUserId() : (user != null ? user.getId() : null);
        String email = user != null ? user.getEmail() : "";
        return new WorkspaceMemberDto(
                member.getId(),
                uid,
                member.getName(),
                email,
                member.getRole().name()
        );
    }

    private UUID requireUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UUID userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userId;
    }
}
