package pl.achievementsengine;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Commands implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(sender instanceof Player) {
            if(args.length == 0) {
                GUIHandler.New((Player) sender, 0);
                return true;
            } else if(!sender.hasPermission("ae.manage")) {
                GUIHandler.New((Player) sender, 0);
                return true;
            }
        }
        if(args.length == 0) {
            ShowHelp(sender, 1);
        } else {
            if(args[0].equalsIgnoreCase("reload")) {
                AchievementsEngine.main.LoadConfig();
                sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§aReloaded!");
            } else if(args[0].equalsIgnoreCase("help")) {
                if(args.length >= 2) {
                    int page = 1;
                    try {
                        page = Integer.valueOf(args[1]);
                    } catch(NumberFormatException e) {
                        sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§cCorrect usage: /ae help <page:int>");
                    } finally {
                        ShowHelp(sender, page);
                    }
                } else {
                    ShowHelp(sender, 1);
                }
            } else if(args[0].equalsIgnoreCase("achievements")) {
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
            } else if(args[0].equalsIgnoreCase("connection")) {
                sender.sendMessage("§2<----------> §6AchievementsEngine §2<---------->");
                String connectionStatus = "§cNot connected";
                if(SQLHandler.isConnected()) {
                    connectionStatus = "§aConnected!";
                }
                sender.sendMessage("§eConnection status: " + connectionStatus);
                sender.sendMessage("§eLast exception: §c" + SQLHandler.lastException);
                sender.sendMessage("§2<----------> §6AchievementsEngine §2<---------->");
            } else if(args[0].equalsIgnoreCase("checkstate")) {
                if(args.length > 1) {
                    sender.sendMessage("§2<----------> §6AchievementsEngine §2<---------->");
                    String state = "§cNOT FOUND";
                    String downloaded = "§cNOT INITIALIZED";
                    if(AchievementsEngine.playerAchievements.containsKey(args[1])) {
                        state = "§aOK!";
                        if(AchievementsEngine.playerAchievements.get(args[1]).initialized) {
                            downloaded = "§aINITIALIZED";
                        }
                    }
                    sender.sendMessage("§ePlayer: §6" + args[1]);
                    sender.sendMessage("§eState: " + state);
                    sender.sendMessage("§eData downloaded: " + downloaded);
                    sender.sendMessage("§2<----------> §6AchievementsEngine §2<---------->");
                } else {
                    sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§cCorrect usage: /ae checkstate <player:Player>");
                }
            } else if(args[0].equalsIgnoreCase("complete")) {
                if(args.length == 3) {
                    boolean found1 = false;
                    boolean found2 = false;
                    for(Player p : Bukkit.getOnlinePlayers()) {
                        if(p.getName().equals(args[1])) {
                            for(Achievement a : AchievementsEngine.achievements) {
                                if(a.ID.equals(args[2])) {
                                    if (!AchievementsEngine.playerAchievements.get(args[1]).completedAchievements.contains(a)) {
                                        a.Complete(AchievementsEngine.playerAchievements.get(args[1]));
                                        sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§aAdded " + args[2]
                                                + "§a to " + args[1] + "'s completed achievements!");
                                    } else {
                                        sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§cPlayer " + args[1] +
                                                " §calready has achievement " + args[2] + " §ccompleted!");
                                    }
                                    found2 = true;
                                    break;
                                }
                            }
                            if(!found2) {
                                sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§cCan't found achievement " + args[2] + "§c!");
                            }
                            found1 = true;
                            break;
                        }
                    }
                    if(!found1) {
                        sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§cCan't found player " + args[1] + "§c, maybe it's offline?");
                    }
                } else {
                    sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§cCorrect usage: /ae complete <player:Player> <id:String>");
                }
            } else if(args[0].equalsIgnoreCase("remove")) {
                if (args.length == 3) {
                    boolean found1 = false;
                    boolean found2 = false;
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.getName().equals(args[1])) {
                            for (Achievement a : AchievementsEngine.achievements) {
                                if (a.ID.equals(args[2])) {
                                    if (AchievementsEngine.playerAchievements.get(args[1]).completedAchievements.contains(a)) {
                                        AchievementsEngine.playerAchievements.get(args[1]).RemoveAchievement(a);
                                        sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§aRemoved " + args[2]
                                                + "§a from " + args[1] + "'s completed achievements!");
                                    } else {
                                        sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§cPlayer " + args[1] +
                                                " §cdon't have achievement " + args[2] + " §ccompleted!");
                                    }
                                    found2 = true;
                                    break;
                                }
                            }
                            if (!found2) {
                                sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§cCan't found achievement " + args[2] + "§c!");
                            }
                            found1 = true;
                            break;
                        }
                    }
                    if (!found1) {
                        sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§cCan't found player " + args[1] + "§c, maybe it's offline?");
                    }
                } else {
                    sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§cCorrect usage: /ae remove <player:Player> <id:String>");
                }
            } else if(args[0].equalsIgnoreCase("transfer")) {
                if(args.length == 3 && !args[1].equals(args[2])) {
                    boolean found = false;
                    for(Player p : Bukkit.getOnlinePlayers()) {
                        if(p.getName().equals(args[1])) {
                            for(Player p2 : Bukkit.getOnlinePlayers()) {
                                if(p2.getName().equals(args[2])) {
                                    found = true;
                                    SQLHandler.transferAchievements(AchievementsEngine.playerAchievements.get(args[1]), AchievementsEngine.playerAchievements.get(args[2]));
                                    sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§aTransferred all achievements from " + args[1] + "§a to " + args[2] + "§a!");
                                    break;
                                }
                            }
                            if(found) break;
                        }
                    }
                    if(!found) {
                        sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§cCannot found player(s)..");
                    }
                } else {
                    sender.sendMessage(AchievementsEngine.ReadLanguage("prefix") + "§cCorrect usage: /ae transfer <from:Player> <to:Player>");
                }
            } else {
                ShowHelp(sender, 1);
            }
        }
        return true;
    }

    private static void ShowHelp(CommandSender sender, int page) {
        sender.sendMessage("§2<----------> §6AchievementsEngine §2<---------->");
        switch(page) {
            case 1:
                sender.sendMessage("§6/ae §e- Open achievements GUI (only for players)");
                sender.sendMessage("§6/ae help <page> §e- Shows help page");
                sender.sendMessage("§6/ae reload §e- Reloads configuration");
                sender.sendMessage("§6/ae achievements §e- Shows the list of achievements");
                sender.sendMessage("§6/ae connection §e- Shows the SQL connection status");
                break;
            case 2:
                sender.sendMessage("§6/ae complete <player> <id> §e- Complete achievement for player");
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
}
