-- Initial schema for cozy-notebooks (MySQL 8.4 / HeatWave compatible).
--
-- Architecture: pages own the entire page document as a JSON column. There is
-- no separate `blocks` table — block structure lives inside pages.content_json
-- and is owned/validated by the frontend. The backend treats content as opaque.
--
-- Conventions:
--   - UUIDs are stored as CHAR(36).
--   - Timestamps are TIMESTAMP(6) (microsecond precision, UTC at the connection layer).
--   - JSON columns use the native JSON type.
--   - Soft delete is implemented via the deleted_at column. We use composite
--     indexes (..., deleted_at, ...) since MySQL does not support partial indexes.
--   - content_hash is SHA-256(ObjectMapper.writeValueAsBytes(content_json)),
--     stored as a 64-char lowercase hex string.

-- ============================================================
-- USERS
-- ============================================================
CREATE TABLE users (
    id            CHAR(36)      NOT NULL,
    email         VARCHAR(255)  NOT NULL,
    display_name  VARCHAR(255),
    password_hash VARCHAR(255),
    created_at    TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at    TIMESTAMP(6)  NULL,
    PRIMARY KEY (id),
    -- For MVP an email is owned by a user forever, even after soft-delete.
    UNIQUE KEY ux_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- NOTEBOOKS
-- ============================================================
CREATE TABLE notebooks (
    id          CHAR(36)     NOT NULL,
    user_id     CHAR(36)     NOT NULL,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    color       VARCHAR(32),
    icon        VARCHAR(64),
    position    INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at  TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    KEY ix_notebooks_user_deleted (user_id, deleted_at),
    CONSTRAINT fk_notebooks_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- PAGES (full document storage)
--
-- content_json holds the whole page document, including the internal blocks
-- array. The backend does not interpret its shape.
-- content_hash and version enable optimistic-concurrency conflict detection.
-- ============================================================
CREATE TABLE pages (
    id           CHAR(36)     NOT NULL,
    user_id      CHAR(36)     NOT NULL,
    notebook_id  CHAR(36)     NOT NULL,
    title        VARCHAR(255) NOT NULL,
    content_json JSON         NOT NULL,
    content_hash CHAR(64)     NOT NULL,
    version      BIGINT       NOT NULL DEFAULT 1,
    created_at   TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at   TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    KEY ix_pages_notebook_deleted (notebook_id, deleted_at),
    KEY ix_pages_user_deleted (user_id, deleted_at),
    CONSTRAINT fk_pages_user     FOREIGN KEY (user_id)     REFERENCES users(id)     ON DELETE CASCADE,
    CONSTRAINT fk_pages_notebook FOREIGN KEY (notebook_id) REFERENCES notebooks(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- PAGE TEMPLATES (also full document storage)
--
-- A template is a reusable page document. Instantiation copies content_json
-- into a brand-new pages row.
-- ============================================================
CREATE TABLE page_templates (
    id           CHAR(36)     NOT NULL,
    user_id      CHAR(36)     NOT NULL,
    name         VARCHAR(255) NOT NULL,
    description  TEXT,
    icon         VARCHAR(64),
    content_json JSON         NOT NULL,
    content_hash CHAR(64)     NOT NULL,
    is_built_in  TINYINT(1)   NOT NULL DEFAULT 0,
    created_at   TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at   TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    KEY ix_page_templates_user_deleted (user_id, deleted_at),
    CONSTRAINT fk_page_templates_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- ASSETS (stub for MVP)
-- ============================================================
CREATE TABLE assets (
    id          CHAR(36)      NOT NULL,
    user_id     CHAR(36)      NOT NULL,
    file_name   VARCHAR(512)  NOT NULL,
    mime_type   VARCHAR(128)  NOT NULL,
    byte_size   BIGINT        NOT NULL,
    storage_url VARCHAR(2048) NOT NULL,
    created_at  TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at  TIMESTAMP(6)  NULL,
    PRIMARY KEY (id),
    KEY ix_assets_user_deleted (user_id, deleted_at),
    CONSTRAINT fk_assets_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- TAGS / PAGE_TAGS (stub for MVP)
-- ============================================================
CREATE TABLE tags (
    id          CHAR(36)     NOT NULL,
    user_id     CHAR(36)     NOT NULL,
    name        VARCHAR(128) NOT NULL,
    color       VARCHAR(32),
    created_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at  TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    -- For MVP a tag name is owned by a user forever, even after soft-delete.
    UNIQUE KEY ux_tags_user_name (user_id, name),
    KEY ix_tags_user_deleted (user_id, deleted_at),
    CONSTRAINT fk_tags_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE page_tags (
    page_id    CHAR(36)     NOT NULL,
    tag_id     CHAR(36)     NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (page_id, tag_id),
    KEY ix_page_tags_tag (tag_id),
    CONSTRAINT fk_page_tags_page FOREIGN KEY (page_id) REFERENCES pages(id) ON DELETE CASCADE,
    CONSTRAINT fk_page_tags_tag  FOREIGN KEY (tag_id)  REFERENCES tags(id)  ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- DEVICES (stub for MVP / future sync)
-- ============================================================
CREATE TABLE devices (
    id           CHAR(36)     NOT NULL,
    user_id      CHAR(36)     NOT NULL,
    name         VARCHAR(255) NOT NULL,
    platform     VARCHAR(32),
    last_seen_at TIMESTAMP(6) NULL,
    created_at   TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at   TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    KEY ix_devices_user_deleted (user_id, deleted_at),
    CONSTRAINT fk_devices_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT ck_devices_platform CHECK (platform IS NULL OR platform IN (
        'ios','android','web','macos','windows','linux'
    ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- SYNC_CHANGES (stub for MVP / future sync)
--
-- Note: 'block' is no longer a valid entity_type — pages are atomic for sync.
-- ============================================================
CREATE TABLE sync_changes (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     CHAR(36)     NOT NULL,
    device_id   CHAR(36)     NULL,
    entity_type VARCHAR(32)  NOT NULL,
    entity_id   CHAR(36)     NOT NULL,
    operation   VARCHAR(16)  NOT NULL,
    payload     JSON         NULL,
    created_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY ix_sync_changes_user_created (user_id, created_at),
    CONSTRAINT fk_sync_changes_user   FOREIGN KEY (user_id)   REFERENCES users(id)   ON DELETE CASCADE,
    CONSTRAINT fk_sync_changes_device FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE SET NULL,
    CONSTRAINT ck_sync_changes_entity_type CHECK (entity_type IN (
        'notebook','page','template','asset','tag'
    )),
    CONSTRAINT ck_sync_changes_operation CHECK (operation IN (
        'create','update','delete','restore','reorder'
    ))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
