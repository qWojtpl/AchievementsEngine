package pl.achievementsengine.util;

import lombok.Getter;

import java.util.HashMap;

@Getter
public class Messages {

    private final HashMap<String, String> messages = new HashMap<>(); // List of all messages from messages.yml
    private final HashMap<String, String> eventTranslation = new HashMap<>();

    public String getMessage(String path) {
        if(messages.containsKey(path)) {
            return messages.get(path);
        } else {
            return "§cCannotRead exception for path \"" + path + "\"";
        }
    }

    public String getEventTranslation(String path) {
        return eventTranslation.getOrDefault(path, path);
    }

    public void addMessage(String key, String message) {
        messages.put(key, message);
    }

    public void addEventTranslation(String key, String translation) {
        eventTranslation.put(key, translation);
    }

    public void clearMessages() {
        messages.clear();
    }

    public void clearEventTranslations() {
        eventTranslation.clear();
    }
}
