package pl.achievementsengine.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.achievementsengine.achievements.Achievement;
import pl.achievementsengine.AchievementsEngine;
import pl.achievementsengine.gui.GUIHandler;
import pl.achievementsengine.achievements.PlayerAchievementState;
import pl.achievementsengine.util.PlayerUtil;

import java.util.ArrayList;
import java.util.List;

public class Commands implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(sender instanceof Player) { // Check if sender is player
            PermissionManager permissionManager = AchievementsEngine.getInstance().getPermissionManager();
            if(!permissionManager.checkIfSenderHasPermission(sender, permissionManager.getPermission("ae.use"))) return true;
            PlayerAchievementState state = PlayerAchievementState.Create((Player) sender);
            if(!state.isInitialized()) {
                sender.sendMessage(AchievementsEngine.getInstance().getMessages().getMessage("not-downloaded"));
                return true;
            }
            if(args.length == 0) { // If not arguments given - open GUI
                GUIHandler.New((Player) sender, 0);
                return true;
            } else if(!sender.hasPermission(permissionManager.getPermission("ae.manage"))) { // If arguments given but player don't have permission - open GUI
                GUIHandler.New((Player) sender, 0);
                return true;
            }
        }
        // Player have permission ae.manage or sender is console
        if(args.length == 0) { // If no arguments - show help (only for console - player will already have GUI)
            ShowHelp(sender, 1);
            return true;
        }
        if(args[0].equalsIgnoreCase("reload")) {
            c_Reload(sender);
        } else if(args[0].equalsIgnoreCase("help")) {
            c_Help(sender, args);
        } else if(args[0].equalsIgnoreCase("achievements")) {
            c_Achievements(sender);
        } else if(args[0].equalsIgnoreCase("complete")) {
            c_Complete(sender, args);
        } else if(args[0].equalsIgnoreCase("remove")) {
            c_Remove(sender, args);
        } else if(args[0].equalsIgnoreCase("reset")) {
            c_Reset(sender, args);
        } else if(args[0].equalsIgnoreCase("transfer")) {
            c_Transfer(sender, args);
        } else {
            ShowHelp(sender, 1);
        }
        return true;
    }

    private void ShowHelp(CommandSender sender, int page) {
        sender.sendMessage("§2<----------> §6AchievementsEngine §2<---------->");
        switch(page) {
            case 1:
                sender.sendMessage("§6/ae §e- Open achievements GUI (only for players)");
                sender.sendMessage("§6/ae help <page> §e- Shows help page");
                sender.sendMessage("§6/ae reload §e- Reloads configuration");
                sender.sendMessage("§6/ae achievements §e- Shows the list of achievements");
                sender.sendMessage("§6/ae complete <player> <id> §e- Complete achievement for player");
                break;
            case 2:
                sender.sendMessage("§6/ae remove <player> <id> §e- Remove achievement from player");
                sender.sendMessage("§6/ae reset <player> <id> §e- Reset progress for player");
                sender.sendMessage("§6/ae transfer <from> <to> §e- Transfer player's achievements to other player");
                break;
            default:
                sender.sendMessage("§cNot found page §e" + page + "§c..");
                break;
        }
        sender.sendMessage("§2<----------> §6Showing page " + page + " / 2 §2<---------->");
    }

    private void c_Reload(CommandSender sender) {
        PermissionManager permissionManager = AchievementsEngine.getInstance().getPermissionManager();
        if(!permissionManager.checkIfSenderHasPermission(sender, permissionManager.getPermission("ae.reload"))) return;
        AchievementsEngine.getInstance().getDataHandler().LoadConfig();
        sender.sendMessage(AchievementsEngine.getInstance().getMessages().getMessage("prefix") + "§aReloaded!");
    }

    private void c_Help(CommandSender sender, String[] args) {
        PermissionManager permissionManager = AchievementsEngine.getInstance().getPermissionManager();
        if(!permissionManager.checkIfSenderHasPermission(sender, permissionManager.getPermission("ae.manage"))) return;
        if(args.length < 2) {
            ShowHelp(sender, 1);
            return;
        }
        int page = 1;
        try {
            page = Integer.parseInt(args[1]);
        } catch(NumberFormatException e) {
            sender.sendMessage(AchievementsEngine.getInstance().getMessages().getMessage("prefix") + "§cCorrect usage: /ae help <page:int>");
        } finally {
            ShowHelp(sender, page);
        }
    }

    private void c_Achievements(CommandSender sender) {
        PermissionManager permissionManager = AchievementsEngine.getInstance().getPermissionManager();
        if(!permissionManager.checkIfSenderHasPermission(sender, permissionManager.getPermission("ae.achievements"))) return;
        sender.sendMessage("§2<----------> §6AchievementsEngine §2<---------->");
        for (Achievement a : AchievementsEngine.getInstance().getAchievementManager().getAchievements()) {
            String events = "";
            if(a.getEvents().size() > 0) {
                events = a.getEvents().get(0);
                int i = 0;
                for (String s : a.getEvents()) {
                    i++;
                    if(i == 1) continue;
                    events = events + ", " + s;
                }
            }
            sender.sendMessage("§e" + a.getID() + "§2§l#§r" + a.getName() + ": " + a.getDescription());
            sender.sendMessage("   §e--> " + events);
        }
        sender.sendMessage("§2<----------> §6AchievementsEngine §2<---------->");
    }

    private void c_Complete(CommandSender sender, String[] args) {
        PermissionManager permissionManager = AchievementsEngine.getInstance().getPermissionManager();
        if(!permissionManager.checkIfSenderHasPermission(sender, permissionManager.getPermission("ae.complete"))) return;
        if(args.length != 3) {
            sender.sendMessage(AchievementsEngine.getInstance().getMessages().getMessage("prefix")
                    + "§cCorrect usage: /ae complete <player:Player> <id:String>");
            return;
        }
        PlayerUtil pu = AchievementsEngine.getInstance().getPlayerUtil();
        Player p = pu.checkIfPlayerExists(args[1]);
        if(p == null) {
            sender.sendMessage(AchievementsEngine.getInstance().getMessages().getMessage("prefix")
                    + "§cCan't found player " + args[1] + "§c, maybe it's offline?");
            return;
        }
        PlayerAchievementState state = PlayerAchievementState.Create(p);
        if (!checkStateInitialized(sender, state)) return;
        if(!args[2].equalsIgnoreCase("*")) {
            Achievement a = AchievementsEngine.getInstance().getAchievementManager().checkIfAchievementExists(args[2]);
            if (a == null) {
                sender.sendMessage(AchievementsEngine.getInstance().getMessages().getMessage("prefix")
                        + "§cCan't found achievement " + args[2] + "§c!");
                return;
            }
            if (!state.getCompletedAchievements().contains(a)) {
                a.Complete(state);
                sender.sendMessage(AchievementsEngine.getInstance().getMessages().getMessage("prefix")
                        + "§aAdded " + args[2] + " §ato " + args[1] + "'s completed achievements!");
            } else {
                sender.sendMessage(AchievementsEngine.getInstance().getMessages().getMessage("prefix")
                        + "§cPlayer " + args[1] + " §calready has achievement " + args[2] + " §ccompleted!");
            }
        } else {
            for(Achievement a : AchievementsEngine.getInstance().getAchievementManager().getAchievements()) {
                if(state.getCompletedAchievements().contains(a)) continue;
                a.Complete(state);
            }
            sender.sendMessage(AchievementsEngine.getInstance().getMessages().getMessage("prefix")
                    + "§aAdded all achievements" + "§a to " + args[1] + "'s completed achievements!");
        }
    }

    private void c_Remove(CommandSender sender, String[] args) {
        PermissionManager permissionManager = AchievementsEngine.getInstance().getPermissionManager();
        if(!permissionManager.checkIfSenderHasPermission(sender, permissionManager.getPermission("ae.remove"))) return;
        if (args.length != 3) {
            sender.sendMessage(AchievementsEngine.getInstance().getMessages().getMessage("prefix")
                    + "§cCorrect usage: /ae remove <player:Player> <id:String>");
            return;
        }
        PlayerUtil pu = AchievementsEngine.getInstance().getPlayerUtil();
        Player p = pu.checkIfPlayerExists(args[1]);
        if (p == null) {
            sender.sendMessage(AchievementsEngine.getInstance().getMessages().getMessage("prefix")
                    + "§cCan't found player " + args[1] + "§c, maybe it's offline?");
            return;
        }
        PlayerAchievementState state = PlayerAchievementState.Create(p);
        if(!checkStateInitialized(sender, state)) return;
        if (!args[2].equalsIgnoreCase("*")) {
            Achievement a = AchievementsEngine.getInstance().getAchievementManager().checkIfAchievementExists(args[2]);
            if (a == null) {
                sender.sendMessage(AchievementsEngine.getInstance().getMessages().getMessage("prefix")
                        + "§cCan't found achievement " + args[2] + "§c!");
                return;
            }
            if (state.getCompletedAchievements().contains(a)) {
                state.RemoveAchievement(a);
                sender.sendMessage(AchievementsEngine.getInstance().getMessages().getMessage("prefix")
                        + "§aRemoved " + args[2] + "§a from " + args[1] + "'s completed achievements!");
            } else {
                sender.sendMessage(AchievementsEngine.getInstance().getMessages().getMessage("prefix")
                        + "§cPlayer " + args[1] + " §cdon't have achievement " + args[2] + " §ccompleted!");
            }
        } else {
            List<Achievement> completed = new ArrayList<>(state.getCompletedAchievements());
            for (Achievement a : completed) {
                state.RemoveAchievement(a);
            }
            sender.sendMessage(AchievementsEngine.getInstance().getMessages().getMessage("prefix") +
                    "§aRemoved all " + args[1] + "'s completed achievements!");
        }
    }

    private void c_Reset(CommandSender sender, String[] args) {
        PermissionManager permissionManager = AchievementsEngine.getInstance().getPermissionManager();
        if(!permissionManager.checkIfSenderHasPermission(sender, permissionManager.getPermission("ae.reset"))) return;
        if(args.length != 3) {
            sender.sendMessage(AchievementsEngine.getInstance().getMessages().getMessage("prefix")
                    + "§cCorrect usage: /ae reset <player:Player> <id:String>");
            return;
        }
        PlayerUtil pu = AchievementsEngine.getInstance().getPlayerUtil();
        Player p = pu.checkIfPlayerExists(args[1]);
        if(p == null) {
            sender.sendMessage(AchievementsEngine.getInstance().getMessages().getMessage("prefix")
                    + "§cCan't found player " + args[1] + "§c, maybe it's offline?");
            return;
        }
        PlayerAchievementState state = PlayerAchievementState.Create(p);
        if(!checkStateInitialized(sender, state)) return;
        if(!args[2].equalsIgnoreCase("*")) {
            Achievement a = AchievementsEngine.getInstance().getAchievementManager().checkIfAchievementExists(args[2]);
            if (a == null) {
                sender.sendMessage(AchievementsEngine.getInstance().getMessages().getMessage("prefix")
                        + "§cCan't found achievement " + args[2] + "§c!");
                return;
            }
            if(state.getCompletedAchievements().contains(a)) {
                sender.sendMessage(AchievementsEngine.getInstance().getMessages().getMessage("prefix")
                        + "§cThis player already has this achievement completed. Use /ae remove instead.");
                return;
            }
            state.getProgress().put(a, new int[a.getEvents().size()]);
            sender.sendMessage(AchievementsEngine.getInstance().getMessages().getMessage("prefix")
                    + "§aReset " + args[1] + "§a's progress in " + args[2] + "§a!");
            AchievementsEngine.getInstance().getDataHandler().updateProgress(state, a);
        } else {
            for(Achievement a : AchievementsEngine.getInstance().getAchievementManager().getAchievements()) {
                if(state.getCompletedAchievements().contains(a)) continue;
                state.getProgress().put(a, new int[a.getEvents().size()]);
                AchievementsEngine.getInstance().getDataHandler().updateProgress(state, a);
            }
            sender.sendMessage(AchievementsEngine.getInstance().getMessages().getMessage("prefix")
                    + "§aReset all " + args[1] + "§a's progress in not-completed achievements!");
        }
    }

    private void c_Transfer(CommandSender sender, String[] args) {
        PermissionManager permissionManager = AchievementsEngine.getInstance().getPermissionManager();
        if(!permissionManager.checkIfSenderHasPermission(sender, permissionManager.getPermission("ae.transfer"))) return;
        if(args.length != 3 || args[1].equals(args[2])) {
            sender.sendMessage(AchievementsEngine.getInstance().getMessages().getMessage("prefix")
                    + "§cCorrect usage: /ae transfer <from:Player> <to:Player>");
            return;
        }
        PlayerUtil pu = AchievementsEngine.getInstance().getPlayerUtil();
        Player p1 = pu.checkIfPlayerExists(args[1]);
        Player p2 = pu.checkIfPlayerExists(args[2]);
        if(p1 == null || p2 == null) {
            sender.sendMessage(AchievementsEngine.getInstance().getMessages().getMessage("prefix")
                    + "§cCannot found player(s)..");
            return;
        }
        PlayerAchievementState state1 = PlayerAchievementState.Create(p1);
        if(!checkStateInitialized(sender, state1)) return;
        PlayerAchievementState state2 = PlayerAchievementState.Create(p2);
        if(!checkStateInitialized(sender, state2)) return;
        AchievementsEngine.getInstance().getDataHandler().transferAchievements(state1, state2);
        sender.sendMessage(AchievementsEngine.getInstance().getMessages().getMessage("prefix")
                + "§aTransferred all achievements from " + args[1] + "§a to " + args[2] + "§a!");
    }

    private boolean checkStateInitialized(CommandSender sender, PlayerAchievementState state) {
        if(!state.isInitialized()) {
            sender.sendMessage(AchievementsEngine.getInstance().getMessages().getMessage("prefix") +
                    "§cThis (or these) player(s) doesn't have initialized state (data is not downloaded), try again later.");
            return false;
        }
        return true;
    }

}
