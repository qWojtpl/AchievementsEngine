package pl.achievementsengine.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PlayerUtil {

    public Player checkIfPlayerExists(String nickname) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().equals(nickname)) {
                return p;
            }
        }
        return null;
    }

}
