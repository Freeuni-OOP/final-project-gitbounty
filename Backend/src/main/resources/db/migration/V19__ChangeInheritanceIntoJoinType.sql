ALTER TABLE issues
    DROP FOREIGN KEY FK_ISSUES_ON_SOURCE_BRANCH;

ALTER TABLE issues
    DROP FOREIGN KEY FK_ISSUES_ON_TARGET_BRANCH;

CREATE TABLE pull_request
(
    id               BIGINT   NOT NULL,
    source_branch_id BIGINT   NULL,
    target_branch_id BIGINT   NOT NULL,
    merged_at        datetime NULL,
    CONSTRAINT pk_pullrequest PRIMARY KEY (id)
);


ALTER TABLE pull_request
    ADD CONSTRAINT FK_PULLREQUEST_ON_ID FOREIGN KEY (id) REFERENCES issues (id);

ALTER TABLE pull_request
    ADD CONSTRAINT FK_PULLREQUEST_ON_SOURCE_BRANCH FOREIGN KEY (source_branch_id) REFERENCES branches (id);

ALTER TABLE pull_request
    ADD CONSTRAINT FK_PULLREQUEST_ON_TARGET_BRANCH FOREIGN KEY (target_branch_id) REFERENCES branches (id);

ALTER TABLE issues
    DROP COLUMN merged_at;

ALTER TABLE issues
    DROP COLUMN source_branch_id;

ALTER TABLE issues
    DROP COLUMN target_branch_id;