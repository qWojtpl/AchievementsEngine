package pl.achievementsengine.achievements;

import org.bukkit.entity.Player;
import pl.achievementsengine.AchievementsEngine;
import pl.achievementsengine.data.DataHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PlayerAchievementState {

    private Player player; // State player
    private List<Achievement> completedAchievements; // Completed achievements
    public HashMap<Achievement, int[]> progress = new HashMap<>(); // Achievement progress

    public PlayerAchievementState(Player player, List<Achievement> completedAchievements) {
        this.player = player;
        this.completedAchievements = completedAchievements;
    }

    public static PlayerAchievementState Create(Player p) { // Create state. If is already created, return exist one
        if(!AchievementsEngine.getInstance().getPlayerStates().containsKey(p.getName())) {
            PlayerAchievementState state = new PlayerAchievementState(p, new ArrayList<>()); // Create object
            AchievementsEngine.getInstance().getPlayerStates().put(p.getName(), state); // Put state to all states
            AchievementsEngine.getInstance().getDataHandler().createPlayerAchievementState(state); // Create data in playerData.yml (and if available in SQL)
            return state;
        } else {
            AchievementsEngine.getInstance().getPlayerStates().get(p.getName()).setPlayer(p); // Update player object
        }
        return AchievementsEngine.getInstance().getPlayerStates().get(p.getName());
    }

    public void UpdateProgress(Achievement achievement, int[] progressArray) { // Update progress
        progress.remove(achievement); // Remove achievement progress
        progress.put(achievement, progressArray); // Add new achievement progress
        AchievementsEngine.getInstance().getDataHandler().updateProgress(this, achievement); // Update progress (Save data)
    }

    public void RemoveAchievement(Achievement achievement) { // Remove achievement from completed achievements
        this.completedAchievements.remove(achievement);
        this.progress.put(achievement, new int[achievement.getEvents().size()]);
        DataHandler dh = AchievementsEngine.getInstance().getDataHandler();
        dh.removeCompletedAchievement(this, achievement);
        dh.updateProgress(this, achievement);
    }

    public Player getPlayer() { // Return player
        return this.player;
    }

    public List<Achievement> getCompletedAchievements() {
        return this.completedAchievements;
    }

    public HashMap<Achievement, int[]> getProgress() {
        return this.progress;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public void setCompletedAchievements(List<Achievement> achievements) {
        this.completedAchievements = achievements;
    }

    public void setProgress(HashMap<Achievement, int[]> progress) {
        this.progress = progress;
    }

}
