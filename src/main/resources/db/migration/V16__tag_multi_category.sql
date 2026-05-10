-- One tag may belong to several categories (M:N join).
CREATE TABLE tag_categories (
    tag_id UUID NOT NULL,
    category VARCHAR(50) NOT NULL,
    CONSTRAINT fk_tag_categories_tag FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE,
    CONSTRAINT pk_tag_categories PRIMARY KEY (tag_id, category)
);

CREATE INDEX idx_tag_categories_category ON tag_categories (category);

INSERT INTO tag_categories (tag_id, category)
SELECT id, category FROM tags;

ALTER TABLE tags DROP COLUMN category;
