package com.cozy.notebooks.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

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

    @Column(name = "parent_page_id", length = 36)
    private UUID parentPageId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "icon")
    private String icon;

    @Column(name = "cover_url")
    private String coverUrl;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "is_favorite", nullable = false)
    private boolean favorite;
}
