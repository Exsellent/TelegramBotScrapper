--changeset Exsellent:07-create_link_tags_table
--preconditions onFail:MARK_RAN
CREATE TABLE link_tags (
                           link_id BIGINT NOT NULL,
                           tag_id BIGINT NOT NULL,
                           PRIMARY KEY (link_id, tag_id),
                           FOREIGN KEY (link_id) REFERENCES link(link_id) ON DELETE CASCADE,
                           FOREIGN KEY (tag_id) REFERENCES tags(tag_id) ON DELETE CASCADE
);

CREATE INDEX idx_link_tags_link_id ON link_tags(link_id);
