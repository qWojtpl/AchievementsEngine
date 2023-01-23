package pl.achievementsengine.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import pl.achievementsengine.AchievementsEngine;

public class PermissionManager {

    public static void registerPermission(String permission, String description) {
        Permission perm = new Permission(permission, description);
        AchievementsEngine.main.getServer().getPluginManager().addPermission(perm);
        AchievementsEngine.permissions.put(permission, perm);
    }

    public static boolean checkIfSenderHasPermission(CommandSender sender, Permission permission) {
        if(!(sender instanceof Player)) return true;
        if(!sender.hasPermission(permission)) {
            sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§cYou don't have permission!");
            return false;
        }
        return true;
    }

    public static Permission getPermission(String permission) {
        if(!AchievementsEngine.permissions.containsKey(permission)) {
            AchievementsEngine.main.getLogger().info("Trying to access permission that not exists (" + permission + ")! " +
                    "Please report it here: https://github.com/qWojtpl/AchievementsEngine/issues");
            return null;
        }
        return AchievementsEngine.permissions.get(permission);
    }

    public static void loadPermissions() {
        registerPermission("ae.use", "Use AchievementsEngine");
        registerPermission("ae.manage", "Manage AchievementsEngine");
        registerPermission("ae.reload", "Reload AchievementsEngine configuration");
        registerPermission("ae.achievements", "See AchievementsEngine achievements");
        registerPermission("ae.checkstate", "Check player's state");
        registerPermission("ae.complete", "Complete achievement for player in AchievementsEngine");
        registerPermission("ae.remove", "Remove achievement from player in AchievementsEngine");
        registerPermission("ae.reset", "Reset progress for player in AchievementsEngine");
        registerPermission("ae.transfer", "Transfer achievements and progress from player to player in AchievementsEngine");
    }

}
