--liquibase formatted sql
--changeset Exsellent:03
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = CURRENT_SCHEMA AND table_name = 'chat' AND column_name = 'created_by'

ALTER TABLE chat DROP COLUMN created_by;
