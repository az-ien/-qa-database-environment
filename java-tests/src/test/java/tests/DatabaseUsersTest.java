package tests;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * JDBC checks for the four users inserted by sql/seed.sql.
 * A connection is opened before each test and closed after it.
 */
public class DatabaseUsersTest {

    private Connection connection;

    // Opens a connection to the disposable qa_test database for the next test.
    @BeforeMethod
    public void openConnection() throws SQLException {
        connection = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/qa_test",
                "qa_user",
                "qa_password");
    }

    // Closes the connection opened for the test, including when the test fails.
    @AfterMethod(alwaysRun = true)
    public void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    // Confirms a fresh database contains the four seeded users and no extra rows.
    @Test
    public void exactlyFourSeededUsersExist() throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM users")) {
            Assert.assertTrue(resultSet.next(), "User count query returned no row");
            Assert.assertEquals(resultSet.getInt(1), 4, "Expected exactly 4 seeded users");
        }
    }

    // Confirms the four seeded emails are stored with the statuses written in the query.
    @Test
    public void seededUsersHaveExpectedStatuses() throws SQLException {
        String query = """
                SELECT COUNT(*)
                FROM users
                WHERE (email = 'qa-test@example.com' AND status = 'active')
                   OR (email = 'inactive@example.com' AND status = 'inactive')
                   OR (email = 'pending@example.com' AND status = 'pending')
                   OR (email = 'admin@example.com' AND status = 'active')
                """;

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            Assert.assertTrue(resultSet.next(), "Status query returned no row");
            Assert.assertEquals(resultSet.getInt(1), 4, "Expected all 4 seeded users to have their specified statuses");
        }
    }

    // Confirms the seeded user named Owen Blake is stored.
    @Test
    public void owenBlakeExists() throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT COUNT(*) FROM users WHERE name = 'Owen Blake'")) {
            Assert.assertTrue(resultSet.next(), "Owen Blake lookup returned no row");
            Assert.assertEquals(resultSet.getInt(1), 1, "Expected Owen Blake to exist");
        }
    }

    // Confirms exactly two seeded users have a salary greater than 400.
    @Test
    public void twoSalariesAreGreaterThan400() throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT COUNT(*) FROM users WHERE salary > 400")) {
            Assert.assertTrue(resultSet.next(), "Salary count query returned no row");
            Assert.assertEquals(resultSet.getInt(1), 2, "Expected 2 users with a salary greater than 400");
        }
    }
}
