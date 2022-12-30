package pl.achievementsengine;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.Properties;

public class SQLHandler {

    public static String host; // SQL host
    public static String port; // SQL port
    public static String username; // SQL username
    public static String password; // SQL password
    public static String database; // SQL database
    public static Connection conn; // Connection
    public static String lastException = "None! :)"; // Last saved exception. Can be seen in-game by typing /ae connection
    public static int refreshInterval = 600;
    public static int refreshTask;

    public static void Connect() {
        if(!isConnected()) {
            try {
                Properties prop = new Properties();
                prop.setProperty("user", username);
                prop.setProperty("password", password);
                prop.setProperty("autoReconnect", "true"); // Mark autoReconnect to prevent from timeout (not always work, so we have RefreshConnection)
                conn = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database, prop); // Connect to MySQL
                AchievementsEngine.main.getLogger().info("Connected to MySQL.");
                TryCreateStructure(); // Create database structure
                for(Player p : Bukkit.getServer().getOnlinePlayers()) { // Create state to all players
                    PlayerAchievementState.Create(p);
                }
            } catch(SQLException e) {
                AchievementsEngine.main.getLogger().info("Achievements engine throws SQL Exception: " + e);
                lastException = String.valueOf(e);
            }
        }
    }

    public static void TryCreateStructure() throws SQLException {
        if(!isConnected()) return;
        String[] structure = new String[]{"CREATE TABLE IF NOT EXISTS PlayerAchievementState (id_user INT NOT NULL PRIMARY KEY AUTO_INCREMENT, name VARCHAR(128));",
        "CREATE TABLE IF NOT EXISTS CompletedAchievements (id_user INT, achievement_key VARCHAR(64), FOREIGN KEY (id_user) REFERENCES PlayerAchievementState(id_user), PRIMARY KEY (id_user, achievement_key));",
        "CREATE TABLE IF NOT EXISTS Progress (id_user INT, achievement_key VARCHAR(64), id_event INT, value INT, FOREIGN KEY (id_user) REFERENCES PlayerAchievementState(id_user), PRIMARY KEY (id_user, achievement_key, id_event));"};
        for (String statement : structure) { // Loop through structure commands
            PreparedStatement ps = conn.prepareStatement(statement);
            ps.executeUpdate();
        }
    }

    public static void Disconnect() {
        if(isConnected()) {
            try {
                conn.close(); // Close connection
                conn = null;
            } catch(SQLException e) {
                AchievementsEngine.main.getLogger().info("Achievements engine throws SQL Exception: " + e);
                lastException = String.valueOf(e);
            }
        }
    }

    public static void RefreshConnection() {
        if(!isConnected()) return;
        try {
            conn.prepareStatement("SELECT * FROM PlayerAchievementState LIMIT 1");
            AchievementsEngine.main.getLogger().info("Refreshed connection.");
        } catch(SQLException e) {
            AchievementsEngine.main.getLogger().info("Achievements engine throws SQL exception while trying to refresh connection: " + e);
            lastException = String.valueOf(e);
        }
    }

    public static void ScheduleRefresh() {
        refreshTask = Bukkit.getScheduler()
                .scheduleSyncRepeatingTask(
                        AchievementsEngine.main, () -> SQLHandler.RefreshConnection(), 0L, 20L*refreshInterval
                ); // Schedule connection refresh
    }
    public static boolean isConnected() {
        return conn != null;
    }

    public static void createPlayerAchievementState(Player p) {
        if(!isConnected()) return;
        try {
            String name = p.getName(); // Get player nickname
            PlayerAchievementState state = AchievementsEngine.playerAchievements.get(name); // Get state
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
                        if(progress.length > result3.getInt("id_event")) { // If there's space for downloaded progress
                            progress[result3.getInt("id_event")] = result3.getInt("value"); // Set progress to downloaded progress
                        }
                        state.progress.put(a, progress); // Save progress to state
                        break;
                    }
                }
            }
            state.initialized = true; // Set state as initialized
            for (String c : state.checkQueue) { // Loop through check queue
                Achievement.Check(state.getPlayer(), c); // Check everything which was added before initialization
            }
            if (state.openGUI) { // If state is marked as "Open GUI after initialization - open it"
                state.openGUI = false;
                GUIHandler.New(state.getPlayer(), 0);
            }
        } catch (SQLException e) {
            AchievementsEngine.main.getLogger().info("Achievements engine throws SQL Exception while trying to create PlayerAchievementState: " + e);
            lastException = String.valueOf(e);
        }
    }

    public static boolean addCompletedAchievement(PlayerAchievementState state, Achievement achievement) { // Add achievement to player's completed achievements
        if(!isConnected()) return false;
        Bukkit.getScheduler().runTaskAsynchronously(AchievementsEngine.main, () -> {
            try {
                String name = state.getPlayer().getName();
                PreparedStatement ps = conn.prepareStatement("INSERT INTO CompletedAchievements VALUES((SELECT id_user FROM PlayerAchievementState WHERE name=?), ?)");
                ps.setString(1, name);
                ps.setString(2, achievement.ID);
                ps.executeUpdate();
            } catch (SQLException e) {
                AchievementsEngine.main.getLogger().info("Achievements engine throws SQL Exception while trying to add completed achievement: " + e);
                lastException = String.valueOf(e);
            }
        });
        return true;
    }

    public static boolean removeCompletedAchievement(PlayerAchievementState state, Achievement achievement) { // Remove achievement from player's completed achievements
        if(!isConnected()) return false;
        Bukkit.getScheduler().runTaskAsynchronously(AchievementsEngine.main, () -> {
            try {
                String name = state.getPlayer().getName();
                PreparedStatement ps = conn.prepareStatement("DELETE FROM CompletedAchievements WHERE id_user=(SELECT id_user FROM PlayerAchievementState WHERE name=?) AND achievement_key=?");
                ps.setString(1, name);
                ps.setString(2, achievement.ID);
                ps.executeUpdate();
                PreparedStatement ps2 = conn.prepareStatement("DELETE FROM Progress WHERE id_user=(SELECT id_user FROM PlayerAchievementState WHERE name=?) AND achievement_key=?");
                ps2.setString(1, name);
                ps2.setString(2, achievement.ID);
                ps2.executeUpdate();
            } catch (SQLException e) {
                AchievementsEngine.main.getLogger().info("Achievements engine throws SQL Exception while trying to remove completed achievement: " + e);
                lastException = String.valueOf(e);
            }
        });
        return true;
    }

    public static boolean transferAchievements(PlayerAchievementState state1, PlayerAchievementState state2) {
        if(!isConnected()) return false;
        Bukkit.getScheduler().runTaskAsynchronously(AchievementsEngine.main, () -> {
            try {
                String name1 = state1.getPlayer().getName();
                String name2 = state2.getPlayer().getName();
                PreparedStatement ps = conn.prepareStatement("DELETE FROM CompletedAchievements WHERE id_user=(SELECT id_user FROM PlayerAchievementState WHERE name=?)");
                ps.setString(1, name1);
                ps.executeUpdate();
                PreparedStatement ps2 = conn.prepareStatement("DELETE FROM Progress WHERE id_user=(SELECT id_user FROM PlayerAchievementState WHERE name=?)");
                ps2.setString(1, name1);
                ps2.executeUpdate();
                PreparedStatement ps3 = conn.prepareStatement("UPDATE CompletedAchievements SET id_user=(SELECT id_user FROM PlayerAchievementState WHERE name=?) WHERE id_user=(SELECT id_user FROM PlayerAchievementState WHERE name=?)");
                ps3.setString(1, name2);
                ps3.setString(2, name1);
                ps3.executeUpdate();
                PreparedStatement ps4 = conn.prepareStatement("UPDATE Progress SET id_user=(SELECT id_user FROM PlayerAchievementState WHERE name=?) WHERE id_user=(SELECT id_user FROM PlayerAchievementState WHERE name=?)");
                ps4.setString(1, name2);
                ps4.setString(2, name1);
                ps4.executeUpdate();
            } catch (SQLException e) {
                AchievementsEngine.main.getLogger().info("Achievements engine throws SQL Exception while trying to transfer achievements: " + e);
                lastException = String.valueOf(e);
            }
        });
        return true;
    }

    public static boolean addProgress(PlayerAchievementState state, Achievement achievement, int id_event) { // Add 1 to player's achievement event progress
        if(!isConnected()) return false;
        Bukkit.getScheduler().runTaskAsynchronously(AchievementsEngine.main, () -> {
            try {
                PreparedStatement ps = conn.prepareStatement("INSERT INTO Progress VALUES((SELECT id_user FROM PlayerAchievementState WHERE name=?), ?, ?, 1) ON DUPLICATE KEY UPDATE value=value+1");
                ps.setString(1, state.getPlayer().getName());
                ps.setString(2, achievement.ID);
                ps.setString(3, String.valueOf(id_event));
                ps.executeUpdate();
            } catch (SQLException e) {
                AchievementsEngine.main.getLogger().info("Achievements engine throws SQL Exception while trying to update progress: " + e);
                lastException = String.valueOf(e);
            }
        });
        return true;
    }
}
