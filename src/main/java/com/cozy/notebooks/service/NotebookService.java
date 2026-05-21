package com.cozy.notebooks.service;

import com.cozy.notebooks.api.dto.NotebookDtos.CreateNotebookRequest;
import com.cozy.notebooks.api.dto.NotebookDtos.NotebookResponse;
import com.cozy.notebooks.api.dto.NotebookDtos.UpdateNotebookRequest;
import com.cozy.notebooks.domain.NotebookEntity;
import com.cozy.notebooks.exception.NotFoundException;
import com.cozy.notebooks.repository.NotebookRepository;
import com.cozy.notebooks.security.CurrentUserProvider;
import com.cozy.notebooks.service.mapper.NotebookMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class NotebookService {

    private static final int MAX_HREF_CODE_ALLOCATION_ATTEMPTS = 64;

    private final NotebookRepository notebookRepository;
    private final NotebookMapper notebookMapper;
    private final CurrentUserProvider currentUserProvider;
    private final HrefCodeGenerator hrefCodeGenerator;

    public NotebookService(NotebookRepository notebookRepository,
                           NotebookMapper notebookMapper,
                           CurrentUserProvider currentUserProvider,
                           HrefCodeGenerator hrefCodeGenerator) {
        this.notebookRepository = notebookRepository;
        this.notebookMapper = notebookMapper;
        this.currentUserProvider = currentUserProvider;
        this.hrefCodeGenerator = hrefCodeGenerator;
    }

    @Transactional(readOnly = true)
    public List<NotebookResponse> list() {
        UUID userId = currentUserProvider.requireId();
        return notebookRepository
                .findByUserIdAndDeletedAtIsNullOrderByPositionAscCreatedAtAsc(userId)
                .stream()
                .map(notebookMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotebookResponse get(UUID id) {
        return notebookMapper.toResponse(loadOwned(id));
    }

    public NotebookResponse create(CreateNotebookRequest request) {
        UUID userId = currentUserProvider.requireId();
        for (int attempt = 0; attempt < MAX_HREF_CODE_ALLOCATION_ATTEMPTS; attempt++) {
            String hrefCode = hrefCodeGenerator.generate();
            if (notebookRepository.existsByUserIdAndHrefCode(userId, hrefCode)) {
                continue;
            }
            try {
                NotebookEntity entity = NotebookEntity.builder()
                        .id(UUID.randomUUID())
                        .userId(userId)
                        .hrefCode(hrefCode)
                        .title(request.title())
                        .description(request.description())
                        .color(request.color())
                        .icon(request.icon())
                        .position(request.position() == null ? 0 : request.position())
                        .build();
                notebookRepository.saveAndFlush(entity);
                return notebookMapper.toResponse(entity);
            } catch (DataIntegrityViolationException ignored) {
                // Rare unique (user_id, href_code) race; retry with a new candidate.
            }
        }
        throw new IllegalStateException("Could not allocate a unique notebook href_code");
    }

    public NotebookResponse update(UUID id, UpdateNotebookRequest request) {
        NotebookEntity entity = loadOwned(id);
        if (request.title() != null) entity.setTitle(request.title());
        if (request.description() != null) entity.setDescription(request.description());
        if (request.color() != null) entity.setColor(request.color());
        if (request.icon() != null) entity.setIcon(request.icon());
        if (request.position() != null) entity.setPosition(request.position());
        return notebookMapper.toResponse(notebookRepository.save(entity));
    }

    public void delete(UUID id) {
        NotebookEntity entity = loadOwned(id);
        entity.setDeletedAt(OffsetDateTime.now());
        notebookRepository.save(entity);
    }

    NotebookEntity loadOwned(UUID id) {
        UUID userId = currentUserProvider.requireId();
        return notebookRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
                .orElseThrow(() -> NotFoundException.of("Notebook", id));
    }
}
