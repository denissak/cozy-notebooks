-- User plan foundation (free / pro). Billing integration will update plan_code later.

ALTER TABLE users
    ADD COLUMN plan_code VARCHAR(32) NOT NULL DEFAULT 'free' AFTER avatar_url;

UPDATE users
SET plan_code = 'free'
WHERE plan_code IS NULL OR plan_code = '';
