package pl.achievementsengine;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;
import pl.achievementsengine.achievements.Achievement;
import pl.achievementsengine.achievements.PlayerAchievementState;
import pl.achievementsengine.commands.CommandHelper;
import pl.achievementsengine.commands.Commands;
import pl.achievementsengine.commands.PermissionManager;
import pl.achievementsengine.data.DataHandler;
import pl.achievementsengine.events.Events;
import pl.achievementsengine.gui.GUIHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class AchievementsEngine extends JavaPlugin {

    public static AchievementsEngine main; // Main plugin instance
    public static List<Achievement> achievements = new ArrayList<>(); // List of all achievements
    public static HashMap<String, PlayerAchievementState> playerStates = new HashMap<>(); // List of all player states
    public static HashMap<String, String> messages = new HashMap<>(); // List of all messages from messages.yml
    public static HashMap<String, Permission> permissions = new HashMap<>();

    @Override
    public void onEnable() {
        main = this; // Set main as this instance
        PermissionManager.loadPermissions(); // Register permissions
        getServer().getPluginManager().registerEvents(new Events(), this); // Register events
        getCommand("achievementsengine").setExecutor(new Commands()); // Register command
        getCommand("achievementsengine").setTabCompleter(new CommandHelper()); // Register tab completer
        DataHandler.LoadConfig(); // Load configuration files
        getLogger().info("Loaded."); // Print to console
        for(Player p : Bukkit.getServer().getOnlinePlayers()) { // Create state to all players
            PlayerAchievementState.Create(p);
        }
    }

    @Override
    public void onDisable() {
        GUIHandler.CloseAllInventories(); // Close all registered inventories to prevent GUI item duping.
        getLogger().info("Bye!"); // Print to console
    }

    public void DisablePlugin() {
        getServer().getPluginManager().disablePlugin(this);
    }


    public static String ReadLanguage(String path) {
        if (messages.containsKey(path)) {
            return messages.get(path);
        } else {
            return "§cCannotRead exception for path \"" + path + "\"";
        }
    }
}
