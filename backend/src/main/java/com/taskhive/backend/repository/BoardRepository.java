package com.taskhive.backend.repository;

import com.taskhive.backend.security.Board;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface BoardRepository extends JpaRepository<Board, UUID> {
    List<Board> findByWorkspaceIdIn(Collection<UUID> workspaceIds);
}
