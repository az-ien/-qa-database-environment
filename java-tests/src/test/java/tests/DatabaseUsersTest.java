package tests;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * JDBC checks for the four users inserted by sql/seed.sql.
 * Connection settings come from environment variables, with disposable defaults.
 */
public class DatabaseUsersTest {

    // The four users inserted by sql/seed.sql, mapped to the status each one must have.
    private static final Map<String, String> EXPECTED_USERS = expectedUsers();

    // Builds the expected email-to-status mapping in a stable order.
    private static Map<String, String> expectedUsers() {
        Map<String, String> users = new LinkedHashMap<>();
        users.put("qa-test@example.com", "active");
        users.put("inactive@example.com", "inactive");
        users.put("pending@example.com", "pending");
        users.put("admin@example.com", "active");
        return Collections.unmodifiableMap(users);
    }

    // Supplies each seeded email so the existence check runs once per address.
    @DataProvider(name = "expectedEmails")
    public Object[][] expectedEmails() {
        return EXPECTED_USERS.keySet().stream()
                .map(email -> new Object[]{email})
                .toArray(Object[][]::new);
    }

    // Supplies each seeded email and its expected status as one TestNG case.
    @DataProvider(name = "expectedUsers")
    public Object[][] expectedUserRows() {
        return EXPECTED_USERS.entrySet().stream()
                .map(entry -> new Object[]{entry.getKey(), entry.getValue()})
                .toArray(Object[][]::new);
    }

    // Confirms PostgreSQL accepts the JDBC connection and answers a trivial query.
    @Test
    public void databaseIsReachable() throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1");
             ResultSet resultSet = statement.executeQuery()) {
            Assert.assertTrue(resultSet.next(), "SELECT 1 returned no row; PostgreSQL is not reachable");
            Assert.assertEquals(resultSet.getInt(1), 1, "Expected SELECT 1 to return 1");
        }
    }

    // Confirms public.users exists, has id, email, status, and created_at, and can be queried.
    @Test
    public void usersTableExistsAndIsQueryable() throws SQLException {
        String tableSql = """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = ?
                  AND table_name = ?
                """;
        String columnSql = """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = ?
                  AND table_name = ?
                """;

        try (Connection connection = openConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(tableSql)) {
                statement.setString(1, "public");
                statement.setString(2, "users");
                try (ResultSet resultSet = statement.executeQuery()) {
                    Assert.assertTrue(resultSet.next(), "Table existence query returned no row");
                    Assert.assertEquals(resultSet.getInt(1), 1, "Expected the public.users table to exist");
                }
            }

            Set<String> columns = new HashSet<>();
            try (PreparedStatement statement = connection.prepareStatement(columnSql)) {
                statement.setString(1, "public");
                statement.setString(2, "users");
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        columns.add(resultSet.getString("column_name"));
                    }
                }
            }
            Assert.assertTrue(columns.contains("id"), "Expected users.id to exist");
            Assert.assertTrue(columns.contains("email"), "Expected users.email to exist");
            Assert.assertTrue(columns.contains("status"), "Expected users.status to exist");
            Assert.assertTrue(columns.contains("created_at"), "Expected users.created_at to exist");

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id, email, status, created_at FROM users ORDER BY id");
                 ResultSet resultSet = statement.executeQuery()) {
                int rows = 0;
                while (resultSet.next()) {
                    rows++;
                    Assert.assertTrue(resultSet.getLong("id") > 0, "Expected a generated user id");
                }
                Assert.assertEquals(rows, EXPECTED_USERS.size(), "Expected the users table to return four seeded rows");
            }
        }
    }

    // Confirms a fresh database contains the four seeded users and no extra rows.
    @Test
    public void exactlyFourSeededUsersExist() throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM users");
             ResultSet resultSet = statement.executeQuery()) {
            Assert.assertTrue(resultSet.next(), "User count query returned no row");
            Assert.assertEquals(resultSet.getInt(1), 4, "Expected exactly 4 seeded users");
        }
    }

    // Confirms one seeded email is stored exactly once.
    @Test(dataProvider = "expectedEmails")
    public void expectedEmailExists(String email) throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT email FROM users WHERE email = ?")) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                Assert.assertTrue(resultSet.next(), "Expected seeded email to exist: " + email);
                Assert.assertEquals(resultSet.getString("email"), email, "Stored email did not match " + email);
                Assert.assertFalse(resultSet.next(), "Expected one row for email: " + email);
            }
        }
    }

    // Confirms one seeded email has the status defined for it in the seed data.
    @Test(dataProvider = "expectedUsers")
    public void emailHasExpectedStatus(String email, String expectedStatus) throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT status FROM users WHERE email = ?")) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                Assert.assertTrue(resultSet.next(), "Expected seeded email to exist: " + email);
                String actualStatus = resultSet.getString("status");
                Assert.assertFalse(resultSet.wasNull(), "Status for " + email + " must not be null");
                Assert.assertEquals(actualStatus, expectedStatus, "Unexpected status for " + email);
                Assert.assertFalse(resultSet.next(), "Expected one status row for email: " + email);
            }
        }
    }

    // Confirms the four seeded email addresses are present and none is repeated.
    @Test
    public void seededEmailsAreUnique() throws SQLException {
        Set<String> emails = new HashSet<>();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT email FROM users");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String email = resultSet.getString("email");
                Assert.assertNotNull(email, "Email must not be null while checking uniqueness");
                Assert.assertTrue(emails.add(email), "Duplicate email found: " + email);
            }
        }

        Assert.assertEquals(emails.size(), 4, "Expected 4 unique seeded email addresses, found " + emails);
        Assert.assertEquals(emails, EXPECTED_USERS.keySet(), "Seeded emails did not match the expected set");
    }

    // Confirms none of the four seeded users has a null email or status.
    @Test
    public void seededEmailAndStatusAreNotNull() throws SQLException {
        String seededSql = "SELECT email, status FROM users WHERE email = ?";
        String nullSql = """
                SELECT COUNT(*)
                FROM users
                WHERE email IS NULL
                   OR status IS NULL
                """;

        try (Connection connection = openConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(seededSql)) {
                for (String email : EXPECTED_USERS.keySet()) {
                    statement.setString(1, email);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        Assert.assertTrue(resultSet.next(), "Expected seeded email to exist: " + email);
                        Assert.assertNotNull(resultSet.getString("email"), "Email must not be null for " + email);
                        Assert.assertNotNull(resultSet.getString("status"), "Status must not be null for " + email);
                    }
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(nullSql);
                 ResultSet resultSet = statement.executeQuery()) {
                Assert.assertTrue(resultSet.next(), "Null-count query returned no row");
                Assert.assertEquals(resultSet.getInt(1), 0, "Expected no users with a null email or status");
            }
        }
    }

    // Opens a read-only connection. Environment variables override the disposable defaults.
    private Connection openConnection() throws SQLException {
        String host = env("DB_HOST", "localhost");
        String port = env("DB_PORT", "5432");
        String database = env("DB_NAME", "qa_test");
        String user = env("DB_USER", "qa_user");
        String password = env("DB_PASSWORD", "qa_password");
        String url = "jdbc:postgresql://" + host + ":" + port + "/" + database;

        try {
            Connection connection = DriverManager.getConnection(url, user, password);
            connection.setReadOnly(true);
            return connection;
        } catch (SQLException exception) {
            throw new SQLException(
                    "Unable to connect to PostgreSQL at " + url + " as user " + user,
                    exception
            );
        }
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }
}
