ALTER TABLE users
    ADD keycloak_id VARCHAR(255) NULL;

ALTER TABLE users
    MODIFY keycloak_id VARCHAR(255) NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT uc_users_keycloak UNIQUE (keycloak_id);

CREATE INDEX idx_users_keycloak_id ON users (keycloak_id);