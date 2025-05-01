--liquibase formatted sql
--changeset Exsellent:09
ALTER TABLE chat_link ADD COLUMN filters JSON;
