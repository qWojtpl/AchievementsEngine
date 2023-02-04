package pl.achievementsengine.util;

import lombok.Getter;

import java.util.HashMap;

@Getter
public class Messages {

    private final HashMap<String, String> messages = new HashMap<>(); // List of all messages from messages.yml

    public String ReadLanguage(String path) {
        if(messages.containsKey(path)) {
            return messages.get(path);
        } else {
            return "§cCannotRead exception for path \"" + path + "\"";
        }
    }

    public void addMessage(String key, String message) {
        messages.put(key, message);
    }

    public void clearMessages() {
        messages.clear();
    }
}
