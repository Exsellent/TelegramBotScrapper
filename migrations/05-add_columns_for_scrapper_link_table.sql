--liquibase formatted sql
--changeset Exsellent:05

ALTER TABLE link
    ADD COLUMN last_check_time TIMESTAMP WITH TIME ZONE,
    ADD COLUMN last_update_time TIMESTAMP WITH TIME ZONE;
