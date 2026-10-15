-- Exactly four predictable dummy users. Safe to rerun: existing emails are left unchanged.

INSERT INTO users (name, email, status, salary)
VALUES
    ('Maya Chen', 'qa-test@example.com', 'active', 480),
    ('Luis Ortega', 'inactive@example.com', 'inactive', 125),
    ('Priya Shah', 'pending@example.com', 'pending', 860),
    ('Owen Blake', 'admin@example.com', 'active', 310)
ON CONFLICT (email) DO NOTHING;
