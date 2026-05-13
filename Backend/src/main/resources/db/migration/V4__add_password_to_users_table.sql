-- V4__add_password_to_users_table.sql
-- Adds a password hash column to users for Spring Security authentication.

ALTER TABLE users
    ADD COLUMN password_hash VARCHAR(255) NULL AFTER email;

