--liquibase formatted sql
--changeset Exsellent:05
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'link'

ALTER TABLE link
    ADD COLUMN last_check_time TIMESTAMP WITH TIME ZONE,
    ADD COLUMN last_update_time TIMESTAMP WITH TIME ZONE;
