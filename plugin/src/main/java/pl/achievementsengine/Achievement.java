package pl.achievementsengine;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import pl.achievementsengine.data.DataHandler;

import java.text.MessageFormat;
import java.util.List;

public class Achievement {

    public String ID; // achievement key
    public String name; // achievement name
    public String description; // achievement description
    public Boolean enabled; // is achievement enabled?
    public List<String> events; // events required to complete this achievement
    public List<String> actions; // actions (commands) which will be triggered if player will complete achievement
    public Material item; // GUI item
    public boolean showProgress; // show achievement progress?

    public Achievement(String id, String name, String description, Boolean enabled, List<String> events, List<String> actions, String item, boolean showProgress) {
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
    }

    public void Complete(PlayerAchievementState state) {
        for(int j = 0; j < state.completedAchievements.size(); j++) { // Loop through player's completed achievements
            if(state.completedAchievements.get(j).ID.equals(this.ID)) { // If this achievement is found in player's complete achievement - return
                return;
            }
        }
        String message = MessageFormat.format(AchievementsEngine.ReadLanguage("complete-message"), name, description, events, actions);
        String[] msg = message.split("%nl%");
        for(String m : msg) {
            state.getPlayer().sendMessage(m);
        }
        state.getPlayer().playSound(state.getPlayer().getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f); // Play sound
        state.completedAchievements.add(this); // Add this achievement to player's completed achievements
        for (String action : actions) { // Trigger all achievement commands
            Bukkit.getScheduler().callSyncMethod(AchievementsEngine.main, () ->
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), MessageFormat.format(action, state.getPlayer().getName(), name)));
        }
        DataHandler.addCompletedAchievement(state, this); // Add this achievement to player's completed achievements (Save data)
        Achievement.Check(state.getPlayer(), "complete " + this.ID); // Let the other achievements know that player completed this achievement
    }

    public static void Check(Player p, String checkable) { // Event interpreter
        if(!p.hasPermission("ae.use")) return; // Check if player has permission
        for(int i = 0; i < AchievementsEngine.achievements.size(); i++) { // Loop through all achievements
            Achievement a = AchievementsEngine.achievements.get(i);
            if(a.enabled) { // Check if achievement is enabled
                PlayerAchievementState playerState;
                playerState = PlayerAchievementState.Create(p); // Create player's state
                if(!playerState.initialized) { // If player state is not initialized, then add this checkable to queue
                    playerState.checkQueue.add(checkable);
                    return;
                }
                int[] progress = playerState.progress.getOrDefault(a, new int[a.events.size()]); // Initialize progress
                int max = 0;
                for(int j = 0; j < a.events.size(); j++) { // Loop through all events from achievement
                    String[] events = a.events.get(j).split(" "); // Split one event into pieces eg. [kill] [1] [player]
                    String[] givenEvents = checkable.split(" "); // Split checkable into pieces eg. [kill] [player]
                    if(events[0].equalsIgnoreCase(givenEvents[0]) && (events[2].equalsIgnoreCase("*") || events[2].equalsIgnoreCase(givenEvents[1]))) { // Check if events[0] equals givenEvents[0] ([kill] = [kill]) and events[2] equals (* - any) or givenEvents[1] ([Player] = [Player]), events[1] is a number
                        boolean match = false; // Mark if all external things (name, chat string) matches
                        if(events.length > 4) { // Check if event length is greater than 4 (event contains named or chat with space) - pickup 1 bedrock named BEDDI (at least 5 arguments)
                            if (events[3].equals("named") && givenEvents[2].equals("named") && givenEvents.length > 3) { // Check if third argument is named and second argument of given event is "named" (checks if event provides named too), and given events contains at least 4 arguments - enchant diamond_sword named xyz
                                String name = givenEvents[3];
                                for(int k = 5; k < events.length; k++) { // If name from event contains spaces - join it
                                    events[4] = events[4] + " " + events[k];
                                }
                                for(int k = 4; k < givenEvents.length; k++) { // If name from given event contains spaces - join it
                                    name = name + " " + givenEvents[k];
                                }
                                if(name.equals(events[4])) { // If name from given event equals name from event, then mark Match as true
                                    match = true;
                                }
                            } else if(events[0].equals("chat")){ // Check if event type is chat
                                String givenMessage = givenEvents[1];
                                String message = events[2];
                                for(int k = 3; k < events.length; k++) { // If message from event contains spaces - join it
                                    message = message + " " + events[k];
                                }
                                for(int k = 2; k < givenEvents.length; k++) { // If message from given event contains spaces - join it
                                    givenMessage = givenMessage + " " + givenEvents[k];
                                }
                                if(message.equals(givenMessage)) { // If message equals given message, then mark Match as true
                                    match = true;
                                }
                            }
                        } else {
                            AchievementsEngine.main.getLogger().info("events length: " + events.length);
                            AchievementsEngine.main.getLogger().info("given length: " + givenEvents.length);
                            match = true;
                        }
                        if(progress[j] < Integer.valueOf(events[1]) && match) { // Check if progress of event is less than required progress of this event
                            progress[j]++; // Add one to progress
                            playerState.UpdateProgress(a, progress); // Update progress
                            if(a.showProgress) { // If achievement has showProgress enabled - show message on a chat
                                String message = MessageFormat.format(AchievementsEngine.ReadLanguage("progress-message"), a.name);
                                String[] msg = message.split("%nl%");
                                for(String m : msg) {
                                    p.sendMessage(m);
                                }
                            }
                        }
                    }
                    max += Integer.valueOf(events[1]); // Add required progress to max progress
                }
                int sum = 0; // Set sum to 0
                for(int pr : progress) { // Loop through player's progresses
                    sum += pr; // Add player's progress to sum
                }
                if(sum == max) a.Complete(playerState); // If sum of player's progresses equals max progress for this achievement - complete it!
            }
        }
    }

    public static Achievement checkIfAchievementExists(String name) {
        for (Achievement a : AchievementsEngine.achievements) {
            if (a.ID.equals(name)) {
                return a;
            }
        }
        return null;
    }
}
