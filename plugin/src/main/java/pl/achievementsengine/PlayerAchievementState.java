package pl.achievementsengine;

import org.bukkit.entity.Player;
import pl.achievementsengine.data.DataHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class PlayerAchievementState {

    public UUID uuid; // State uuid
    public Player player; // State player
    public List<Achievement> completedAchievements = new ArrayList<>(); // Completed achievements
    public HashMap<Achievement, int[]> progress = new HashMap<>(); // Achievement progress
    public boolean initialized = false; // Is state initialized?
    public List<String> checkQueue = new ArrayList<>(); // Check achievements after initialization
    public boolean openGUI = false; // Open GUI after initialization?


    public static PlayerAchievementState Create(Player p) { // Create state. If is already created, return exist one
        if(!AchievementsEngine.playerStates.containsKey(p.getName())) {
            PlayerAchievementState state = new PlayerAchievementState(); // Create object
            state.player = p; // Save player
            state.uuid = p.getUniqueId(); // Save UUID
            state.completedAchievements = new ArrayList<>(); // Create completed achievements
            AchievementsEngine.playerStates.put(p.getName(), state); // Put state to all states
            DataHandler.createPlayerAchievementState(p); // Create data in playerData.yml (and if available in SQL)
            return state;
        } else {
            AchievementsEngine.playerStates.get(p.getName()).player = p; // Update player object
        }
        return AchievementsEngine.playerStates.get(p.getName());
    }

    public Player getPlayer() { // Return player
        return this.player;
    }

    public void UpdateProgress(Achievement achievement, int[] progressArray) { // Update progress
        progress.remove(achievement); // Remove achievement progress
        progress.put(achievement, progressArray); // Add new achievement progress
        DataHandler.updateProgress(this, achievement); // Update progress (Save data)
    }

    public void RemoveAchievement(Achievement achievement) { // Remove achievement from completed achievements
        this.completedAchievements.remove(achievement);
        this.progress.put(achievement, new int[achievement.events.size()]);
        DataHandler.removeCompletedAchievement(this, achievement);
        DataHandler.updateProgress(this, achievement);
    }
}
