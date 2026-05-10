-- Stable machine code per catalogue tag; API i18n keys: tag.catalog.{code}.name / .description
ALTER TABLE tags ADD COLUMN code VARCHAR(64);

UPDATE tags
SET code = UPPER(REPLACE(REPLACE(TRIM(name), '-', '_'), ' ', '_'));

UPDATE tags
SET code = SUBSTRING(code FROM 1 FOR 64);

ALTER TABLE tags ALTER COLUMN code SET NOT NULL;

CREATE UNIQUE INDEX uk_tags_code ON tags (code);
