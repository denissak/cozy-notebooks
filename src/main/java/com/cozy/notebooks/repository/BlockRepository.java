package com.cozy.notebooks.repository;

import com.cozy.notebooks.domain.BlockEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BlockRepository extends JpaRepository<BlockEntity, UUID> {

    List<BlockEntity> findByPageIdAndUserIdAndDeletedAtIsNullOrderByPositionAscCreatedAtAsc(
            UUID pageId, UUID userId);

    List<BlockEntity> findByPageIdAndUserIdAndDeletedAtIsNull(UUID pageId, UUID userId);

    Optional<BlockEntity> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);
}
