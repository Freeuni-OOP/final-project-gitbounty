CREATE TABLE branches
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    codebase_id      BIGINT                NOT NULL,
    name             VARCHAR(255)          NOT NULL,
    latest_commit_id BIGINT                NULL,
    updated_at       datetime              NOT NULL,
    CONSTRAINT pk_branches PRIMARY KEY (id)
);

ALTER TABLE branches
    ADD CONSTRAINT uc_branch_in_codebase UNIQUE (codebase_id, name);

ALTER TABLE branches
    ADD CONSTRAINT FK_BRANCHES_ON_CODEBASE FOREIGN KEY (codebase_id) REFERENCES codebases (id);

ALTER TABLE branches
    ADD CONSTRAINT FK_BRANCHES_ON_LATEST_COMMIT FOREIGN KEY (latest_commit_id) REFERENCES commits (id);