package com.simc.modules.shop;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopCommand implements CommandExecutor, TabCompleter {
    private final ShopModule module;
    private final Map<String, Long> resetConfirmMap = new HashMap<>();

    public ShopCommand(ShopModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§c该命令只能由玩家执行。可使用 /" + label + " sudo ...");
                return true;
            }
            module.openShop((Player) sender, 1);
            return true;
        }

        if (!equalsIgnoreCase(args[0], "sudo")) {
            sender.sendMessage("§c未知子命令。可用: sudo");
            return true;
        }

        if (!sender.isOp()) {
            sender.sendMessage("§c你没有权限执行 sudo 子命令。");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§c用法: /si-shop sudo <reload|resetshop>");
            return true;
        }

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "reload":
                module.reload();
                sender.sendMessage(module.getMessage("reload-success"));
                return true;
            case "resetshop":
                return handleResetShop(sender);
            default:
                sender.sendMessage("§c未知 sudo 子命令。可用: reload, resetshop");
                return true;
        }
    }

    private boolean handleResetShop(CommandSender sender) {
        String key = sender.getName().toLowerCase();
        long now = System.currentTimeMillis();
        long last = resetConfirmMap.getOrDefault(key, 0L);

        if (now - last > 15000L) {
            resetConfirmMap.put(key, now);
            sender.sendMessage(module.getMessage("reset-confirm"));
            return true;
        }

        resetConfirmMap.remove(key);
        module.resetShopPages();
        module.reload();
        sender.sendMessage(module.getMessage("reset-success"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterPrefix(Arrays.asList("sudo"), args[0]);
        }

        if (args.length == 2 && equalsIgnoreCase(args[0], "sudo")) {
            if (!sender.isOp()) {
                return new ArrayList<>();
            }
            return filterPrefix(Arrays.asList("reload", "resetshop"), args[1]);
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
