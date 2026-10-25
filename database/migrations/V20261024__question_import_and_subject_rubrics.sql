-- Existing PostgreSQL database only. Stop the backend, take pg_dump first.
-- psql -v ON_ERROR_STOP=1 -U postgres -d aives -f this-file.sql
-- Do NOT rerun aives_schema.sql on an existing database: that script drops tables.
BEGIN;
LOCK TABLE questions, rubrics IN ACCESS EXCLUSIVE MODE;

-- Preserve the original rubric rows before changing their ownership mapping.
CREATE TABLE IF NOT EXISTS rubrics_backup_20261024 AS SELECT * FROM rubrics;
ALTER TABLE questions ADD COLUMN IF NOT EXISTS difficulty VARCHAR(10) DEFAULT 'MEDIUM';
UPDATE questions SET difficulty = 'MEDIUM' WHERE difficulty IS NULL;
ALTER TABLE questions ALTER COLUMN difficulty SET DEFAULT 'MEDIUM';
ALTER TABLE questions ADD COLUMN IF NOT EXISTS notes TEXT;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'questions'::regclass AND conname = 'questions_difficulty_check') THEN
        ALTER TABLE questions ADD CONSTRAINT questions_difficulty_check CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD'));
    END IF;
END $$;

ALTER TABLE rubrics ADD COLUMN IF NOT EXISTS subject_id INTEGER;
DO $$
DECLARE constraint_row RECORD;
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = 'rubrics' AND column_name = 'question_id') THEN
        UPDATE rubrics r SET subject_id = q.subject_id FROM questions q
        WHERE r.question_id = q.id AND r.subject_id IS NULL;
        ALTER TABLE rubrics ALTER COLUMN question_id DROP NOT NULL;
        -- A legacy ON DELETE CASCADE must no longer delete a shared subject rubric.
        FOR constraint_row IN SELECT conname FROM pg_constraint
            WHERE conrelid = 'rubrics'::regclass AND contype = 'f' AND confrelid = 'questions'::regclass
        LOOP
            EXECUTE format('ALTER TABLE rubrics DROP CONSTRAINT %I', constraint_row.conname);
        END LOOP;
        -- Retain question_id as historical provenance; no row or score is discarded.
    END IF;
    IF EXISTS (SELECT 1 FROM rubrics WHERE subject_id IS NULL) THEN
        RAISE EXCEPTION 'Cannot map orphan rubrics to a subject. Migration rolled back; resolve manually.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'rubrics'::regclass AND conname = 'rubrics_subject_fk') THEN
        ALTER TABLE rubrics ADD CONSTRAINT rubrics_subject_fk FOREIGN KEY (subject_id) REFERENCES subjects(id);
    END IF;
END $$;
ALTER TABLE rubrics ALTER COLUMN subject_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS ix_rubrics_subject_order ON rubrics(subject_id, sort_order, id);
COMMIT;
-- Review migrated subject criteria and their totals manually. Historical per-question
-- rubrics are preserved, NOT arbitrarily merged/rescaled into a new 10-point rubric.
