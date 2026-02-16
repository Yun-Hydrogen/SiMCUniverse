package com.simc.modules.livescore;

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
import java.util.Map;
import java.util.UUID;

public class LiveScoreCommand implements CommandExecutor, TabCompleter {
    private final LiveScoreModule module;

    public LiveScoreCommand(LiveScoreModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || equalsIgnoreCase(args[0], "help")) {
            sendHelp(sender);
            return true;
        }

        if (equalsIgnoreCase(args[0], "show")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                sender.sendMessage("§a你当前的生存分: §e" + module.getScore(player.getUniqueId()));
            }
            sendRanking(sender);
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
            sender.sendMessage("§c用法: /si-livescore sudo <add|set|reset|pause|start|reload>");
            return true;
        }

        String action = args[1].toLowerCase();
        switch (action) {
            case "add":
                if (args.length < 4) {
                    sender.sendMessage("§c用法: /si-livescore sudo add <player> <score>");
                    return true;
                }
                return sudoAdd(sender, args[2], args[3]);
            case "set":
                if (args.length < 4) {
                    sender.sendMessage("§c用法: /si-livescore sudo set <player> <score>");
                    return true;
                }
                return sudoSet(sender, args[2], args[3]);
            case "reset":
                module.resetAllScores();
                sender.sendMessage("§a已重置所有玩家生存分。");
                return true;
            case "pause":
                module.pause();
                sender.sendMessage("§e已暂停所有玩家生存分计算。");
                return true;
            case "start":
                module.start();
                sender.sendMessage("§a已继续所有玩家生存分计算。");
                return true;
            case "reload":
                module.reload();
                sender.sendMessage("§alivescore.yml 已重载。");
                return true;
            default:
                sender.sendMessage("§c未知 sudo 子命令。可用: add, set, reset, pause, start, reload");
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
        sender.sendMessage("§a已为 §e" + target.getName() + " §a增加 §e" + delta + " §a点，当前生存分: §e" + latest);
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
        sender.sendMessage("§a已将 §e" + target.getName() + " §a的生存分设为 §e" + value);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6==== Si LiveScore 帮助 ====");
        sender.sendMessage("§f/si-livescore help §7- 查看命令帮助（含 sudo）");
        sender.sendMessage("§f/si-livescore show §7- 查看自己的生存分并输出全服排行");
        sender.sendMessage("§f/si-livescore sudo add <player> <score> §7- 增减玩家生存分");
        sender.sendMessage("§f/si-livescore sudo set <player> <score> §7- 设置玩家生存分");
        sender.sendMessage("§f/si-livescore sudo reset §7- 重置所有玩家生存分");
        sender.sendMessage("§f/si-livescore sudo pause §7- 暂停生存分计算");
        sender.sendMessage("§f/si-livescore sudo start §7- 继续生存分计算");
        sender.sendMessage("§f/si-livescore sudo reload §7- 重载 livescore.yml");
    }

    private void sendRanking(CommandSender sender) {
        List<Map.Entry<UUID, Integer>> ranking = module.getRanking();
        sender.sendMessage("§6==== 生存分排行 ====");

        if (ranking.isEmpty()) {
            sender.sendMessage("§7暂无数据。");
            return;
        }

        int rank = 1;
        for (Map.Entry<UUID, Integer> entry : ranking) {
            if (rank > 10) {
                break;
            }

            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null || name.isBlank()) {
                name = entry.getKey().toString().substring(0, 8);
            }

            sender.sendMessage("§f" + rank + ". §b" + name + " §7- §e" + entry.getValue());
            rank++;
        }
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
            return filterPrefix(Arrays.asList("add", "set", "reset", "pause", "start", "reload"), args[1]);
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
