ALTER TABLE issues
    ADD COLUMN assigned_to_id BIGINT NULL;

ALTER TABLE issues
    ADD CONSTRAINT fk_issues_assigned_to
        FOREIGN KEY (assigned_to_id) REFERENCES users(id);
