package com.simc.modules.killscore;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KillScoreCommand implements CommandExecutor, TabCompleter {
    private final KillScoreModule module;

    public KillScoreCommand(KillScoreModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || equalsIgnoreCase(args[0], "help")) {
            sendHelp(sender);
            return true;
        }

        if (equalsIgnoreCase(args[0], "show")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§c控制台无法使用该命令，请使用 sudo 子命令。");
                return true;
            }

            Player player = (Player) sender;
            int score = module.getScore(player.getUniqueId());
            sender.sendMessage(module.formatText("§a你当前的 %killscore_name%: §e" + score));
            return true;
        }

        if (equalsIgnoreCase(args[0], "sudo")) {
            if (!sender.isOp()) {
                sender.sendMessage("§c你没有权限使用 sudo 子命令。");
                return true;
            }

            return handleSudo(sender, args);
        }

        sender.sendMessage("§c未知子命令，请使用 /" + label + " help");
        return true;
    }

    private boolean handleSudo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c用法: /si-killscore sudo <add|set|reset|reload>");
            return true;
        }

        String action = args[1].toLowerCase();
        switch (action) {
            case "add":
                if (args.length < 4) {
                    sender.sendMessage("§c用法: /si-killscore sudo add <player> <score>");
                    return true;
                }
                return sudoAdd(sender, args[2], args[3]);
            case "set":
                if (args.length < 4) {
                    sender.sendMessage("§c用法: /si-killscore sudo set <player> <score>");
                    return true;
                }
                return sudoSet(sender, args[2], args[3]);
            case "reset":
                module.resetAllScores();
                sender.sendMessage(module.formatText("§a已重置所有玩家的 %killscore_name%。"));
                return true;
            case "reload":
                module.reload();
                sender.sendMessage("§akillscore.yml 已重载。");
                return true;
            default:
                sender.sendMessage("§c未知 sudo 子命令。可用: add, set, reset, reload");
                return true;
        }
    }

    private boolean sudoAdd(CommandSender sender, String playerName, String scoreText) {
        OfflinePlayer target = findPlayer(playerName);
        if (target == null) {
            sender.sendMessage("§c未找到玩家: " + playerName);
            return true;
        }

        Integer delta = parseInt(scoreText);
        if (delta == null) {
            sender.sendMessage("§c分数必须是整数，可为负值。");
            return true;
        }

        module.addScore(target.getUniqueId(), delta);
        int latest = module.getScore(target.getUniqueId());
        sender.sendMessage(module.formatText("§a已为 §e" + target.getName() + " §a增加 §e" + delta + " §a点，当前 %killscore_name%: §e" + latest));
        return true;
    }

    private boolean sudoSet(CommandSender sender, String playerName, String scoreText) {
        OfflinePlayer target = findPlayer(playerName);
        if (target == null) {
            sender.sendMessage("§c未找到玩家: " + playerName);
            return true;
        }

        Integer value = parseInt(scoreText);
        if (value == null) {
            sender.sendMessage("§c分数必须是整数，可为负值。");
            return true;
        }

        module.setScore(target.getUniqueId(), value);
        sender.sendMessage(module.formatText("§a已将 §e" + target.getName() + " §a的 %killscore_name% 设为 §e" + value));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6==== Si KillScore 帮助 ====");
        sender.sendMessage(module.formatText("§f/si-killscore help §7- 查看命令帮助"));
        sender.sendMessage(module.formatText("§f/si-killscore show §7- 查看自己的 %killscore_name%"));
        sender.sendMessage(module.formatText("§f/si-killscore sudo add <player> <score> §7- 增减玩家 %killscore_name%"));
        sender.sendMessage(module.formatText("§f/si-killscore sudo set <player> <score> §7- 设置玩家 %killscore_name%"));
        sender.sendMessage(module.formatText("§f/si-killscore sudo reset §7- 重置所有玩家 %killscore_name%"));
        sender.sendMessage("§f/si-killscore sudo reload §7- 重载 killscore.yml");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterPrefix(Arrays.asList("help", "show", "sudo"), args[0]);
        }

        if (args.length == 2 && equalsIgnoreCase(args[0], "sudo")) {
            if (!sender.isOp()) {
                return new ArrayList<>();
            }
            return filterPrefix(Arrays.asList("add", "set", "reset", "reload"), args[1]);
        }

        if (args.length == 3 && equalsIgnoreCase(args[0], "sudo")
                && (equalsIgnoreCase(args[1], "add") || equalsIgnoreCase(args[1], "set"))) {
            if (!sender.isOp()) {
                return new ArrayList<>();
            }
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                names.add(online.getName());
            }
            return filterPrefix(names, args[2]);
        }

        if (args.length == 4 && equalsIgnoreCase(args[0], "sudo")
                && (equalsIgnoreCase(args[1], "add") || equalsIgnoreCase(args[1], "set"))) {
            if (!sender.isOp()) {
                return new ArrayList<>();
            }
            return filterPrefix(Arrays.asList("1", "10", "-1", "-10"), args[3]);
        }

        return new ArrayList<>();
    }

    private List<String> filterPrefix(List<String> source, String prefix) {
        List<String> result = new ArrayList<>();
        String lower = prefix.toLowerCase();
        for (String item : source) {
            if (item.toLowerCase().startsWith(lower)) {
                result.add(item);
            }
        }
        return result;
    }

    private boolean equalsIgnoreCase(String a, String b) {
        return a != null && a.equalsIgnoreCase(b);
    }

    private Integer parseInt(String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private OfflinePlayer findPlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        if (!offline.hasPlayedBefore() && !offline.isOnline()) {
            return null;
        }
        return offline;
    }
}
