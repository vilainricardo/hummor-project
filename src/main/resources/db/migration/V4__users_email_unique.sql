-- MindSignal API: canonical login/contact address; unique across users (PostgreSQL).
-- Rows may temporarily have NULL during legacy backfill—API requires email on writes.
ALTER TABLE users ADD COLUMN IF NOT EXISTS email VARCHAR(320);

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email ON users (email);
