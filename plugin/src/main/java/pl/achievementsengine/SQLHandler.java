package pl.achievementsengine;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.HashMap;
import java.util.Properties;

public class SQLHandler {

    public static String host; // SQL host
    public static String port; // SQL port
    public static String username; // SQL username
    public static String password; // SQL password
    public static String database; // SQL database
    public static String lastException = "None! :)"; // Last saved exception. Can be seen in-game by typing /ae connection
    public static SQLHandler mainConnection;
    public Connection connection;

    public SQLHandler Connect() {
        try {
            Properties prop = new Properties();
            prop.setProperty("user", username);
            prop.setProperty("password", password);
            this.connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database, prop); // Connect to MySQL
            if (SQLHandler.mainConnection == this) {
                TryCreateStructure(); // Create database structure
            }
        } catch (SQLException e) {
            AchievementsEngine.main.getLogger().info("SQL Exception: " + e);
            lastException = String.valueOf(e);
        }
        return this;
    }

    public static void TryCreateStructure() throws SQLException {
        String[] structure = new String[]{"CREATE TABLE IF NOT EXISTS PlayerAchievementState (id_user INT NOT NULL PRIMARY KEY AUTO_INCREMENT, name VARCHAR(128));",
        "CREATE TABLE IF NOT EXISTS CompletedAchievements (id_user INT, achievement_key VARCHAR(64), FOREIGN KEY (id_user) REFERENCES PlayerAchievementState(id_user), PRIMARY KEY (id_user, achievement_key));",
        "CREATE TABLE IF NOT EXISTS Progress (id_user INT, achievement_key VARCHAR(64), id_event INT, value INT, FOREIGN KEY (id_user) REFERENCES PlayerAchievementState(id_user), PRIMARY KEY (id_user, achievement_key, id_event));"};
        for (String statement : structure) { // Loop through structure commands
            PreparedStatement ps = SQLHandler.mainConnection.connection.prepareStatement(statement);
            ps.executeUpdate();
        }
    }

    public void Disconnect() {
        try {
            this.connection.close(); // Close connection
        } catch(SQLException e) {
            AchievementsEngine.main.getLogger().info("SQL Exception: " + e);
            lastException = String.valueOf(e);
        }
    }

    public static void createPlayerAchievementState(Player p) {
        Bukkit.getScheduler().runTaskAsynchronously(AchievementsEngine.main, () -> {
            try {
                SQLHandler handler = new SQLHandler().Connect();
                Connection conn = handler.connection;
                String name = p.getName(); // Get player nickname
                PlayerAchievementState state = AchievementsEngine.playerStates.get(name); // Get state
                PreparedStatement ps = conn.prepareStatement("SELECT name FROM PlayerAchievementState WHERE name=?"); // Check if player is in database
                ps.setString(1, name); // Set ? to player's nickname
                ResultSet result = ps.executeQuery(); // Get results
                int size = 0;
                while (result.next()) { // Get size of results
                    size++;
                }
                if (size == 0) { // If player is not in database
                    PreparedStatement ps2 = conn.prepareStatement("INSERT INTO PlayerAchievementState VALUES(default, ?)"); // Insert player
                    ps2.setString(1, name);
                    ps2.executeUpdate();
                } else { // If player is in database - download completed achievements and progress
                    PreparedStatement ps2 = conn.prepareStatement("SELECT achievement_key FROM CompletedAchievements JOIN(PlayerAchievementState) USING (id_user) WHERE name=?");
                    ps2.setString(1, name);
                    ResultSet result2 = ps2.executeQuery();
                    while (result2.next()) { // Loop through results
                        int i = 0;
                        for (Achievement a : AchievementsEngine.achievements) { // Loop through achievements
                            if (AchievementsEngine.achievements.get(i).ID.equalsIgnoreCase(result2.getString("achievement_key"))) { // If achievements list contains achievement with downloaded key
                                state.completedAchievements.add(a); // Add this achievement to player's completed achievements
                                break;
                            }
                            i++;
                        }
                    }
                }
                PreparedStatement ps3 = conn.prepareStatement("SELECT achievement_key, id_event, value FROM Progress WHERE id_user=(SELECT id_user FROM PlayerAchievementState WHERE name=?)");
                ps3.setString(1, name);
                ResultSet result3 = ps3.executeQuery();
                while (result3.next()) { // Loop through results
                    for (Achievement a : AchievementsEngine.achievements) { // Loop through achievements
                        if (a.ID.equals(result3.getString("achievement_key"))) { // If there's achievement with downloaded key
                            int[] progress;
                            if (state.progress.containsKey(a)) { // If progress for this achievement is already set to any value
                                progress = state.progress.get(a); // Get progress
                            } else {
                                progress = new int[a.events.size()]; // Create new, empty progress
                            }
                            if (progress.length > result3.getInt("id_event")) { // If there's space for downloaded progress
                                progress[result3.getInt("id_event")] = result3.getInt("value"); // Set progress to downloaded progress
                            }
                            state.progress.put(a, progress); // Save progress to state
                            break;
                        }
                    }
                }
                state.initialized = true; // Set state as initialized
                for (String c : state.checkQueue) { // Loop through check queue
                    Bukkit.getScheduler().scheduleSyncDelayedTask(AchievementsEngine.main, () -> Achievement.Check(state.getPlayer(), c)); // Check everything which was added before initialization
                }
                if (state.openGUI) { // If state is marked as "Open GUI after initialization - open it"
                    state.openGUI = false;
                    Bukkit.getScheduler().scheduleSyncDelayedTask(AchievementsEngine.main, () -> GUIHandler.New(state.getPlayer(), 0));
                }
                handler.Disconnect();
            } catch (SQLException e) {
                AchievementsEngine.main.getLogger().info("SQL Exception while trying to create PlayerAchievementState: " + e);
                lastException = String.valueOf(e);
            }
        });
    }

    public static void addCompletedAchievement(PlayerAchievementState state, Achievement achievement) { // Add achievement to player's completed achievements
        Bukkit.getScheduler().runTaskAsynchronously(AchievementsEngine.main, () -> {
            try {
                SQLHandler handler = new SQLHandler().Connect();
                Connection conn = handler.connection;
                String name = state.getPlayer().getName();
                PreparedStatement ps = conn.prepareStatement("INSERT INTO CompletedAchievements VALUES((SELECT id_user FROM PlayerAchievementState WHERE name=?), ?)");
                ps.setString(1, name);
                ps.setString(2, achievement.ID);
                ps.executeUpdate();
                handler.Disconnect();
            } catch (SQLException e) {
                AchievementsEngine.main.getLogger().info("SQL Exception while trying to add completed achievement: " + e);
                lastException = String.valueOf(e);
            }
        });
    }

    public static void removeCompletedAchievement(PlayerAchievementState state, Achievement achievement) { // Remove achievement from player's completed achievements
        Bukkit.getScheduler().runTaskAsynchronously(AchievementsEngine.main, () -> {
            try {
                SQLHandler handler = new SQLHandler().Connect();
                Connection conn = handler.connection;
                String name = state.getPlayer().getName();
                PreparedStatement ps = conn.prepareStatement("DELETE FROM CompletedAchievements WHERE id_user=(SELECT id_user FROM PlayerAchievementState WHERE name=?) AND achievement_key=?");
                ps.setString(1, name);
                ps.setString(2, achievement.ID);
                ps.executeUpdate();
                PreparedStatement ps2 = conn.prepareStatement("DELETE FROM Progress WHERE id_user=(SELECT id_user FROM PlayerAchievementState WHERE name=?) AND achievement_key=?");
                ps2.setString(1, name);
                ps2.setString(2, achievement.ID);
                ps2.executeUpdate();
                handler.Disconnect();
            } catch (SQLException e) {
                AchievementsEngine.main.getLogger().info("SQL Exception while trying to remove completed achievement: " + e);
                lastException = String.valueOf(e);
            }
        });
    }

    public static void transferAchievements(PlayerAchievementState state1, PlayerAchievementState state2) {
        Bukkit.getScheduler().runTaskAsynchronously(AchievementsEngine.main, () -> {
            try {
                SQLHandler handler = new SQLHandler().Connect();
                Connection conn = handler.connection;
                String name1 = state1.getPlayer().getName();
                String name2 = state2.getPlayer().getName();
                PreparedStatement ps1 = conn.prepareStatement("UPDATE CompletedAchievements SET id_user=(SELECT id_user FROM PlayerAchievementState WHERE name=?) WHERE id_user=(SELECT id_user FROM PlayerAchievementState WHERE name=?)");
                ps1.setString(1, name2);
                ps1.setString(2, name1);
                ps1.executeUpdate();
                PreparedStatement ps2 = conn.prepareStatement("UPDATE Progress SET id_user=(SELECT id_user FROM PlayerAchievementState WHERE name=?) WHERE id_user=(SELECT id_user FROM PlayerAchievementState WHERE name=?)");
                ps2.setString(1, name2);
                ps2.setString(2, name1);
                ps2.executeUpdate();
                handler.Disconnect();
            } catch (SQLException e) {
                AchievementsEngine.main.getLogger().info("SQL Exception while trying to transfer achievements: " + e);
                lastException = String.valueOf(e);
            }
        });
    }

    public static void addProgress(PlayerAchievementState state, Achievement achievement, int id_event) { // Add 1 to player's achievement event progress
        Bukkit.getScheduler().runTaskAsynchronously(AchievementsEngine.main, () -> {
            try {
                SQLHandler handler = new SQLHandler().Connect();
                Connection conn = handler.connection;
                PreparedStatement ps = conn.prepareStatement("INSERT INTO Progress VALUES((SELECT id_user FROM PlayerAchievementState WHERE name=?), ?, ?, 1) ON DUPLICATE KEY UPDATE value=value+1");
                ps.setString(1, state.getPlayer().getName());
                ps.setString(2, achievement.ID);
                ps.setString(3, String.valueOf(id_event));
                ps.executeUpdate();
                handler.Disconnect();
            } catch (SQLException e) {
                AchievementsEngine.main.getLogger().info("SQL Exception while trying to update progress: " + e);
                lastException = String.valueOf(e);
            }
        });
    }

    public static void resetProgress(PlayerAchievementState state, Achievement achievement) {
        Bukkit.getScheduler().runTaskAsynchronously(AchievementsEngine.main, () -> {
            try {
                SQLHandler handler = new SQLHandler().Connect();
                Connection conn = handler.connection;
                PreparedStatement ps = conn.prepareStatement("DELETE FROM PROGRESS WHERE id_user=(SELECT id_user FROM PlayerAchievementState WHERE name=?) AND achievement_key=?");
                ps.setString(1, state.getPlayer().getName());
                ps.setString(2, achievement.ID);
                ps.executeUpdate();
                handler.Disconnect();
            } catch (SQLException e) {
                AchievementsEngine.main.getLogger().info("SQL Exception while trying to reset progress: " + e);
                lastException = String.valueOf(e);
            }
        });
    }
}
