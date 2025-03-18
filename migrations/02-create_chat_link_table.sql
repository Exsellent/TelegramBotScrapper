--liquibase formatted sql
--changeset Exsellent:02
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'chat'

CREATE TABLE chat_link
(
    chat_id         BIGINT NOT NULL,
    link_id         BIGINT NOT NULL,
    shared_at       TIMESTAMP WITH TIME ZONE NOT NULL,

    PRIMARY KEY (chat_id, link_id),
    FOREIGN KEY (chat_id) REFERENCES chat(chat_id),
    FOREIGN KEY (link_id) REFERENCES link(link_id)
);
