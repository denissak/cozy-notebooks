package com.cozy.notebooks.service.mapper;

import com.cozy.notebooks.api.dto.PageTemplateDtos;
import com.cozy.notebooks.domain.PageTemplateEntity;
import org.springframework.stereotype.Component;

@Component
public class PageTemplateMapper {

    public PageTemplateDtos.TemplateResponse toResponse(PageTemplateEntity e) {
        return new PageTemplateDtos.TemplateResponse(
                e.getId(),
                e.getHrefCode(),
                e.getName(),
                e.getDescription(),
                e.getIcon(),
                e.getContentJson(),
                e.getContentHash(),
                e.isBuiltIn(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
