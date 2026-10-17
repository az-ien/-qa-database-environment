import { test as base } from '@playwright/test';
import { Client } from 'pg';

// A test that asks for db receives an open PostgreSQL client.
type DatabaseFixtures = {
  db: Client;
};

// Opens a connection before each test that asks for it, then closes that connection.
export const test = base.extend<DatabaseFixtures>({
  db: async ({}, use) => {
    // Open the disposable qa_test database on this machine as qa_user.
    const db = new Client({
      connectionString: 'postgresql://qa_user:qa_password@localhost:5432/qa_test',
    });
    // Connect before the test body runs.
    await db.connect();
    // Hand this connection to the test.
    await use(db);
    // Close the connection after the test, including when the test fails.
    await db.end();
  },
});

export { expect } from '@playwright/test';
