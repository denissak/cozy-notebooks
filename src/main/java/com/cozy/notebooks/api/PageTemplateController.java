package com.cozy.notebooks.api;

import com.cozy.notebooks.api.dto.PageDtos.PageResponse;
import com.cozy.notebooks.api.dto.PageTemplateDtos.CreatePageFromTemplateRequest;
import com.cozy.notebooks.api.dto.PageTemplateDtos.CreateTemplateRequest;
import com.cozy.notebooks.api.dto.PageTemplateDtos.TemplateResponse;
import com.cozy.notebooks.api.dto.PageTemplateDtos.UpdateTemplateRequest;
import com.cozy.notebooks.service.PageTemplateService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/templates")
@Tag(name = "Templates")
public class PageTemplateController {

    private final PageTemplateService templateService;

    public PageTemplateController(PageTemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public List<TemplateResponse> list() {
        return templateService.list();
    }

    @PostMapping
    public ResponseEntity<TemplateResponse> create(@Valid @RequestBody CreateTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.create(request));
    }

    @GetMapping("/{templateId}")
    public TemplateResponse get(@PathVariable UUID templateId) {
        return templateService.get(templateId);
    }

    @PatchMapping("/{templateId}")
    public TemplateResponse update(@PathVariable UUID templateId,
                                   @Valid @RequestBody UpdateTemplateRequest request) {
        return templateService.update(templateId, request);
    }

    @DeleteMapping("/{templateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID templateId) {
        templateService.delete(templateId);
    }

    @PostMapping("/{templateId}/create-page")
    public ResponseEntity<PageResponse> createPage(@PathVariable UUID templateId,
                                                   @Valid @RequestBody CreatePageFromTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.createPage(templateId, request));
    }
}
