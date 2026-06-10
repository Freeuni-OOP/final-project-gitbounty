ALTER TABLE issues
    ADD CONSTRAINT uc_issues_number UNIQUE (number);

ALTER TABLE issues
    MODIFY target_branch_id BIGINT NOT NULL;