package com.cozy.notebooks.service;

import com.cozy.notebooks.api.dto.PageDtos.CreatePageRequest;
import com.cozy.notebooks.api.dto.PageDtos.PageResponse;
import com.cozy.notebooks.api.dto.PageDtos.UpdatePageRequest;
import com.cozy.notebooks.domain.NotebookEntity;
import com.cozy.notebooks.domain.PageEntity;
import com.cozy.notebooks.exception.BadRequestException;
import com.cozy.notebooks.exception.NotFoundException;
import com.cozy.notebooks.repository.PageRepository;
import com.cozy.notebooks.security.CurrentUserProvider;
import com.cozy.notebooks.service.mapper.PageMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PageService {

    private final PageRepository pageRepository;
    private final PageMapper pageMapper;
    private final NotebookService notebookService;
    private final CurrentUserProvider currentUserProvider;

    public PageService(PageRepository pageRepository,
                       PageMapper pageMapper,
                       NotebookService notebookService,
                       CurrentUserProvider currentUserProvider) {
        this.pageRepository = pageRepository;
        this.pageMapper = pageMapper;
        this.notebookService = notebookService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<PageResponse> listForNotebook(UUID notebookId) {
        UUID userId = currentUserProvider.requireId();
        // Ensures the notebook belongs to the current user; throws 404 otherwise.
        notebookService.loadOwned(notebookId);
        return pageRepository
                .findByNotebookIdAndUserIdAndDeletedAtIsNullOrderByPositionAscCreatedAtAsc(notebookId, userId)
                .stream()
                .map(pageMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse get(UUID id) {
        return pageMapper.toResponse(loadOwned(id));
    }

    public PageResponse create(UUID notebookId, CreatePageRequest request) {
        UUID userId = currentUserProvider.requireId();
        NotebookEntity notebook = notebookService.loadOwned(notebookId);

        if (request.parentPageId() != null) {
            PageEntity parent = loadOwned(request.parentPageId());
            if (!parent.getNotebookId().equals(notebookId)) {
                throw new BadRequestException("Parent page must belong to the same notebook");
            }
        }

        PageEntity entity = PageEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .notebookId(notebook.getId())
                .parentPageId(request.parentPageId())
                .title(request.title())
                .icon(request.icon())
                .coverUrl(request.coverUrl())
                .position(request.position() == null ? 0 : request.position())
                .favorite(Boolean.TRUE.equals(request.favorite()))
                .build();
        return pageMapper.toResponse(pageRepository.save(entity));
    }

    public PageResponse update(UUID id, UpdatePageRequest request) {
        PageEntity entity = loadOwned(id);

        if (request.parentPageId() != null) {
            PageEntity parent = loadOwned(request.parentPageId());
            if (!parent.getNotebookId().equals(entity.getNotebookId())) {
                throw new BadRequestException("Parent page must belong to the same notebook");
            }
            if (parent.getId().equals(entity.getId())) {
                throw new BadRequestException("Page cannot be its own parent");
            }
            entity.setParentPageId(request.parentPageId());
        }

        if (request.title() != null) entity.setTitle(request.title());
        if (request.icon() != null) entity.setIcon(request.icon());
        if (request.coverUrl() != null) entity.setCoverUrl(request.coverUrl());
        if (request.position() != null) entity.setPosition(request.position());
        if (request.favorite() != null) entity.setFavorite(request.favorite());

        return pageMapper.toResponse(pageRepository.save(entity));
    }

    public void delete(UUID id) {
        PageEntity entity = loadOwned(id);
        entity.setDeletedAt(OffsetDateTime.now());
        pageRepository.save(entity);
    }

    PageEntity loadOwned(UUID id) {
        UUID userId = currentUserProvider.requireId();
        return pageRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
                .orElseThrow(() -> NotFoundException.of("Page", id));
    }
}
