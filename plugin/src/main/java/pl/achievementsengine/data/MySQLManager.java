package pl.achievementsengine.data;

import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import pl.achievementsengine.AchievementsEngine;
import pl.achievementsengine.achievements.Achievement;
import pl.achievementsengine.achievements.PlayerAchievementState;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Getter
public class MySQLManager {

    private Logger log;
    private AchievementsEngine plugin;
    private DatabaseConnector mainConnector;
    private boolean updating;

    public void create() {
        this.plugin = AchievementsEngine.getInstance();
        this.log = plugin.getLogger();
        initiateDB();
    }

    @SneakyThrows
    private void initiateDB() {
        if(!AchievementsEngine.getInstance().getDataHandler().isUseSQL()) return;
        this.mainConnector = new DatabaseConnector(); // Get main connection
        if(mainConnector.checkConnection()) {
            if(!existTable("players")) { // If table doesn't exist - create it NOW (not async)
                execute("CREATE TABLE IF NOT EXISTS players ("+
                        " id_player INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                        " nick VARCHAR(16) NOT NULL UNIQUE" +
                        " );", null);
            }
            if(!existTable("achievements")) {
                execute("CREATE TABLE IF NOT EXISTS achievements (" +
                        " id_achievement INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                        " achievement_key VARCHAR(128) NOT NULL UNIQUE" +
                        " );", null);
            }
            if(!existTable("progress")) {
                execute("CREATE TABLE IF NOT EXISTS progress (" +
                        " id_player INT NOT NULL," +
                        " id_achievement INT NOT NULL," +
                        " event INT NOT NULL," +
                        " progress INT NOT NULL," +
                        " FOREIGN KEY (id_player) REFERENCES players(id_player) ON DELETE CASCADE," +
                        " FOREIGN KEY (id_achievement) REFERENCES achievements(id_achievement) ON DELETE CASCADE," +
                        " UNIQUE(id_player, id_achievement, event)" +
                        " );", null);
            }
            if(!existTable("completed")) {
                execute("CREATE TABLE IF NOT EXISTS completed (" +
                        " id_player INT NOT NULL," +
                        " id_achievement INT NOT NULL," +
                        " FOREIGN KEY (id_player) REFERENCES players(id_player) ON DELETE CASCADE," +
                        " FOREIGN KEY (id_achievement) REFERENCES achievements(id_achievement) ON DELETE CASCADE," +
                        " UNIQUE(id_player, id_achievement)" +
                        " );", null);
            }
        } else {
            log.severe("Cannot initiate database");
            AchievementsEngine.getInstance().getDataHandler().foundException();
        }
        if(mainConnector.checkConnection()) mainConnector.getConnection().close(); // Close connection
    }

    private boolean existTable(String table) {
        String database = mainConnector.getDatabase();
        try(Connection connection = mainConnector.getConnection();
        // Thanks CrySis for sharing this way to do it <3
        ResultSet tables = connection.getMetaData().getTables(database, null, table, new String[]{"TABLE"})) {
            return tables.next();
        } catch(SQLException e) {
            generateException("existTable()", "META CHECK", new String[]{"TABLE: " + table}, e.toString());
            return false;
        }
    }

    public void generateException(String section, String query, String[] args, String exception) {
        log.severe("----------------------------------");
        log.severe("    Error at " + section);
        log.severe("    While executing query: " + query);
        if(args != null) {
            if (args.length > 0) {
                String arguments = args[0];
                for (int i = 1; i < args.length; i++) {
                    arguments += ", " + args[i];
                }
                log.severe("    Arguments: " + arguments);
            }
        }
        log.severe("    SQL Exception: " + exception);
        log.severe("----------------------------------");
        AchievementsEngine.getInstance().getDataHandler().foundException();
    }

    public void saveAsyncSQL() {
        if(updating) return;
        DataHandler dh = AchievementsEngine.getInstance().getDataHandler();
        List<String[]> queue = new ArrayList<>(dh.getSqlQueue());
        if(queue.size() == 0) return;
        Bukkit.getScheduler().runTaskAsynchronously(AchievementsEngine.getInstance(), () -> { // Run in async
            updating = true;
            DatabaseConnector connector = new DatabaseConnector(); // Create connection
            for(String[] sql : queue) { // Loop through normal SQL queue
                String query = sql[0];
                String[] args = new String[sql.length-1];
                for(int i = 1; i < sql.length; i++) {
                    args[i-1] = sql[i];
                }
                if(AchievementsEngine.getInstance().isForcedDisable()) {
                    AchievementsEngine.getInstance().getLogger().severe("Dropping from queue: " + query);
                    if(args.length > 0) {
                        String arguments = args[0];
                        for(int i = 1; i < args.length; i++) {
                            arguments += ", " + args[i];
                        }
                        AchievementsEngine.getInstance().getLogger().severe("   -> Args: " + arguments);
                    }
                }
                if(!connector.checkConnection()) {
                    generateException("saveAsyncSQL()", query, args, "Connection is null");
                    continue;
                }
                if(!AchievementsEngine.getInstance().isEnabled()) {
                    return;
                }
                dh.getSqlQueue().remove(0);
                try(Connection connection = connector.getConnection();
                    PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                    for (int i = 0; i < args.length; i++) {
                        preparedStatement.setString(i + 1, args[i]);
                    }
                    preparedStatement.executeUpdate(); // Execute update
                } catch(SQLException e) {
                    generateException("saveAsyncSQL()", query, args, e.toString());
                }
            }
            updating = false;
        });
    }

