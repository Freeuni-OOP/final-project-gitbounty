CREATE TABLE commits
(
    id           BIGINT AUTO_INCREMENT NOT NULL,
    codebase_id  BIGINT                NOT NULL,
    commit_hash  VARCHAR(40)           NOT NULL,
    author_name  VARCHAR(100)          NOT NULL,
    author_email VARCHAR(255)          NOT NULL,
    message      LONGTEXT              NOT NULL,
    committed_at datetime              NOT NULL,
    CONSTRAINT pk_commits PRIMARY KEY (id)
);

ALTER TABLE commits
    ADD CONSTRAINT uc_codebase_with_commit UNIQUE (codebase_id, commit_hash);

ALTER TABLE commits
    ADD CONSTRAINT FK_COMMITS_ON_CODEBASE FOREIGN KEY (codebase_id) REFERENCES codebases (id) ON DELETE CASCADE;