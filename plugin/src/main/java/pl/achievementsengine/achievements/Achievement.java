package pl.achievementsengine.achievements;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import pl.achievementsengine.AchievementsEngine;
import pl.achievementsengine.data.DataHandler;

import java.text.MessageFormat;
import java.util.List;

@Getter
public class Achievement {

    private final String ID; // achievement key
    private final String name; // achievement name
    private final String description; // achievement description
    private final boolean enabled; // is achievement enabled?
    private final List<String> events; // events required to complete this achievement
    private final List<String> actions; // actions (commands) which will be triggered if player will complete achievement
    private final Material item; // GUI item
    private final boolean showProgress; // show achievement progress?

    public Achievement(String id, String name, String description, boolean enabled, List<String> events, List<String> actions, String item, boolean showProgress) {
        this.ID = id;
        this.name = name;
        this.description = description;
        this.enabled = enabled;
        this.events = events;
        this.actions = actions;
        if(Material.getMaterial(item.toUpperCase()) == null) { // If not found material, use bedrock
            this.item = Material.BEDROCK;
        } else {
            this.item = Material.getMaterial(item.toUpperCase());
        }
        this.showProgress = showProgress;
        for(String event : events) {
            String[] ev = event.split(" ");
            AchievementsEngine.getInstance().getEvents().registerEvent(ev[0], this);
        }
    }

    public void Complete(PlayerAchievementState state) {
        for(int j = 0; j < state.getCompletedAchievements().size(); j++) { // Loop through player's completed achievements
            if(state.getCompletedAchievements().get(j).ID.equals(this.ID)) { // If this achievement is found in player's complete achievement - return
                return;
            }
        }
        String message = MessageFormat.format(AchievementsEngine.ReadLanguage("complete-message"), name, description, events, actions);
        String[] msg = message.split("%nl%");
        for(String m : msg) {
            state.getPlayer().sendMessage(m);
        }
        state.getPlayer().playSound(state.getPlayer().getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f); // Play sound
        state.getCompletedAchievements().add(this); // Add this achievement to player's completed achievements
        for (String action : actions) { // Trigger all achievement commands
            Bukkit.getScheduler().callSyncMethod(AchievementsEngine.getInstance(), () ->
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), MessageFormat.format(action, state.getPlayer().getName(), name)));
        }
        DataHandler.addCompletedAchievement(state, this); // Add this achievement to player's completed achievements (Save data)
        AchievementsEngine.getInstance().getEvents().checkForAchievementEvents(state.getPlayer(), "complete " + this.ID); // Let the other achievements know that player completed this achievement
    }
}
