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
    public static final String PLAYERS = "players";
    public static final String ACHIEVEMENTS = "achievements";

    public MySQLManager() {
        this.plugin = AchievementsEngine.getInstance();
        this.log = plugin.getLogger();
        initiateDB();
    }

    @SneakyThrows
    private void initiateDB() {
        this.connector = new DatabaseConnector();
        if(connector.checkConnection()) {
            if(!existTable(PLAYERS)) {
                execute("CREATE TABLE IF NOT EXISTS achievements ( " +
                        "id_achievement INT NOT NULL PRIMARY KEY AUTO_INCREMENT," +
                        "name VARCHAR(128)" +
                        ")");
            }
        } else {
            log.warning("Cannot inititate database");
        }
        if(connector.getConnection() != null) connector.getConnection().close();
    }

    private boolean existTable(String table) {
        String database = "872991_osiagniecia";
        try(Connection connection = connector.getConnection();
        ResultSet tables = connection.getMetaData().getTables(database, null, table, new String[]{"TABLE"})) {
            return tables.next();
        } catch(SQLException e) {
            log.severe("SQL Exception: " + e);
            return false;
        }
    }

    public void execute(String query) {
        try(Connection connection = connector.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.executeUpdate();
        } catch(SQLException e) {
            log.severe("Error at execute() SQL Exception: " + e);
        }
    }

    public List<String> getAchievements(String offset) {
        if(offset == null) {
            offset = "0";
        } else {
            offset = "" + (Integer.parseInt(offset) - 1) * 10;
        }
        // LIMIT <OFFSET>, <LIMIT>
        String query = "SELECT * FROM " + ACHIEVEMENTS + " LIMIT 10 OFFSET " + offset;
        List<String> achievements = new ArrayList<>();
        try(Connection connection = connector.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            final ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next()) {
                achievements.add(resultSet.getString("name"));
            }
        } catch(SQLException e) {
            log.severe("Error at execute() SQL Exception: " + e);
        }
        return achievements;
    }
}
