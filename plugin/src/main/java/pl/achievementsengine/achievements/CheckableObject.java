package pl.achievementsengine.achievements;

import lombok.Getter;
import org.bukkit.entity.Player;

@Getter
public class CheckableObject {

    private final Player player;
    private final String checkable;
    private final Achievement achievement;


    public CheckableObject(Player player, String checkable, Achievement achievement) {
        this.player = player;
        this.checkable = checkable;
        this.achievement = achievement;
    }

}
