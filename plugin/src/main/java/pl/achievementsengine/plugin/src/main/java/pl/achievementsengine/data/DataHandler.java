package pl.achievementsengine.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pl.achievementsengine.Achievement;
import pl.achievementsengine.AchievementsEngine;
import pl.achievementsengine.PlayerAchievementState;
import sun.security.krb5.Config;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class DataHandler {

    public static boolean useSQL = false;
    public static YamlConfiguration data;

    public static void createPlayerAchievementState(Player p) {
        if(useSQL) {
            SQLHandler.createPlayerAchievementState(p);
        }
        String nick = p.getName();
        ConfigurationSection section = data.getConfigurationSection("users." + nick);
        if(section != null) {
            for(String key : section.getKeys(false)) {
                for(Achievement a : AchievementsEngine.achievements) {
                    if(a.ID.equals(key)) {
                        if(data.getBoolean("users." + nick + "." + key + ".completed")) {
                            PlayerAchievementState state = PlayerAchievementState.Create(p);
                            state.completedAchievements.add(a);
                            List<Integer> progressList = data.getIntegerList("users." + nick + "." + key + ".progress");
                            int[] progress = new int[a.events.size()];
                            int i = 0;
                            for(int value : progressList) {
                                progress[i] = value;
                                i++;
                            }
                        }
                        break;
                    }
                }
            }
        }
        createFile();
    }

    public static void createFile() {
        File dataFile = new File(AchievementsEngine.main.getDataFolder(), "playerData.yml");
        if(!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch(IOException e) {
                AchievementsEngine.main.getLogger().info("Cannot create playerData.yml - disabling plugin..");
                AchievementsEngine.main.DisablePlugin();
                return;
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

}
