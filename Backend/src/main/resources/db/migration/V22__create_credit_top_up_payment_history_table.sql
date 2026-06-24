CREATE TABLE credit_top_up_payments
(
    id              BIGINT AUTO_INCREMENT NOT NULL,
    user_id         BIGINT         NOT NULL,
    amount_paid     DECIMAL(10, 2) NOT NULL,
    credits_granted DECIMAL(10, 2) NOT NULL,
    cardholder_name VARCHAR(255)   NOT NULL,
    card_brand      VARCHAR(50)    NOT NULL,
    card_last4      VARCHAR(4)     NOT NULL,
    expiry_month    INT            NOT NULL,
    expiry_year     INT            NOT NULL,
    idempotency_key VARCHAR(100)   NOT NULL,
    status          VARCHAR(50)    NOT NULL,
    created_at      datetime       NOT NULL,
    updated_at      datetime       NOT NULL,
    CONSTRAINT pk_credit_top_up_payments PRIMARY KEY (id),
    CONSTRAINT chk_credit_top_up_payments_amount_paid_positive CHECK (amount_paid > 0),
    CONSTRAINT chk_credit_top_up_payments_credits_granted_transaction CHECK (credits_granted > 0 AND credits_granted <= 1000)
);

ALTER TABLE credit_top_up_payments
    ADD CONSTRAINT FK_CREDIT_TOP_UP_PAYMENTS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

CREATE INDEX idx_credit_top_up_payments_user_id ON credit_top_up_payments (user_id);
CREATE UNIQUE INDEX uq_credit_top_up_payments_user_idempotency ON credit_top_up_payments (user_id, idempotency_key);
