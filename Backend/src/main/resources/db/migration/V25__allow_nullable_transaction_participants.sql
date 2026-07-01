ALTER TABLE transactions
    MODIFY COLUMN from_user_id BIGINT NULL,
    MODIFY COLUMN to_user_id BIGINT NULL;