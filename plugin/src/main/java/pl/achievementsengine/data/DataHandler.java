package pl.achievementsengine.data;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pl.achievementsengine.achievements.Achievement;
import pl.achievementsengine.AchievementsEngine;
import pl.achievementsengine.gui.GUIHandler;
import pl.achievementsengine.achievements.PlayerAchievementState;
import pl.achievementsengine.util.Messages;
import pl.achievementsengine.util.PlayerUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Getter
public class DataHandler {

    private final HashMap<String, YamlConfiguration> playerYAML = new HashMap<>();
    private final HashMap<String, String> SQLInfo = new HashMap<>();
    private final HashMap<String, PlayerAchievementState> pendingStates = new HashMap<>();
    private final List<String[]> sqlQueue = new ArrayList<>();
    private final HashMap<String, String[]> importantSQLQueue = new HashMap<>();
    private final HashMap<PlayerAchievementState, List<Achievement>> updateProgressQueue = new HashMap<>(); // For SQL
    private final MySQLManager manager;

    // Config fields
    private int saveInterval;
    private int saveTask = -1;
    private boolean useYAML;
    private boolean useSQL;
    private boolean logSave;
    private boolean keepPlayersInMemory;
    private boolean disableOnException;

    public HashMap<String, String> getSQLInfo() {
        return this.SQLInfo;
    }

    public DataHandler(MySQLManager manager) {
        this.manager = manager;
    }

    public void LoadConfig() {
        saveAll(false); // Save all data
        if(saveTask != -1) { // If task is not -1 (default) cancel saveTask
            Bukkit.getScheduler().cancelTask(saveTask);
        }
        AchievementsEngine.getInstance().getEvents().clearRegisteredEvents(); // Clear registered events
        AchievementsEngine.getInstance().getPlayerStates().clear(); // Reset player states list
        AchievementsEngine.getInstance().getAchievementManager().getAchievements().clear(); // Reset achievements list
        GUIHandler.CloseAllInventories(); // Close all registered inventories to prevent GUI item duping.
        SQLInfo.clear(); // Clear SQL info
        loadConfig(); // Load config
        loadAchievementsFile(); // Load achievements
        loadMessagesFile(); // Load messages
        PlayerAchievementState.CreateForOnline();
    }

    public void foundException() {
        if(disableOnException) {
            AchievementsEngine.getInstance().disablePlugin();
        }
    }

    public void addToPending(PlayerAchievementState state) {
        getPendingStates().put(state.getPlayer().getName(), state); // Add state to pending states that will be saved (YAML)
    }

