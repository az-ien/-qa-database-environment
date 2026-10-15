"""Validate the freshly seeded PostgreSQL users table."""

import psycopg  # Driver that lets Python talk to PostgreSQL.


class TestDatabaseUsers:
    """A connection is opened before each test and closed after it."""

    # Pytest runs this before each test method.
    def setup_method(self):
        # Open the disposable qa_test database on this machine as qa_user.
        self.connection = psycopg.connect(
            "postgresql://qa_user:qa_password@localhost:5432/qa_test"
        )

    # Pytest runs this after each test method, including when the test fails.
    def teardown_method(self):
        # Close the connection when it exists and is still open.
        if self.connection is not None and not self.connection.closed:
            self.connection.close()

    # Confirms a fresh database contains the four seeded users and no extra rows.
    def test_exactly_four_seeded_users_exist(self):
        # Open a cursor, which is the object that sends SQL and reads results.
        with self.connection.cursor() as cursor:
            # Ask the database how many rows are in the users table.
            cursor.execute("SELECT COUNT(*) FROM users")
            # Read the single count row returned by that query.
            row = cursor.fetchone()

        # Fail if the database did not return a count.
        assert row is not None, "User count query returned no row"
        # Fail unless that count is exactly 4.
        assert row[0] == 4, f"Expected exactly 4 seeded users, found {row[0]}"

    # Confirms the four seeded emails are stored with the statuses written in the query.
    def test_seeded_users_have_expected_statuses(self):
        # Count rows that match one of the four expected email and status pairs.
        query = """
            SELECT COUNT(*)
            FROM users
            WHERE (email = 'qa-test@example.com' AND status = 'active')
               OR (email = 'inactive@example.com' AND status = 'inactive')
               OR (email = 'pending@example.com' AND status = 'pending')
               OR (email = 'admin@example.com' AND status = 'active')
        """
        # Open a cursor for this query.
        with self.connection.cursor() as cursor:
            # Run the status query.
            cursor.execute(query)
            # Read the count of matching users.
            row = cursor.fetchone()

        # Fail if the database did not return a count.
        assert row is not None, "Status query returned no row"
        # Fail unless all four expected email and status pairs were found.
        assert row[0] == 4, f"Expected all 4 seeded users to have their specified statuses, found {row[0]}"

    # Confirms the seeded user named Owen Blake is stored.
    def test_owen_blake_exists(self):
        # Open a cursor for the name lookup.
        with self.connection.cursor() as cursor:
            # Count users whose name is exactly Owen Blake.
            cursor.execute("SELECT COUNT(*) FROM users WHERE name = 'Owen Blake'")
            # Read that count.
            row = cursor.fetchone()

        # Fail if the database did not return a count.
        assert row is not None, "Owen Blake lookup returned no row"
        # Fail unless exactly one Owen Blake row exists.
        assert row[0] == 1, f"Expected Owen Blake to exist, found {row[0]}"

    # Confirms exactly two seeded users have a salary greater than 400.
    def test_two_salaries_are_greater_than_400(self):
        # Open a cursor for the salary check.
        with self.connection.cursor() as cursor:
            # Count users whose salary is greater than 400.
            cursor.execute("SELECT COUNT(*) FROM users WHERE salary > 400")
            # Read that count.
            row = cursor.fetchone()

        # Fail if the database did not return a count.
        assert row is not None, "Salary count query returned no row"
        # Fail unless exactly two users earn more than 400.
        assert row[0] == 2, f"Expected 2 users with a salary greater than 400, found {row[0]}"
