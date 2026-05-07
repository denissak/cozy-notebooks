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

import java.util.UUID;

/**
 * A page is a self-contained JSON document. The backend treats {@code contentJson}
 * as opaque flexible JSON — it does not interpret block structure inside it.
 *
 * <p>{@code contentHash} is a SHA-256 hex digest of the serialized
 * {@code contentJson}, recomputed on every successful write.
 * {@code version} starts at 1 and is incremented on each replace-content
 * update; it is intentionally not annotated with {@code @Version} so that
 * conflict detection happens explicitly via the {@code baseHash} request
 * field rather than implicit JPA optimistic locking.
 */
@Entity
@Table(name = "pages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageEntity extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 36)
    private UUID userId;

    @Column(name = "notebook_id", nullable = false, length = 36)
    private UUID notebookId;

    @Column(name = "title", nullable = false)
    private String title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_json", nullable = false, columnDefinition = "json")
    private JsonNode contentJson;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "version", nullable = false)
    private long version;
}
