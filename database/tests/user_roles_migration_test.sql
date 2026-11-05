\set ON_ERROR_STOP on
-- Run only inside an isolated scratch database, never the real aives database.
CREATE SCHEMA role_migration_test;
SET search_path TO role_migration_test;
CREATE TABLE users(id INTEGER PRIMARY KEY, username VARCHAR(50) NOT NULL UNIQUE,
                   role VARCHAR(20) NOT NULL CHECK (upper(role) IN ('ADMIN','LECTURER','STUDENT')));
INSERT INTO users VALUES (1,'admin','ADMIN'),(2,'gv','LECTURER'),(3,'sv1','STUDENT'),(4,'sv2','student');
\ir ../migrations/V20261105__normalize_user_roles.sql
\ir ../migrations/V20261105__normalize_user_roles.sql
DO $$ BEGIN
    IF (SELECT count(*) FROM roles) <> 3 THEN RAISE EXCEPTION 'Roles not seeded exactly once'; END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='role_migration_test' AND table_name='users' AND column_name='role') THEN
        RAISE EXCEPTION 'Legacy role column still present';
    END IF;
    IF (SELECT count(*) FROM users u JOIN roles r ON r.id = u.role_id WHERE r.name = 'STUDENT') <> 2 THEN
        RAISE EXCEPTION 'Student backfill failed';
    END IF;
    IF (SELECT r.name FROM users u JOIN roles r ON r.id = u.role_id WHERE u.id = 1) <> 'ADMIN' THEN RAISE EXCEPTION 'Admin backfill failed'; END IF;
    IF (SELECT count(*) FROM users_role_backup_20261105) <> 4 THEN RAISE EXCEPTION 'Backup missing'; END IF;
END $$;
SET search_path TO public;
-- An unknown legacy role must roll back the whole migration.
CREATE SCHEMA role_migration_bad_test;
SET search_path TO role_migration_bad_test;
CREATE TABLE users(id INTEGER PRIMARY KEY, username VARCHAR(50) NOT NULL, role VARCHAR(20) NOT NULL);
INSERT INTO users VALUES (1,'x','GUEST');
\set ON_ERROR_STOP off
\ir ../migrations/V20261105__normalize_user_roles.sql
\set ON_ERROR_STOP on
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='role_migration_bad_test' AND table_name='users' AND column_name='role') THEN
        RAISE EXCEPTION 'Failed migration did not roll back';
    END IF;
    IF to_regclass('role_migration_bad_test.roles') IS NOT NULL THEN RAISE EXCEPTION 'Roles table left behind'; END IF;
END $$;
SET search_path TO public;
DROP SCHEMA role_migration_test CASCADE;
DROP SCHEMA role_migration_bad_test CASCADE;
SELECT 'USER_ROLES_MIGRATION_BACKFILL_IDEMPOTENCY_ROLLBACK_PASS' AS result;
