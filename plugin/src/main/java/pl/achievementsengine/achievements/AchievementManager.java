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
            if(progress[i] >= Integer.parseInt(event[1])) { // If progress is greater or equals event's required progress then skip
                continue;
            }
            if(!event[0].equalsIgnoreCase(givenEvent[0])) { // If first element is not the same (break != break, kill != kill) - skip
                continue;
            }
            if(givenEvent.length > 3) {
                if(givenEvent[2].equalsIgnoreCase("named")) {
                    if(!a.getEvents().get(i).contains(" named ") && checkable.contains(" named ")) {
                        checkable = givenEvent[0] + " " + givenEvent[1];
                    }
                }
            }
            if(event[2].equalsIgnoreCase("*")) { // If event is * (eg. kill 10 *, fish 10 *)
                event[2] = givenEvent[1]; // Set event[2] to second element of checkable (eg. checkable is "break bedrock" so "break *" -> "break bedrock")
            }
            if(event[2].contains("*%")) { // If event[2] contains ANY LIKE
                if(!checkable.contains(event[2].replace("*%", ""))) {
                    continue;
                } else {
                    event[2] = givenEvent[1];
                }
            }
            String eventCheck = event[0];
            for(int j = 2; j < event.length; j++) {
                eventCheck = eventCheck + " " + event[j];
            }
            if(!eventCheck.equalsIgnoreCase(checkable)) {
                AchievementsEngine.getInstance().getLogger().info("Continued at 3 - " + eventCheck + " AND " + checkable);
                continue;
            }
            sum += 1;
            progress[i] += 1;
            state.UpdateProgress(a, progress); // Update progress
            if(a.isAnnounceProgress()) { // If achievement has showProgress enabled - show message on a chat
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

    @Deprecated
    public void CheckOld(Player p, String checkable, Achievement a) { // Event interpreter
        PermissionManager permissionManager = AchievementsEngine.getInstance().getPermissionManager();
        if(!p.hasPermission(permissionManager.getPermission("ae.use"))) return; // Check if player has permission
        PlayerAchievementState playerState = PlayerAchievementState.Create(p); // Get player's state
        if(!playerState.isInitialized()) { // If player's state is not initialized - add checkable to queue and return
            playerState.getQueue().add(new CheckableObject(p, checkable, a));
            return;
        }
        if(playerState.getCompletedAchievements().contains(a)) return; // If player completed this achievement - return
        int[] progress = playerState.getProgress().getOrDefault(a, new int[a.getEvents().size()]); // Initialize progress
        int max = 0; // Progress required to complete this achievement
        for(int j = 0; j < a.getEvents().size(); j++) { // Loop through all events from achievement
            String[] events = a.getEvents().get(j).split(" "); // Split one event into pieces eg. [kill] [1] [player]
            String[] givenEvents = checkable.split(" "); // Split checkable into pieces eg. [kill] [player]
            // Check if events[0] equals givenEvents[0] ([kill] = [kill]) and events[2] equals (* - any) or givenEvents[1] ([Player] = [Player]) or events[2] contains *% - any like and events[2] contains givenEvents[1], events[1] is a number
            if(events[0].equalsIgnoreCase(givenEvents[0]) && (events[2].equalsIgnoreCase("*") || events[2].equalsIgnoreCase(givenEvents[1]) || (events[2].contains("*%") && givenEvents[1].toLowerCase().contains(events[2].replace("*%", "").toLowerCase()) ))) {
                boolean match = false; // Mark if all external things (name, chat string) matches
                if(events.length > 4 && givenEvents.length > 3) { // Check if event length is greater than 4 (event contains named or chat with space) - pickup 1 bedrock named BEDDI (at least 5 arguments)
                    // Check if third argument is named and second argument of given event is "named" (checks if event provides named too), and given events contains at least 4 arguments - enchant diamond_sword named xyz
                    if (events[3].equalsIgnoreCase("named") && givenEvents[2].equals("named")) {
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
                    } else if(events[0].equalsIgnoreCase("chat") || events[0].equalsIgnoreCase("sign")) { // Check if event type is chat or sign
                        String givenMessage = givenEvents[1];
                        String message = events[2];
                        for(int k = 3; k < events.length; k++) { // If message from event contains spaces - join it
                            message = message + " " + events[k];
                        }
                        for(int k = 2; k < givenEvents.length; k++) { // If message from given event contains spaces - join it
                            givenMessage = givenMessage + " " + givenEvents[k];
                        }
                        AchievementsEngine.getInstance().getLogger().info(message);
                        AchievementsEngine.getInstance().getLogger().info(givenMessage);
                        if(message.equals(givenMessage)) { // If message equals given message, then mark Match as true
                            match = true;
                        }
                    }
                } else {
                    match = true;
                }
                // Check if progress of event is less than required progress of this event
                if(progress[j] < Integer.parseInt(events[1]) && match) {
                    progress[j]++; // Add one to progress
                    playerState.UpdateProgress(a, progress); // Update progress
                    if(a.isAnnounceProgress()) { // If achievement has showProgress enabled - show message on a chat
                        String message = MessageFormat.format(AchievementsEngine.getInstance().getMessages()
                                .getMessage("progress-message"), a.getName());
                        String[] msg = message.split("%nl%");
                        for(String m : msg) {
                            p.sendMessage(m);
                        }
                    }
                }
            }
            max += Integer.parseInt(events[1]); // Add required progress to max progress
        }
        int sum = 0; // Set sum to 0
        for(int pr : progress) { // Loop through player's progress
            sum += pr; // Add player's progress to sum
        }
        if(a.getRequiredProgress() != 0) {
            max = a.getRequiredProgress();
        }
        if(sum >= max) a.Complete(playerState); // If sum of player's progresses equals max progress for this achievement - complete it!
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
