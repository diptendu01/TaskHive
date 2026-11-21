package com.taskhive.backend.repository;

import com.taskhive.backend.security.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID> {
    List<Card> findByListIdInOrderByPositionAsc(Collection<UUID> listIds);
    List<Card> findByListIdOrderByPositionAsc(UUID listId);
}
