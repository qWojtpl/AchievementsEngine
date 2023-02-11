package pl.achievementsengine.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import pl.achievementsengine.AchievementsEngine;

import java.util.HashMap;

public class PermissionManager {

    private final HashMap<String, Permission> permissions = new HashMap<>();

    public void registerPermission(String permission, String description) {
        Permission perm = new Permission(permission, description);
        AchievementsEngine.getInstance().getServer().getPluginManager().addPermission(perm);
        this.permissions.put(permission, perm);
    }

    public boolean checkIfSenderHasPermission(CommandSender sender, Permission permission) {
        if(!(sender instanceof Player)) return true;
        if(!sender.hasPermission(permission)) {
            sender.sendMessage(AchievementsEngine.getInstance().getMessages().getMessage("prefix") + "§cYou don't have permission!");
            return false;
        }
        return true;
    }

    public Permission getPermission(String permission) {
        if(!this.permissions.containsKey(permission)) {
            AchievementsEngine.getInstance().getLogger().info("Trying to access permission that not exists (" + permission + ")! " +
                    "Please report it here: https://github.com/qWojtpl/AchievementsEngine/issues");
            return null;
        }
        return this.permissions.get(permission);
    }

    public void loadPermissions() {
        registerPermission("ae.use", "Use AchievementsEngine");
        registerPermission("ae.manage", "Manage AchievementsEngine");
        registerPermission("ae.reload", "Reload AchievementsEngine configuration");
        registerPermission("ae.achievements", "See AchievementsEngine achievements");
        registerPermission("ae.complete", "Complete achievement for player in AchievementsEngine");
        registerPermission("ae.remove", "Remove achievement from player in AchievementsEngine");
        registerPermission("ae.reset", "Reset progress for player in AchievementsEngine");
        registerPermission("ae.transfer", "Transfer achievements and progress from player to player in AchievementsEngine");
    }

}
