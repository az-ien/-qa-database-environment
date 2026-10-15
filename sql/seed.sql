-- Exactly four predictable dummy users. Safe to rerun: existing emails are left unchanged.

INSERT INTO users (email, status)
VALUES
    ('qa-test@example.com', 'active'),
    ('inactive@example.com', 'inactive'),
    ('pending@example.com', 'pending'),
    ('admin@example.com', 'active')
ON CONFLICT (email) DO NOTHING;
