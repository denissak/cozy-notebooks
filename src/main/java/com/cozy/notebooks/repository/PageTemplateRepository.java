package com.cozy.notebooks.repository;

import com.cozy.notebooks.domain.PageTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PageTemplateRepository extends JpaRepository<PageTemplateEntity, UUID> {

    List<PageTemplateEntity> findByUserIdAndDeletedAtIsNullOrderByCreatedAtAsc(UUID userId);

    Optional<PageTemplateEntity> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    boolean existsByUserIdAndHrefCode(UUID userId, String hrefCode);
}
