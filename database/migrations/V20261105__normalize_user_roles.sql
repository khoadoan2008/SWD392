-- Existing PostgreSQL database only (created before roles were moved to their own table).
-- Stop the backend, take pg_dump first, then:
-- psql -v ON_ERROR_STOP=1 -U postgres -d aives -f this-file.sql
-- Safe to run again: every step checks the current state first.
BEGIN;
LOCK TABLE users IN ACCESS EXCLUSIVE MODE;

CREATE TABLE IF NOT EXISTS roles (
    id          INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(20)  NOT NULL UNIQUE,
    description VARCHAR(200)
);
INSERT INTO roles (name, description)
SELECT v.name, v.description
FROM (VALUES ('ADMIN', 'Quản trị hệ thống'), ('LECTURER', 'Giảng viên'), ('STUDENT', 'Sinh viên')) AS v(name, description)
WHERE NOT EXISTS (SELECT 1 FROM roles r WHERE upper(r.name) = v.name);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = current_schema() AND table_name = 'users' AND column_name = 'role') THEN
        CREATE TABLE IF NOT EXISTS users_role_backup_20261105 AS SELECT id, role FROM users;
        ALTER TABLE users ADD COLUMN IF NOT EXISTS role_id INT;
        UPDATE users u SET role_id = r.id FROM roles r
        WHERE u.role_id IS NULL AND upper(trim(u.role)) = upper(r.name);
        IF EXISTS (SELECT 1 FROM users WHERE role_id IS NULL) THEN
            RAISE EXCEPTION 'Some users have an unknown role. Migration rolled back; fix users.role manually.';
        END IF;
        ALTER TABLE users DROP COLUMN role;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'users'::regclass AND conname = 'users_role_fk') THEN
        ALTER TABLE users ADD CONSTRAINT users_role_fk FOREIGN KEY (role_id) REFERENCES roles(id);
    END IF;
END $$;
ALTER TABLE users ALTER COLUMN role_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS ix_users_role_id ON users(role_id);
COMMIT;
