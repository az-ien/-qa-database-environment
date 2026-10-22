import { test, expect } from './database.fixture';



//result.rows is that array. result.rows[0] is the only row. result.rows[0].count is the value in the count column. A query that returns four user rows uses the same array, with four objects in it instead of one.



// Confirms a fresh database contains the four seeded users and no extra rows.
test('exactly four seeded users exist', async ({ db }) => {
  // Ask the database how many rows are in the users table.
  const result = await db.query('SELECT COUNT(*) FROM users');
  // Read the count. PostgreSQL returns COUNT(*) as a string.
  const count = Number(result.rows[0].count);

  // Fail unless that count is exactly 4.
  expect(count).toBe(4);
});


// Confirms the seeded user named Owen Blake is stored.
test('Owen Blake exists', async ({ db }) => {
  // Read the user row stored for Owen Blake's email. Column names are the object keys.
  const result = await db.query(
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
  const result = await db.query('SELECT * FROM users WHERE salary > 400');
  // Read all of those user rows.
  const rows = result.rows;

  // Check every returned row.
  for (const row of rows) {
    // Fail unless this row's salary column is greater than 400.
    expect(row.salary).toBeGreaterThan(400);
  }
  
  // Fail unless the query returned exactly two rows.
  expect(rows).toHaveLength(2);

});


// Check how many active users exist. The seeded users have two active and two inactive.
test('users that are active',async({db}) => {
  const result  = await db.query("Select * from users where status  = 'active'");
  const rows  =result.rows;
  for (const row of rows)
  {
    expect(row.status).toBe('active');
    expect(['qa-test@example.com', 'admin@example.com']).toContain(row.email);
  }
  expect(rows).toHaveLength(2)

  

});
