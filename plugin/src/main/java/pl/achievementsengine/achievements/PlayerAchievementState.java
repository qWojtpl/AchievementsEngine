package pl.achievementsengine.achievements;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import pl.achievementsengine.AchievementsEngine;
import pl.achievementsengine.data.DataHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Getter
public class PlayerAchievementState {

    @Setter
    private Player player; // State player
    @Setter
    private List<Achievement> completedAchievements; // Completed achievements
    @Setter
    private HashMap<Achievement, int[]> progress = new HashMap<>(); // Achievement progress
    @Setter
    private boolean initialized;
    private int initializeLevel = 0;
    private final List<CheckableObject> queue = new ArrayList<>();


    public PlayerAchievementState(Player player, List<Achievement> completedAchievements) {
        this.player = player;
        this.completedAchievements = completedAchievements;
    }

    public static PlayerAchievementState Create(Player p) { // Create state. If is already created, return exist one
        if (!AchievementsEngine.getInstance().getPlayerStates().containsKey(p.getName())) {
            PlayerAchievementState state = new PlayerAchievementState(p, new ArrayList<>()); // Create object
            AchievementsEngine.getInstance().getPlayerStates().put(p.getName(), state); // Put state to all states
            AchievementsEngine.getInstance().getDataHandler().createPlayerAchievementState(state); // Read data from playerData.yml (and if available from SQL)
            return state;
        } else {
            AchievementsEngine.getInstance().getPlayerStates().get(p.getName()).setPlayer(p); // Update player object
        }
        return AchievementsEngine.getInstance().getPlayerStates().get(p.getName());
    }

    public static void CreateForOnline() {
        for(Player p : AchievementsEngine.getInstance().getServer().getOnlinePlayers()) { // Create state to all players
            PlayerAchievementState.Create(p);
        }
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

    public void setInitializeLevel(int level) {
        this.initializeLevel = level;
        if(this.initializeLevel >= 2) {
            this.initialized = true;
            for(CheckableObject co : getQueue()) {
                AchievementsEngine.getInstance().getAchievementManager().Check(co.getPlayer(), co.getCheckable(), co.getAchievement());
            }
            getQueue().clear();
        }
    }

}
