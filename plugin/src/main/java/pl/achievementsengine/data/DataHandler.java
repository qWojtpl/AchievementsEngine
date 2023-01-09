package pl.achievementsengine.data;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pl.achievementsengine.Achievement;
import pl.achievementsengine.AchievementsEngine;
import pl.achievementsengine.GUIHandler;
import pl.achievementsengine.PlayerAchievementState;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DataHandler {

    public static boolean useSQL = false;
    public static YamlConfiguration data;
    public static File dataFile;

    public static void createPlayerAchievementState(Player p) {
        PlayerAchievementState state = PlayerAchievementState.Create(p);
        if (useSQL) {
            SQLHandler.createPlayerAchievementState(p);
        }
        createFile();
        String nick = p.getName();
        ConfigurationSection section = data.getConfigurationSection("users." + nick);
        if (section == null) {
            state.initialized = true;
            return;
        }
        for (String key : section.getKeys(false)) {
            for (Achievement a : AchievementsEngine.achievements) {
                if (!a.ID.equals(key)) {
                    continue;
                }
                if (data.getBoolean("users." + nick + "." + key + ".completed")) {
                    state.completedAchievements.add(a);
                }
                List<Integer> progressList = data.getIntegerList("users." + nick + "." + key + ".progress");
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
        if (!useSQL) {
            if (state.openGUI) {
                state.openGUI = false;
                Bukkit.getScheduler().scheduleSyncDelayedTask(AchievementsEngine.main, () -> GUIHandler.New(state.getPlayer(), 0));
            }
        }
    }

    public static void addCompletedAchievement(PlayerAchievementState state, Achievement achievement) {
        if(useSQL) {
            SQLHandler.addCompletedAchievement(state, achievement);
        }
        createFile();
        data.set("users." + state.getPlayer().getName() + "." + achievement.ID + ".completed", true);
        try {
            data.save(dataFile);
        } catch(IOException e) {
            AchievementsEngine.main.getLogger().info("Cannot add " + achievement.ID + " to " + state.getPlayer().getName() + "'s completed achievements..");
            AchievementsEngine.main.getLogger().info("IO Exception: " + e);
        }
    }

    public static void removeCompletedAchievement(PlayerAchievementState state, Achievement achievement) {
        if(useSQL) {
            SQLHandler.addCompletedAchievement(state, achievement);
        }
        createFile();
        data.set("users." + state.getPlayer().getName() + "." + achievement.ID + ".completed", false);
        try {
            data.save(dataFile);
        } catch(IOException e) {
            AchievementsEngine.main.getLogger().info("Cannot remove " + achievement.ID + " from " + state.getPlayer().getName() + "'s completed achievements..");
            AchievementsEngine.main.getLogger().info("IO Exception: " + e);
        }
    }

    public static void updateProgress(PlayerAchievementState state, Achievement achievement) {
        createFile();
        int[] progress = state.progress.get(achievement);
        List<Integer> newProgress = new ArrayList<>();
        for(int i = 0; i < progress.length; i++) {
            newProgress.add(progress[i]);
        }
        data.set("users." + state.getPlayer().getName() + "." + achievement.ID + ".progress", newProgress);
        try {
            data.save(dataFile);
        } catch(IOException e) {
            AchievementsEngine.main.getLogger().info("Cannot update progress at " + achievement.ID
                    + " (" + state.getPlayer().getName() + "'s progress)");
            AchievementsEngine.main.getLogger().info("IO Exception: " + e);
        }
    }

    public static void transferAchievements(PlayerAchievementState state1, PlayerAchievementState state2) {
        createFile();
        data.set("users." + state1.getPlayer().getName(), null);
        try {
            data.save(dataFile);
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

    public static void createFile() {
        dataFile = new File(AchievementsEngine.main.getDataFolder(), "playerData.yml");
        if(!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch(IOException e) {
                AchievementsEngine.main.getLogger().info("Cannot create playerData.yml - disabling plugin..");
                AchievementsEngine.main.getLogger().info("IO Exception: " + e);
                AchievementsEngine.main.DisablePlugin();
                return;
            }
        } else {
            if(!dataFile.canRead() || !dataFile.canWrite()) {
                AchievementsEngine.main.getLogger().info("Cannot read or write to playerData.yml - disabling plugin..");
                AchievementsEngine.main.DisablePlugin();
                return;
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

}
