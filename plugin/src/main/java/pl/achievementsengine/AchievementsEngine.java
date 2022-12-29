package pl.achievementsengine;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class AchievementsEngine extends JavaPlugin {

    public static AchievementsEngine main;
    public FileConfiguration yml; // Current reading yml
    public static List<Achievement> achievements = new ArrayList<>(); // List of all achievements
    public static HashMap<String, PlayerAchievementState> playerAchievements = new HashMap<>(); // List of all player states
    public static HashMap<String, String> messages = new HashMap<>(); // List of all messages from messages.yml

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new Events(), this); // Register events
        getCommand("achievementsengine").setExecutor(new Commands()); // Register command
        main = this; // Set main as this instance
        LoadConfig(); // Load configuration files
        SQLHandler.Connect(); // Connect to MySQL
        getLogger().info("Loaded."); // Print to console
    }

    @Override
    public void onDisable() {
        SQLHandler.Disconnect(); // Disconnect from MySQL
        getLogger().info("Bye!"); // Print to console
    }

    public void LoadConfig() {
        playerAchievements = new HashMap<>();
        GUIHandler.CloseAllInventories(); // Close all registered inventories to prevent GUI item duping.
        getServer().getScheduler().cancelTask(SQLHandler.refreshTask); // Cancel connection refresh scheduler
        loadDatabaseFile(); // Load database
        loadAchievementsFile(); // Load achievements
        loadMessagesFile(); // Load messages
    }

    public String ReadStringPath(String path) {
        if (yml.getString(path) == null) {
            yml.set(path, "");
        }
        return yml.getString(path);
    }

    public static String ReadLanguage(String path) {
        if (messages.containsKey(path)) {
            return messages.get(path);
        } else {
            return "§cCannotRead exception for path \"" + path + "\"";
        }
    }

    private void loadDatabaseFile() {
        File sqlFile = new File(getDataFolder(), "database.yml");
        if (!sqlFile.exists()) { // If file doesn't exist, create default file
            try {
                sqlFile.createNewFile();
                yml = YamlConfiguration.loadConfiguration(sqlFile);
                yml.set("sql.host", "localhost");
                yml.set("sql.port", 3306);
                yml.set("sql.username", "username");
                yml.set("sql.password", "password");
                yml.set("sql.database", "database");
                yml.set("sql-settings.refreshConnectionInterval", "600");
                yml.save(sqlFile);
            } catch (IOException e) {
                getLogger().info("Cannot create database.yml - exception: " + e);
                return;
            }
        }
        yml = YamlConfiguration.loadConfiguration(sqlFile);
        SQLHandler.host = ReadStringPath("sql.host");
        SQLHandler.port = ReadStringPath("sql.port");
        SQLHandler.username = ReadStringPath("sql.username");
        SQLHandler.password = ReadStringPath("sql.password");
        SQLHandler.database = ReadStringPath("sql.database");
        SQLHandler.refreshInterval = yml.getInt("sql-settings.refreshConnectionInterval");
    }

    private void loadAchievementsFile() {
        File achFile = new File(getDataFolder(), "achievements.yml");
        if (!achFile.exists()) { // If file doesn't exist, create default file
            try {
                achFile.createNewFile();
                yml = YamlConfiguration.loadConfiguration(achFile);
                yml.set("achievements.0.enabled", true);
                yml.set("achievements.0.name", "§2§lSample Achievement");
                yml.set("achievements.0.description", "§aUse /ae command and get 1 diamond.");
                yml.set("achievements.0.item", "BEDROCK");
                yml.set("achievements.0.showProgress", false);
                List<String> list = new ArrayList<>();
                list.add("command 1 /ae");
                yml.set("achievements.0.events", list);
                list = new ArrayList<>();
                list.add("give {0} minecraft:diamond 1");
                yml.set("achievements.0.actions", list);
                yml.save(achFile);
            } catch (IOException e) {
                getLogger().info("Cannot create achievements.yml - exception: " + e);
                return;
            }
        }
        yml = YamlConfiguration.loadConfiguration(achFile);
        achievements = new ArrayList<>();
        ConfigurationSection section = yml.getConfigurationSection("achievements");
        for (String key : section.getKeys(false)) {
            if (yml.getString("achievements." + key + ".name") != null) {
                achievements.add(
                        new Achievement(key, ReadStringPath("achievements." + key + ".name"),
                                ReadStringPath("achievements." + key + ".description"),
                                yml.getBoolean("achievements." + key + ".enabled"), yml.getStringList("achievements." + key + ".events"),
                                yml.getStringList("achievements." + key + ".actions"), yml.getString("achievements." + key + ".item"),
                                yml.getBoolean("achievements." + key + ".showProgress"))); // Create new achievement from yml
                getLogger().info("Loaded achievement: " + key);
            }
        }
    }

    private void loadMessagesFile() {
        File msgFile = new File(getDataFolder(), "messages.yml");
        if (!msgFile.exists()) {
            try { // If file doesn't exist, create default file
                msgFile.createNewFile();
                yml = YamlConfiguration.loadConfiguration(msgFile);
                yml.set("messages.prefix", "§2[§6AchievementsEngine§2] ");
                yml.set("messages.gui-title", "Achievements (Page {0}/{1})");
                yml.set("messages.gui-next", "§f§lNext page");
                yml.set("messages.gui-previous", "§f§lPrevious page");
                yml.set("messages.complete-message", "§6§k--------------%nl%%nl%§a§lNew achievement!%nl%§a§lUnlocked: {0}%nl%%nl%§6§k--------------");
                yml.set("messages.progress-message", "§aYou made progress in achievement {0}§a!");
                yml.set("messages.progress", "§6§lProgress:");
                yml.set("messages.progress-field-prefix", "§7§l- §b");
                yml.set("messages.completed", "§aCOMPLETED!");
                yml.save(msgFile);
            } catch (IOException e) {
                getLogger().info("Cannot create messages.yml");
                return;
            }
        }
        yml = YamlConfiguration.loadConfiguration(msgFile);
        ConfigurationSection section = yml.getConfigurationSection("messages");
        messages = new HashMap<>();
        for (String key : section.getKeys(false)) {
            messages.put(key, ReadStringPath("messages." + key));
        }
    }
}
