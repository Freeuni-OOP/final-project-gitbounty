ALTER TABLE issues
    ADD CONSTRAINT uc_issues_number_per_repo UNIQUE (number, repository_id);