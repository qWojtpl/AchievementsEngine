package pl.achievementsengine;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pl.achievementsengine.achievements.AchievementManager;
import pl.achievementsengine.achievements.PlayerAchievementState;
import pl.achievementsengine.commands.CommandHelper;
import pl.achievementsengine.commands.Commands;
import pl.achievementsengine.commands.PermissionManager;
import pl.achievementsengine.data.DataHandler;
import pl.achievementsengine.data.MySQLManager;
import pl.achievementsengine.events.Events;
import pl.achievementsengine.gui.GUIHandler;
import pl.achievementsengine.util.Messages;

import java.util.HashMap;

@Getter
public final class AchievementsEngine extends JavaPlugin {

    private static AchievementsEngine main; // Main plugin instance
    private PermissionManager permissionManager;
    private AchievementManager achievementManager;
    private MySQLManager manager;
    private Events events;
    private Messages messages;
    private DataHandler dataHandler;
    private final HashMap<String, PlayerAchievementState> playerStates = new HashMap<>(); // List of all player states

    @Override
    public void onEnable() {
        main = this; // Set main as this instance
        this.permissionManager = new PermissionManager();
        this.achievementManager = new AchievementManager();
        permissionManager.loadPermissions(); // Register permissions
        this.events = new Events();
        this.messages = new Messages();
        this.dataHandler = new DataHandler();
        getServer().getPluginManager().registerEvents(events, this); // Register events
        getCommand("achievementsengine").setExecutor(new Commands()); // Register command
        getCommand("achievementsengine").setTabCompleter(new CommandHelper()); // Register tab completer
        dataHandler.LoadConfig(); // Load configuration files
        getLogger().info("Loaded."); // Print to console
        for(Player p : Bukkit.getServer().getOnlinePlayers()) { // Create state to all players
            PlayerAchievementState.Create(p);
        }
        this.manager = new MySQLManager();
    }

    @Override
    public void onDisable() {
        GUIHandler.CloseAllInventories(); // Close all registered inventories to prevent GUI item duping.
        getLogger().info("Bye!"); // Print to console
    }

    public void DisablePlugin() {
        getServer().getPluginManager().disablePlugin(this);
    }

    public static AchievementsEngine getInstance() {
        return main;
    }

}
