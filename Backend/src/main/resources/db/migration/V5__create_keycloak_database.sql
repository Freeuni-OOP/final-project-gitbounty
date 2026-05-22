-- V5__create_keycloak_database.sql
-- Creates the Keycloak database and grants a dedicated user access to it.

CREATE DATABASE IF NOT EXISTS keycloak_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'keycloak_user'@'%' IDENTIFIED BY 'keycloak_password';
GRANT ALL PRIVILEGES ON keycloak_dev.* TO 'keycloak_user'@'%';
FLUSH PRIVILEGES;

