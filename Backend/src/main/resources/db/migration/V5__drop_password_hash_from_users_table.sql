-- V5__drop_password_hash_from_users_table.sql
-- Removes the password hash column now that Keycloak handles authentication.

ALTER TABLE users
    DROP COLUMN password_hash;

