package pl.achievementsengine.data;

import lombok.SneakyThrows;
import pl.achievementsengine.AchievementsEngine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class MySQLManager {

    private final Logger log;
    private final AchievementsEngine plugin;
    private DatabaseConnector connector;

    public MySQLManager() {
        this.plugin = AchievementsEngine.getInstance();
        this.log = plugin.getLogger();
        initiateDB();
    }

    @SneakyThrows
    private void initiateDB() {
        this.connector = new DatabaseConnector();
        if(connector.checkConnection()) {
            if(!existTable("players")) {
                execute("CREATE TABLE IF NOT EXISTS players (" +
                        " id_player INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                        " nick VARCHAR(16)" +
                        " );", null);
            }
            if(!existTable("achievements")) {
                execute("CREATE TABLE IF NOT EXISTS achievements (" +
                        " achievement_key VARCHAR(128) PRIMARY KEY" +
                        " );", null);
            }
            if(!existTable("progress")) {
                execute("CREATE TABLE IF NOT EXISTS progress (" +
                        " id_player INT," +
                        " achievement_key VARCHAR(128)," +
                        " event INT," +
                        " progress INT," +
                        " FOREIGN KEY (id_player) REFERENCES players(id_player)," +
                        " FOREIGN KEY (achievement_key) REFERENCES achievements(achievement_key)" +
                        ");", null);
            }
            if(!existTable("completed")) {
                execute("CREATE TABLE IF NOT EXISTS completed (" +
                        " id_player INT," +
                        " achievement_key VARCHAR(128)," +
                        " FOREIGN KEY (id_player) REFERENCES players(id_player)," +
                        " FOREIGN KEY (achievement_key) REFERENCES achievements(achievement_key)" +
                        ");", null);
            }
        } else {
            log.warning("Cannot initiate database");
        }
        if(connector.getConnection() != null) connector.getConnection().close();
    }

    private boolean existTable(String table) {
        String database = connector.getDatabase();
        try(Connection connection = connector.getConnection();
        ResultSet tables = connection.getMetaData().getTables(database, null, table, new String[]{"TABLE"})) {
            return tables.next();
        } catch(SQLException e) {
            log.severe("SQL Exception: " + e);
            return false;
        }
    }

    public void execute(String query, String[] args) {
        if(connector.getConnection() == null) {
            log.severe("Error at execute() - connection is null");
        }
        try(Connection connection = connector.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            if(args != null) {
                for (int i = 0; i < args.length; i++) {
                    preparedStatement.setString(i + 1, args[i]);
                }
            }
            preparedStatement.executeUpdate();
        } catch(SQLException e) {
            log.severe("Error at execute() SQL Exception: " + e);
        }
    }

}
