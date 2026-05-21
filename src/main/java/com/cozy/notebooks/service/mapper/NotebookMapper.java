package com.cozy.notebooks.service.mapper;

import com.cozy.notebooks.api.dto.NotebookDtos;
import com.cozy.notebooks.domain.NotebookEntity;
import org.springframework.stereotype.Component;

@Component
public class NotebookMapper {

    public NotebookDtos.NotebookResponse toResponse(NotebookEntity e) {
        return new NotebookDtos.NotebookResponse(
                e.getId(),
                e.getHrefCode(),
                e.getTitle(),
                e.getDescription(),
                e.getColor(),
                e.getIcon(),
                e.getPosition(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
