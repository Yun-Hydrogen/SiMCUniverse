package com.simc.commands;

import com.simc.SiMCUniverse;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class SimcCommand implements CommandExecutor, TabCompleter {
    private final SiMCUniverse plugin;

    public SimcCommand(SiMCUniverse plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§aSiMCUniverse §7| §f运行中，版本: §b" + plugin.getDescription().getVersion());
            sender.sendMessage("§7用法: §f/simc reload §7或 §f/simc version");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload":
                plugin.reloadConfig();
                sender.sendMessage("§aSiMCUniverse 配置已重载。");
                return true;
            case "version":
                sender.sendMessage("§aSiMCUniverse 版本: §b" + plugin.getDescription().getVersion());
                return true;
            default:
                sender.sendMessage("§c未知子命令。可用: reload, version");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            if ("reload".startsWith(input)) {
                result.add("reload");
            }
            if ("version".startsWith(input)) {
                result.add("version");
            }
        }
        return result;
    }
}
