package mtcg.server.database;

import java.sql.Connection;

public interface DatabaseConnector {
    Connection connect();
}
