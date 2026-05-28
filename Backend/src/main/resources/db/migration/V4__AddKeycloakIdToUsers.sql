ALTER TABLE users
    ADD keycloak_id VARCHAR(255) NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT uc_users_keycloak UNIQUE (keycloak_id);