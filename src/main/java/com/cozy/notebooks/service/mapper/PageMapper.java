package com.cozy.notebooks.service.mapper;

import com.cozy.notebooks.api.dto.PageDtos;
import com.cozy.notebooks.domain.PageEntity;
import org.springframework.stereotype.Component;

@Component
public class PageMapper {

    public PageDtos.PageResponse toResponse(PageEntity e) {
        return new PageDtos.PageResponse(
                e.getId(),
                e.getNotebookId(),
                e.getParentPageId(),
                e.getTitle(),
                e.getIcon(),
                e.getCoverUrl(),
                e.getPosition(),
                e.isFavorite(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
