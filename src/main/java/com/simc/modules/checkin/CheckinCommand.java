package com.simc.modules.checkin;

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
import java.util.UUID;

public class CheckinCommand implements CommandExecutor, TabCompleter {
    private final CheckinModule module;

    public CheckinCommand(CheckinModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§c该命令只能由玩家打开 GUI。请使用 sudo 子命令。");
                return true;
            }
            module.openMainGui((Player) sender, 1);
            return true;
        }

        if (!equalsIgnoreCase(args[0], "sudo")) {
            sender.sendMessage("§c未知子命令。可用: sudo");
            return true;
        }

        if (!sender.isOp()) {
            sender.sendMessage("§c你没有权限执行 sudo 命令。");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§c用法: /si-checkin sudo <reload|reset|set|lock|unlock>");
            return true;
        }

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "reload":
                module.reload();
                sender.sendMessage(module.msg("reload-success"));
                return true;
            case "reset":
                if (args.length < 3) {
                    sender.sendMessage("§c用法: /si-checkin sudo reset <签到内部id>");
                    return true;
                }
                module.resetTaskAllPlayers(args[2]);
                sender.sendMessage(module.msg("reset-success").replace("%task_id%", args[2].toLowerCase()));
                return true;
            case "set":
                if (args.length < 5) {
                    sender.sendMessage("§c用法: /si-checkin sudo set <玩家> <签到内部id> <进度>");
                    return true;
                }
                return handleSet(sender, args[2], args[3], args[4]);
            case "lock":
                if (args.length < 3) {
                    sender.sendMessage("§c用法: /si-checkin sudo lock <签到内部id>");
                    return true;
                }
                if (module.setTaskLock(args[2], CheckinModule.LockOverride.LOCKED)) {
                    sender.sendMessage(module.msg("lock-success").replace("%task_id%", args[2].toLowerCase()));
                } else {
                    sender.sendMessage("§c未找到签到任务: " + args[2]);
                }
                return true;
            case "unlock":
                if (args.length < 3) {
                    sender.sendMessage("§c用法: /si-checkin sudo unlock <签到内部id>");
                    return true;
                }
                if (module.setTaskLock(args[2], CheckinModule.LockOverride.UNLOCKED)) {
                    sender.sendMessage(module.msg("unlock-success").replace("%task_id%", args[2].toLowerCase()));
                } else {
                    sender.sendMessage("§c未找到签到任务: " + args[2]);
                }
                return true;
            default:
                sender.sendMessage("§c未知 sudo 子命令。可用: reload, reset, set, lock, unlock");
                return true;
        }
    }

    private boolean handleSet(CommandSender sender, String playerName, String taskId, String progressText) {
        Integer progress = parseInt(progressText);
        if (progress == null || progress < 0) {
            sender.sendMessage("§c进度必须为非负整数。");
            return true;
        }

        OfflinePlayer target = findPlayer(playerName);
        if (target == null) {
            sender.sendMessage("§c未找到玩家: " + playerName);
            return true;
        }

        UUID uuid = target.getUniqueId();
        if (!module.setPlayerTaskProgress(uuid, taskId, progress)) {
            sender.sendMessage("§c未找到签到任务: " + taskId);
            return true;
        }

        String name = target.getName() == null ? playerName : target.getName();
        sender.sendMessage(module.msg("set-success")
                .replace("%player%", name)
                .replace("%task_id%", taskId.toLowerCase())
                .replace("%progress%", String.valueOf(progress)));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterPrefix(List.of("sudo"), args[0]);
        }

        if (args.length == 2 && equalsIgnoreCase(args[0], "sudo")) {
            if (!sender.isOp()) {
                return new ArrayList<>();
            }
            return filterPrefix(Arrays.asList("reload", "reset", "set", "lock", "unlock"), args[1]);
        }

        if (args.length == 3 && equalsIgnoreCase(args[0], "sudo")) {
            if (equalsIgnoreCase(args[1], "set")) {
                List<String> players = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    players.add(p.getName());
                }
                return filterPrefix(players, args[2]);
            }
            if (equalsIgnoreCase(args[1], "reset") || equalsIgnoreCase(args[1], "lock") || equalsIgnoreCase(args[1], "unlock")) {
                return filterPrefix(module.getTaskIds(), args[2]);
            }
        }

        if (args.length == 4 && equalsIgnoreCase(args[0], "sudo") && equalsIgnoreCase(args[1], "set")) {
            return filterPrefix(module.getTaskIds(), args[3]);
        }

        if (args.length == 5 && equalsIgnoreCase(args[0], "sudo") && equalsIgnoreCase(args[1], "set")) {
            return filterPrefix(Arrays.asList("0", "1", "7", "30"), args[4]);
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

    private Integer parseInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean equalsIgnoreCase(String a, String b) {
        return a != null && a.equalsIgnoreCase(b);
    }

    private OfflinePlayer findPlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }

        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            String offlineName = offline.getName();
            if (offlineName != null && offlineName.equalsIgnoreCase(name)
                    && (offline.hasPlayedBefore() || offline.isOnline())) {
                return offline;
            }
        }

        return null;
    }
}
