CREATE TABLE codebase_members
(
    id      BIGINT AUTO_INCREMENT NOT NULL,
    repo_id BIGINT                NOT NULL,
    user_id BIGINT                NOT NULL,
    `role`  VARCHAR(50)           NOT NULL,
    CONSTRAINT pk_codebase_members PRIMARY KEY (id)
);

ALTER TABLE codebase_members
    ADD CONSTRAINT uc_a44c82c4baa952d5631c75cf9 UNIQUE (repo_id, user_id);

ALTER TABLE codebase_members
    ADD CONSTRAINT FK_CODEBASE_MEMBERS_ON_REPO FOREIGN KEY (repo_id) REFERENCES codebases (id) ON DELETE CASCADE;

ALTER TABLE codebase_members
    ADD CONSTRAINT FK_CODEBASE_MEMBERS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;