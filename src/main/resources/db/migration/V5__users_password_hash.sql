-- MindSignal API: optional local credential (Argon2-encoded string); null for OAuth-only accounts.
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash TEXT;
