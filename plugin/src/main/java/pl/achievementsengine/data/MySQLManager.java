package pl.achievementsengine.data;

import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import pl.achievementsengine.AchievementsEngine;
import pl.achievementsengine.achievements.Achievement;
import pl.achievementsengine.achievements.PlayerAchievementState;

import java.sql.*;
import java.util.logging.Logger;

@Getter
public class MySQLManager {

    private final Logger log;
    private final AchievementsEngine plugin;
    private DatabaseConnector mainConnector;

    public MySQLManager() {
        this.plugin = AchievementsEngine.getInstance();
        this.log = plugin.getLogger();
        initiateDB();
    }

    @SneakyThrows
    private void initiateDB() {
        this.mainConnector = new DatabaseConnector();
        if(mainConnector.checkConnection()) {
            if(!existTable("players")) {
                executeNow("CREATE TABLE IF NOT EXISTS players ("+
                        " id_player INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                        " nick VARCHAR(16) NOT NULL UNIQUE" +
                        " );", null);
            }
            if(!existTable("achievements")) {
                executeNow("CREATE TABLE IF NOT EXISTS achievements (" +
                        " id_achievement INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                        " achievement_key VARCHAR(128) NOT NULL UNIQUE" +
                        " );", null);
            }
            if(!existTable("progress")) {
                executeNow("CREATE TABLE IF NOT EXISTS progress (" +
                        " id_player INT NOT NULL," +
                        " id_achievement INT NOT NULL," +
                        " event INT NOT NULL," +
                        " progress INT NOT NULL," +
                        " FOREIGN KEY (id_player) REFERENCES players(id_player)," +
                        " FOREIGN KEY (id_achievement) REFERENCES achievements(id_achievement)," +
                        " UNIQUE(id_player, id_achievement, event)" +
                        " );", null);
            }
            if(!existTable("completed")) {
                executeNow("CREATE TABLE IF NOT EXISTS completed (" +
                        " id_player INT NOT NULL," +
                        " id_achievement INT NOT NULL," +
                        " FOREIGN KEY (id_player) REFERENCES players(id_player)," +
                        " FOREIGN KEY (id_achievement) REFERENCES achievements(id_achievement)," +
                        " UNIQUE(id_player, id_achievement)" +
                        " );", null);
            }
        } else {
            log.warning("Cannot initiate database");
        }
        if(mainConnector.getConnection() != null) mainConnector.getConnection().close();
    }

    private boolean existTable(String table) {
        String database = mainConnector.getDatabase();
        try(Connection connection = mainConnector.getConnection();
        ResultSet tables = connection.getMetaData().getTables(database, null, table, new String[]{"TABLE"})) {
            return tables.next();
        } catch(SQLException e) {
            log.severe("SQL Exception: " + e);
            return false;
        }
    }

    public void execute(String query, String[] args) {
        Bukkit.getScheduler().runTaskAsynchronously(AchievementsEngine.getInstance(), () -> {
            DatabaseConnector connector = new DatabaseConnector();
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
                if(!connector.getConnection().isClosed()) {
                    connector.getConnection().close();
                }
            } catch(SQLException e) {
                log.severe("Error at execute(), SQL Exception: " + e);
            }
        });
    }

    public void executeNow(String query, String[] args) {
        if(mainConnector.getConnection() == null) {
            log.severe("Error at executeNow() - connection is null");
            return;
        }
        try(Connection connection = mainConnector.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            if(args != null) {
                for (int i = 0; i < args.length; i++) {
                    preparedStatement.setString(i + 1, args[i]);
                }
            }
            preparedStatement.executeUpdate();
        } catch(SQLException e) {
            log.severe("Error at executeNow(), SQL Exception: " + e);
        }
    }

    public void loadCompleted(PlayerAchievementState state) {
        Bukkit.getScheduler().runTaskAsynchronously(AchievementsEngine.getInstance(), () -> {
            DatabaseConnector connector = new DatabaseConnector();
            if(connector.getConnection() == null) {
                log.severe("Error at loadCompleted() - connection is null");
                return;
            }
            String query = "SELECT DISTINCT achievement_key FROM completed JOIN players USING (id_player) " +
                    "JOIN achievements USING(id_achievement) WHERE players.nick = ?";
            try(Connection connection = connector.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, state.getPlayer().getName());
                ResultSet rs = preparedStatement.executeQuery();
                if(rs != null) {
                    while (rs.next()) {
                        state.getCompletedAchievements().add(AchievementsEngine.getInstance().getAchievementManager().checkIfAchievementExists(
                                rs.getString("achievement_key")));
                    }
                }
                if(!connector.getConnection().isClosed()) {
                    connector.getConnection().close();
                }
            } catch(SQLException e) {
                log.severe("Error at loadCompleted(), SQL Exception: " + e);
            } finally {
                state.setInitialized(true);
            }
        });
    }

    public void loadProgress(PlayerAchievementState state) {
        Bukkit.getScheduler().runTaskAsynchronously(AchievementsEngine.getInstance(), () -> {
            DatabaseConnector connector = new DatabaseConnector();
            if(connector.getConnection() == null) {
                log.severe("Error at loadProgress() - connection is null");
                return;
            }
            ResultSet rs = null;
            String query = "SELECT DISTINCT achievement_key, event, progress FROM progress JOIN players USING (id_player) " +
                    "JOIN achievements USING(id_achievement) WHERE players.nick = ?";
            try(Connection connection = connector.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, state.getPlayer().getName());
                rs = preparedStatement.executeQuery();
                if(rs != null) {
                    while (rs.next()) {
                        Achievement a = AchievementsEngine.getInstance().getAchievementManager()
                                .checkIfAchievementExists(rs.getString("achievement_key"));
                        if(a == null) {
                            AchievementsEngine.getInstance().getLogger().severe("Trying to load achievement with key " +
                                    rs.getString("achievement_key") + " which doesn't exist!");
                            return;
                        }
                        int[] progress = state.getProgress().getOrDefault(a, new int[a.getEvents().size()]);
                        progress[rs.getInt("event")] = rs.getInt("progress");
                        state.getProgress().put(a, progress);
                    }
                }
                if(!connector.getConnection().isClosed()) {
                    connector.getConnection().close();
                }
            } catch(SQLException e) {
                log.severe("Error at loadProgress(), SQL Exception: " + e);
            }
        });
    }

    public void updateProgress(PlayerAchievementState state, Achievement a) {
        for(int event = 0; event < a.getEvents().size(); event++) {
            String query = "INSERT INTO progress VALUES((SELECT id_player FROM players WHERE nick=?), " +
                    "(SELECT id_achievement FROM achievements WHERE achievement_key=?), ?, ?) ON DUPLICATE KEY UPDATE progress=?";
            int[] progress = state.getProgress().get(a);
            String[] args = new String[]{state.getPlayer().getName(), a.getID(), String.valueOf(event),
                    String.valueOf(progress[event]), String.valueOf(progress[event])};
            execute(query, args);
        }
    }

}
