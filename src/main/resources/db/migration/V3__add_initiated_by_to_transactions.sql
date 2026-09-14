ALTER TABLE transactions
ADD COLUMN initiated_by VARCHAR(255) NOT NULL DEFAULT 'system@bank.internal';

ALTER TABLE transactions
ALTER COLUMN initiated_by DROP DEFAULT;