package com.cozy.notebooks.service;

import com.cozy.notebooks.api.dto.BlockDtos.BlockResponse;
import com.cozy.notebooks.api.dto.BlockDtos.CreateBlockRequest;
import com.cozy.notebooks.api.dto.BlockDtos.ReorderRequest;
import com.cozy.notebooks.api.dto.BlockDtos.UpdateBlockRequest;
import com.cozy.notebooks.domain.BlockEntity;
import com.cozy.notebooks.domain.PageEntity;
import com.cozy.notebooks.exception.BadRequestException;
import com.cozy.notebooks.exception.NotFoundException;
import com.cozy.notebooks.repository.BlockRepository;
import com.cozy.notebooks.security.CurrentUserProvider;
import com.cozy.notebooks.service.mapper.BlockMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class BlockService {

    private final BlockRepository blockRepository;
    private final BlockMapper blockMapper;
    private final PageService pageService;
    private final CurrentUserProvider currentUserProvider;
    private final ObjectMapper objectMapper;

    public BlockService(BlockRepository blockRepository,
                        BlockMapper blockMapper,
                        PageService pageService,
                        CurrentUserProvider currentUserProvider,
                        ObjectMapper objectMapper) {
        this.blockRepository = blockRepository;
        this.blockMapper = blockMapper;
        this.pageService = pageService;
        this.currentUserProvider = currentUserProvider;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<BlockResponse> listForPage(UUID pageId) {
        UUID userId = currentUserProvider.requireId();
        // Authorization: ensures the page belongs to the current user.
        pageService.loadOwned(pageId);
        return blockRepository
                .findByPageIdAndUserIdAndDeletedAtIsNullOrderByPositionAscCreatedAtAsc(pageId, userId)
                .stream()
                .map(blockMapper::toResponse)
                .toList();
    }

    public BlockResponse create(UUID pageId, CreateBlockRequest request) {
        UUID userId = currentUserProvider.requireId();
        PageEntity page = pageService.loadOwned(pageId);

        if (request.parentBlockId() != null) {
            BlockEntity parent = loadOwned(request.parentBlockId());
            if (!parent.getPageId().equals(pageId)) {
                throw new BadRequestException("Parent block must belong to the same page");
            }
        }

        BlockEntity entity = BlockEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .notebookId(page.getNotebookId())
                .pageId(page.getId())
                .parentBlockId(request.parentBlockId())
                .content(emptyIfNull(request.content()))
                .settings(emptyIfNull(request.settings()))
                .position(request.position() == null ? 0 : request.position())
                .build();
        entity.setTypeEnum(request.type());

        return blockMapper.toResponse(blockRepository.save(entity));
    }

    public BlockResponse update(UUID id, UpdateBlockRequest request) {
        BlockEntity entity = loadOwned(id);

        if (request.parentBlockId() != null) {
            BlockEntity parent = loadOwned(request.parentBlockId());
            if (!parent.getPageId().equals(entity.getPageId())) {
                throw new BadRequestException("Parent block must belong to the same page");
            }
            if (parent.getId().equals(entity.getId())) {
                throw new BadRequestException("Block cannot be its own parent");
            }
            entity.setParentBlockId(request.parentBlockId());
        }
        if (request.type() != null) entity.setTypeEnum(request.type());
        if (request.content() != null) entity.setContent(request.content());
        if (request.settings() != null) entity.setSettings(request.settings());
        if (request.position() != null) entity.setPosition(request.position());

        return blockMapper.toResponse(blockRepository.save(entity));
    }

    public void delete(UUID id) {
        BlockEntity entity = loadOwned(id);
        entity.setDeletedAt(OffsetDateTime.now());
        blockRepository.save(entity);
    }

    public List<BlockResponse> reorder(UUID pageId, ReorderRequest request) {
        UUID userId = currentUserProvider.requireId();
        pageService.loadOwned(pageId);

        List<BlockEntity> blocks = blockRepository
                .findByPageIdAndUserIdAndDeletedAtIsNull(pageId, userId);

        Map<UUID, BlockEntity> byId = new HashMap<>();
        for (BlockEntity b : blocks) byId.put(b.getId(), b);

        if (request.blockIds().size() != blocks.size()) {
            throw new BadRequestException(
                    "Reorder list size (%d) does not match number of blocks on page (%d)"
                            .formatted(request.blockIds().size(), blocks.size()));
        }

        for (UUID id : request.blockIds()) {
            if (!byId.containsKey(id)) {
                throw new BadRequestException("Block %s does not belong to page %s".formatted(id, pageId));
            }
        }

        for (int i = 0; i < request.blockIds().size(); i++) {
            BlockEntity b = byId.get(request.blockIds().get(i));
            b.setPosition(i);
        }
        blockRepository.saveAll(blocks);

        return blockRepository
                .findByPageIdAndUserIdAndDeletedAtIsNullOrderByPositionAscCreatedAtAsc(pageId, userId)
                .stream()
                .map(blockMapper::toResponse)
                .toList();
    }

    BlockEntity loadOwned(UUID id) {
        UUID userId = currentUserProvider.requireId();
        return blockRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
                .orElseThrow(() -> NotFoundException.of("Block", id));
    }

    private JsonNode emptyIfNull(JsonNode node) {
        return node == null ? objectMapper.createObjectNode() : node;
    }
}
