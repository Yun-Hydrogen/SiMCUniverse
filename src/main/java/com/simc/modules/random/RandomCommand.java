package com.simc.modules.random;

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

public class RandomCommand implements CommandExecutor, TabCompleter {
    private final RandomModule module;

    public RandomCommand(RandomModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§c该命令只能由玩家打开 GUI。请使用 sudo 子命令。");
                return true;
            }
            Player player = (Player) sender;
            module.openMainGui(player);
            module.playOpenGuiSound(player);
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
            sender.sendMessage("§c用法: /si-random sudo <reload|reset|set>");
            return true;
        }

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "reload":
                module.reload();
                sender.sendMessage(module.getMessage("reload-success"));
                return true;
            case "reset":
                module.resetAllPity();
                sender.sendMessage(module.getMessage("reset-success"));
                return true;
            case "set":
                if (args.length < 4) {
                    sender.sendMessage("§c用法: /si-random sudo set <玩家> <点数>");
                    return true;
                }
                return handleSet(sender, args[2], args[3]);
            default:
                sender.sendMessage("§c未知 sudo 子命令。可用: reload, reset, set");
                return true;
        }
    }

    private boolean handleSet(CommandSender sender, String name, String pointText) {
        Integer point = parseInt(pointText);
        if (point == null || point < 0) {
            sender.sendMessage("§c点数必须是非负整数。");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(name);
        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            sender.sendMessage("§c未找到玩家: " + name);
            return true;
        }

        module.setPityPoint(target.getUniqueId(), point);
        sender.sendMessage(module.formatSetMessage(target.getName() == null ? name : target.getName(), point));
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
            return filterPrefix(Arrays.asList("reload", "reset", "set"), args[1]);
        }

        if (args.length == 3 && equalsIgnoreCase(args[0], "sudo") && equalsIgnoreCase(args[1], "set")) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return filterPrefix(names, args[2]);
        }

        if (args.length == 4 && equalsIgnoreCase(args[0], "sudo") && equalsIgnoreCase(args[1], "set")) {
            return filterPrefix(Arrays.asList("0", "10", "50", "100"), args[3]);
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
}
