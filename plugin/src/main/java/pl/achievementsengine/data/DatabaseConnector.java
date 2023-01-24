package pl.achievementsengine.data;

import pl.achievementsengine.AchievementsEngine;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

public class DatabaseConnector {

    private final Logger log;
    private Connection connection;
    private final String host;
    private final String user;
    private final String password;
    private final String database;
    private final int port;

    public DatabaseConnector() {
        AchievementsEngine plugin = AchievementsEngine.getInstance();
        this.host = "sql.pukawka.pl";
        this.user = "872991";
        this.password = "QyoGKHpdK2eVThT";
        this.database = "872991_osiagniecia";
        this.port = 3306;
        this.log = plugin.getLogger();
    }

    private void initialize() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://"
                                + host + ":"
                                + port + "/"
                                + database
                                + "?autoReconnect=true&useUnicode=true&characterEncoding=utf-8",
                        user, password);
        } catch(ClassNotFoundException e) {
            log.severe("Class not found exception: " + e.getMessage());
        } catch(SQLException e) {
            log.severe("SQL Exception: " + e.getMessage());
        }
    }

    public Connection getConnection() {
        try {
            if(connection == null || connection.isClosed()) {
                initialize();
            }
        } catch(SQLException e) {
            log.severe("Failed to get connection: " + e.getMessage());
        }
        return connection;
    }

    public boolean checkConnection() {
        return getConnection() != null;
    }

}
