package com.cozy.notebooks.service.mapper;

import com.cozy.notebooks.api.dto.BlockDtos;
import com.cozy.notebooks.domain.BlockEntity;
import org.springframework.stereotype.Component;

@Component
public class BlockMapper {

    public BlockDtos.BlockResponse toResponse(BlockEntity e) {
        return new BlockDtos.BlockResponse(
                e.getId(),
                e.getNotebookId(),
                e.getPageId(),
                e.getParentBlockId(),
                e.getTypeEnum(),
                e.getContent(),
                e.getSettings(),
                e.getPosition(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
