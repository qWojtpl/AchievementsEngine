package pl.achievementsengine.data;

import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import pl.achievementsengine.AchievementsEngine;
import pl.achievementsengine.achievements.PlayerAchievementState;

import java.sql.*;
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
                        " nick VARCHAR(16) UNIQUE" +
                        ");", null);
            }
            if(!existTable("achievements")) {
                execute("CREATE TABLE IF NOT EXISTS achievements (" +
                        " id_achievement INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                        " achievement_key VARCHAR(128) UNIQUE" +
                        ");", null);
            }
            if(!existTable("progress")) {
                execute("CREATE TABLE IF NOT EXISTS progress (" +
                        " id_player INT," +
                        " id_achievement INT," +
                        " event INT," +
                        " progress INT," +
                        " FOREIGN KEY (id_player) REFERENCES players(id_player)," +
                        " FOREIGN KEY (id_achievement) REFERENCES achievements(id_achievement)" +
                        ");", null);
            }
            if(!existTable("completed")) {
                execute("CREATE TABLE IF NOT EXISTS completed (" +
                        " id_player INT," +
                        " id_achievement INT," +
                        " FOREIGN KEY (id_player) REFERENCES players(id_player)," +
                        " FOREIGN KEY (id_achievement) REFERENCES achievements(id_achievement)" +
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
            return;
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
            log.severe("Error at execute(), SQL Exception: " + e);
        }
    }

    public void loadCompleted(PlayerAchievementState state) {
        if(connector.getConnection() == null) {
            log.severe("Error at loadCompleted() - connection is null");
            return;
        }
        ResultSet rs = null;
        String query = "SELECT DISTINCT achievement_key FROM completed JOIN players USING (id_player) " +
                "JOIN achievements USING(id_achievement) WHERE players.nick = ?";
        try(Connection connection = connector.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, state.getPlayer().getName());
            rs = preparedStatement.executeQuery();
            if(rs != null) {
                while (rs.next()) {
                    state.getCompletedAchievements().add(AchievementsEngine.getInstance().getAchievementManager().checkIfAchievementExists(
                            rs.getString("achievement_key")));
                }
            }
        } catch(SQLException e) {
            log.severe("Error at loadCompleted(), SQL Exception: " + e);
        }
    }

}
