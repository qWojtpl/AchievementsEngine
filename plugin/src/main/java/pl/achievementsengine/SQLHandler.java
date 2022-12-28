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
    public static String lastException = "None! :)"; // Last saved exception. Can be see in-game by typing /ae connection

    public static void Connect() {
        if(!isConnected()) {
            try {
                Properties prop = new Properties();
                prop.setProperty("user", username);
                prop.setProperty("password", password);
                prop.setProperty("autoReconnect", "true"); // Mark autoReconnect to prevent from timeout
                conn = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database, prop); // Connect to MySQL
                Bukkit.getLogger().info("Achievements engine connected to MySQL.");
                TryCreateStructure(); // Create database structure
                for(Player p : Bukkit.getServer().getOnlinePlayers()) { // Create state to all players
                    PlayerAchievementState.Create(p);
                }
            } catch(SQLException e) {
                Bukkit.getLogger().info("Achievements engine throws SQL Exception: " + e);
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
                Bukkit.getLogger().info("Achievements engine throws SQL Exception: " + e);
                lastException = String.valueOf(e);
            }
        }
    }

    public static boolean isConnected() {
        return conn != null;
    }

    public static void createPlayerAchievementState(Player p) {
        if(!isConnected()) return;
        try {
            String name = p.getName();
            PlayerAchievementState state = AchievementsEngine.playerAchievements.get(name);
            PreparedStatement ps = conn.prepareStatement("SELECT name FROM PlayerAchievementState WHERE name=?");
            ps.setString(1, name);
            ResultSet result = ps.executeQuery();
            int size = 0;
            while (result.next()) {
                size++;
            }
            if (size == 0) {
                PreparedStatement ps2 = conn.prepareStatement("INSERT INTO PlayerAchievementState VALUES(default, ?)");
                ps2.setString(1, name);
                ps2.executeUpdate();
            } else {
                PreparedStatement ps2 = conn.prepareStatement("SELECT achievement_key FROM CompletedAchievements JOIN(PlayerAchievementState) USING (id_user) WHERE name=?");
                ps2.setString(1, name);
                ResultSet result2 = ps2.executeQuery();
                while (result2.next()) {
                    int i = 0;
                    for (Achievement a : AchievementsEngine.achievements) {
                        if (AchievementsEngine.achievements.get(i).ID.equalsIgnoreCase(result2.getString("achievement_key"))) {
                            state.completedAchievements.add(a);
                            break;
                        }
                        i++;
                    }
                }
            }
            PreparedStatement ps3 = conn.prepareStatement("SELECT achievement_key, id_event, value FROM Progress WHERE id_user=(SELECT id_user FROM PlayerAchievementState WHERE name=?)");
            ps3.setString(1, name);
            ResultSet result3 = ps3.executeQuery();
            while(result3.next()) {
                for (Achievement a : AchievementsEngine.achievements) {
                    if(a.ID.equals(result3.getString("achievement_key"))) {
                        int[] progress;
                        if (state.progress.containsKey(a)) {
                            progress = state.progress.get(a);
                        } else {
                            progress = new int[a.events.size()];
                        }
                        progress[result3.getInt("id_event")] = result3.getInt("value");
                        state.progress.put(a, progress);
                        break;
                    }
                }
            }
            state.initialized = true;
            for (String c : state.checkQueue) {
                Achievement.Check(state.getPlayer(), c);
            }
            if (state.openGUI) {
                state.openGUI = false;
                GUIHandler.New(state.getPlayer(), 0);
            }
        } catch (SQLException e) {
            Bukkit.getLogger().info("Achievements engine throws SQL Exception while trying to create PlayerAchievementState: " + e);
            lastException = String.valueOf(e);
        }
    }

    public static boolean addCompletedAchievement(PlayerAchievementState state, Achievement achievement) {
        if(!isConnected()) return false;
        Bukkit.getScheduler().runTaskAsynchronously(AchievementsEngine.main, () -> {
            try {
                String name = state.getPlayer().getName();
                PreparedStatement ps = conn.prepareStatement("INSERT INTO CompletedAchievements VALUES((SELECT id_user FROM PlayerAchievementState WHERE name=?), ?)");
                ps.setString(1, name);
                ps.setString(2, achievement.ID);
                ps.executeUpdate();
            } catch (SQLException e) {
                Bukkit.getLogger().info("Achievements engine throws SQL Exception while trying to add completed achievement: " + e);
                lastException = String.valueOf(e);
            }
        });
        return true;
    }

    public static boolean removeCompletedAchievement(PlayerAchievementState state, Achievement achievement) {
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
                Bukkit.getLogger().info("Achievements engine throws SQL Exception while trying to remove completed achievement: " + e);
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
                Bukkit.getLogger().info("Achievements engine throws SQL Exception while trying to transfer achievements: " + e);
                lastException = String.valueOf(e);
            }
        });
        return true;
    }

    public static boolean addProgress(PlayerAchievementState state, Achievement achievement, int id_event) {
        if(!isConnected()) return false;
        Bukkit.getScheduler().runTaskAsynchronously(AchievementsEngine.main, () -> {
            try {
                PreparedStatement ps = conn.prepareStatement("INSERT INTO Progress VALUES((SELECT id_user FROM PlayerAchievementState WHERE name=?), ?, ?, 1) ON DUPLICATE KEY UPDATE value=value+1");
                ps.setString(1, state.getPlayer().getName());
                ps.setString(2, achievement.ID);
                ps.setString(3, String.valueOf(id_event));
                ps.executeUpdate();
            } catch (SQLException e) {
                Bukkit.getLogger().info("Achievements engine throws SQL Exception while trying to update progress: " + e);
                lastException = String.valueOf(e);
            }
        });
        return true;
    }
}
