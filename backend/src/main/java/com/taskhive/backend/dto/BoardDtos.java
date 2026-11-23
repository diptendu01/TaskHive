package com.taskhive.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class BoardDtos {

    private BoardDtos() {
    }

    public record BoardSummaryDto(
            UUID id,
            String name,
            String slug,
            String avatarColor,
            UUID workspaceId
    ) {
    }

    public record TaskListDto(
            UUID id,
            String title,
            double position,
            UUID boardId
    ) {
    }

    public record CardDto(
            UUID id,
            String title,
            String description,
            double position,
            UUID listId
    ) {
    }

    public record BoardDetailDto(
            UUID id,
            String name,
            String slug,
            String avatarColor,
            UUID workspaceId,
            List<TaskListDto> lists,
            List<CardDto> cards
    ) {
    }

    public record WorkspaceMemberDto(
            UUID id,
            UUID userId,
            String name,
            String email,
            String role
    ) {
    }

    public record CreateBoardRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 64) String avatarColor
    ) {
    }

    public record UpdateBoardRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 64) String avatarColor
    ) {
    }

    public record CreateListRequest(
            @NotBlank @Size(max = 120) String title
    ) {
    }

    public record UpdateListRequest(
            @NotBlank @Size(max = 120) String title
    ) {
    }

    public record ReorderListRequest(
            UUID beforeListId,
            UUID afterListId
    ) {
    }

    public record CreateCardRequest(
            @NotNull UUID listId,
            @NotBlank @Size(max = 512) String title,
            @Size(max = 4096) String description
    ) {
    }

    public record UpdateCardRequest(
            @NotBlank @Size(max = 512) String title,
            @Size(max = 4096) String description
    ) {
    }

    public record MoveCardRequest(
            @NotNull UUID cardId,
            @NotNull UUID newListId,
            Double newPosition,
            UUID beforeCardId,
            UUID afterCardId
    ) {
    }

    public record InviteMemberRequest(
            @NotBlank @Email @Size(max = 180) String email,
            String role
    ) {
    }
}
