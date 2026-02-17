package com.simc.modules.task;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TaskCommand implements CommandExecutor, TabCompleter {
    private final TaskModule module;

    public TaskCommand(TaskModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§c该命令只能由玩家打开 GUI。请使用 sudo 子命令。\n");
                return true;
            }
            module.openMainGui((Player) sender, 1);
            return true;
        }

        if (!equalsIgnoreCase(args[0], "sudo")) {
            sender.sendMessage("§c未知子命令。可用: sudo");
            return true;
        }

        if (!sender.isOp() && !sender.hasPermission("simc.task.sudo")) {
            sender.sendMessage("§c你没有权限执行 sudo 命令。");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§c用法: /si-task sudo <reload|reset>");
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
                    sender.sendMessage("§c用法: /si-task sudo reset <taskID>");
                    return true;
                }
                module.resetTaskAllPlayers(args[2]);
                sender.sendMessage(module.msg("reset-success").replace("%task_id%", args[2].toLowerCase()));
                return true;
            default:
                sender.sendMessage("§c未知 sudo 子命令。可用: reload, reset");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterPrefix(List.of("sudo"), args[0]);
        }

        if (args.length == 2 && equalsIgnoreCase(args[0], "sudo")) {
            if (!sender.isOp() && !sender.hasPermission("simc.task.sudo")) {
                return new ArrayList<>();
            }
            return filterPrefix(Arrays.asList("reload", "reset"), args[1]);
        }

        if (args.length == 3 && equalsIgnoreCase(args[0], "sudo") && equalsIgnoreCase(args[1], "reset")) {
            return filterPrefix(module.getTaskIds(), args[2]);
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
}
