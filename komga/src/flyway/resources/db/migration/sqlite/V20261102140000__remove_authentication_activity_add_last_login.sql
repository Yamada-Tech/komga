-- Remove authentication activity tracking (not needed in this fork)
DROP INDEX IF EXISTS idx__authentication_activity__user_id;
DROP TABLE IF EXISTS AUTHENTICATION_ACTIVITY;

-- Keep only the last login date per user
ALTER TABLE USER
    ADD COLUMN LAST_LOGIN_DATE datetime NULL DEFAULT NULL;
