-- Add credit_balance to users table
ALTER TABLE users
    ADD COLUMN credit_balance DECIMAL(10, 2) DEFAULT 0 NOT NULL;

-- Link the existing transactions table directly to the new bounties table
ALTER TABLE transactions
    ADD COLUMN bounty_id BIGINT,
ADD CONSTRAINT FK_TRANSACTIONS_ON_BOUNTY FOREIGN KEY (bounty_id) REFERENCES bounties (id);