package com.cozy.notebooks.api;

import com.cozy.notebooks.api.dto.NotebookDtos.CreateNotebookRequest;
import com.cozy.notebooks.api.dto.NotebookDtos.NotebookResponse;
import com.cozy.notebooks.api.dto.NotebookDtos.UpdateNotebookRequest;
import com.cozy.notebooks.service.NotebookService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notebooks")
@Tag(name = "Notebooks")
public class NotebookController {

    private final NotebookService notebookService;

    public NotebookController(NotebookService notebookService) {
        this.notebookService = notebookService;
    }

    @GetMapping
    public List<NotebookResponse> list() {
        return notebookService.list();
    }

    @PostMapping
    public ResponseEntity<NotebookResponse> create(@Valid @RequestBody CreateNotebookRequest request) {
        NotebookResponse created = notebookService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{notebookId}")
    public NotebookResponse get(@PathVariable UUID notebookId) {
        return notebookService.get(notebookId);
    }

    @PatchMapping("/{notebookId}")
    public NotebookResponse update(@PathVariable UUID notebookId,
                                   @Valid @RequestBody UpdateNotebookRequest request) {
        return notebookService.update(notebookId, request);
    }

    @DeleteMapping("/{notebookId}")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID notebookId) {
        notebookService.delete(notebookId);
    }
}
