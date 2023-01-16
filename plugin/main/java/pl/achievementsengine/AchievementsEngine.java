package pl.achievementsengine;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pl.achievementsengine.achievements.Achievement;
import pl.achievementsengine.achievements.PlayerAchievementState;
import pl.achievementsengine.commands.CommandHelper;
import pl.achievementsengine.commands.Commands;
import pl.achievementsengine.data.DataHandler;
import pl.achievementsengine.events.Events;
import pl.achievementsengine.gui.GUIHandler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class AchievementsEngine extends JavaPlugin {

    public static AchievementsEngine main; // Main plugin instance
    public static List<Achievement> achievements = new ArrayList<>(); // List of all achievements
    public static HashMap<String, PlayerAchievementState> playerStates = new HashMap<>(); // List of all player states
    public static HashMap<String, String> messages = new HashMap<>(); // List of all messages from messages.yml

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new Events(), this); // Register events
        getCommand("achievementsengine").setExecutor(new Commands()); // Register command
        getCommand("achievementsengine").setTabCompleter(new CommandHelper()); // Register tab completer
        main = this; // Set main as this instance
        LoadConfig(); // Load configuration files
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

    public void LoadConfig() {
        playerStates = new HashMap<>(); // Reset player states list
        achievements = new ArrayList<>(); // Reset achievements list
        GUIHandler.CloseAllInventories(); // Close all registered inventories to prevent GUI item duping.
        DataHandler.loadAchievementsFile(); // Load achievements
        DataHandler.loadMessagesFile(); // Load messages
    }

    public static String ReadLanguage(String path) {
        if (messages.containsKey(path)) {
            return messages.get(path);
        } else {
            return "§cCannotRead exception for path \"" + path + "\"";
        }
    }
}
