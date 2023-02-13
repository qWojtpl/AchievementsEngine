package pl.achievementsengine.data;

import lombok.Getter;
import pl.achievementsengine.AchievementsEngine;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Logger;

@Getter
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
        HashMap<String, String> SQLInfo = plugin.getDataHandler().getSQLInfo();
        this.host = SQLInfo.get("host");
        this.user = SQLInfo.get("user");
        this.password = SQLInfo.get("password");
        this.database = SQLInfo.get("database");
        if(SQLInfo.get("port") != null) {
            this.port = Integer.parseInt(SQLInfo.get("port"));
        } else {
            this.port = 3306;
        }
        this.log = plugin.getLogger();
    }

    private void initialize() {
        if(!AchievementsEngine.getInstance().getDataHandler().isUseSQL()) return;
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

    public synchronized Connection getConnection() {
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