    public void createPlayerAchievementState(PlayerAchievementState state) {
        Player p = state.getPlayer(); // Get player from state
        String nick = p.getName(); // Get player's nick
        if(useYAML) {
            File dataFile = createPlayerFile(p); // Create or get player's file
            YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile); // Load player's file
            playerYAML.put(nick, data); // Add player's YAML to HashMap
            ConfigurationSection section = data.getConfigurationSection(nick); // Get data
            if(section == null) { // If section nick. is not found..
                state.setInitialized(true); // Set state as initialized
                return;
            }
            for(String key : section.getKeys(false)) { // Loop through achievement keys in player's file
                for(Achievement a : AchievementsEngine.getInstance().getAchievementManager().getAchievements()) { // Find achievement
                    if(!a.getID().equals(key)) { // If ID is not this achievement - skip
                        continue;
                    }
                    if(data.getBoolean(nick + "." + key + ".c")) { // Check if achievement is completed
                        state.getCompletedAchievements().add(a); // Add achievement to player's completed achievements
                    }
                    List<Integer> progressList = data.getIntegerList(nick + "." + key + ".p"); // Read progress
                    int[] progress = new int[a.getEvents().size()]; // Initialize progress
                    int i = 0; // Set iterator
                    for(int value : progressList) {
                        progress[i] = value; // Set progress of index to value from YAML
                        i++;
                    }
                    state.getProgress().put(a, progress); // Put progress to player's state
                    break;
                }
            }
            state.setInitialized(true); // Set state as initialized
        }
        if(useSQL) {
            state.setInitialized(false);
            getImportantSQLQueue().put("INSERT IGNORE INTO players VALUES(default, ?)", new String[]{nick}); // Add player to database
            getManager().loadCompleted(state); // Load player's completed achievements
            getManager().loadProgress(state); // Load player's progress
        }
    }

    public void addCompletedAchievement(PlayerAchievementState state, Achievement achievement) {
        if(useYAML) {
            addToPending(state); // Add state to pending to save
            YamlConfiguration data = playerYAML.get(state.getPlayer().getName()); // Get YAML
            data.set(state.getPlayer().getName() + "." + achievement.getID() + ".c", true); // Mark achievement as completed in YAML
        }
        if(useSQL) {
            getSqlQueue().add(new String[]{"INSERT IGNORE INTO completed VALUES((SELECT id_player FROM players WHERE nick=?), " +
                    "(SELECT id_achievement FROM achievements WHERE achievement_key=?))",
                    state.getPlayer().getName(),
                    achievement.getID()
            }); // Mark achievement as completed for player in database
        }
    }

    public void removeCompletedAchievement(PlayerAchievementState state, Achievement achievement) {
        if(useYAML) {
            addToPending(state); // Add state to pending to save
            YamlConfiguration data = playerYAML.get(state.getPlayer().getName()); // Get YAML
            data.set(state.getPlayer().getName() + "." + achievement.getID() + ".c", false); // Mark achievement as not completed in YAML
        }
        if(useSQL) {
            getSqlQueue().add(new String[]{"DELETE FROM completed WHERE id_player=(SELECT id_player FROM players WHERE nick=?) " +
                    "AND id_achievement=(SELECT id_achievement FROM achievements WHERE achievement_key=?)",
                    state.getPlayer().getName(),
                    achievement.getID()
            }); // Delete record from database
        }
    }

    public void updateProgress(PlayerAchievementState state, Achievement achievement) {
        if(useYAML) {
            addToPending(state); // Add state to pending to save
            int[] progress = state.getProgress().get(achievement); // Get progress
            List<Integer> newProgress = new ArrayList<>(); // Create list
            for(int i = 0; i < progress.length; i++) { // Add progress' values to list
                newProgress.add(progress[i]);
            }
            YamlConfiguration data = playerYAML.get(state.getPlayer().getName()); // Get YAML
            data.set(state.getPlayer().getName() + "." + achievement.getID() + ".p", newProgress); // Put that list to YAML
        }
        if(useSQL) {
            if(!getUpdateProgressQueue().containsKey(state)) { // If updateProgressQueue doesn't contain state
                getUpdateProgressQueue().put(state, new ArrayList<>()); // Assign empty list to player's state
            }
            if(!getUpdateProgressQueue().get(state).contains(achievement)) {
                getUpdateProgressQueue().get(state).add(achievement); // Add achievement to list
            }
        }
    }

    public void saveYAML(PlayerAchievementState state, boolean async, boolean flush) { // Save only YAML for player
        if(!useYAML) return;
        if(async) {
            Bukkit.getScheduler().runTaskAsynchronously(AchievementsEngine.getInstance(), () -> {
                try {
                    playerYAML.get(state.getPlayer().getName()).save(createPlayerFile(state.getPlayer())); // Get player's YAML and save it to file
                } catch (IOException e) {
                    AchievementsEngine.getInstance().getLogger().severe("IO exception: Cannot save player data (" + state.getPlayer().getName() + ")");
                    foundException();
                }
                if(flush) {
                    flushPlayers();
                }
            });
        } else {
            try {
                playerYAML.get(state.getPlayer().getName()).save(createPlayerFile(state.getPlayer())); // Get player's YAML and save it to file
            } catch (IOException e) {
                AchievementsEngine.getInstance().getLogger().severe("IO exception: Cannot save player data (" + state.getPlayer().getName() + ")");
                foundException();
            }
            if(flush) {
                flushPlayers();
            }
        }
    }

    public void saveSQL(boolean async) { // Save only SQL (loop through SQL queue)
        if(!useSQL) return;
        for(String key : getImportantSQLQueue().keySet()) { // Firstly, we're looping through important queue
            getManager().execute(key, getImportantSQLQueue().get(key)); // Execute it NOW - without async
        }
        getImportantSQLQueue().clear(); // Clear important queue
        addProgressToQueue();
        if(async) {
            getManager().saveAsyncSQL();
        } else {
            //List<String[]> queue = new ArrayList<>(getSqlQueue());
            for(String[] sql : getSqlQueue()) { // Loop through normal SQL queue
                String[] args = new String[sql.length - 1]; // Create args (-1 because [0] is query)
                for (int i = 1; i < sql.length; i++) {
                    args[i-1] = sql[i];
                }
                getManager().execute(sql[0], args);
            }
            getSqlQueue().clear(); // Clear queue
            flushPlayers();
        }
    }

    public void addProgressToQueue() {
        for(PlayerAchievementState state : getUpdateProgressQueue().keySet()) {
            for(Achievement a : getUpdateProgressQueue().get(state)) {
                for (int event = 0; event < a.getEvents().size(); event++) { // Loop through all achievement's events
                    String query = "INSERT INTO progress VALUES((SELECT id_player FROM players WHERE nick=?), " +
                            "(SELECT id_achievement FROM achievements WHERE achievement_key=?), ?, ?) ON DUPLICATE KEY UPDATE progress=?"; // SQL
                    int[] progress = state.getProgress().getOrDefault(a, new int[a.getEvents().size()]); // Get progress
                    String[] args = new String[]{query, state.getPlayer().getName(), a.getID(), String.valueOf(event),
                            String.valueOf(progress[event]), String.valueOf(progress[event])}; // Create arguments
                    getSqlQueue().add(args);
                }
            }
        }
        getUpdateProgressQueue().clear(); // Clear updateProgress queue
    }

    public void saveAll(boolean inAsync) { // Save ALL data
        if(logSave) AchievementsEngine.getInstance().getLogger().info("Saving data.. " +
                "(" + getPendingStates().size() + " YAMLs, " + getSqlQueue().size() + " SQL queries)");
        if(useYAML) {
            if(!getPendingStates().isEmpty()) {
                int i = 0;
                for(String key : getPendingStates().keySet()) { // Loop through pending states
                    PlayerAchievementState state = getPendingStates().get(key);
                    saveYAML(state, inAsync, (i == getPendingStates().size()-1)); // Save YAML for player
                    i++;
                }
                getPendingStates().clear(); // Clear pending states
            }
        }
        if(useSQL) {
            saveSQL(inAsync); // Save SQL
        }
    }

    public void flushPlayers() {
        if(!keepPlayersInMemory) {
            PlayerUtil pu = AchievementsEngine.getInstance().getPlayerUtil();
            HashMap<String, PlayerAchievementState> states = new HashMap<>(AchievementsEngine.getInstance().getPlayerStates());
            for(String player : states.keySet()) {
                if(pu.checkIfPlayerExists(player) == null) {
                    if(useYAML) {
                        getPlayerYAML().remove(player);
                    }
                    if(useSQL) {
                        AchievementsEngine.getInstance().getPlayerStates().remove(player);
                    }
                }
            }
            if(logSave) {
                AchievementsEngine.getInstance().getLogger().info("Flushing players...");
            }
        }
    }

    public void transferAchievements(PlayerAchievementState state1, PlayerAchievementState state2) {
        if(useYAML) {
            addToPending(state2);
            File dataFile1 = createPlayerFile(state1.getPlayer());
            File dataFile2 = createPlayerFile(state2.getPlayer());
            YamlConfiguration data1 = playerYAML.get(state1.getPlayer().getName());
            try {
                data1.set(state1.getPlayer().getName(), null);
                data1.save(dataFile1);
            } catch (IOException e) {
                AchievementsEngine.getInstance().getLogger().severe("Cannot transfer achievements from " + state1.getPlayer().getName()
                        + " to " + state2.getPlayer().getName());
                AchievementsEngine.getInstance().getLogger().severe("IO Exception: " + e);
                foundException();
            }
        }
        if(useSQL) {
            getSqlQueue().add(new String[]{"DELETE FROM completed WHERE id_player=(SELECT id_player FROM players WHERE nick=?)", state1.getPlayer().getName()});
            getSqlQueue().add(new String[]{"DELETE FROM progress WHERE id_player=(SELECT id_player FROM players WHERE nick=?)", state1.getPlayer().getName()});
        }
        state2.setCompletedAchievements(new ArrayList<>(state1.getCompletedAchievements()));
        state2.setProgress(new HashMap<>(state1.getProgress()));
        state1.setCompletedAchievements(new ArrayList<>());
        state1.setProgress(new HashMap<>());
        for(Achievement a : state2.getCompletedAchievements()) {
            addCompletedAchievement(state2, a);
        }
        for(Achievement a : state2.getProgress().keySet()) {
            updateProgress(state2, a);
        }
    }

    public File createPlayerFile(Player p) {
        File dataFile = new File(AchievementsEngine.getInstance().getDataFolder(), "/playerData/" + p.getName() + ".yml");
        if(!dataFile.exists()) { // If file not exists
            try {
                File directory = new File(AchievementsEngine.getInstance().getDataFolder(), "/playerData/");
                if(!directory.exists()) directory.mkdir(); // If directory doesn't exist - create it!
                dataFile.createNewFile(); // Create player's file
            } catch(IOException e) {
                AchievementsEngine.getInstance().getLogger().severe("Cannot create " + p.getName() + ".yml");
                AchievementsEngine.getInstance().getLogger().severe("IO Exception: " + e);
                foundException();
            }
        } else {
            if(!dataFile.canRead() || !dataFile.canWrite()) { // Check if server can read and write to file
                AchievementsEngine.getInstance().getLogger().severe("Cannot use " + p.getName() + ".yml - File cannot be read or written");
            }
        }
        return dataFile;
    }

    public void loadAchievementsFile() {
        File achFile = new File(AchievementsEngine.getInstance().getDataFolder(), "achievements.yml"); // Get achievements.yml
        if (!achFile.exists()) { // If file doesn't exist, create default file
            AchievementsEngine.getInstance().saveResource("achievements.yml", false);
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(achFile); // Load YAML
        ConfigurationSection section = yml.getConfigurationSection("achievements"); // Get section
        if(section == null) return; // If section doesn't exist - return
        String[] arguments = new String[section.getKeys(false).size()]; // SQL arguments
        String query = "INSERT IGNORE INTO achievements VALUES"; // SQL first query
        boolean first = true; // Is this achievement first?
        int i = 0; // Iterator
        for (String key : section.getKeys(false)) {
            if(key.length() > 128) { // Max key length is 128
                AchievementsEngine.getInstance().getLogger().severe("Cannot load achievement " + key + " - achievement key is too long..");
                continue;
            }
            if (yml.getString("achievements." + key + ".name") != null &&
                    yml.getBoolean("achievements." + key + ".enabled")) { // Check if name is not null and achievement is enabled
                if(useSQL && !first) {
                    query += ", ";
                }
                first = false;
                AchievementsEngine.getInstance().getAchievementManager().getAchievements().add(
                        new Achievement(key, yml.getString("achievements." + key + ".name"),
                                yml.getString("achievements." + key + ".description"),
                                yml.getStringList("achievements." + key + ".events"),
                                yml.getStringList("achievements." + key + ".actions"),
                                yml.getString("achievements." + key + ".item"),
                                yml.getBoolean("achievements." + key + ".showProgress"),
                                yml.getBoolean("achievements." + key + ".announceProgress"),
                                yml.getInt("achievements." + key + ".requiredProgress"),
                                yml.getString("achievements." + key + ".world"))); // Create new achievement from yml
                if(section.getKeys(false).size() <= 16) {
                    AchievementsEngine.getInstance().getLogger().info("Loaded achievement: " + key);
                }
                if(useSQL) {
                    query += "(default, ?)";
                    arguments[i] = key;
                    i++;
                }
            }
        }
        if(section.getKeys(false).size() > 16) {
            AchievementsEngine.getInstance().getLogger().info("Loaded ("
                    + section.getKeys(false).size() + ") achievements!");
        }
        if(useSQL) {
            getImportantSQLQueue().put(query, arguments); // If using SQL - add achievements to important queue
        }
    }

    public void loadMessagesFile() {
        File msgFile = new File(AchievementsEngine.getInstance().getDataFolder(), "messages.yml"); // Get file
        if(!msgFile.exists()) { // If file doesn't exist, create default file
            AchievementsEngine.getInstance().saveResource("messages.yml", false);
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(msgFile); // Load YAML
        ConfigurationSection section = yml.getConfigurationSection("messages"); // Get section
        Messages messages = AchievementsEngine.getInstance().getMessages();
        if(section != null) {
            messages.clearMessages(); // Clear messages
            for(String key : section.getKeys(false)) {
                messages.addMessage(key, yml.getString("messages." + key)); // Add message to list
            }
        }
        section = yml.getConfigurationSection("event-translation");
        if(section != null) {
            messages.clearEventTranslations();
            for(String key : section.getKeys(false)) {
                messages.addEventTranslation(key, yml.getString("event-translation." + key));
            }
        }
    }

    public void loadConfig() {
        File configFile = new File(AchievementsEngine.getInstance().getDataFolder(), "config.yml"); // Get file
        if(!configFile.exists()) { // If file doesn't exist, create default file
            AchievementsEngine.getInstance().saveResource("config.yml", false);
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(configFile); // Load YAML
        if(yml.getBoolean("config.useSQL")) { // If using SQL set SQL info
            SQLInfo.put("host", yml.getString("sql.host"));
            SQLInfo.put("user", yml.getString("sql.user"));
            SQLInfo.put("password", yml.getString("sql.password"));
            SQLInfo.put("database", yml.getString("sql.database"));
            SQLInfo.put("port", yml.getString("sql.port"));
        }
        this.saveInterval = yml.getInt("config.saveInterval"); // Save interval
        this.useYAML = yml.getBoolean("config.useYAML"); // Is using YAML?
        this.useSQL = yml.getBoolean("config.useSQL"); // Is using SQL?
        this.logSave = yml.getBoolean("config.logSave"); // When set to true every save will send message to console
        this.keepPlayersInMemory = yml.getBoolean("config.keepPlayersInMemory"); // When set to true, all player's states (completed achievements, progress) etc. is saved in memory.
        this.disableOnException = yml.getBoolean("config.disableOnException"); // If set to true then when SQL exception appear the plugin will be disabled
        this.saveTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(AchievementsEngine.getInstance(),
                () -> saveAll(true),
                20L * saveInterval, 20L * saveInterval); // Create task that will save data every x seconds
        if(useYAML && useSQL) { // Create warning
            AchievementsEngine.getInstance().getLogger().warning("ATTENTION! YOU'RE USING YAML AND SQL TO SAVE DATA. THIS MAY CAUSE MANY ERRORS.");
        }
    }
}
