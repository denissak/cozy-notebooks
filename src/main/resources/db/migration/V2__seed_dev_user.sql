-- Seed a dev user for the MVP mock-user provider.
-- This user is referenced by COZY_MOCK_USER_ID in application.yml.
INSERT IGNORE INTO users (id, email, display_name, password_hash, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'dev@cozy.local',
    'Dev User',
    NULL,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
);
