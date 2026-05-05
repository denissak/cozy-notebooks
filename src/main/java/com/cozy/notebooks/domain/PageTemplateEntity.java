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

@Entity
@Table(name = "page_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageTemplateEntity extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 36)
    private UUID userId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "icon")
    private String icon;

    /**
     * Array of block descriptors:
     * [{ "type": "...", "content": {...}, "settings": {...}, "position": N }, ...]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "blocks", nullable = false, columnDefinition = "json")
    private JsonNode blocks;

    @Column(name = "is_built_in", nullable = false)
    private boolean builtIn;
}
