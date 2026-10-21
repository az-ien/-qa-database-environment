package tests; // Places this class in the tests package.

import java.sql.Connection; // TestNG assertions that fail the test when a check is wrong.
import java.sql.DriverManager; // Marks the method that runs after each test.
import java.sql.ResultSet; // Marks the method that runs before each test.
import java.sql.SQLException; // Marks a method as a test.
import java.sql.Statement; // A live session with PostgreSQL.

import org.testng.Assert; // Opens a JDBC connection from a URL, user, and password.
import org.testng.annotations.AfterMethod; // The rows returned by a query.
import org.testng.annotations.BeforeMethod; // The checked exception thrown when JDBC or SQL fails.
import org.testng.annotations.Test; // An object that sends one SQL string to the database.

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
    

    //result.rows is that array. result.rows[0] is the only row. result.rows[0].count is the value in the count column. A query that returns four user rows uses the same array, with four objects in it instead of one.



    // Confirms a fresh database contains the four seeded users and no extra rows.
    @Test
    public void exactlyFourSeededUsersExist() throws SQLException {
        // Create a statement and ask how many rows are in users.
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM users")) {
            // Move onto the single count row.
            resultSet.next();
            // Read the first column, which is the count, and require it to be 4.
            Assert.assertEquals(resultSet.getInt(1), 4, "Expected exactly 4 seeded users");
        } // Closing this block also closes the statement and the result set.
    }


    // Confirms the seeded user named Owen Blake is stored.
    @Test
    public void owenBlakeExists() throws SQLException {
        // Create a statement and read the user row stored for Owen Blake's email.
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT name, email, status, salary FROM users WHERE email = 'admin@example.com'")) {
            // Move onto the returned user row.
            resultSet.next();
            // Fail unless the name column in this row is Owen Blake.
            Assert.assertEquals(resultSet.getString("name"), "Owen Blake", "Expected the returned row to contain Owen Blake");
        } // Closing this block also closes the statement and the result set.
    }

    // Confirms two user rows were returned and each salary is greater than 400.
    @Test
    public void twoSalariesAreGreaterThan400() throws SQLException {
        // Create a statement and read every column for users whose salary is greater than 400.
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT * FROM users WHERE salary > 400")) {
            // Holds the latest row number. Declared here so the check after the loop can read it.
            int totalRowCount = 0;
            // rowCount starts at 1, so the value stored on the last row equals the number of rows.
            for (int rowCount = 1; resultSet.next(); rowCount++) {
                // Fail unless this row's salary column is greater than 400.
                Assert.assertTrue(resultSet.getInt("salary") > 400, "Expected salary to be greater than 400");
                // Store this row number, replacing the value stored for the previous row.
                totalRowCount = rowCount;
            }
            // Fail unless the last stored row number is 2.
            Assert.assertEquals(totalRowCount, 2, "Expected 2 users with a salary greater than 400");
        } // Closing this block also closes the statement and the result set.
    }
}
