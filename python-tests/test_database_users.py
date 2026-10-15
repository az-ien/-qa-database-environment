"""Validate the freshly seeded PostgreSQL users table."""

import psycopg


class TestDatabaseUsers:
    """A connection is opened before each test and closed after it."""

    # Opens a connection to the disposable qa_test database for the next test.
    def setup_method(self):
        self.connection = psycopg.connect(
            "postgresql://qa_user:qa_password@localhost:5432/qa_test"
        )

    # Closes the connection opened for the test, including when the test fails.
    def teardown_method(self):
        if self.connection is not None and not self.connection.closed:
            self.connection.close()

    # Confirms a fresh database contains the four seeded users and no extra rows.
    def test_exactly_four_seeded_users_exist(self):
        with self.connection.cursor() as cursor:
            cursor.execute("SELECT COUNT(*) FROM users")
            row = cursor.fetchone()

        assert row is not None, "User count query returned no row"
        assert row[0] == 4, f"Expected exactly 4 seeded users, found {row[0]}"

    # Confirms the four seeded emails are stored with the statuses written in the query.
    def test_seeded_users_have_expected_statuses(self):
        query = """
            SELECT COUNT(*)
            FROM users
            WHERE (email = 'qa-test@example.com' AND status = 'active')
               OR (email = 'inactive@example.com' AND status = 'inactive')
               OR (email = 'pending@example.com' AND status = 'pending')
               OR (email = 'admin@example.com' AND status = 'active')
        """
        with self.connection.cursor() as cursor:
            cursor.execute(query)
            row = cursor.fetchone()

        assert row is not None, "Status query returned no row"
        assert row[0] == 4, f"Expected all 4 seeded users to have their specified statuses, found {row[0]}"

    # Confirms the seeded user named Owen Blake is stored.
    def test_owen_blake_exists(self):
        with self.connection.cursor() as cursor:
            cursor.execute("SELECT COUNT(*) FROM users WHERE name = 'Owen Blake'")
            row = cursor.fetchone()

        assert row is not None, "Owen Blake lookup returned no row"
        assert row[0] == 1, f"Expected Owen Blake to exist, found {row[0]}"

    # Confirms exactly two seeded users have a salary greater than 400.
    def test_two_salaries_are_greater_than_400(self):
        with self.connection.cursor() as cursor:
            cursor.execute("SELECT COUNT(*) FROM users WHERE salary > 400")
            row = cursor.fetchone()

        assert row is not None, "Salary count query returned no row"
        assert row[0] == 2, f"Expected 2 users with a salary greater than 400, found {row[0]}"
