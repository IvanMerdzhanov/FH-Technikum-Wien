package mtcg.server.database;

import java.sql.*;
public class DatabaseConnector {
    private static final String URL = "jdbc:postgresql://localhost:5432/MonsterTradingCardsGame";
    private static final String USER = "postgres";
    private static final String PASS = "admin1";

    public static Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(URL, USER, PASS);
            System.out.println("Connected to the PostgreSQL server successfully.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }

    // Example method to execute a query
    public static void executeQuery(String query) {
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                // Assuming 'username' is a column in your 'users' table
                String username = rs.getString("username");
                System.out.println("User: " + username);
                // Add more columns as needed
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }


}
