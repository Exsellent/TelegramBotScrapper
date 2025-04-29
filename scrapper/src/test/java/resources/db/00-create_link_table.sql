--liquibase formatted sql
--changeset Exsellent:00
--preconditions onFail:MARK_RAN
CREATE TABLE link
(
    link_id         BIGINT GENERATED ALWAYS AS IDENTITY,
    url             TEXT NOT NULL,
    description     TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      TEXT NOT NULL,

    PRIMARY KEY (link_id),
    UNIQUE (url)
);
