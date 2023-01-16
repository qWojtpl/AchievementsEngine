package pl.achievementsengine.data;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
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

    public static FileConfiguration yml; // Current reading yml

    public static void createPlayerAchievementState(Player p) {
        PlayerAchievementState state = PlayerAchievementState.Create(p);
        createFile(p);
        File dataFile = getPlayerFile(state.getPlayer());
        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        String nick = p.getName();
        ConfigurationSection section = data.getConfigurationSection("user." + nick);
        if (section == null) {
            state.initialized = true;
            return;
        }
        for (String key : section.getKeys(false)) {
            for (Achievement a : AchievementsEngine.achievements) {
                if (!a.ID.equals(key)) {
                    continue;
                }
                if (data.getBoolean("user." + nick + "." + key + ".completed")) {
                    state.completedAchievements.add(a);
                }
                List<Integer> progressList = data.getIntegerList("user." + nick + "." + key + ".progress");
                int[] progress = new int[a.events.size()];
                int i = 0;
                for (int value : progressList) {
                    progress[i] = value;
                    i++;
                }
                state.progress.put(a, progress);
                break;
            }
        }
        state.initialized = true;
        if (state.openGUI) {
            state.openGUI = false;
            GUIHandler.New(state.getPlayer(), 0);
        }
    }

    public static void addCompletedAchievement(PlayerAchievementState state, Achievement achievement) {
        createFile(state.getPlayer());
        File dataFile = getPlayerFile(state.getPlayer());
        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        data.set("user." + state.getPlayer().getName() + "." + achievement.ID + ".completed", true);
        try {
            data.save(dataFile);
        } catch(IOException e) {
            AchievementsEngine.main.getLogger().info("Cannot add " + achievement.ID + " to " + state.getPlayer().getName() + "'s completed achievements..");
            AchievementsEngine.main.getLogger().info("IO Exception: " + e);
        }
    }

    public static void removeCompletedAchievement(PlayerAchievementState state, Achievement achievement) {
        createFile(state.getPlayer());
        File dataFile = getPlayerFile(state.getPlayer());
        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        data.set("user." + state.getPlayer().getName() + "." + achievement.ID + ".completed", false);
        try {
            data.save(dataFile);
        } catch(IOException e) {
            AchievementsEngine.main.getLogger().info("Cannot remove " + achievement.ID + " from " + state.getPlayer().getName() + "'s completed achievements..");
            AchievementsEngine.main.getLogger().info("IO Exception: " + e);
        }
    }

    public static void updateProgress(PlayerAchievementState state, Achievement achievement) {
        createFile(state.getPlayer());
        int[] progress = state.progress.get(achievement);
        List<Integer> newProgress = new ArrayList<>();
        for(int i = 0; i < progress.length; i++) {
            newProgress.add(progress[i]);
        }
        File dataFile = getPlayerFile(state.getPlayer());
        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        data.set("user." + state.getPlayer().getName() + "." + achievement.ID + ".progress", newProgress);
        try {
            data.save(dataFile);
        } catch(IOException e) {
            AchievementsEngine.main.getLogger().info("Cannot update progress at " + achievement.ID
                    + " (" + state.getPlayer().getName() + "'s progress)");
            AchievementsEngine.main.getLogger().info("IO Exception: " + e);
        }
    }

    public static void transferAchievements(PlayerAchievementState state1, PlayerAchievementState state2) {
        createFile(state2.getPlayer());
        createFile(state1.getPlayer());
        File dataFile1 = getPlayerFile(state1.getPlayer());
        YamlConfiguration data1 = YamlConfiguration.loadConfiguration(dataFile1);
        try {
            data1.set("user." + state1.getPlayer().getName(), null);
            data1.save(dataFile1);
        } catch(IOException e) {
            AchievementsEngine.main.getLogger().info("Cannot transfer achievements from " + state1.getPlayer().getName()
                    + " to " + state2.getPlayer().getName());
            AchievementsEngine.main.getLogger().info("IO Exception: " + e);
        }
        state2.completedAchievements = new ArrayList<>(state1.completedAchievements);
        state2.progress = new HashMap<>(state1.progress);
        state1.completedAchievements = new ArrayList<>();
        state1.progress = new HashMap<>();
        for(Achievement a : state2.completedAchievements) {
            DataHandler.addCompletedAchievement(state2, a);
        }
        for(Achievement a : state2.progress.keySet()) {
            DataHandler.updateProgress(state2, a);
        }
    }

    public static void createFile(Player p) {
        File dataFile = getPlayerFile(p);
        if(!dataFile.exists()) {
            try {
                File directory = new File(AchievementsEngine.main.getDataFolder(), "/playerData/");
                if(!directory.exists()) directory.mkdir();
                dataFile.createNewFile();
            } catch(IOException e) {
                AchievementsEngine.main.getLogger().info("Cannot create " + p.getName() + ".yml");
                AchievementsEngine.main.getLogger().info("IO Exception: " + e);
            }
        } else {
            if(!dataFile.canRead() || !dataFile.canWrite()) {
                AchievementsEngine.main.getLogger().info("Cannot create " + p.getName() + ".yml");
            }
        }
    }

    public static File getPlayerFile(Player p) {
        return new File(AchievementsEngine.main.getDataFolder(), "/playerData/" + p.getName() + ".yml");
    }

    public static String ReadStringPath(String path) {
        if (yml.getString(path) == null) {
            yml.set(path, "");
        }
        return yml.getString(path);
    }

    public static void loadAchievementsFile() {
        File achFile = new File(AchievementsEngine.main.getDataFolder(), "achievements.yml");
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
                AchievementsEngine.main.getLogger().info("Cannot create achievements.yml - exception: " + e);
                return;
            }
        }
        yml = YamlConfiguration.loadConfiguration(achFile);
        ConfigurationSection section = yml.getConfigurationSection("achievements");
        if(section == null) return;
        for (String key : section.getKeys(false)) {
            if (yml.getString("achievements." + key + ".name") != null) {
                AchievementsEngine.main.achievements.add(
                        new Achievement(key, ReadStringPath("achievements." + key + ".name"),
                                ReadStringPath("achievements." + key + ".description"),
                                yml.getBoolean("achievements." + key + ".enabled"), yml.getStringList("achievements." + key + ".events"),
                                yml.getStringList("achievements." + key + ".actions"), yml.getString("achievements." + key + ".item"),
                                yml.getBoolean("achievements." + key + ".showProgress"))); // Create new achievement from yml
                AchievementsEngine.main.getLogger().info("Loaded achievement: " + key);
            }
        }
    }

    public static void loadMessagesFile() {
        File msgFile = new File(AchievementsEngine.main.getDataFolder(), "messages.yml");
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
                AchievementsEngine.main.getLogger().info("Cannot create messages.yml");
                return;
            }
        }
        yml = YamlConfiguration.loadConfiguration(msgFile);
        ConfigurationSection section = yml.getConfigurationSection("messages");
        if(section == null) return;
        AchievementsEngine.messages = new HashMap<>();
        for (String key : section.getKeys(false)) {
            AchievementsEngine.messages.put(key, ReadStringPath("messages." + key));
        }
    }

}
