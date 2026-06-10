-- Add discriminator column to issues table for SINGLE_TABLE inheritance
ALTER TABLE issues ADD COLUMN type VARCHAR(31) NOT NULL DEFAULT 'ISSUE';

-- Add PR-specific columns to issues table
ALTER TABLE issues ADD COLUMN source_branch_id BIGINT;
ALTER TABLE issues ADD COLUMN target_branch_id BIGINT;
ALTER TABLE issues ADD COLUMN merged_at datetime NULL;


-- Add foreign keys for PR-specific columns
ALTER TABLE issues
    ADD CONSTRAINT FK_ISSUES_ON_SOURCE_BRANCH FOREIGN KEY (source_branch_id) REFERENCES branches (id) ON DELETE SET NULL;

ALTER TABLE issues
    ADD CONSTRAINT FK_ISSUES_ON_TARGET_BRANCH FOREIGN KEY (target_branch_id) REFERENCES branches (id) ON DELETE RESTRICT;


