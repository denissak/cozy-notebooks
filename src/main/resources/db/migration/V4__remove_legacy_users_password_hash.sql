-- Password hashes are stored on user_identity (email provider); users.password_hash is legacy.

ALTER TABLE users DROP COLUMN password_hash;