    public void execute(String query, String[] args) {
        if(mainConnector == null) return;
        if(AchievementsEngine.getInstance().isForcedDisable() && AchievementsEngine.getInstance().isDropQueue()) {
            AchievementsEngine.getInstance().getLogger().severe("Dropping from queue: " + query);
            if(args.length > 0) {
                String arguments = args[0];
                for(int i = 1; i < args.length; i++) {
                    arguments += ", " + args[i];
                }
                AchievementsEngine.getInstance().getLogger().severe("   -> Args: " + arguments);
            }
            return;
        }
        if(!mainConnector.checkConnection()) {
            generateException("executeNow()", query, args, "Connection is null");
            return;
        }
        try(Connection connection = mainConnector.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            if(args != null) { // Load arguments for preparedStatement
                for (int i = 0; i < args.length; i++) {
                    preparedStatement.setString(i + 1, args[i]);
                }
            }
            preparedStatement.executeUpdate(); // Execute update
        } catch(SQLException e) {
            generateException("executeNow()", query, args, e.toString());
        }
    }

    public void loadCompleted(PlayerAchievementState state) {
        Bukkit.getScheduler().runTaskAsynchronously(AchievementsEngine.getInstance(), () -> { // Run in async
            String query = "SELECT DISTINCT achievement_key FROM completed JOIN players USING (id_player) " +
                    "JOIN achievements USING(id_achievement) WHERE players.nick = ?"; // SQL query
            if(AchievementsEngine.getInstance().isForcedDisable()) {
                AchievementsEngine.getInstance().getLogger().severe("Dropping from queue: " + query);
                AchievementsEngine.getInstance().getLogger().severe("   -> Args: " + state.getPlayer().getName());
                return;
            }
            DatabaseConnector connector = new DatabaseConnector(); // Create connection
            if(!connector.checkConnection()) {
                generateException("loadCompleted()", query, new String[]{"PLAYER: " + state.getPlayer().getName()}, "Connection is null");
                return;
            }
            try(Connection connection = connector.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, state.getPlayer().getName());
                ResultSet rs = preparedStatement.executeQuery(); // Execute query
                if(rs != null) {
                    while (rs.next()) {
                        Achievement a = AchievementsEngine.getInstance().getAchievementManager().checkIfAchievementExists(
                                rs.getString("achievement_key"));
                        if(a == null) {
                            AchievementsEngine.getInstance().getLogger().warning("Trying to load achievement with key " +
                                    rs.getString("achievement_key") + " which doesn't exist!");
                            continue;
                        }
                        state.getCompletedAchievements().add(a); // Load completed achievements
                    }
                }
                state.setInitializeLevel(state.getInitializeLevel() + 1);
                if(!connector.getConnection().isClosed()) {
                    connector.getConnection().close(); // Close connection
                }
            } catch(SQLException e) {
                generateException("loadCompleted()", query, new String[]{"PLAYER: " + state.getPlayer().getName()}, e.toString());
            }
        });
    }

    public void loadProgress(PlayerAchievementState state) {
        Bukkit.getScheduler().runTaskAsynchronously(AchievementsEngine.getInstance(), () -> { // Run in async
            String query = "SELECT DISTINCT achievement_key, event, progress FROM progress JOIN players USING (id_player) " +
                    "JOIN achievements USING(id_achievement) WHERE players.nick = ?"; // SQL query
            if(AchievementsEngine.getInstance().isForcedDisable()) {
                AchievementsEngine.getInstance().getLogger().severe("Dropping from queue: " + query);
                AchievementsEngine.getInstance().getLogger().severe("   -> Args: " + state.getPlayer().getName());
                return;
            }
            DatabaseConnector connector = new DatabaseConnector(); // Create connection
            if(!connector.checkConnection()) {
                generateException("loadProgress()", query, new String[]{"PLAYER: " + state.getPlayer().getName()}, "Connection is null");
                return;
            }
            try(Connection connection = connector.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, state.getPlayer().getName());
                ResultSet rs = preparedStatement.executeQuery(); // Execute query
                if(rs != null) {
                    while (rs.next()) {
                        Achievement a = AchievementsEngine.getInstance().getAchievementManager()
                                .checkIfAchievementExists(rs.getString("achievement_key")); // Get achievement
                        if(a == null) { // If achievement is null..
                            AchievementsEngine.getInstance().getLogger().warning("Trying to load achievement with key " +
                                    rs.getString("achievement_key") + " which doesn't exist!");
                            continue;
                        }
                        int[] progress = state.getProgress().getOrDefault(a, new int[a.getEvents().size()]); // Get progress or initialize it
                        progress[rs.getInt("event")] = rs.getInt("progress"); // Load progress for event
                        state.getProgress().put(a, progress); // Put progress to state
                    }
                }
                state.setInitializeLevel(state.getInitializeLevel() + 1);
                if(!connector.getConnection().isClosed()) {
                    connector.getConnection().close(); // Close connection
                }
            } catch(SQLException e) {
                generateException("loadProgress()", query, new String[]{"PLAYER: " + state.getPlayer().getName()}, e.toString());
            }
        });
    }
}
