-- ============================================
-- Fix: p_external_api_logs api_provider CHECK constraint
-- Issue: DB has 'CHATGPT' but code uses 'GEMINI'
-- ============================================

-- Connect to notification database
\c oneforlogis_notification

-- Drop old constraint
ALTER TABLE p_external_api_logs
    DROP CONSTRAINT IF EXISTS p_external_api_logs_api_provider_check;

-- Add new constraint with GEMINI instead of CHATGPT
ALTER TABLE p_external_api_logs
    ADD CONSTRAINT p_external_api_logs_api_provider_check
    CHECK (api_provider IN ('SLACK', 'GEMINI', 'NAVER_MAPS'));

-- Verify the change
SELECT conname, pg_get_constraintdef(oid)
FROM pg_constraint
WHERE conname = 'p_external_api_logs_api_provider_check';

\echo 'Successfully updated api_provider constraint from CHATGPT to GEMINI'