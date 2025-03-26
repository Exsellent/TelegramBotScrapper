--liquibase formatted sql
--changeset Exsellent:04
--preconditions onFail:MARK_RAN
ALTER TABLE chat_link DROP CONSTRAINT IF EXISTS chat_link_chat_id_fkey;
ALTER TABLE chat_link DROP CONSTRAINT IF EXISTS chat_link_link_id_fkey;

ALTER TABLE chat_link
    ADD CONSTRAINT chat_link_chat_id_fkey
        FOREIGN KEY (chat_id) REFERENCES chat(chat_id) ON DELETE CASCADE;

ALTER TABLE chat_link
    ADD CONSTRAINT chat_link_link_id_fkey
        FOREIGN KEY (link_id) REFERENCES link(link_id) ON DELETE CASCADE;
