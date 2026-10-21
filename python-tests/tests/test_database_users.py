"""Validate the freshly seeded PostgreSQL users table."""

import psycopg  # Driver that lets Python talk to PostgreSQL.
import pytest  # Provides the fixture that passes a connection into each test.
from expects import be_above, contain, equal, expect  # be_above compares a salary; contain looks for a value in a row; equal compares a count.
from psycopg.rows import dict_row  # Returns each row as a dictionary keyed by column name.


# Opens a connection before each test that asks for it, then closes that connection.
@pytest.fixture
def connect():
    # Open the disposable qa_test database on this machine as qa_user.
    DBconnect = psycopg.connect(
        "postgresql://qa_user:qa_password@localhost:5432/qa_test"
    )
    # Hand this connection to the test.
    yield DBconnect
    # Close the connection after the test, including when the test fails.
    DBconnect.close()



#result.rows is that array. result.rows[0] is the only row. result.rows[0].count is the value in the count column. A query that returns four user rows uses the same array, with four objects in it instead of one.

# Confirms a fresh database contains the four seeded users and no extra rows.
def test_exactly_four_seeded_users_exist(connect):
    # Open a cursor, which is the object that sends SQL and reads results.
    with connect.cursor() as cursor:
        # Ask the database how many rows are in the users table.
        cursor.execute("SELECT COUNT(*) FROM users")
        # Read the single count row returned by that query.
        row = cursor.fetchone()

    # Fail unless that count is exactly 4.
    expect(row[0]).to(equal(4))




# Confirms the seeded user named Owen Blake is stored.
def test_owen_blake_exists(connect):
    # Open a cursor for the name lookup.
    with connect.cursor() as cursor:
        # Read the user row stored for Owen Blake's email.
        cursor.execute(
            "SELECT name, email, status, salary FROM users WHERE email = 'admin@example.com'"
        )
        # Read that user row.
        row = cursor.fetchone()

    # Fail unless the returned row contains the name Owen Blake.
    expect(row).to(contain("Owen Blake"))


# Confirms two user rows were returned and each salary is greater than 400.
def test_salaries_are_greater_than_400(connect):
    # Open a cursor that returns each row as a dictionary keyed by column name.
    with connect.cursor(row_factory=dict_row) as cursor:
        # Read every column for users whose salary is greater than 400.
        cursor.execute("SELECT * FROM users WHERE salary > 400")
        # Read all of those user rows.
        rows = cursor.fetchall()

    # Fail unless the query returned exactly two rows.
    expect(len(rows)).to(equal(2))
    # Check every returned row.
    for row in rows:
        # Fail unless this row's salary column is greater than 400.
        expect(row["salary"]).to(be_above(400))
