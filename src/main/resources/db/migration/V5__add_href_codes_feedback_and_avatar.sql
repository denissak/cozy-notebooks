-- Notebook and template shareable codes, feedback inbox, OAuth avatar URL.

ALTER TABLE notebooks
    ADD COLUMN href_code VARCHAR(18) NULL AFTER user_id;

ALTER TABLE page_templates
    ADD COLUMN href_code VARCHAR(18) NULL AFTER user_id;

-- Backfill deterministic 18-char codes (URL-safe alphabet, no sequential ids).
UPDATE notebooks
SET href_code = CONCAT(
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-notebook-href-backfill-v5'), 512), 1, 13), 16, 10), 33) + 1, 1),
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-notebook-href-backfill-v5'), 512), 7, 13), 16, 10), 33) + 1, 1),
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-notebook-href-backfill-v5'), 512), 13, 13), 16, 10), 33) + 1, 1),
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-notebook-href-backfill-v5'), 512), 19, 13), 16, 10), 33) + 1, 1),
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-notebook-href-backfill-v5'), 512), 25, 13), 16, 10), 33) + 1, 1),
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-notebook-href-backfill-v5'), 512), 31, 13), 16, 10), 33) + 1, 1),
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-notebook-href-backfill-v5'), 512), 37, 13), 16, 10), 33) + 1, 1),
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-notebook-href-backfill-v5'), 512), 43, 13), 16, 10), 33) + 1, 1),
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-notebook-href-backfill-v5'), 512), 49, 13), 16, 10), 33) + 1, 1),
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-notebook-href-backfill-v5'), 512), 55, 13), 16, 10), 33) + 1, 1),
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-notebook-href-backfill-v5'), 512), 61, 13), 16, 10), 33) + 1, 1),
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-notebook-href-backfill-v5'), 512), 67, 13), 16, 10), 33) + 1, 1),
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-notebook-href-backfill-v5'), 512), 73, 13), 16, 10), 33) + 1, 1),
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-notebook-href-backfill-v5'), 512), 79, 13), 16, 10), 33) + 1, 1),
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-notebook-href-backfill-v5'), 512), 85, 13), 16, 10), 33) + 1, 1),
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-notebook-href-backfill-v5'), 512), 91, 13), 16, 10), 33) + 1, 1),
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-notebook-href-backfill-v5'), 512), 97, 13), 16, 10), 33) + 1, 1),
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-notebook-href-backfill-v5'), 512), 103, 13), 16, 10), 33) + 1, 1)
    )
WHERE href_code IS NULL;

UPDATE page_templates
SET href_code = CONCAT(
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-page-template-href-backfill-v5'), 512), 1, 13), 16, 10), 33) + 1, 1),
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-page-template-href-backfill-v5'), 512), 7, 13), 16, 10), 33) + 1, 1),
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-page-template-href-backfill-v5'), 512), 13, 13), 16, 10), 33) + 1, 1),
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-page-template-href-backfill-v5'), 512), 19, 13), 16, 10), 33) + 1, 1),
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-page-template-href-backfill-v5'), 512), 25, 13), 16, 10), 33) + 1, 1),
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-page-template-href-backfill-v5'), 512), 31, 13), 16, 10), 33) + 1, 1),
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-page-template-href-backfill-v5'), 512), 37, 13), 16, 10), 33) + 1, 1),
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-page-template-href-backfill-v5'), 512), 43, 13), 16, 10), 33) + 1, 1),
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-page-template-href-backfill-v5'), 512), 49, 13), 16, 10), 33) + 1, 1),
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-page-template-href-backfill-v5'), 512), 55, 13), 16, 10), 33) + 1, 1),
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-page-template-href-backfill-v5'), 512), 61, 13), 16, 10), 33) + 1, 1),
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-page-template-href-backfill-v5'), 512), 67, 13), 16, 10), 33) + 1, 1),
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-page-template-href-backfill-v5'), 512), 73, 13), 16, 10), 33) + 1, 1),
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-page-template-href-backfill-v5'), 512), 79, 13), 16, 10), 33) + 1, 1),
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-page-template-href-backfill-v5'), 512), 85, 13), 16, 10), 33) + 1, 1),
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-page-template-href-backfill-v5'), 512), 91, 13), 16, 10), 33) + 1, 1),
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-page-template-href-backfill-v5'), 512), 97, 13), 16, 10), 33) + 1, 1),
        SUBSTRING('abcdefghijkmnopqrstuvwxyz23456789', MOD(CONV(SUBSTRING(SHA2(CONCAT(user_id, id, 'cozy-page-template-href-backfill-v5'), 512), 103, 13), 16, 10), 33) + 1, 1)
    )
WHERE href_code IS NULL;

ALTER TABLE notebooks
    MODIFY COLUMN href_code VARCHAR(18) NOT NULL;

ALTER TABLE page_templates
    MODIFY COLUMN href_code VARCHAR(18) NOT NULL;

ALTER TABLE notebooks
    ADD UNIQUE KEY ux_notebooks_user_href (user_id, href_code),
    ADD KEY ix_notebooks_href_code (href_code);

ALTER TABLE page_templates
    ADD UNIQUE KEY ux_page_templates_user_href (user_id, href_code),
    ADD KEY ix_page_templates_href_code (href_code);

ALTER TABLE users
    ADD COLUMN avatar_url VARCHAR(1024) NULL AFTER display_name;

CREATE TABLE feedback (
    id         CHAR(36) NOT NULL,
    user_id    CHAR(36) NULL,
    `type`     VARCHAR(32) NOT NULL,
    message    TEXT       NOT NULL,
    status     VARCHAR(32) NOT NULL DEFAULT 'new',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY ix_feedback_user_id (user_id),
    KEY ix_feedback_status (status),
    KEY ix_feedback_created_at (created_at),
    CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE SET NULL,
    CONSTRAINT ck_feedback_type CHECK (`type` IN ('bug','idea','question','other')),
    CONSTRAINT ck_feedback_status CHECK (status IN ('new','reviewed','closed'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
