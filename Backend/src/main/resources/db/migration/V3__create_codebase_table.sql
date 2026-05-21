-- V3__create_codebase_table.sql
-- Creates the codebases table and links each row to the user who owns it.

CREATE TABLE IF NOT EXISTS codebases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    git_url VARCHAR(2048) NOT NULL,
    owner_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_codebases_name UNIQUE (name),
    CONSTRAINT fk_codebases_owner FOREIGN KEY (owner_id) REFERENCES users(id)
);

