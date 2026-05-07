package com.cozy.notebooks.repository;

import com.cozy.notebooks.domain.PageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PageRepository extends JpaRepository<PageEntity, UUID> {

    List<PageEntity> findByNotebookIdAndUserIdAndDeletedAtIsNullOrderByCreatedAtAsc(
            UUID notebookId, UUID userId);

    Optional<PageEntity> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);
}
