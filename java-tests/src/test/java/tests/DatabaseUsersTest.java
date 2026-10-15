package tests; // Places this class in the tests package.

import org.testng.Assert; // TestNG assertions that fail the test when a check is wrong.
import org.testng.annotations.AfterMethod; // Marks the method that runs after each test.
import org.testng.annotations.BeforeMethod; // Marks the method that runs before each test.
import org.testng.annotations.Test; // Marks a method as a test.

import java.sql.Connection; // A live session with PostgreSQL.
import java.sql.DriverManager; // Opens a JDBC connection from a URL, user, and password.
import java.sql.ResultSet; // The rows returned by a query.
import java.sql.SQLException; // The checked exception thrown when JDBC or SQL fails.
import java.sql.Statement; // An object that sends one SQL string to the database.

/**
 * JDBC checks for the four users inserted by sql/seed.sql.
 * A connection is opened before each test and closed after it.
 */
public class DatabaseUsersTest {

    // Holds the connection opened for the test that is about to run.
    private Connection connection;

    // TestNG runs this before each test method.
    @BeforeMethod
    public void openConnection() throws SQLException {
        // Connect to the disposable qa_test database on this machine as qa_user.
        connection = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/qa_test",
                "qa_user",
                "qa_password");
    }

    // TestNG runs this after each test method, including when the test fails.
    @AfterMethod(alwaysRun = true)
    public void closeConnection() throws SQLException {
        // Close the connection when it exists and is still open.
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    // Confirms a fresh database contains the four seeded users and no extra rows.
    @Test
    public void exactlyFourSeededUsersExist() throws SQLException {
        // Create a statement and ask how many rows are in users.
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM users")) {
            // Move to the single count row. Fail if no row came back.
            Assert.assertTrue(resultSet.next(), "User count query returned no row");
            // Read the first column, which is the count, and require it to be 4.
            Assert.assertEquals(resultSet.getInt(1), 4, "Expected exactly 4 seeded users");
        } // Closing this block also closes the statement and the result set.
    }

    // Confirms the four seeded emails are stored with the statuses written in the query.
    @Test
    public void seededUsersHaveExpectedStatuses() throws SQLException {
        // Count rows that match one of the four expected email and status pairs.
        String query = """
                SELECT COUNT(*)
                FROM users
                WHERE (email = 'qa-test@example.com' AND status = 'active')
                   OR (email = 'inactive@example.com' AND status = 'inactive')
                   OR (email = 'pending@example.com' AND status = 'pending')
                   OR (email = 'admin@example.com' AND status = 'active')
                """;

        // Create a statement and run the status query.
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            // Move to the count row. Fail if no row came back.
            Assert.assertTrue(resultSet.next(), "Status query returned no row");
            // Require all four expected email and status pairs to be present.
            Assert.assertEquals(resultSet.getInt(1), 4, "Expected all 4 seeded users to have their specified statuses");
        } // Closing this block also closes the statement and the result set.
    }

    // Confirms the seeded user named Owen Blake is stored.
    @Test
    public void owenBlakeExists() throws SQLException {
        // Create a statement and count users named Owen Blake.
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT COUNT(*) FROM users WHERE name = 'Owen Blake'")) {
            // Move to the count row. Fail if no row came back.
            Assert.assertTrue(resultSet.next(), "Owen Blake lookup returned no row");
            // Require exactly one Owen Blake row.
            Assert.assertEquals(resultSet.getInt(1), 1, "Expected Owen Blake to exist");
        } // Closing this block also closes the statement and the result set.
    }

    // Confirms exactly two seeded users have a salary greater than 400.
    @Test
    public void twoSalariesAreGreaterThan400() throws SQLException {
        // Create a statement and count users whose salary is greater than 400.
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT COUNT(*) FROM users WHERE salary > 400")) {
            // Move to the count row. Fail if no row came back.
            Assert.assertTrue(resultSet.next(), "Salary count query returned no row");
            // Require that count to be 2.
            Assert.assertEquals(resultSet.getInt(1), 2, "Expected 2 users with a salary greater than 400");
        } // Closing this block also closes the statement and the result set.
    }
}
