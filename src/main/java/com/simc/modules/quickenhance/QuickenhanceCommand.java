package com.simc.modules.quickenhance;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class QuickenhanceCommand implements CommandExecutor, TabCompleter {
    private final QuickenhanceModule module;

    public QuickenhanceCommand(QuickenhanceModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§7用法: §f/si-quickenhance sudo reload");
            return true;
        }

        if (!equalsIgnoreCase(args[0], "sudo")) {
            sender.sendMessage("§c未知子命令。可用: sudo");
            return true;
        }

        if (!sender.isOp() && !sender.hasPermission("simc.quickenhance.sudo")) {
            sender.sendMessage("§c你没有权限执行 sudo 命令。");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§c用法: /si-quickenhance sudo reload");
            return true;
        }

        if (!equalsIgnoreCase(args[1], "reload")) {
            sender.sendMessage("§c未知 sudo 子命令。可用: reload");
            return true;
        }

        module.reload();
        sender.sendMessage("§aQuickenhance 配置已重载。");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterPrefix(List.of("sudo"), args[0]);
        }
        if (args.length == 2 && equalsIgnoreCase(args[0], "sudo")) {
            if (!sender.isOp() && !sender.hasPermission("simc.quickenhance.sudo")) {
                return new ArrayList<>();
            }
            return filterPrefix(List.of("reload"), args[1]);
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
