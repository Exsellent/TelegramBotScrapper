--liquibase formatted sql

--changeset Exsellent:06-create_tags_table
--preconditions onFail:MARK_RAN
CREATE TABLE tags (
                      tag_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                      name TEXT NOT NULL,
                      CONSTRAINT unique_tag_name UNIQUE (name)
);
