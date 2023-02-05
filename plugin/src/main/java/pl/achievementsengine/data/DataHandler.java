package pl.achievementsengine.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pl.achievementsengine.achievements.Achievement;
import pl.achievementsengine.AchievementsEngine;
import pl.achievementsengine.gui.GUIHandler;
import pl.achievementsengine.achievements.PlayerAchievementState;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DataHandler {

    private final HashMap<String, YamlConfiguration> playerYAML = new HashMap<>();
    private final HashMap<String, String> SQLInfo = new HashMap<>();

    public HashMap<String, String> getSQLInfo() {
        return this.SQLInfo;
    }

    public void LoadConfig() {
        AchievementsEngine.getInstance().getPlayerStates().clear(); // Reset player states list
        AchievementsEngine.getInstance().getAchievementManager().getAchievements().clear(); // Reset achievements list
        GUIHandler.CloseAllInventories(); // Close all registered inventories to prevent GUI item duping.
        SQLInfo.clear();
        loadConfig(); // Load config
        loadAchievementsFile(); // Load achievements
        loadMessagesFile(); // Load messages
    }

    public void createPlayerAchievementState(Player p) {
        PlayerAchievementState state = PlayerAchievementState.Create(p);
        File dataFile = createPlayerFile(p);
        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        String nick = p.getName();
        ConfigurationSection section = data.getConfigurationSection("user." + nick);
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            for (Achievement a : AchievementsEngine.getInstance().getAchievementManager().getAchievements()) {
                if (!a.getID().equals(key)) {
                    continue;
                }
                if (data.getBoolean("user." + nick + "." + key + ".completed")) {
                    state.getCompletedAchievements().add(a);
                }
                List<Integer> progressList = data.getIntegerList("user." + nick + "." + key + ".progress");
                int[] progress = new int[a.getEvents().size()];
                int i = 0;
                for (int value : progressList) {
                    progress[i] = value;
                    i++;
                }
                state.getProgress().put(a, progress);
                break;
            }
        }
    }

    public void addCompletedAchievement(PlayerAchievementState state, Achievement achievement) {
        File dataFile = createPlayerFile(state.getPlayer());
        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        data.set("user." + state.getPlayer().getName() + "." + achievement.getID() + ".completed", true);
        try {
            data.save(dataFile);
        } catch(IOException e) {
            AchievementsEngine.getInstance().getLogger().info("Cannot add " + achievement.getID() + " to " + state.getPlayer().getName() + "'s completed achievements..");
            AchievementsEngine.getInstance().getLogger().info("IO Exception: " + e);
        }
    }

    public void removeCompletedAchievement(PlayerAchievementState state, Achievement achievement) {
        File dataFile = createPlayerFile(state.getPlayer());
        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        data.set("user." + state.getPlayer().getName() + "." + achievement.getID() + ".completed", false);
        try {
            data.save(dataFile);
        } catch(IOException e) {
            AchievementsEngine.getInstance().getLogger().info("Cannot remove " + achievement.getID() + " from " + state.getPlayer().getName() + "'s completed achievements..");
            AchievementsEngine.getInstance().getLogger().info("IO Exception: " + e);
        }
    }

    public void updateProgress(PlayerAchievementState state, Achievement achievement) {
        int[] progress = state.getProgress().get(achievement);
        List<Integer> newProgress = new ArrayList<>();
        for(int i = 0; i < progress.length; i++) {
            newProgress.add(progress[i]);
        }
        File dataFile = createPlayerFile(state.getPlayer());
        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        data.set("user." + state.getPlayer().getName() + "." + achievement.getID() + ".progress", newProgress);
        try {
            data.save(dataFile);
        } catch(IOException e) {
            AchievementsEngine.getInstance().getLogger().info("Cannot update progress at " + achievement.getID()
                    + " (" + state.getPlayer().getName() + "'s progress)");
            AchievementsEngine.getInstance().getLogger().info("IO Exception: " + e);
        }
    }

    public void transferAchievements(PlayerAchievementState state1, PlayerAchievementState state2) {
        File dataFile1 = createPlayerFile(state1.getPlayer());
        File dataFile2 = createPlayerFile(state2.getPlayer());
        YamlConfiguration data1 = YamlConfiguration.loadConfiguration(dataFile1);
        try {
            data1.set("user." + state1.getPlayer().getName(), null);
            data1.save(dataFile1);
        } catch(IOException e) {
            AchievementsEngine.getInstance().getLogger().info("Cannot transfer achievements from " + state1.getPlayer().getName()
                    + " to " + state2.getPlayer().getName());
            AchievementsEngine.getInstance().getLogger().info("IO Exception: " + e);
        }
        state2.setCompletedAchievements(new ArrayList<>(state1.getCompletedAchievements()));
        state2.setProgress(new HashMap<>(state1.getProgress()));
        state1.setCompletedAchievements(new ArrayList<>());
        state1.setProgress(new HashMap<>());
        for(Achievement a : state2.getCompletedAchievements()) {
            addCompletedAchievement(state2, a);
        }
        for(Achievement a : state2.progress.keySet()) {
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
        for (String key : section.getKeys(false)) {
            if(key.length() > 128) {
                AchievementsEngine.getInstance().getLogger().warning("Cannot load achievement " + key + " - achievement key is too long..");
            }
            if (yml.getString("achievements." + key + ".name") != null) {
                AchievementsEngine.getInstance().getAchievementManager().getAchievements().add(
                        new Achievement(key, yml.getString("achievements." + key + ".name"),
                                yml.getString("achievements." + key + ".description"),
                                yml.getBoolean("achievements." + key + ".enabled"), yml.getStringList("achievements." + key + ".events"),
                                yml.getStringList("achievements." + key + ".actions"), yml.getString("achievements." + key + ".item"),
                                yml.getBoolean("achievements." + key + ".showProgress"))); // Create new achievement from yml
                AchievementsEngine.getInstance().getLogger().info("Loaded achievement: " + key);
            }
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
    }
}
