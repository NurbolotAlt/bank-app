CREATE TABLE loans (
    id BIGSERIAL PRIMARY KEY,
    lender_account_id BIGINT NOT NULL REFERENCES accounts(id),
    borrower_account_id BIGINT NOT NULL REFERENCES accounts(id),
    principal_amount NUMERIC(19, 4) NOT NULL,
    interest_rate NUMERIC(5, 2) NOT NULL,
    total_owed NUMERIC(19, 4) NOT NULL,
    remaining_amount NUMERIC(19, 4) NOT NULL,
    due_date TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
