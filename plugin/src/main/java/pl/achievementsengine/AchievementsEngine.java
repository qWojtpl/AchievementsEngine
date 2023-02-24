package pl.achievementsengine;

import lombok.Getter;
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
import pl.achievementsengine.util.PlayerUtil;

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
    private PlayerUtil playerUtil;
    private final HashMap<String, PlayerAchievementState> playerStates = new HashMap<>(); // List of all player states

    @Override
    public void onEnable() {
        main = this; // Set main as this instance
        this.dataHandler = new DataHandler();
        this.permissionManager = new PermissionManager();
        this.achievementManager = new AchievementManager();
        this.events = new Events();
        this.messages = new Messages();
        this.playerUtil = new PlayerUtil();
        getServer().getPluginManager().registerEvents(events, this); // Register events
        getCommand("achievementsengine").setExecutor(new Commands()); // Register command
        getCommand("achievementsengine").setTabCompleter(new CommandHelper()); // Register tab completer
        dataHandler.LoadConfig(); // Load configuration files
        permissionManager.loadPermissions(); // Register permissions
        this.manager = new MySQLManager();
        getLogger().info("Loaded."); // Print to console
    }

    @Override
    public void onDisable() {
        getDataHandler().saveAll(false); // Save all data not in async (if plugin is disabled we can't register async)
        GUIHandler.CloseAllInventories(); // Close all registered inventories to prevent GUI item duping.
        getLogger().info("Bye!"); // Print to console
    }

    public void disablePlugin() {
        getLogger().warning("Forcing plugin disable...");
        getServer().getPluginManager().disablePlugin(this);
    }

    public static AchievementsEngine getInstance() {
        return main;
    }

}
