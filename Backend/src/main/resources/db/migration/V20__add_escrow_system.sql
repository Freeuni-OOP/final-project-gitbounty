-- Add bounty_amount to issues table
ALTER TABLE issues
ADD COLUMN bounty_amount DECIMAL(10, 2) DEFAULT 0;

-- Add credit_balance to users table
ALTER TABLE users
ADD COLUMN credit_balance DECIMAL(10, 2) DEFAULT 0;

-- Add issue_id reference to transactions table to link bounties to transactions
ALTER TABLE transactions
ADD COLUMN issue_id BIGINT,
ADD CONSTRAINT FK_TRANSACTIONS_ON_ISSUE FOREIGN KEY (issue_id) REFERENCES issues (id);


