package pl.achievementsengine.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import pl.achievementsengine.achievements.Achievement;
import pl.achievementsengine.AchievementsEngine;
import pl.achievementsengine.achievements.PlayerAchievementState;

import java.util.ArrayList;
import java.util.List;

public class CommandHelper implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player) || !sender.hasPermission("ae.manage")) {
            return null;
        }
        List<String> completions = new ArrayList<>();
        if(args.length == 1) {
            completions.add("help");
            completions.add("reload");
            completions.add("achievements");
            completions.add("complete");
            completions.add("remove");
            completions.add("reset");
            completions.add("checkstate");
            completions.add("transfer");
        } else if(args.length == 2) {
            if(args[0].equalsIgnoreCase("complete") || args[0].equalsIgnoreCase("remove") ||
                    args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("transfer") ||
                    args[0].equalsIgnoreCase("checkstate")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
            } else if(args[0].equalsIgnoreCase("help")) {
                completions.add("1");
                completions.add("2");
            }
        } else if(args.length == 3) {
            if(args[0].equalsIgnoreCase("complete") || args[0].equalsIgnoreCase("reset")) {
                for (Achievement a : AchievementsEngine.achievements) {
                    completions.add(a.ID);
                }
            } else if(args[0].equalsIgnoreCase("remove")) {
                Player argumentPlayer = Commands.checkIfPlayerExists(args[1]);
                if(argumentPlayer != null) {
                    PlayerAchievementState state = PlayerAchievementState.Create(argumentPlayer);
                    for(Achievement a : state.completedAchievements) {
                        completions.add(a.ID);
                    }
                    completions.add("*");
                }
            } else if(args[0].equalsIgnoreCase("transfer")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
            }
        }
        return StringUtil.copyPartialMatches(args[args.length-1], completions, new ArrayList<>());
    }

}
