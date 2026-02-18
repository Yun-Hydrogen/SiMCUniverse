package com.simc.modules.protection;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProtectionCommand implements CommandExecutor, TabCompleter {
    private final ProtectionModule module;

    public ProtectionCommand(ProtectionModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§7用法: §f/si-protection sudo <reload|set>");
            return true;
        }

        if (!equalsIgnoreCase(args[0], "sudo")) {
            sender.sendMessage("§c未知子命令。可用: sudo");
            return true;
        }

        if (!sender.isOp() && !sender.hasPermission("simc.protection.sudo")) {
            sender.sendMessage("§c你没有权限执行 sudo 命令。");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§c用法: /si-protection sudo <reload|set>");
            return true;
        }

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "reload":
                module.reload();
                sender.sendMessage("§aProtection 配置已重载。");
                return true;
            case "set":
                if (args.length < 4) {
                    sender.sendMessage("§c用法: /si-protection sudo set <spawnprotection|joinprotection> <time:second>");
                    return true;
                }

                Integer seconds = parseInt(args[3]);
                if (seconds == null || seconds < 0) {
                    sender.sendMessage("§c时间必须为非负整数（秒）。");
                    return true;
                }

                if (!module.setProtectionSeconds(args[2], seconds)) {
                    sender.sendMessage("§c未知类型: " + args[2] + "，可用: spawnprotection, joinprotection");
                    return true;
                }

                sender.sendMessage("§a已设置 " + args[2].toLowerCase() + " 为 §e" + seconds + "§a 秒。");
                return true;
            default:
                sender.sendMessage("§c未知 sudo 子命令。可用: reload, set");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterPrefix(List.of("sudo"), args[0]);
        }

        if (args.length == 2 && equalsIgnoreCase(args[0], "sudo")) {
            if (!sender.isOp() && !sender.hasPermission("simc.protection.sudo")) {
                return new ArrayList<>();
            }
            return filterPrefix(Arrays.asList("reload", "set"), args[1]);
        }

        if (args.length == 3 && equalsIgnoreCase(args[0], "sudo") && equalsIgnoreCase(args[1], "set")) {
            return filterPrefix(Arrays.asList("spawnprotection", "joinprotection"), args[2]);
        }

        if (args.length == 4 && equalsIgnoreCase(args[0], "sudo") && equalsIgnoreCase(args[1], "set")) {
            return filterPrefix(Arrays.asList("0", "30", "45", "60", "120"), args[3]);
        }

        return new ArrayList<>();
    }

    private Integer parseInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
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
}
