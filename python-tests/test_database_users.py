"""Validate the freshly seeded PostgreSQL users table."""

from __future__ import annotations

import os
from collections.abc import Iterator

import psycopg
import pytest
from psycopg import Connection

EXPECTED_USERS = {
    "qa-test@example.com": "active",
    "inactive@example.com": "inactive",
    "pending@example.com": "pending",
    "admin@example.com": "active",
}


def connection_kwargs() -> dict[str, object]:
    """Disposable local and CI defaults. Environment variables override them."""
    return {
        "host": os.environ.get("DB_HOST", "localhost"),
        "port": int(os.environ.get("DB_PORT", "5432")),
        "dbname": os.environ.get("DB_NAME", "qa_test"),
        "user": os.environ.get("DB_USER", "qa_user"),
        "password": os.environ.get("DB_PASSWORD", "qa_password"),
        "connect_timeout": 10,
    }


@pytest.fixture
def db_connection() -> Iterator[Connection]:
    """Open one read-only connection and close it when the test ends."""
    settings = connection_kwargs()
    try:
        connection = psycopg.connect(**settings)
    except psycopg.OperationalError as exc:
        pytest.fail(
            "Could not connect to PostgreSQL at "
            f"{settings['host']}:{settings['port']}/{settings['dbname']} "
            f"as {settings['user']}: {exc}"
        )

    connection.read_only = True
    try:
        yield connection
    finally:
        connection.close()


def test_postgresql_connectivity(db_connection: Connection) -> None:
    with db_connection.cursor() as cursor:
        cursor.execute("SELECT 1")
        row = cursor.fetchone()

    assert row is not None, "SELECT 1 returned no row; PostgreSQL is not reachable"
    assert row[0] == 1, f"Expected SELECT 1 to return 1, got {row[0]!r}"


def test_users_table_exists_and_is_queryable(db_connection: Connection) -> None:
    with db_connection.cursor() as cursor:
        cursor.execute(
            """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = %s
              AND table_name = %s
            """,
            ("public", "users"),
        )
        table_count = cursor.fetchone()
        assert table_count is not None, "Table existence query returned no row"
        assert table_count[0] == 1, "Expected the public.users table to exist"

        cursor.execute(
            """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = %s
              AND table_name = %s
            """,
            ("public", "users"),
        )
        columns = {row[0] for row in cursor.fetchall()}
        expected_columns = {"id", "email", "status", "created_at"}
        assert expected_columns <= columns, (
            f"Expected columns {sorted(expected_columns)}, found {sorted(columns)}"
        )

        cursor.execute("SELECT id, email, status, created_at FROM users ORDER BY id")
        rows = cursor.fetchall()

    assert len(rows) == len(EXPECTED_USERS), (
        f"Expected to query {len(EXPECTED_USERS)} users, found {len(rows)}"
    )


def test_exactly_four_seeded_users_exist(db_connection: Connection) -> None:
    with db_connection.cursor() as cursor:
        cursor.execute("SELECT COUNT(*) FROM users")
        row = cursor.fetchone()

    assert row is not None, "User count query returned no row"
    assert row[0] == 4, f"Expected exactly 4 seeded users, found {row[0]}"


@pytest.mark.parametrize("email", sorted(EXPECTED_USERS))
def test_expected_email_exists(db_connection: Connection, email: str) -> None:
    with db_connection.cursor() as cursor:
        cursor.execute("SELECT email FROM users WHERE email = %s", (email,))
        row = cursor.fetchone()

    assert row is not None, f"Expected seeded email {email!r} to exist"
    assert row[0] == email, f"Expected stored email {email!r}, found {row[0]!r}"


@pytest.mark.parametrize(("email", "expected_status"), sorted(EXPECTED_USERS.items()))
def test_email_has_expected_status(
    db_connection: Connection,
    email: str,
    expected_status: str,
) -> None:
    with db_connection.cursor() as cursor:
        cursor.execute("SELECT status FROM users WHERE email = %s", (email,))
        row = cursor.fetchone()

    assert row is not None, f"Expected seeded email {email!r} to exist"
    assert row[0] == expected_status, (
        f"Expected status {expected_status!r} for {email!r}, found {row[0]!r}"
    )


def test_seeded_emails_are_unique(db_connection: Connection) -> None:
    with db_connection.cursor() as cursor:
        cursor.execute("SELECT email FROM users")
        emails = [row[0] for row in cursor.fetchall()]

    assert len(emails) == 4, f"Expected 4 seeded emails, found {len(emails)}: {emails}"
    assert len(set(emails)) == 4, f"Expected 4 unique email addresses, found {emails}"
    assert set(emails) == set(EXPECTED_USERS), (
        f"Expected emails {sorted(EXPECTED_USERS)}, found {sorted(str(email) for email in emails)}"
    )


def test_seeded_email_and_status_are_not_null(db_connection: Connection) -> None:
    with db_connection.cursor() as cursor:
        cursor.execute(
            """
            SELECT email, status
            FROM users
            WHERE email = ANY(%s)
            """,
            (list(EXPECTED_USERS),),
        )
        rows = cursor.fetchall()

        cursor.execute(
            """
            SELECT COUNT(*)
            FROM users
            WHERE email IS NULL
               OR status IS NULL
            """
        )
        null_count = cursor.fetchone()

    assert len(rows) == 4, f"Expected 4 seeded users when checking nulls, found {len(rows)}"
    for email, status in rows:
        assert email is not None, "Seeded user email must not be null"
        assert status is not None, f"Seeded status for {email!r} must not be null"
    assert null_count is not None, "Null-count query returned no row"
    assert null_count[0] == 0, (
        f"Expected no users with a null email or status, found {null_count[0]}"
    )
