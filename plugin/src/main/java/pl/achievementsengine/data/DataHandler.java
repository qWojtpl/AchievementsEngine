package pl.achievementsengine.data;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.checkerframework.checker.units.qual.A;
import pl.achievementsengine.achievements.Achievement;
import pl.achievementsengine.AchievementsEngine;
import pl.achievementsengine.gui.GUIHandler;
import pl.achievementsengine.achievements.PlayerAchievementState;

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
    private final HashMap<PlayerAchievementState, List<Achievement>> updateProgressQueue = new HashMap<>();
    private int saveInterval;
    private int saveTask = -1;
    private boolean useYAML;
    private boolean useSQL;

    public HashMap<String, String> getSQLInfo() {
        return this.SQLInfo;
    }

    public void LoadConfig() {
        saveAll();
        if(saveTask != -1) {
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
    }

    public void addToPending(PlayerAchievementState state) {
        getPendingStates().put(state.getPlayer().getName(), state);
    }

    public void createPlayerAchievementState(PlayerAchievementState state) {
        Player p = state.getPlayer();
        String nick = p.getName();
        if(useYAML) {
            File dataFile = createPlayerFile(p);
            YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
            playerYAML.put(nick, data);
            ConfigurationSection section = data.getConfigurationSection(nick);
            if(section == null) {
                state.setInitialized(true);
                return;
            }
            for(String key : section.getKeys(false)) {
                for(Achievement a : AchievementsEngine.getInstance().getAchievementManager().getAchievements()) {
                    if(!a.getID().equals(key)) {
                        continue;
                    }
                    if(data.getBoolean(nick + "." + key + ".completed")) {
                        state.getCompletedAchievements().add(a);
                    }
                    List<Integer> progressList = data.getIntegerList(nick + "." + key + ".progress");
                    int[] progress = new int[a.getEvents().size()];
                    int i = 0;
                    for(int value : progressList) {
                        progress[i] = value;
                        i++;
                    }
                    state.getProgress().put(a, progress);
                    break;
                }
            }
            state.setInitialized(true);
        }
        if(useSQL) {
            getSqlQueue().add(new String[]{"INSERT IGNORE INTO players VALUES(default, ?)", nick});
            AchievementsEngine.getInstance().getManager().loadCompleted(state);
            AchievementsEngine.getInstance().getManager().loadProgress(state);
        }
    }

    public void addCompletedAchievement(PlayerAchievementState state, Achievement achievement) {
        if(useYAML) {
            addToPending(state);
            YamlConfiguration data = playerYAML.get(state.getPlayer().getName());
            data.set(state.getPlayer().getName() + "." + achievement.getID() + ".completed", true);
        }
        if(useSQL) {
            getSqlQueue().add(new String[]{"INSERT IGNORE INTO completed VALUES((SELECT id_player FROM players WHERE nick=?), " +
                    "(SELECT id_achievement FROM achievements WHERE achievement_key=?))",
                    state.getPlayer().getName(),
                    achievement.getID()
            });
        }
    }

    public void removeCompletedAchievement(PlayerAchievementState state, Achievement achievement) {
        if(useYAML) {
            addToPending(state);
            YamlConfiguration data = playerYAML.get(state.getPlayer().getName());
            data.set(state.getPlayer().getName() + "." + achievement.getID() + ".completed", false);
        }
        if(useSQL) {
            getSqlQueue().add(new String[]{"DELETE FROM completed WHERE id_player=(SELECT id_player FROM players WHERE nick=?) " +
                    "AND id_achievement=(SELECT id_achievement FROM achievements WHERE achievement_key=?)",
                    state.getPlayer().getName(),
                    achievement.getID()
            });
        }
    }

    public void updateProgress(PlayerAchievementState state, Achievement achievement) {
        if(useYAML) {
            addToPending(state);
            int[] progress = state.getProgress().get(achievement);
            List<Integer> newProgress = new ArrayList<>();
            for(int i = 0; i < progress.length; i++) {
                newProgress.add(progress[i]);
            }
            YamlConfiguration data = playerYAML.get(state.getPlayer().getName());
            data.set(state.getPlayer().getName() + "." + achievement.getID() + ".progress", newProgress);
        }
        if(useSQL) {
            if(!getUpdateProgressQueue().containsKey(state)) {
                getUpdateProgressQueue().put(state, new ArrayList<>());
            }
            getUpdateProgressQueue().get(state).add(achievement);
        }
    }

    public void saveYAML(PlayerAchievementState state) {
        if(!useYAML) return;
        try {
            playerYAML.get(state.getPlayer().getName()).save(createPlayerFile(state.getPlayer()));
        } catch (IOException e) {
            AchievementsEngine.getInstance().getLogger().severe("IO exception: Cannot save player data (" + state.getPlayer().getName() + ")");
        }
    }

    public void saveSQL() {
        if(!useSQL) return;
        for(String key : getImportantSQLQueue().keySet()) {
            AchievementsEngine.getInstance().getManager().executeNow(key, getImportantSQLQueue().get(key));
        }
        getImportantSQLQueue().clear();
        for(String[] sql : getSqlQueue()) {
            String[] args = new String[sql.length-1];
            for(int i = 1; i < sql.length; i++) {
                args[i-1] = sql[i];
            }
            AchievementsEngine.getInstance().getManager().execute(sql[0], args);
        }
        getSqlQueue().clear();
        for(PlayerAchievementState state : getUpdateProgressQueue().keySet()) {
            for(Achievement a : getUpdateProgressQueue().get(state)) {
                AchievementsEngine.getInstance().getManager().updateProgress(state, a);
            }
        }
        getUpdateProgressQueue().clear();
    }

    public void saveAll() {
        if(useYAML) {
            for(String key : getPendingStates().keySet()) {
                PlayerAchievementState state = getPendingStates().get(key);
                saveYAML(state);
            }
            getPendingStates().clear();
        }
        if(useSQL) {
            saveSQL();
        }
    }

    public void transferAchievements(PlayerAchievementState state1, PlayerAchievementState state2) {
        addToPending(state2);
        File dataFile1 = createPlayerFile(state1.getPlayer());
        File dataFile2 = createPlayerFile(state2.getPlayer());
        if(useYAML) {
            YamlConfiguration data1 = playerYAML.get(state1.getPlayer().getName());
            try {
                data1.set(state1.getPlayer().getName(), null);
                data1.save(dataFile1);
            } catch (IOException e) {
                AchievementsEngine.getInstance().getLogger().info("Cannot transfer achievements from " + state1.getPlayer().getName()
                        + " to " + state2.getPlayer().getName());
                AchievementsEngine.getInstance().getLogger().info("IO Exception: " + e);
            }
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
        if(!dataFile.exists()) {
            try {
                File directory = new File(AchievementsEngine.getInstance().getDataFolder(), "/playerData/");
                if(!directory.exists()) directory.mkdir();
                dataFile.createNewFile();
            } catch(IOException e) {
                AchievementsEngine.getInstance().getLogger().info("Cannot create " + p.getName() + ".yml");
                AchievementsEngine.getInstance().getLogger().info("IO Exception: " + e);
            }
        } else {
            if(!dataFile.canRead() || !dataFile.canWrite()) {
                AchievementsEngine.getInstance().getLogger().info("Cannot create " + p.getName() + ".yml");
            }
        }
        return dataFile;
    }

    public void loadAchievementsFile() {
        File achFile = new File(AchievementsEngine.getInstance().getDataFolder(), "achievements.yml");
        if (!achFile.exists()) { // If file doesn't exist, create default file
            AchievementsEngine.getInstance().saveResource("achievements.yml", false);
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(achFile);
        ConfigurationSection section = yml.getConfigurationSection("achievements");
        if(section == null) return;
        String[] arguments = new String[section.getKeys(false).size()];
        String query = "INSERT IGNORE INTO achievements VALUES";
        boolean first = true;
        int i = 0;
        for (String key : section.getKeys(false)) {
            if(key.length() > 128) {
                AchievementsEngine.getInstance().getLogger().warning("Cannot load achievement " + key + " - achievement key is too long..");
            }
            if (yml.getString("achievements." + key + ".name") != null && yml.getBoolean("achievements." + key + ".enabled")) {
                if(useSQL && !first) {
                    query = query + ", ";
                }
                first = false;
                AchievementsEngine.getInstance().getAchievementManager().getAchievements().add(
                        new Achievement(key, yml.getString("achievements." + key + ".name"),
                                yml.getString("achievements." + key + ".description"),
                                yml.getStringList("achievements." + key + ".events"),
                                yml.getStringList("achievements." + key + ".actions"),
                                yml.getString("achievements." + key + ".item"),
                                yml.getBoolean("achievements." + key + ".showProgress"),
                                yml.getBoolean("achievements." + key + ".announceProgress"))); // Create new achievement from yml
                AchievementsEngine.getInstance().getLogger().info("Loaded achievement: " + key);
                if(useSQL) {
                    query = query + "(default, ?)";
                    arguments[i] = key;
                    i++;
                }
            }
        }
        if(useSQL) {
            getImportantSQLQueue().put(query, arguments);
        }
    }

    public void loadMessagesFile() {
        File msgFile = new File(AchievementsEngine.getInstance().getDataFolder(), "messages.yml");
        if (!msgFile.exists()) { // If file doesn't exist, create default file
            AchievementsEngine.getInstance().saveResource("messages.yml", false);
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(msgFile);
        ConfigurationSection section = yml.getConfigurationSection("messages");
        if(section == null) return;
        AchievementsEngine.getInstance().getMessages().clearMessages();
        for (String key : section.getKeys(false)) {
            AchievementsEngine.getInstance().getMessages().addMessage(key, yml.getString("messages." + key));
        }
    }

    public void loadConfig() {
        File configFile = new File(AchievementsEngine.getInstance().getDataFolder(), "config.yml");
        if(!configFile.exists()) {
            AchievementsEngine.getInstance().saveResource("config.yml", false);
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(configFile);
        if(yml.getBoolean("config.useSQL")) {
            SQLInfo.put("host", yml.getString("sql.host"));
            SQLInfo.put("user", yml.getString("sql.user"));
            SQLInfo.put("password", yml.getString("sql.password"));
            SQLInfo.put("database", yml.getString("sql.database"));
            SQLInfo.put("port", yml.getString("sql.port"));
        }
        this.saveInterval = yml.getInt("config.saveInterval");
        this.useYAML = yml.getBoolean("config.useYAML");
        this.useSQL = yml.getBoolean("config.useSQL");
        this.saveTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(AchievementsEngine.getInstance(),
                this::saveAll, 20L * saveInterval, 20L * saveInterval);
        if(useYAML && useSQL) {
            AchievementsEngine.getInstance().getLogger().warning("ATTENTION! YOU'RE USING YAML AND SQL TO SAVE DATA. THIS MAY CAUSE MANY ERRORS.");
        }
    }
}
