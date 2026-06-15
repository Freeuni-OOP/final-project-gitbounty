CREATE TABLE transactions
(
    id            BIGINT AUTO_INCREMENT NOT NULL,
    from_user_id  BIGINT         NOT NULL,
    to_user_id    BIGINT         NOT NULL,
    amount        DECIMAL(10, 2) NOT NULL,
    status        VARCHAR(50)    NOT NULL,
    `description` VARCHAR(500) NULL,
    created_at    datetime       NOT NULL,
    updated_at    datetime       NOT NULL,
    resolved_at   datetime NULL,
    CONSTRAINT pk_transactions PRIMARY KEY (id)
);

ALTER TABLE transactions
    ADD CONSTRAINT FK_TRANSACTIONS_ON_FROM_USER FOREIGN KEY (from_user_id) REFERENCES users (id);

ALTER TABLE transactions
    ADD CONSTRAINT FK_TRANSACTIONS_ON_TO_USER FOREIGN KEY (to_user_id) REFERENCES users (id);