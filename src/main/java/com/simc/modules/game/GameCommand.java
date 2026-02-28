package com.simc.modules.game;

import com.simc.utils.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GameCommand implements CommandExecutor, TabCompleter {
    private final GameModule module;

    public GameCommand(GameModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!module.isEnabled()) {
            sender.sendMessage("§cGame 模块当前已禁用。");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§c用法: /si-game sudo <...> 或 /si-game <functionID>");
            return true;
        }

        if (equalsIgnoreCase(args[0], "sudo")) {
            if (!sender.isOp()) {
                sender.sendMessage("§c你没有权限执行 sudo 命令。");
                return true;
            }
            return handleSudo(sender, args);
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("§c该命令只能由玩家执行。请使用 sudo 子命令。 ");
            return true;
        }

        module.handlePlayerFunctionCommand((Player) sender, args[0]);
        return true;
    }

    private boolean handleSudo(CommandSender sender, String[] args) {
        if (args.length >= 2 && equalsIgnoreCase(args[1], "reload")) {
            module.reload();
            sender.sendMessage("§aGame 配置已重载。");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§c用法: /si-game sudo <functionID> <list|start|stop|restart|make-victory|make-defeat> [gameid]");
            return true;
        }

        String functionId = args[1];
        String action = args[2].toLowerCase();

        if (equalsIgnoreCase(action, "list")) {
            List<String> items = module.listGames(functionId);
            if (items.isEmpty()) {
                sender.sendMessage("§7该玩法下没有有效游戏，或玩法不存在。 ");
                return true;
            }
            sender.sendMessage("§6[" + functionId.toLowerCase() + "] 可用游戏列表:");
            for (String row : items) {
                String[] pair = row.split("\\|", 2);
                String gameId = pair.length > 0 ? pair[0] : "unknown";
                String gameName = pair.length > 1 ? pair[1] : "unknown";
                sender.sendMessage("§7- §f" + gameId + " §8| §e" + gameName);
            }
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage("§c缺少 gameid。用法: /si-game sudo " + functionId + " " + action + " <gameid>");
            return true;
        }

        String gameId = args[3];
        GameModule.CommandFeedback feedback = text -> sender.sendMessage(Utils.colorize(text));

        switch (action) {
            case "start":
                return module.startGame(functionId, gameId, feedback);
            case "stop":
                return module.stopGame(functionId, gameId, feedback);
            case "restart":
                return module.restartGame(functionId, gameId, feedback);
            case "make-victory":
                return module.forceVictory(functionId, gameId, feedback);
            case "make-defeat":
                return module.forceDefeat(functionId, gameId, feedback);
            default:
                sender.sendMessage("§c未知 sudo 子命令。可用: reload, list, start, stop, restart, make-victory, make-defeat");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> first = new ArrayList<>(module.getFunctionIds());
            first.add("sudo");
            return filterPrefix(first, args[0]);
        }

        if (!equalsIgnoreCase(args[0], "sudo")) {
            return new ArrayList<>();
        }

        if (!sender.isOp()) {
            return new ArrayList<>();
        }

        if (args.length == 2) {
            List<String> second = new ArrayList<>(module.getFunctionIds());
            second.add("reload");
            return filterPrefix(second, args[1]);
        }

        if (args.length == 3) {
            if (equalsIgnoreCase(args[1], "reload")) {
                return new ArrayList<>();
            }
            return filterPrefix(Arrays.asList("list", "start", "stop", "restart", "make-victory", "make-defeat"), args[2]);
        }

        if (args.length == 4) {
            if (!module.isSupportedFunction(args[1])) {
                return new ArrayList<>();
            }
            if (equalsIgnoreCase(args[2], "start") || equalsIgnoreCase(args[2], "restart") || equalsIgnoreCase(args[2], "list")) {
                return filterPrefix(module.getBasedefendGameIds(), args[3]);
            }
            if (equalsIgnoreCase(args[2], "stop") || equalsIgnoreCase(args[2], "make-victory") || equalsIgnoreCase(args[2], "make-defeat")) {
                return filterPrefix(module.getRunningBasedefendGameIds(), args[3]);
            }
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
