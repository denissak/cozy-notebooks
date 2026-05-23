package com.cozy.notebooks.service;

import com.cozy.notebooks.api.dto.PageDtos.CreatePageRequest;
import com.cozy.notebooks.api.dto.PageDtos.PageResponse;
import com.cozy.notebooks.api.dto.PageDtos.UpdatePageRequest;
import com.cozy.notebooks.domain.NotebookEntity;
import com.cozy.notebooks.domain.PageEntity;
import com.cozy.notebooks.exception.BadRequestException;
import com.cozy.notebooks.exception.ConflictException;
import com.cozy.notebooks.exception.NotFoundException;
import com.cozy.notebooks.repository.PageRepository;
import com.cozy.notebooks.security.CurrentUserProvider;
import com.cozy.notebooks.service.mapper.PageMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Page document operations. The backend persists the entire page as a JSON
 * document; block structure inside the document is opaque to this service.
 *
 * <p>Conflict detection on update uses an explicit {@code baseHash} field
 * on the request, compared as a string against the stored {@code contentHash}.
 * If they don't match, {@link ConflictException} is thrown (HTTP 409).
 */
@Service
@Transactional
public class PageService {

    private final PageRepository pageRepository;
    private final PageMapper pageMapper;
    private final NotebookService notebookService;
    private final CurrentUserProvider currentUserProvider;
    private final PageContentHashService hashService;
    private final UserPlanLimitsService userPlanLimitsService;

    public PageService(PageRepository pageRepository,
                       PageMapper pageMapper,
                       NotebookService notebookService,
                       CurrentUserProvider currentUserProvider,
                       PageContentHashService hashService,
                       UserPlanLimitsService userPlanLimitsService) {
        this.pageRepository = pageRepository;
        this.pageMapper = pageMapper;
        this.notebookService = notebookService;
        this.currentUserProvider = currentUserProvider;
        this.hashService = hashService;
        this.userPlanLimitsService = userPlanLimitsService;
    }

    @Transactional(readOnly = true)
    public List<PageResponse> listForNotebook(UUID notebookId) {
        UUID userId = currentUserProvider.requireId();
        // Authorization: ensures the notebook belongs to the current user.
        notebookService.loadOwned(notebookId);
        return pageRepository
                .findByNotebookIdAndUserIdAndDeletedAtIsNullOrderByCreatedAtAsc(notebookId, userId)
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
        userPlanLimitsService.validateCanCreatePage(userId, notebook.getId());

        JsonNode content = requireContent(request.content());
        String hash = hashService.hash(content);

        PageEntity entity = PageEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .notebookId(notebook.getId())
                .title(request.title())
                .contentJson(content)
                .contentHash(hash)
                .version(1L)
                .build();
        return pageMapper.toResponse(pageRepository.save(entity));
    }

    /**
     * Internal entry point used by template instantiation. Bypasses the
     * request DTO's bean-validation but re-uses the same hashing/versioning
     * pipeline as a normal create.
     */
    public PageResponse createFromContent(UUID notebookId, String title, JsonNode content) {
        return create(notebookId, new CreatePageRequest(title, content));
    }

    public PageResponse update(UUID id, UpdatePageRequest request) {
        PageEntity entity = loadOwned(id);

        if (request.baseHash() != null && !request.baseHash().equals(entity.getContentHash())) {
            throw new ConflictException("Page content was modified by another update");
        }

        JsonNode content = requireContent(request.content());

        if (request.title() != null) {
            entity.setTitle(request.title());
        }
        entity.setContentJson(content);
        entity.setContentHash(hashService.hash(content));
        entity.setVersion(entity.getVersion() + 1);

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

    private static JsonNode requireContent(JsonNode content) {
        if (content == null || content.isNull()) {
            throw new BadRequestException("content must be provided");
        }
        return content;
    }
}
