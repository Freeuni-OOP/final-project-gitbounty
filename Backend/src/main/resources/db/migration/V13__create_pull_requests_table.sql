CREATE TABLE pull_requests
(
    id                BIGINT AUTO_INCREMENT NOT NULL,
    codebase_id       BIGINT                NOT NULL,
    pr_number         INT                   NOT NULL,
    title             VARCHAR(255)          NOT NULL,
    description       TEXT,
    status            VARCHAR(50)           NOT NULL,
    source_branch_id  BIGINT                NULL,
    target_branch_id  BIGINT                NOT NULL,
    author_id         BIGINT                NOT NULL,
    created_at        datetime              NOT NULL,
    updated_at        datetime              NOT NULL,
    merged_at         datetime              NULL,
    CONSTRAINT pk_pull_requests PRIMARY KEY (id)
);

ALTER TABLE pull_requests
    ADD CONSTRAINT uc_pr_in_codebase UNIQUE (codebase_id, pr_number);

ALTER TABLE pull_requests
    ADD CONSTRAINT FK_PULLREQUESTS_ON_CODEBASE FOREIGN KEY (codebase_id) REFERENCES codebases (id) ON DELETE CASCADE;

ALTER TABLE pull_requests
    ADD CONSTRAINT FK_PULLREQUESTS_ON_SOURCE_BRANCH FOREIGN KEY (source_branch_id) REFERENCES branches (id) ON DELETE SET NULL;

ALTER TABLE pull_requests
    ADD CONSTRAINT FK_PULLREQUESTS_ON_TARGET_BRANCH FOREIGN KEY (target_branch_id) REFERENCES branches (id) ON DELETE RESTRICT;

ALTER TABLE pull_requests
    ADD CONSTRAINT FK_PULLREQUESTS_ON_AUTHOR FOREIGN KEY (author_id) REFERENCES users (id);


