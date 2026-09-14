WITH new_user AS (
    INSERT INTO users (email, password_hash, created_at)
    VALUES ('system@bank.internal', 'N/A', now())
    RETURNING id
),
new_account AS (
    INSERT INTO accounts (user_id, account_number, currency, status, created_at)
    SELECT id, 'EXTERNAL', 'USD', 'ACTIVE', now()
    FROM new_user
    RETURNING id
),
new_transaction AS (
    INSERT INTO transactions (idempotency_key, type, status, created_at)
    VALUES ('seed-external-init', 'SEED', 'COMPLETED', now())
    RETURNING id
)
INSERT INTO ledger_entries (account_id, transaction_id, amount, entry_type, created_at)
SELECT new_account.id, new_transaction.id, 999999999999.0000, 'CREDIT', now()
FROM new_account, new_transaction;