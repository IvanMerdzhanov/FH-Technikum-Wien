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




}
