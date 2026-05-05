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
@Table(name = "notebooks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotebookEntity extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 36)
    private UUID userId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "color")
    private String color;

    @Column(name = "icon")
    private String icon;

    @Column(name = "position", nullable = false)
    private int position;
}
