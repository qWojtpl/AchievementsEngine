package pl.achievementsengine.achievements;

import lombok.Getter;
import org.bukkit.entity.Player;
import pl.achievementsengine.AchievementsEngine;
import pl.achievementsengine.commands.PermissionManager;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

@Getter
public class AchievementManager {

    private final List<Achievement> achievements = new ArrayList<>(); // List of all achievements

    public void Check(Player p, String checkable, Achievement a) {
        PermissionManager permissionManager = AchievementsEngine.getInstance().getPermissionManager();
        if(!p.hasPermission(permissionManager.getPermission("ae.use"))) return; // Check if player has permission
        if(a.getWorld() != null) {
            if(!p.getWorld().getName().equalsIgnoreCase(a.getWorld())) return;
        }
        PlayerAchievementState state = PlayerAchievementState.Create(p);
        if(!state.isInitialized()) {
            state.getQueue().add(new CheckableObject(p, checkable, a)); // Add this to queue
            return;
        }
        if(state.getCompletedAchievements().contains(a)) return; // If player has completed this achievement then return
        int[] progress = state.getProgress().getOrDefault(a, new int[a.getEvents().size()]); // Get progress
        int required = 0, sum = 0; // Required achievement progress to complete and player's progress sum
        checkable = checkable.toLowerCase(); // Make checkable lowercase
        String[] givenEvent = checkable.split(" "); // Split checkable
        for(int i = 0; i < a.getEvents().size(); i++) { // Loop through all achievement's events
            String[] event = a.getEvents().get(i).toLowerCase().split(" "); // Split event and make it lowercase
            required += Integer.parseInt(event[1]); // Add this event required progress to required
            sum += progress[i]; // Add current progress to sum
            if(progress[i] >= Integer.parseInt(event[1])) continue; // If progress is greater or equals event's required progress then skip
            if(!event[0].equalsIgnoreCase(givenEvent[0])) continue; // If first element is not the same (break != break, kill != kill) - skip
            if(givenEvent.length > 3) {
                if(givenEvent[2].equalsIgnoreCase("named")) {
                    if(!a.getEvents().get(i).contains(" named ") && checkable.contains(" named ")) { // If given contains named, but source event doesn't
                        checkable = givenEvent[0] + " " + givenEvent[1];
                    }
                }
            }
            if(event[2].equalsIgnoreCase("*")) { // If event is * (eg. kill 10 *, fish 10 *)
                event[2] = givenEvent[1]; // Set event[2] to second element of checkable (fe. checkable is "break bedrock" so "break *" -> "break bedrock")
                if(givenEvent[0].equalsIgnoreCase("chat") || givenEvent[0].equalsIgnoreCase("sign")) {
                    for(int j = 2; j < givenEvent.length; j++) {
                        event[2] += " " + givenEvent[j];
                    }
                }
            }
            if(event[2].contains("*%")) { // If event[2] contains ANY LIKE ([0]break [1]10 [2]*%_ore)
                if(!checkable.contains(event[2].replace("*%", ""))) {
                    continue;
                } else { // If checkable contains event[2] (replaced *% with nothing)
                    event[2] = givenEvent[1];
                    if(givenEvent[0].equalsIgnoreCase("chat") || givenEvent[0].equalsIgnoreCase("sign")) {
                        for(int j = 2; j < givenEvent.length; j++) {
                            event[2] += " " + givenEvent[j];
                        }
                    }
                }
            }
            String eventCheck = event[0];
            for(int j = 2; j < event.length; j++) {
                eventCheck += " " + event[j];
            }
            if(!eventCheck.equalsIgnoreCase(checkable)) continue;
            sum += 1;
            progress[i] += 1;
            state.UpdateProgress(a, progress); // Update progress
            if(a.isAnnounceProgress()) { // If achievement has announceProgress enabled - show message on a chat
                String message = MessageFormat.format(AchievementsEngine.getInstance().getMessages()
                        .getMessage("progress-message"), a.getName());
                String[] msg = message.split("%nl%");
                for(String m : msg) {
                    p.sendMessage(m);
                }
            }
        }
        if(a.getRequiredProgress() != 0) {
            required = a.getRequiredProgress();
        }
        if(sum >= required) a.Complete(state);
    }

    public Achievement checkIfAchievementExists(String name) {
        for (Achievement a : getAchievements()) {
            if (a.getID().equals(name)) {
                return a;
            }
        }
        return null;
    }

}
