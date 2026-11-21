package com.taskhive.backend.repository;

import com.taskhive.backend.security.TaskList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskListRepository extends JpaRepository<TaskList, UUID> {
    List<TaskList> findByBoardIdOrderByPositionAsc(UUID boardId);
}
