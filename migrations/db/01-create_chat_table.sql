--liquibase formatted sql
--changeset Exsellent:01
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'chat'
CREATE TABLE chat
(
    chat_id         BIGINT NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      TEXT NOT NULL,
    PRIMARY KEY (chat_id)
);
