package com.cozy.notebooks.service;

import com.cozy.notebooks.api.dto.PageDtos.CreatePageRequest;
import com.cozy.notebooks.api.dto.PageDtos.PageResponse;
import com.cozy.notebooks.api.dto.PageTemplateDtos.CreatePageFromTemplateRequest;
import com.cozy.notebooks.api.dto.PageTemplateDtos.CreateTemplateRequest;
import com.cozy.notebooks.api.dto.PageTemplateDtos.TemplateBlock;
import com.cozy.notebooks.api.dto.PageTemplateDtos.TemplateResponse;
import com.cozy.notebooks.api.dto.PageTemplateDtos.UpdateTemplateRequest;
import com.cozy.notebooks.domain.BlockEntity;
import com.cozy.notebooks.domain.BlockType;
import com.cozy.notebooks.domain.PageTemplateEntity;
import com.cozy.notebooks.exception.BadRequestException;
import com.cozy.notebooks.exception.NotFoundException;
import com.cozy.notebooks.repository.BlockRepository;
import com.cozy.notebooks.repository.PageTemplateRepository;
import com.cozy.notebooks.security.CurrentUserProvider;
import com.cozy.notebooks.service.mapper.PageTemplateMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class PageTemplateService {

    private final PageTemplateRepository templateRepository;
    private final BlockRepository blockRepository;
    private final PageTemplateMapper templateMapper;
    private final PageService pageService;
    private final CurrentUserProvider currentUserProvider;
    private final ObjectMapper objectMapper;

    public PageTemplateService(PageTemplateRepository templateRepository,
                               BlockRepository blockRepository,
                               PageTemplateMapper templateMapper,
                               PageService pageService,
                               CurrentUserProvider currentUserProvider,
                               ObjectMapper objectMapper) {
        this.templateRepository = templateRepository;
        this.blockRepository = blockRepository;
        this.templateMapper = templateMapper;
        this.pageService = pageService;
        this.currentUserProvider = currentUserProvider;
        this.objectMapper = objectMapper;
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
        PageTemplateEntity entity = PageTemplateEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name(request.name())
                .description(request.description())
                .icon(request.icon())
                .blocks(serializeBlocks(request.blocks()))
                .builtIn(false)
                .build();
        return templateMapper.toResponse(templateRepository.save(entity));
    }

    public TemplateResponse update(UUID id, UpdateTemplateRequest request) {
        PageTemplateEntity entity = loadOwned(id);
        if (request.name() != null) entity.setName(request.name());
        if (request.description() != null) entity.setDescription(request.description());
        if (request.icon() != null) entity.setIcon(request.icon());
        if (request.blocks() != null) entity.setBlocks(serializeBlocks(request.blocks()));
        return templateMapper.toResponse(templateRepository.save(entity));
    }

    public void delete(UUID id) {
        PageTemplateEntity entity = loadOwned(id);
        entity.setDeletedAt(OffsetDateTime.now());
        templateRepository.save(entity);
    }

    public PageResponse createPage(UUID templateId, CreatePageFromTemplateRequest request) {
        UUID userId = currentUserProvider.requireId();
        PageTemplateEntity template = loadOwned(templateId);

        String title = request.title() == null || request.title().isBlank()
                ? template.getName()
                : request.title();

        PageResponse page = pageService.create(
                request.notebookId(),
                new CreatePageRequest(title, request.parentPageId(), template.getIcon(),
                        null, 0, false));

        JsonNode templateBlocks = template.getBlocks();
        if (templateBlocks != null && templateBlocks.isArray()) {
            List<BlockEntity> toSave = new ArrayList<>();
            int idx = 0;
            for (JsonNode node : templateBlocks) {
                BlockType type = BlockType.fromWire(textOrNull(node, "type"));
                if (type == null) {
                    throw new BadRequestException("Template block is missing 'type'");
                }
                JsonNode content = node.has("content") ? node.get("content") : objectMapper.createObjectNode();
                JsonNode settings = node.has("settings") ? node.get("settings") : objectMapper.createObjectNode();
                int position = node.has("position") && node.get("position").isInt()
                        ? node.get("position").asInt() : idx;

                BlockEntity block = BlockEntity.builder()
                        .id(UUID.randomUUID())
                        .userId(userId)
                        .notebookId(page.notebookId())
                        .pageId(page.id())
                        .content(content)
                        .settings(settings)
                        .position(position)
                        .build();
                block.setTypeEnum(type);
                toSave.add(block);
                idx++;
            }
            blockRepository.saveAll(toSave);
        }

        return page;
    }

    private JsonNode serializeBlocks(List<TemplateBlock> blocks) {
        ArrayNode array = objectMapper.createArrayNode();
        if (blocks == null) {
            return array;
        }
        for (int i = 0; i < blocks.size(); i++) {
            TemplateBlock b = blocks.get(i);
            ObjectNode node = objectMapper.createObjectNode();
            node.put("type", b.type().wireValue());
            node.set("content", b.content() == null ? objectMapper.createObjectNode() : b.content());
            node.set("settings", b.settings() == null ? objectMapper.createObjectNode() : b.settings());
            node.put("position", b.position() == null ? i : b.position());
            array.add(node);
        }
        return array;
    }

    private static String textOrNull(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) return null;
        return node.get(field).asText().trim().toLowerCase(Locale.ROOT);
    }

    private PageTemplateEntity loadOwned(UUID id) {
        UUID userId = currentUserProvider.requireId();
        return templateRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
                .orElseThrow(() -> NotFoundException.of("Template", id));
    }
}
