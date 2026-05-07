package com.cozy.notebooks.service;

import com.cozy.notebooks.api.dto.PageDtos.PageResponse;
import com.cozy.notebooks.api.dto.PageTemplateDtos.CreatePageFromTemplateRequest;
import com.cozy.notebooks.api.dto.PageTemplateDtos.CreateTemplateRequest;
import com.cozy.notebooks.api.dto.PageTemplateDtos.TemplateResponse;
import com.cozy.notebooks.api.dto.PageTemplateDtos.UpdateTemplateRequest;
import com.cozy.notebooks.domain.PageTemplateEntity;
import com.cozy.notebooks.exception.BadRequestException;
import com.cozy.notebooks.exception.NotFoundException;
import com.cozy.notebooks.repository.PageTemplateRepository;
import com.cozy.notebooks.security.CurrentUserProvider;
import com.cozy.notebooks.service.mapper.PageTemplateMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Page templates store a full page document and can be instantiated into a
 * brand-new page by copying {@code content_json} verbatim. The backend does
 * not interpret block structure on either side.
 */
@Service
@Transactional
public class PageTemplateService {

    private final PageTemplateRepository templateRepository;
    private final PageTemplateMapper templateMapper;
    private final PageService pageService;
    private final CurrentUserProvider currentUserProvider;
    private final PageContentHashService hashService;

    public PageTemplateService(PageTemplateRepository templateRepository,
                               PageTemplateMapper templateMapper,
                               PageService pageService,
                               CurrentUserProvider currentUserProvider,
                               PageContentHashService hashService) {
        this.templateRepository = templateRepository;
        this.templateMapper = templateMapper;
        this.pageService = pageService;
        this.currentUserProvider = currentUserProvider;
        this.hashService = hashService;
    }

    @Transactional(readOnly = true)
    public List<TemplateResponse> list() {
        UUID userId = currentUserProvider.requireId();
        return templateRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtAsc(userId)
                .stream()
                .map(templateMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TemplateResponse get(UUID id) {
        return templateMapper.toResponse(loadOwned(id));
    }

    public TemplateResponse create(CreateTemplateRequest request) {
        UUID userId = currentUserProvider.requireId();
        JsonNode content = requireContent(request.content());

        PageTemplateEntity entity = PageTemplateEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name(request.name())
                .description(request.description())
                .icon(request.icon())
                .contentJson(content)
                .contentHash(hashService.hash(content))
                .builtIn(false)
                .build();
        return templateMapper.toResponse(templateRepository.save(entity));
    }

    public TemplateResponse update(UUID id, UpdateTemplateRequest request) {
        PageTemplateEntity entity = loadOwned(id);
        if (request.name() != null) entity.setName(request.name());
        if (request.description() != null) entity.setDescription(request.description());
        if (request.icon() != null) entity.setIcon(request.icon());
        if (request.content() != null) {
            JsonNode content = requireContent(request.content());
            entity.setContentJson(content);
            entity.setContentHash(hashService.hash(content));
        }
        return templateMapper.toResponse(templateRepository.save(entity));
    }

    public void delete(UUID id) {
        PageTemplateEntity entity = loadOwned(id);
        entity.setDeletedAt(OffsetDateTime.now());
        templateRepository.save(entity);
    }

    public PageResponse createPage(UUID templateId, CreatePageFromTemplateRequest request) {
        PageTemplateEntity template = loadOwned(templateId);

        String title = request.title() == null || request.title().isBlank()
                ? template.getName()
                : request.title();

        return pageService.createFromContent(
                request.notebookId(),
                title,
                template.getContentJson()
        );
    }

    private PageTemplateEntity loadOwned(UUID id) {
        UUID userId = currentUserProvider.requireId();
        return templateRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
                .orElseThrow(() -> NotFoundException.of("Template", id));
    }

    private static JsonNode requireContent(JsonNode content) {
        if (content == null || content.isNull()) {
            throw new BadRequestException("content must be provided");
        }
        return content;
    }
}
