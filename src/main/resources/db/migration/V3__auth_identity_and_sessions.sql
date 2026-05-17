-- Authentication: identities linked to internal users plus refresh-token sessions.

CREATE TABLE user_identity (
    id                CHAR(36)      NOT NULL,
    user_id           CHAR(36)      NOT NULL,
    provider          VARCHAR(32)   NOT NULL,
    provider_subject  VARCHAR(255)  NOT NULL,
    email             VARCHAR(320)  NULL,
    email_verified    TINYINT(1)    NOT NULL DEFAULT 0,
    password_hash     VARCHAR(255)  NULL,
    created_at        TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    last_login_at     TIMESTAMP(6)  NULL,
    deleted_at        TIMESTAMP(6)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY ux_user_identity_provider_subject (provider, provider_subject),
    KEY ix_user_identity_user_id (user_id),
    KEY ix_user_identity_email (email),
    KEY ix_user_identity_deleted_at (deleted_at),
    CONSTRAINT fk_user_identity_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE refresh_token_session (
    id                 CHAR(36)      NOT NULL,
    user_id            CHAR(36)      NOT NULL,
    device_id          CHAR(36)      NULL,
    refresh_token_hash CHAR(64)      NOT NULL,
    expires_at         TIMESTAMP(6)  NOT NULL,
    revoked_at         TIMESTAMP(6)  NULL,
    created_at         TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_used_at       TIMESTAMP(6)  NULL,
    user_agent         VARCHAR(512)  NULL,
    ip_address         VARCHAR(64)   NULL,
    PRIMARY KEY (id),
    UNIQUE KEY ux_refresh_token_session_hash (refresh_token_hash),
    KEY ix_refresh_token_session_user_id (user_id),
    KEY ix_refresh_token_session_expires_at (expires_at),
    KEY ix_refresh_token_session_revoked_at (revoked_at),
    CONSTRAINT fk_refresh_token_session_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
