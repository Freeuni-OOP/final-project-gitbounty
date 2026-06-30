ALTER TABLE transactions
    ADD CONSTRAINT chk_transactions_has_participant
        CHECK (from_user_id IS NOT NULL OR to_user_id IS NOT NULL);