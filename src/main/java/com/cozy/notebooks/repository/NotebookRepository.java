package com.cozy.notebooks.repository;

import com.cozy.notebooks.domain.NotebookEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotebookRepository extends JpaRepository<NotebookEntity, UUID> {

    List<NotebookEntity> findByUserIdAndDeletedAtIsNullOrderByPositionAscCreatedAtAsc(UUID userId);

    Optional<NotebookEntity> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    boolean existsByUserIdAndHrefCode(UUID userId, String hrefCode);
}
