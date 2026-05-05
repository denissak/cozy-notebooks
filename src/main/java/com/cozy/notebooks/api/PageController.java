package com.cozy.notebooks.api;

import com.cozy.notebooks.api.dto.PageDtos.CreatePageRequest;
import com.cozy.notebooks.api.dto.PageDtos.PageResponse;
import com.cozy.notebooks.api.dto.PageDtos.UpdatePageRequest;
import com.cozy.notebooks.service.PageService;
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
@RequestMapping("/api/v1")
@Tag(name = "Pages")
public class PageController {

    private final PageService pageService;

    public PageController(PageService pageService) {
        this.pageService = pageService;
    }

    @GetMapping("/notebooks/{notebookId}/pages")
    public List<PageResponse> listForNotebook(@PathVariable UUID notebookId) {
        return pageService.listForNotebook(notebookId);
    }

    @PostMapping("/notebooks/{notebookId}/pages")
    public ResponseEntity<PageResponse> create(@PathVariable UUID notebookId,
                                               @Valid @RequestBody CreatePageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pageService.create(notebookId, request));
    }

    @GetMapping("/pages/{pageId}")
    public PageResponse get(@PathVariable UUID pageId) {
        return pageService.get(pageId);
    }

    @PatchMapping("/pages/{pageId}")
    public PageResponse update(@PathVariable UUID pageId,
                               @Valid @RequestBody UpdatePageRequest request) {
        return pageService.update(pageId, request);
    }

    @DeleteMapping("/pages/{pageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID pageId) {
        pageService.delete(pageId);
    }
}
