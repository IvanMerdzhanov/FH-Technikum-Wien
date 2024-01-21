package mtcg.server.database;

import java.sql.Connection;

public class MockDatabaseConnector implements DatabaseConnector {
    @Override
    public Connection connect() {
        // Mock the connection here
        return null; // Replace with a mock object as needed
    }
}


