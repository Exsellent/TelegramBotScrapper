--liquibase formatted sql
--changeset Exsellent:08
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = CURRENT_SCHEMA AND table_name = 'link' AND column_name = 'created_by'

ALTER TABLE link DROP COLUMN created_by;
