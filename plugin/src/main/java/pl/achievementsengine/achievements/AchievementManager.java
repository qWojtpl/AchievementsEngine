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

    public void Check(Player p, String checkable, Achievement a) { // Event interpreter
        PermissionManager permissionManager = AchievementsEngine.getInstance().getPermissionManager();
        if(!p.hasPermission(permissionManager.getPermission("ae.use"))) return; // Check if player has permission
        PlayerAchievementState playerState = PlayerAchievementState.Create(p); // Get player's state
        int[] progress = playerState.getProgress().getOrDefault(a, new int[a.getEvents().size()]); // Initialize progress
        int max = 0;
        for(int j = 0; j < a.getEvents().size(); j++) { // Loop through all events from achievement
            String[] events = a.getEvents().get(j).split(" "); // Split one event into pieces eg. [kill] [1] [player]
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
                    match = true;
                }
                if(progress[j] < Integer.parseInt(events[1]) && match) { // Check if progress of event is less than required progress of this event
                    progress[j]++; // Add one to progress
                    playerState.UpdateProgress(a, progress); // Update progress
                    if(a.isAnnounceProgress()) { // If achievement has showProgress enabled - show message on a chat
                        String message = MessageFormat.format(AchievementsEngine.getInstance().getMessages().getMessage("progress-message"), a.getName());
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
        for(int pr : progress) { // Loop through player's progresses
            sum += pr; // Add player's progress to sum
        }
        if(sum == max) a.Complete(playerState); // If sum of player's progresses equals max progress for this achievement - complete it!
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
