\set ON_ERROR_STOP on
-- Run only inside an isolated scratch database, never the real aives database.
CREATE SCHEMA import_migration_test;
SET search_path TO import_migration_test;
CREATE TABLE subjects(id INTEGER PRIMARY KEY);
CREATE TABLE questions(id INTEGER PRIMARY KEY, subject_id INTEGER NOT NULL REFERENCES subjects(id));
CREATE TABLE rubrics(id INTEGER PRIMARY KEY, question_id INTEGER NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
                     criterion TEXT NOT NULL, max_score NUMERIC(5,2), sort_order INTEGER);
INSERT INTO subjects VALUES(1);
INSERT INTO questions VALUES(10,1),(11,1);
INSERT INTO rubrics VALUES(100,10,'Legacy A',4,1),(101,11,'Legacy B',6,2);
\ir ../migrations/V20261024__question_import_and_subject_rubrics.sql
\ir ../migrations/V20261024__question_import_and_subject_rubrics.sql
DO $$ BEGIN
    IF (SELECT count(*) FROM rubrics_backup_20261024) <> 2 OR (SELECT count(*) FROM rubrics WHERE subject_id=1) <> 2 THEN
        RAISE EXCEPTION 'Backup or backfill failed';
    END IF;
    IF EXISTS (SELECT 1 FROM questions WHERE difficulty <> 'MEDIUM' OR difficulty IS NULL) THEN RAISE EXCEPTION 'Difficulty backfill failed'; END IF;
END $$;
DELETE FROM questions WHERE id=10;
INSERT INTO rubrics(id,subject_id,criterion,max_score,sort_order) VALUES(102,1,'New shared criterion',2,3);
DO $$ BEGIN
    IF (SELECT count(*) FROM rubrics) <> 3 THEN RAISE EXCEPTION 'Legacy cascading delete still active'; END IF;
END $$;
SET search_path TO public;
-- A failed orphan backfill must not leave half-applied DDL/data.
CREATE SCHEMA import_migration_orphan_test;
SET search_path TO import_migration_orphan_test;
CREATE TABLE subjects(id INTEGER PRIMARY KEY);
CREATE TABLE questions(id INTEGER PRIMARY KEY, subject_id INTEGER NOT NULL);
CREATE TABLE rubrics(id INTEGER PRIMARY KEY, question_id INTEGER NOT NULL, criterion TEXT, max_score NUMERIC, sort_order INTEGER);
INSERT INTO rubrics VALUES(1,999,'Orphan',1,1);
\set ON_ERROR_STOP off
\ir ../migrations/V20261024__question_import_and_subject_rubrics.sql
\set ON_ERROR_STOP on
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='import_migration_orphan_test' AND table_name='questions' AND column_name='difficulty') THEN
        RAISE EXCEPTION 'Failed migration did not roll back';
    END IF;
    IF (SELECT count(*) FROM rubrics) <> 1 THEN RAISE EXCEPTION 'Orphan row was lost'; END IF;
END $$;
SELECT 'MIGRATION_BACKUP_BACKFILL_IDEMPOTENCY_CASCADE_ROLLBACK_PASS' AS result;
