package com.simc.commands;

import com.simc.SiMCUniverse;
import com.simc.managers.PluginManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SimcCommand implements CommandExecutor, TabCompleter {
    private final SiMCUniverse plugin;

    public SimcCommand(SiMCUniverse plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || equalsIgnoreCase(args[0], "sudo") && args.length == 1) {
            sender.sendMessage("§7用法: §f/simcuniverse sudo help");
            return true;
        }

        if (!equalsIgnoreCase(args[0], "sudo")) {
            sender.sendMessage("§c未知子命令，请使用 /simcuniverse sudo help");
            return true;
        }

        if (!sender.isOp()) {
            sender.sendMessage("§c你没有权限执行该命令。");
            return true;
        }

        PluginManager manager = plugin.getPluginManagerInstance();
        String sub = args.length >= 2 ? args[1].toLowerCase() : "help";

        switch (sub) {
            case "help":
                sendHelp(sender);
                return true;
            case "list":
                sender.sendMessage("§6==== SiMCUniverse 模块状态 ====");
                for (Map.Entry<String, Boolean> entry : manager.listModuleStatus().entrySet()) {
                    sender.sendMessage("§f- §b" + entry.getKey() + " §7: " + (entry.getValue() ? "§aenabled" : "§cdisabled"));
                }
                return true;
            case "disable":
                if (args.length < 3) {
                    sender.sendMessage("§c用法: /simcuniverse sudo disable <module>");
                    return true;
                }
                if (manager.disableModule(args[2])) {
                    sender.sendMessage("§e模块已禁用: §b" + args[2].toLowerCase());
                } else {
                    sender.sendMessage("§c未知模块: " + args[2]);
                }
                return true;
            case "enable":
                if (args.length < 3) {
                    sender.sendMessage("§c用法: /simcuniverse sudo enable <module>");
                    return true;
                }
                if (manager.enableModule(args[2])) {
                    sender.sendMessage("§a模块已启用: §b" + args[2].toLowerCase());
                } else {
                    sender.sendMessage("§c未知模块: " + args[2]);
                }
                return true;
            case "reload":
                if (args.length < 3) {
                    sender.sendMessage("§c用法: /simcuniverse sudo reload <module|plugin-self>");
                    return true;
                }
                if (equalsIgnoreCase(args[2], "plugin-self")) {
                    manager.reloadPluginSelf();
                    sender.sendMessage("§a插件已重载。");
                    return true;
                }
                if (manager.reloadModule(args[2])) {
                    sender.sendMessage("§a模块已重载: §b" + args[2].toLowerCase());
                } else {
                    sender.sendMessage("§c未知模块: " + args[2]);
                }
                return true;
            case "version":
                sender.sendMessage("§aSiMCUniverse 版本: §b" + plugin.getDescription().getVersion());
                return true;
            default:
                sender.sendMessage("§c未知 sudo 子命令，请使用 /simcuniverse sudo help");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterPrefix(List.of("sudo"), args[0]);
        }

        if (!sender.isOp()) {
            return new ArrayList<>();
        }

        if (args.length == 2 && equalsIgnoreCase(args[0], "sudo")) {
            return filterPrefix(Arrays.asList("help", "list", "disable", "enable", "reload", "version"), args[1]);
        }

        if (args.length == 3 && equalsIgnoreCase(args[0], "sudo")) {
            List<String> modules = new ArrayList<>(plugin.getPluginManagerInstance().listModuleStatus().keySet());
            if (equalsIgnoreCase(args[1], "disable") || equalsIgnoreCase(args[1], "enable")) {
                return filterPrefix(modules, args[2]);
            }
            if (equalsIgnoreCase(args[1], "reload")) {
                modules.add("plugin-self");
                return filterPrefix(modules, args[2]);
            }
        }

        return new ArrayList<>();
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6==== SiMCUniverse Sudo 帮助 ====");
        sender.sendMessage("§f/simcuniverse sudo help §7- 显示命令帮助");
        sender.sendMessage("§f/simcuniverse sudo list §7- 显示模块与状态");
        sender.sendMessage("§f/simcuniverse sudo disable <module> §7- 禁用模块");
        sender.sendMessage("§f/simcuniverse sudo enable <module> §7- 启用模块");
        sender.sendMessage("§f/simcuniverse sudo reload <module> §7- 重载模块");
        sender.sendMessage("§f/simcuniverse sudo reload plugin-self §7- 重载插件");
        sender.sendMessage("§f/simcuniverse sudo version §7- 显示插件版本");
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
