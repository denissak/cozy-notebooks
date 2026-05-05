package com.cozy.notebooks.api;

import com.cozy.notebooks.api.dto.BlockDtos.BlockResponse;
import com.cozy.notebooks.api.dto.BlockDtos.CreateBlockRequest;
import com.cozy.notebooks.api.dto.BlockDtos.ReorderRequest;
import com.cozy.notebooks.api.dto.BlockDtos.UpdateBlockRequest;
import com.cozy.notebooks.service.BlockService;
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
@Tag(name = "Blocks")
public class BlockController {

    private final BlockService blockService;

    public BlockController(BlockService blockService) {
        this.blockService = blockService;
    }

    @GetMapping("/pages/{pageId}/blocks")
    public List<BlockResponse> listForPage(@PathVariable UUID pageId) {
        return blockService.listForPage(pageId);
    }

    @PostMapping("/pages/{pageId}/blocks")
    public ResponseEntity<BlockResponse> create(@PathVariable UUID pageId,
                                                @Valid @RequestBody CreateBlockRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(blockService.create(pageId, request));
    }

    @PatchMapping("/blocks/{blockId}")
    public BlockResponse update(@PathVariable UUID blockId,
                                @Valid @RequestBody UpdateBlockRequest request) {
        return blockService.update(blockId, request);
    }

    @DeleteMapping("/blocks/{blockId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID blockId) {
        blockService.delete(blockId);
    }

    @PostMapping("/pages/{pageId}/blocks/reorder")
    public List<BlockResponse> reorder(@PathVariable UUID pageId,
                                       @Valid @RequestBody ReorderRequest request) {
        return blockService.reorder(pageId, request);
    }
}
