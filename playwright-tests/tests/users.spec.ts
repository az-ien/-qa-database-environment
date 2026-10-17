import { test, expect } from './database.fixture';

// One users row returned by node-pg. Column names are the object keys.
type UserRow = {
  name: string;
  email: string;
  status: string;
  salary: number;
};

// Confirms a fresh database contains the four seeded users and no extra rows.
test('exactly four seeded users exist', async ({ db }) => {
  // Ask the database how many rows are in the users table.
  const result = await db.query('SELECT COUNT(*) FROM users');
  // Read the count. PostgreSQL returns COUNT(*) as a string.
  const count = Number(result.rows[0].count);

  // Fail unless that count is exactly 4.
  expect(count).toBe(4);
});

// Confirms the four seeded emails are stored with the statuses written in the query.
test('seeded users have expected statuses', async ({ db }) => {
  // Count rows that match one of the four expected email and status pairs.
  const result = await db.query(`
    SELECT COUNT(*)
    FROM users
    WHERE (email = 'qa-test@example.com' AND status = 'active')
       OR (email = 'inactive@example.com' AND status = 'inactive')
       OR (email = 'pending@example.com' AND status = 'pending')
       OR (email = 'admin@example.com' AND status = 'active')
  `);
  // Read the count of matching users.
  const count = Number(result.rows[0].count);

  // Fail unless all four expected email and status pairs were found.
  expect(count).toBe(4);
});

// Confirms the seeded user named Owen Blake is stored.
test('Owen Blake exists', async ({ db }) => {
  // Read the user row stored for Owen Blake's email. Column names are the object keys.
  const result = await db.query<UserRow>(
    "SELECT name, email, status, salary FROM users WHERE email = 'admin@example.com'"
  );
  // Read that user row.
  const row = result.rows[0];

  // Fail unless the name field is Owen Blake.
  expect(row.name).toBe('Owen Blake');
});

// Confirms two user rows were returned and each salary is greater than 400.
test('salaries are greater than 400', async ({ db }) => {
  // Read every column for users whose salary is greater than 400.
  const result = await db.query<UserRow>('SELECT * FROM users WHERE salary > 400');
  // Read all of those user rows.
  const rows = result.rows;

  // Fail unless the query returned exactly two rows.
  expect(rows).toHaveLength(2);
  // Check every returned row.
  for (const row of rows) {
    // Fail unless this row's salary column is greater than 400.
    expect(row.salary).toBeGreaterThan(400);
  }
});
