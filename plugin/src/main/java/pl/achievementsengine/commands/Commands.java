package pl.achievementsengine.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.achievementsengine.Achievement;
import pl.achievementsengine.AchievementsEngine;
import pl.achievementsengine.GUIHandler;
import pl.achievementsengine.PlayerAchievementState;
import pl.achievementsengine.data.DataHandler;

import java.util.ArrayList;
import java.util.List;

public class Commands implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(sender instanceof Player) { // Check if sender is player
            if(args.length == 0) { // If not arguments given - open GUI
                GUIHandler.New((Player) sender, 0);
                return true;
            } else if(!sender.hasPermission("ae.manage")) { // If arguments given but player don't have permission - open GUI
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
        } else if(args[0].equalsIgnoreCase("checkstate")) {
            c_CheckState(sender, args);
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
                sender.sendMessage("§6/ae remove <player> <id> §e- Remove achievement for player");
                sender.sendMessage("§6/ae reset <player> <id> §e- Reset progress for player");
                sender.sendMessage("§6/ae checkstate <player> §e- Check player's state");
                sender.sendMessage("§6/ae transfer <from> <to> §e- Transfer player's achievements to other player");
                break;
            default:
                sender.sendMessage("§cNot found page §e" + page + "§c..");
                break;
        }
        sender.sendMessage("§2<----------> §6Showing page " + page + " / 2 §2<---------->");
    }

    private void c_Reload(CommandSender sender) {
        if(!checkPlayerPermission(sender, "ae.reload")) return;
        AchievementsEngine.main.LoadConfig();
        sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§aReloaded!");
    }

    private void c_Help(CommandSender sender, String[] args) {
        if(!checkPlayerPermission(sender, "ae.manage")) return;
        if(args.length < 2) {
            ShowHelp(sender, 1);
            return;
        }
        int page = 1;
        try {
            page = Integer.valueOf(args[1]);
        } catch(NumberFormatException e) {
            sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§cCorrect usage: /ae help <page:int>");
        } finally {
            ShowHelp(sender, page);
        }
    }

    private void c_Achievements(CommandSender sender) {
        if(!checkPlayerPermission(sender, "ae.achievements")) return;
        sender.sendMessage("§2<----------> §6AchievementsEngine §2<---------->");
        for (Achievement a : AchievementsEngine.achievements) {
            String events = "";
            for (String s : a.events) {
                events = events + s + ", ";
            }
            sender.sendMessage("§e" + a.ID + "§2§l#§r" + a.name + ": " + a.description);
            sender.sendMessage("   §e--> " + events);
        }
        sender.sendMessage("§2<----------> §6AchievementsEngine §2<---------->");
    }

    private void c_CheckState(CommandSender sender, String[] args) {
        if(!checkPlayerPermission(sender, "ae.checkstate")) return;
        if(args.length != 2) {
            sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§cCorrect usage: /ae checkstate <player:Player>");
            return;
        }
        sender.sendMessage("§2<----------> §6AchievementsEngine §2<---------->");
        String state = "§cNOT FOUND";
        String downloaded = "§cNOT INITIALIZED";
        if(AchievementsEngine.playerStates.containsKey(args[1])) {
            state = "§aOK!";
            if(AchievementsEngine.playerStates.get(args[1]).initialized) {
                downloaded = "§aINITIALIZED";
            }
        }
        sender.sendMessage("§ePlayer: §6" + args[1]);
        sender.sendMessage("§eState: " + state);
        sender.sendMessage("§eData downloaded: " + downloaded);
        sender.sendMessage("§2<----------> §6AchievementsEngine §2<---------->");
    }

    private void c_Complete(CommandSender sender, String[] args) {
        if(!checkPlayerPermission(sender, "ae.complete")) return;
        if(args.length != 3) {
            sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§cCorrect usage: /ae complete <player:Player> <id:String>");
            return;
        }
        Player p = checkIfPlayerExists(args[1]);
        if(p == null) {
            sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§cCan't found player " + args[1] + "§c, maybe it's offline?");
            return;
        }
        Achievement a = Achievement.checkIfAchievementExists(args[2]);
        if(a == null) {
            sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§cCan't found achievement " + args[2] + "§c!");
            return;
        }
        PlayerAchievementState state = PlayerAchievementState.Create(p);
        if(!state.initialized) {
            sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§cThis player doesn't have initialized state!");
            return;
        }
        if (!state.completedAchievements.contains(a)) {
            a.Complete(state);
            sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§aAdded " + args[2]
                    + "§a to " + args[1] + "'s completed achievements!");
        } else {
            sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§cPlayer " + args[1] +
                    " §calready has achievement " + args[2] + " §ccompleted!");
        }
    }

    private void c_Remove(CommandSender sender, String[] args) {
        if(!checkPlayerPermission(sender, "ae.remove")) return;
        if (args.length != 3) {
            sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§cCorrect usage: /ae remove <player:Player> <id:String>");
            return;
        }
        Player p = checkIfPlayerExists(args[1]);
        if (p == null) {
            sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§cCan't found player " + args[1] + "§c, maybe it's offline?");
            return;
        }
        PlayerAchievementState state = PlayerAchievementState.Create(p);
        if(!state.initialized) {
            sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§cThis player doesn't have initialized state!");
            return;
        }
        if (!args[2].equalsIgnoreCase("*")) {
            Achievement a = Achievement.checkIfAchievementExists(args[2]);
            if (a == null) {
                sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§cCan't found achievement " + args[2] + "§c!");
                return;
            }
            if (state.completedAchievements.contains(a)) {
                state.RemoveAchievement(a);
                sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§aRemoved " + args[2] +
                        "§a from " + args[1] + "'s completed achievements!");
            } else {
                sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§cPlayer " + args[1] +
                        " §cdon't have achievement " + args[2] + " §ccompleted!");
            }
        } else {
            List<Achievement> completed = new ArrayList<>(state.completedAchievements);
            for (Achievement a : completed) {
                state.RemoveAchievement(a);
            }
            sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§aRemoved all " + args[1] + "'s completed achievements!");
        }
    }

    private void c_Reset(CommandSender sender, String[] args) {
        if(!checkPlayerPermission(sender, "ae.reset")) return;
        if(args.length != 3) {
            sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§cCorrect usage: /ae reset <player:Player> <id:String>");
            return;
        }
        if(checkIfPlayerExists(args[1]) == null) {
            sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§cCan't found player " + args[1] + "§c, maybe it's offline?");
            return;
        }
        Achievement a = Achievement.checkIfAchievementExists(args[2]);
        if(a == null) {
            sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§cCan't found achievement " + args[2] + "§c!");
            return;
        }
        PlayerAchievementState state = AchievementsEngine.playerStates.get(args[1]);
        if(!state.initialized) {
            sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§cThis player doesn't have initialized state!");
            return;
        }
        if(state.completedAchievements.contains(a)) {
            sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§cThis player already has this achievement completed. Use /ae remove instead.");
            return;
        }
        state.progress.put(a, new int[a.events.size()]);
        sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§aReset all " + args[1] + "§a's progress!");
        DataHandler.updateProgress(state, a);
    }

    private void c_Transfer(CommandSender sender, String[] args) {
        if(!checkPlayerPermission(sender, "ae.transfer")) return;
        if(args.length != 3 || args[1].equals(args[2])) {
            sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§cCorrect usage: /ae transfer <from:Player> <to:Player>");
            return;
        }
        Player p1 = checkIfPlayerExists(args[1]);
        Player p2 = checkIfPlayerExists(args[2]);
        if(p1 == null || p2 == null) {
            sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§cCannot found player(s)..");
            return;
        }
        PlayerAchievementState state1 = PlayerAchievementState.Create(p1);
        PlayerAchievementState state2 = PlayerAchievementState.Create(p2);
        if(!state1.initialized || !state2.initialized) {
            sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§cThis player/these players don't have initialized state!");
            return;
        }
        DataHandler.transferAchievements(state1, state2);
        sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§aTransferred all achievements from "
                + args[1] + "§a to " + args[2] + "§a!");
    }

    public static Player checkIfPlayerExists(String nickname) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().equals(nickname)) {
                return p;
            }
        }
        return null;
    }

    public static boolean checkPlayerPermission(CommandSender sender, String permission) {
        if(!(sender instanceof Player)) return true;
        if(!sender.hasPermission(permission)) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return false;
        }
        return true;
    }
}
