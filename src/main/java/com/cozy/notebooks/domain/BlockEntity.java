package com.cozy.notebooks.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "blocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockEntity extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 36)
    private UUID userId;

    @Column(name = "notebook_id", nullable = false, length = 36)
    private UUID notebookId;

    @Column(name = "page_id", nullable = false, length = 36)
    private UUID pageId;

    @Column(name = "parent_block_id", length = 36)
    private UUID parentBlockId;

    /**
     * Stored in lowercase to match the DB CHECK constraint and to keep
     * the wire-format identical to what the API exposes.
     */
    @Column(name = "type", nullable = false, length = 32)
    private String type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", nullable = false, columnDefinition = "json")
    private JsonNode content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", nullable = false, columnDefinition = "json")
    private JsonNode settings;

    @Column(name = "position", nullable = false)
    private int position;

    public BlockType getTypeEnum() {
        return type == null ? null : BlockType.valueOf(type.toUpperCase(Locale.ROOT));
    }

    public void setTypeEnum(BlockType blockType) {
        this.type = blockType == null ? null : blockType.wireValue();
    }
}
