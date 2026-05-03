-- Join table linking catalogue tags to user accounts (SAD tags + users §7.x complement).
CREATE TABLE user_tags (
    user_id UUID NOT NULL,
    tag_id UUID NOT NULL,
    PRIMARY KEY (user_id, tag_id),
    CONSTRAINT fk_user_tags_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_tags_tag FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE
);

CREATE INDEX idx_user_tags_tag_id ON user_tags (tag_id);
