package com.simc.modules.game;

import com.simc.SiMCUniverse;
import com.simc.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class GameModule {
    private static final String FUNCTION_BASEDEFEND = "basedefend";
    private static final BarColor[] BAR_COLORS = new BarColor[]{
            BarColor.BLUE,
            BarColor.GREEN,
            BarColor.YELLOW,
            BarColor.PURPLE,
            BarColor.PINK,
            BarColor.RED,
            BarColor.WHITE
    };

    private final SiMCUniverse plugin;
    private final Random random = new Random();

    private boolean enabled;

    private File moduleFolder;
    private File configFile;
    private File basedefendFolder;
    private YamlConfiguration config;

    private final Map<String, String> messages = new HashMap<>();
    private final Map<String, BaseDefendDefinition> basedefendGames = new LinkedHashMap<>();
    private final Map<String, BaseDefendRuntime> runningBaseDefendGames = new HashMap<>();
    private final Map<UUID, Double> lastBaseDefendRanking = new HashMap<>();

    private GameCommand command;
    private GameListener listener;

    public GameModule(SiMCUniverse plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        if (enabled) {
            return;
        }

        loadAll();
        registerCommand();
        registerListener();
        enabled = true;
        plugin.getLogger().info("Game module initialized.");
    }

    public void shutdown() {
        if (!enabled) {
            return;
        }

        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }

        for (String gameId : new ArrayList<>(runningBaseDefendGames.keySet())) {
            stopBaseDefend(gameId, false, "shutdown");
        }

        enabled = false;
        plugin.getLogger().info("Game module shutdown completed.");
    }

    public void reload() {
        loadAll();
    }

    public void enable() {
        initialize();
    }

    public void disable() {
        shutdown();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<String> getFunctionIds() {
        return List.of(FUNCTION_BASEDEFEND);
    }

    public boolean isSupportedFunction(String functionId) {
        return FUNCTION_BASEDEFEND.equals(normalize(functionId));
    }

    public List<String> listGames(String functionId) {
        if (!isSupportedFunction(functionId)) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>();
        for (BaseDefendDefinition def : basedefendGames.values()) {
            list.add(def.id + "|" + def.displayName);
        }
        return list;
    }

    public boolean startGame(String functionId, String gameId, CommandFeedback feedback) {
        if (!isSupportedFunction(functionId)) {
            feedback.send(msg("unknown-function").replace("%function_id%", functionId));
            return false;
        }

        String key = normalize(gameId);
        BaseDefendDefinition definition = basedefendGames.get(key);
        if (definition == null) {
            feedback.send(msg("unknown-game").replace("%game_id%", gameId));
            return false;
        }
        if (runningBaseDefendGames.containsKey(key)) {
            feedback.send(msg("already-running").replace("%game_id%", key));
            return false;
        }

        BaseDefendRuntime runtime = new BaseDefendRuntime(definition);
        runningBaseDefendGames.put(key, runtime);

        runtime.bossBar = Bukkit.createBossBar("", BarColor.RED, BarStyle.SEGMENTED_20);
        runtime.bossBar.setVisible(true);
        refreshBossBar(runtime);
        for (Player player : Bukkit.getOnlinePlayers()) {
            runtime.bossBar.addPlayer(player);
        }

        runtime.timerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            BaseDefendRuntime current = runningBaseDefendGames.get(key);
            if (current == null) {
                return;
            }

            current.remainingSeconds -= 1;
            if (current.remainingSeconds <= 0) {
                finishBaseDefend(key, false, "timeout");
                return;
            }
            refreshBossBar(current);
        }, 20L, 20L);

        if (!definition.runningSoundId.isBlank()) {
            runtime.runningSoundTask = Bukkit.getScheduler().runTaskTimer(plugin,
                () -> playSoundForGame(definition, definition.runningSoundId),
                1L, definition.soundIntervalTicks);
        }

        runtime.spawnTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            BaseDefendRuntime current = runningBaseDefendGames.get(key);
            if (current == null) {
                return;
            }
            spawnWave(current);
        }, 1L, definition.spawnWaveIntervalSeconds * 20L);

        if (definition.boundaryParticleEnabled) {
            runtime.boundaryTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                BaseDefendRuntime current = runningBaseDefendGames.get(key);
                if (current == null) {
                    return;
                }
                spawnBoundaryParticles(current.definition);
            }, 1L, definition.boundaryParticleIntervalTicks);
        }

        feedback.send(msg("start-success")
                .replace("%game_id%", key)
                .replace("%game_name%", definition.displayName));
        return true;
    }

    public boolean stopGame(String functionId, String gameId, CommandFeedback feedback) {
        if (!isSupportedFunction(functionId)) {
            feedback.send(msg("unknown-function").replace("%function_id%", functionId));
            return false;
        }
        String key = normalize(gameId);
        if (!runningBaseDefendGames.containsKey(key)) {
            feedback.send(msg("not-running").replace("%game_id%", key));
            return false;
        }

        stopBaseDefend(key, true, "manual-stop");
        feedback.send(msg("stop-success").replace("%game_id%", key));
        return true;
    }

    public boolean restartGame(String functionId, String gameId, CommandFeedback feedback) {
        if (!isSupportedFunction(functionId)) {
            feedback.send(msg("unknown-function").replace("%function_id%", functionId));
            return false;
        }

        String key = normalize(gameId);
        BaseDefendDefinition def = basedefendGames.get(key);
        if (def == null) {
            feedback.send(msg("unknown-game").replace("%game_id%", gameId));
            return false;
        }

        if (runningBaseDefendGames.containsKey(key)) {
            stopBaseDefend(key, false, "restart");
        }

        return startGame(functionId, key, feedback);
    }

    public boolean forceVictory(String functionId, String gameId, CommandFeedback feedback) {
        if (!isSupportedFunction(functionId)) {
            feedback.send(msg("unknown-function").replace("%function_id%", functionId));
            return false;
        }
        String key = normalize(gameId);
        if (!runningBaseDefendGames.containsKey(key)) {
            feedback.send(msg("not-running").replace("%game_id%", key));
            return false;
        }

        finishBaseDefend(key, true, "manual-victory");
        feedback.send(msg("force-victory-success").replace("%game_id%", key));
        return true;
    }

    public boolean forceDefeat(String functionId, String gameId, CommandFeedback feedback) {
        if (!isSupportedFunction(functionId)) {
            feedback.send(msg("unknown-function").replace("%function_id%", functionId));
            return false;
        }
        String key = normalize(gameId);
        if (!runningBaseDefendGames.containsKey(key)) {
            feedback.send(msg("not-running").replace("%game_id%", key));
            return false;
        }

        finishBaseDefend(key, false, "manual-defeat");
        feedback.send(msg("force-defeat-success").replace("%game_id%", key));
        return true;
    }

    public void onPlayerJoin(Player player) {
        if (!enabled || player == null) {
            return;
        }
        for (BaseDefendRuntime runtime : runningBaseDefendGames.values()) {
            if (runtime.bossBar != null) {
                runtime.bossBar.addPlayer(player);
            }
        }
    }

    public void onPlayerQuit(Player player) {
        if (!enabled || player == null) {
            return;
        }
        for (BaseDefendRuntime runtime : runningBaseDefendGames.values()) {
            if (runtime.bossBar != null) {
                runtime.bossBar.removePlayer(player);
            }
        }
    }

    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!enabled || event == null || runningBaseDefendGames.isEmpty()) {
            return;
        }

        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) {
            return;
        }

        EntityType type = event.getEntityType();
        Location location = event.getLocation();
        if (location == null || location.getWorld() == null) {
            return;
        }

        for (BaseDefendRuntime runtime : runningBaseDefendGames.values()) {
            BaseDefendDefinition def = runtime.definition;
            if (def.world == null || !def.world.equals(location.getWorld().getName())) {
                continue;
            }
            if (!def.countedTypes.contains(type)) {
                continue;
            }
            if (!isInsideSphere(location, def.centerX, def.centerY, def.centerZ, def.radius + 12.0)) {
                continue;
            }

            runtime.countedEntityIds.add(event.getEntity().getUniqueId());
        }
    }

    public void onEntityDamagedByPlayer(Player damager, Entity victim, double finalDamage) {
        if (!enabled || damager == null || victim == null || finalDamage <= 0D || runningBaseDefendGames.isEmpty()) {
            return;
        }

        for (Map.Entry<String, BaseDefendRuntime> entry : runningBaseDefendGames.entrySet()) {
            String gameId = entry.getKey();
            BaseDefendRuntime runtime = entry.getValue();
            BaseDefendDefinition def = runtime.definition;

            if (victim.getType() == null || !def.countedTypes.contains(victim.getType())) {
                continue;
            }
            if (victim.getLocation() == null || victim.getWorld() == null) {
                continue;
            }
            if (!def.world.equals(victim.getWorld().getName())) {
                continue;
            }
            if (!isInsideSphere(victim.getLocation(), def.centerX, def.centerY, def.centerZ, def.radius)) {
                continue;
            }

            runtime.currentDamage = Math.min(def.totalRequiredDamage, runtime.currentDamage + finalDamage);
            runtime.playerDamage.merge(damager.getUniqueId(), finalDamage, Double::sum);
            runtime.countedEntityIds.add(victim.getUniqueId());
            refreshBossBar(runtime);

            if (runtime.currentDamage >= def.totalRequiredDamage) {
                finishBaseDefend(gameId, true, "damage-complete");
            }
        }
    }

    public void handlePlayerFunctionCommand(Player player, String functionId) {
        if (!isSupportedFunction(functionId)) {
            player.sendMessage(msg("unknown-function").replace("%function_id%", functionId));
            return;
        }

        if (lastBaseDefendRanking.isEmpty()) {
            player.sendMessage(msg("no-last-ranking"));
            return;
        }

        player.sendMessage(Utils.colorize(msg("ranking-title")));
        List<Map.Entry<UUID, Double>> list = new ArrayList<>(lastBaseDefendRanking.entrySet());
        list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        int index = 1;
        for (Map.Entry<UUID, Double> row : list) {
            if (index > 10) {
                break;
            }
            String name = Bukkit.getOfflinePlayer(row.getKey()).getName();
            if (name == null || name.isBlank()) {
                name = row.getKey().toString().substring(0, 8);
            }
            String line = msg("ranking-line")
                    .replace("%rank%", String.valueOf(index))
                    .replace("%player%", name)
                    .replace("%damage%", String.format(Locale.US, "%.1f", row.getValue()));
            player.sendMessage(Utils.colorize(line));
            index++;
        }
    }

    public List<String> getBasedefendGameIds() {
        return new ArrayList<>(basedefendGames.keySet());
    }

    public List<String> getRunningBasedefendGameIds() {
        return new ArrayList<>(runningBaseDefendGames.keySet());
    }

    private void spawnWave(BaseDefendRuntime runtime) {
        BaseDefendDefinition def = runtime.definition;
        World world = Bukkit.getWorld(def.world);
        if (world == null || def.spawnEntries.isEmpty()) {
            return;
        }

        int count = Math.max(1, def.spawnCountPerWave);
        for (int i = 0; i < count; i++) {
            SpawnMobEntry entry = def.spawnEntries.get(random.nextInt(def.spawnEntries.size()));
            Location spawnLoc = randomSurfaceLocation(world, def);
            if (spawnLoc == null) {
                continue;
            }
            Entity spawned = spawnConfiguredMob(world, spawnLoc, entry);
            if (spawned != null) {
                runtime.countedEntityIds.add(spawned.getUniqueId());
            }
        }
    }

    private Entity spawnConfiguredMob(World world, Location location, SpawnMobEntry entry) {
        if (entry.nbt == null || entry.nbt.isBlank()) {
            return world.spawnEntity(location, entry.type);
        }

        String dimensionId = world.getKey() == null ? "minecraft:overworld" : world.getKey().toString();
        String cmd = String.format(Locale.US,
                "execute in %s run summon %s %.3f %.3f %.3f %s",
                dimensionId,
                entry.entityId,
                location.getX(),
                location.getY(),
                location.getZ(),
                entry.nbt);

        Boolean oldFeedback = world.getGameRuleValue(GameRule.SEND_COMMAND_FEEDBACK);
        try {
            if (oldFeedback != null) {
                world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, false);
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        } finally {
            if (oldFeedback != null) {
                world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, oldFeedback);
            }
        }

        return null;
    }

    private Location randomSurfaceLocation(World world, BaseDefendDefinition def) {
        if (world == null) {
            return null;
        }

        for (int i = 0; i < 12; i++) {
            double r = random.nextDouble() * def.radius;
            double theta = random.nextDouble() * Math.PI * 2D;
            double x = def.centerX + Math.cos(theta) * r;
            double z = def.centerZ + Math.sin(theta) * r;
            int y = world.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z));
            return new Location(world, x + 0.5, y + 1.0, z + 0.5);
        }

        int y = world.getHighestBlockYAt((int) Math.floor(def.centerX), (int) Math.floor(def.centerZ));
        return new Location(world, def.centerX + 0.5, y + 1.0, def.centerZ + 0.5);
    }

    private boolean isInsideSphere(Location loc, double x, double y, double z, double radius) {
        double dx = loc.getX() - x;
        double dy = loc.getY() - y;
        double dz = loc.getZ() - z;
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    private void refreshBossBar(BaseDefendRuntime runtime) {
        BaseDefendDefinition def = runtime.definition;
        if (runtime.bossBar == null) {
            return;
        }

        double total = Math.max(1D, def.totalRequiredDamage);
        double left = Math.max(0D, total - runtime.currentDamage);
        double perBar = total / Math.max(1, def.hpBarCount);
        long barsLeft = (long) Math.ceil(left / Math.max(1D, perBar));

        double currentBarLeft;
        if (barsLeft <= 0) {
            currentBarLeft = 0D;
        } else {
            currentBarLeft = left % perBar;
            if (currentBarLeft <= 0D) {
                currentBarLeft = perBar;
            }
        }

        double progress = Math.max(0D, Math.min(1D, currentBarLeft / Math.max(1D, perBar)));
        BarColor currentColor = pickBarColor((int) barsLeft);
        runtime.bossBar.setColor(currentColor);

        if (runtime.lastBarsLeft > 0 && barsLeft < runtime.lastBarsLeft) {
            int dropped = (int) (runtime.lastBarsLeft - barsLeft);
            playPipeBreakSound(def, dropped);
        }
        runtime.lastBarsLeft = barsLeft;

        String barsText = barColorCode(currentColor) + "&lx" + Math.max(0, barsLeft);

        String titleTemplate = msg("bossbar-title");
        String title = titleTemplate
                .replace("%game_name%", def.displayName)
                .replace("%time_left%", String.valueOf(Math.max(0, runtime.remainingSeconds)))
                .replace("%bars_left%", "")
                .replace("%hp_left%", String.format(Locale.US, "%.1f", left))
                .replace("%hp_total%", String.format(Locale.US, "%.1f", total));

        title = title + " " + barsText;

        runtime.bossBar.setTitle(Utils.colorize(title));
        runtime.bossBar.setProgress(progress);
    }

    private void playPipeBreakSound(BaseDefendDefinition def, int droppedPipes) {
        if (def == null || droppedPipes <= 0) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld() == null || !player.getWorld().getName().equals(def.world)) {
                continue;
            }
            if (!isInsideSphere(player.getLocation(), def.centerX, def.centerY, def.centerZ, def.radius + 80D)) {
                continue;
            }
            for (int i = 0; i < droppedPipes; i++) {
                long delay = i * 2L;
                Bukkit.getScheduler().runTaskLater(plugin,
                        () -> player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1.0f, 1.0f),
                        delay);
            }
        }
    }

    private String barColorCode(BarColor color) {
        if (color == null) {
            return "&f";
        }
        switch (color) {
            case BLUE:
                return "&9";
            case GREEN:
                return "&a";
            case YELLOW:
                return "&e";
            case PURPLE:
                return "&5";
            case PINK:
                return "&d";
            case RED:
                return "&c";
            case WHITE:
                return "&f";
            default:
                return "&f";
        }
    }

    private BarColor pickBarColor(int barsLeft) {
        if (barsLeft <= 0) {
            return BAR_COLORS[0];
        }
        int idx = (barsLeft - 1) % BAR_COLORS.length;
        return BAR_COLORS[idx];
    }

    private void spawnBoundaryParticles(BaseDefendDefinition def) {
        if (def == null) {
            return;
        }

        World world = Bukkit.getWorld(def.world);
        if (world == null) {
            return;
        }

        int points = Math.max(12, def.boundaryParticlePoints);
        double[] yOffsets = new double[]{-def.radius * 0.5, 0D, def.radius * 0.5};
        for (double yOffset : yOffsets) {
            double layerRadiusSq = def.radius * def.radius - yOffset * yOffset;
            if (layerRadiusSq <= 0) {
                continue;
            }
            double layerRadius = Math.sqrt(layerRadiusSq);
            for (int i = 0; i < points; i++) {
                double angle = (Math.PI * 2D) * i / points;
                double x = def.centerX + Math.cos(angle) * layerRadius;
                double z = def.centerZ + Math.sin(angle) * layerRadius;
                double y = def.centerY + yOffset;
                world.spawnParticle(Particle.END_ROD, x, y, z, 1, 0D, 0D, 0D, 0D);
            }
        }
    }

    private void finishBaseDefend(String gameId, boolean victory, String reason) {
        BaseDefendRuntime runtime = runningBaseDefendGames.remove(gameId);
        if (runtime == null) {
            return;
        }

        if (runtime.timerTask != null) {
            runtime.timerTask.cancel();
        }
        if (runtime.runningSoundTask != null) {
            runtime.runningSoundTask.cancel();
        }
        if (runtime.spawnTask != null) {
            runtime.spawnTask.cancel();
        }
        if (runtime.boundaryTask != null) {
            runtime.boundaryTask.cancel();
        }
        if (runtime.bossBar != null) {
            runtime.bossBar.removeAll();
            runtime.bossBar.setVisible(false);
        }

        lastBaseDefendRanking.clear();
        lastBaseDefendRanking.putAll(runtime.playerDamage);

        List<String> commands = victory ? runtime.definition.victoryCommands : runtime.definition.defeatCommands;
        for (String raw : commands) {
            String cmd = raw.replace("%game_id%", runtime.definition.id)
                    .replace("%game_name%", runtime.definition.displayName)
                    .replace("%reason%", reason == null ? "" : reason);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }

        playFinishLoopSound(runtime.definition, victory);

        broadcast(victory
                ? msg("victory-broadcast").replace("%game_name%", runtime.definition.displayName)
                : msg("defeat-broadcast").replace("%game_name%", runtime.definition.displayName));
    }

    private void stopBaseDefend(String gameId, boolean executeDefeatCommands, String reason) {
        BaseDefendRuntime runtime = runningBaseDefendGames.remove(gameId);
        if (runtime == null) {
            return;
        }

        if (runtime.timerTask != null) {
            runtime.timerTask.cancel();
        }
        if (runtime.runningSoundTask != null) {
            runtime.runningSoundTask.cancel();
        }
        if (runtime.spawnTask != null) {
            runtime.spawnTask.cancel();
        }
        if (runtime.boundaryTask != null) {
            runtime.boundaryTask.cancel();
        }
        if (runtime.bossBar != null) {
            runtime.bossBar.removeAll();
            runtime.bossBar.setVisible(false);
        }

        if (executeDefeatCommands) {
            for (String raw : runtime.definition.defeatCommands) {
                String cmd = raw.replace("%game_id%", runtime.definition.id)
                        .replace("%game_name%", runtime.definition.displayName)
                        .replace("%reason%", reason == null ? "" : reason);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
        }
    }

    private void broadcast(String text) {
        String finalText = Utils.colorize(text);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(finalText);
        }
    }

    private void playFinishLoopSound(BaseDefendDefinition def, boolean victory) {
        if (def == null) {
            return;
        }

        String soundId = victory ? def.victorySoundId : def.defeatSoundId;
        if (soundId == null || soundId.isBlank()) {
            return;
        }

        int loops = Math.max(1, def.finishSoundLoops);
        for (int i = 0; i < loops; i++) {
            long delay = (long) i * def.soundIntervalTicks;
            Bukkit.getScheduler().runTaskLater(plugin, () -> playSoundForGame(def, soundId), delay);
        }
    }

    private void playSoundForGame(BaseDefendDefinition def, String soundId) {
        if (def == null || soundId == null || soundId.isBlank()) {
            return;
        }

        World world = Bukkit.getWorld(def.world);
        if (world == null) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld() == null || !player.getWorld().getName().equals(def.world)) {
                continue;
            }
            if (!isInsideSphere(player.getLocation(), def.centerX, def.centerY, def.centerZ, def.radius + 80D)) {
                continue;
            }
            player.playSound(player.getLocation(), soundId, SoundCategory.MASTER, def.soundVolume, def.soundPitch);
        }
    }

    private void loadAll() {
        for (String gameId : new ArrayList<>(runningBaseDefendGames.keySet())) {
            stopBaseDefend(gameId, false, "reload");
        }

        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder.");
        }

        moduleFolder = new File(plugin.getDataFolder(), "game");
        if (!moduleFolder.exists() && !moduleFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create game module folder.");
        }

        basedefendFolder = new File(moduleFolder, "basedefend");
        if (!basedefendFolder.exists() && !basedefendFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create game basedefend folder.");
        }

        configFile = new File(moduleFolder, "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("game/config.yml", false);
        }

        ensureDefaultBasedefendWhenEmpty();

        config = YamlConfiguration.loadConfiguration(configFile);
        loadMessages();
        loadBasedefendGames();

        saveConfig();
    }

    private void ensureDefaultBasedefendWhenEmpty() {
        File[] files = basedefendFolder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files != null && files.length > 0) {
            return;
        }
        plugin.saveResource("game/basedefend/example.yml", false);
    }

    private void loadMessages() {
        messages.clear();

        putDefaultMessage("module-disabled", "&cGame 模块已禁用。");
        putDefaultMessage("unknown-function", "&c未知玩法: %function_id%");
        putDefaultMessage("unknown-game", "&c未知游戏ID: %game_id%");
        putDefaultMessage("already-running", "&e该游戏已在运行: %game_id%");
        putDefaultMessage("not-running", "&c该游戏未在运行: %game_id%");
        putDefaultMessage("reload-success", "&aGame 配置已重载。");
        putDefaultMessage("start-success", "&a已启动游戏 %game_name% (&f%game_id%&a)");
        putDefaultMessage("stop-success", "&e已中止游戏 %game_id%");
        putDefaultMessage("force-victory-success", "&a已强制胜利: %game_id%");
        putDefaultMessage("force-defeat-success", "&e已强制失败: %game_id%");
        putDefaultMessage("list-title", "&6[%function_id%] 可用游戏列表:");
        putDefaultMessage("list-line", "&7- &f%game_id% &8| &e%game_name%");
        putDefaultMessage("victory-broadcast", "&a据点防御挑战成功：%game_name%");
        putDefaultMessage("defeat-broadcast", "&c据点防御挑战失败：%game_name%");
        putDefaultMessage("bossbar-title", "&6%game_name% &7| &f剩余时间: &e%time_left%s &7| &f%hp_left%&7/&f%hp_total% %bars_left%");
        putDefaultMessage("no-last-ranking", "&7该玩法还没有最近一次结算记录。");
        putDefaultMessage("ranking-title", "&6=== basedefend 最近一次伤害排行 ===");
        putDefaultMessage("ranking-line", "&e%rank%. &f%player% &7- &c%damage%");
    }

    private void putDefaultMessage(String key, String value) {
        String path = "messages." + key;
        if (!config.contains(path)) {
            config.set(path, value);
        }
        messages.put(key, config.getString(path, value));
    }

    private void loadBasedefendGames() {
        basedefendGames.clear();

        File[] files = basedefendFolder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) {
            return;
        }

        List<File> sorted = new ArrayList<>(List.of(files));
        sorted.sort(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));

        Set<String> usedIds = new HashSet<>();

        for (File file : sorted) {
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
            ensureBasedefendDefaults(yml, file);
            BaseDefendDefinition def = parseBaseDefend(yml, file.getName(), usedIds);
            if (def != null) {
                basedefendGames.put(def.id, def);
                usedIds.add(def.id);
            }
        }
    }

    private void ensureBasedefendDefaults(YamlConfiguration yml, File file) {
        boolean changed = false;

        if (!yml.contains("spawn-count-per-wave")) {
            double oldMultiplier = yml.getDouble("spawn-multiplier", 2D);
            int migratedCount = Math.max(1, (int) Math.round(6D * Math.max(0.1D, oldMultiplier)));
            yml.set("spawn-count-per-wave", migratedCount);
            changed = true;
        }

        if (!yml.contains("spawn-wave-interval-seconds")) {
            int oldTicks = Math.max(1, yml.getInt("spawn-wave-interval-ticks", 20));
            int migratedSeconds = Math.max(1, (int) Math.ceil(oldTicks / 20.0));
            yml.set("spawn-wave-interval-seconds", migratedSeconds);
            changed = true;
        }

        if (!yml.contains("sounds.running")) {
            yml.set("sounds.running", "");
            changed = true;
        }
        if (!yml.contains("sounds.victory")) {
            yml.set("sounds.victory", "minecraft:ui.toast.challenge_complete");
            changed = true;
        }
        if (!yml.contains("sounds.defeat")) {
            yml.set("sounds.defeat", "minecraft:entity.wither.death");
            changed = true;
        }
        if (!yml.contains("sounds.interval-ticks")) {
            yml.set("sounds.interval-ticks", 40);
            changed = true;
        }
        if (!yml.contains("sounds.volume")) {
            yml.set("sounds.volume", 1.0);
            changed = true;
        }
        if (!yml.contains("sounds.pitch")) {
            yml.set("sounds.pitch", 1.0);
            changed = true;
        }
        if (!yml.contains("sounds.finish-loops")) {
            yml.set("sounds.finish-loops", 3);
            changed = true;
        }

        if (!changed) {
            return;
        }

        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to patch basedefend config " + file.getName() + ": " + e.getMessage());
        }
    }

    private BaseDefendDefinition parseBaseDefend(YamlConfiguration yml, String fileName, Set<String> usedIds) {
        String id = normalize(yml.getString("id", ""));
        if (!id.matches("[a-z0-9_-]+")) {
            plugin.getLogger().warning("Skip game config with invalid id in " + fileName);
            return null;
        }
        if (usedIds.contains(id)) {
            plugin.getLogger().warning("Skip duplicate game id in " + fileName + ": " + id);
            return null;
        }

        String displayName = yml.getString("name", "据点防御");

        ConfigurationSection center = yml.getConfigurationSection("center");
        if (center == null) {
            plugin.getLogger().warning("Skip game config (missing center) in " + fileName);
            return null;
        }

        String world = center.getString("world", "world");
        double x = center.getDouble("x", 0D);
        double y = center.getDouble("y", 64D);
        double z = center.getDouble("z", 0D);

        double radius = Math.max(1D, yml.getDouble("radius", 20D));
        double totalRequiredDamage = Math.max(1D, yml.getDouble("required-total-damage", 5000D));
        int hpBarCount = Math.max(1, yml.getInt("hp-bars", 50));
        int timeLimitSeconds = Math.max(1, yml.getInt("time-limit-seconds", 300));

        int spawnCountPerWave = Math.max(1, yml.getInt("spawn-count-per-wave", 12));
        int spawnWaveIntervalSeconds = Math.max(1, yml.getInt("spawn-wave-interval-seconds", 1));

        ConfigurationSection sounds = yml.getConfigurationSection("sounds");
        String runningSoundId = normalizeSoundId(sounds == null ? "" : sounds.getString("running", ""));
        String victorySoundId = normalizeSoundId(sounds == null ? "minecraft:ui.toast.challenge_complete" : sounds.getString("victory", "minecraft:ui.toast.challenge_complete"));
        String defeatSoundId = normalizeSoundId(sounds == null ? "minecraft:entity.wither.death" : sounds.getString("defeat", "minecraft:entity.wither.death"));
        int soundIntervalTicks = Math.max(1, sounds == null ? 40 : sounds.getInt("interval-ticks", 40));
        float soundVolume = (float) clamp(sounds == null ? 1.0 : sounds.getDouble("volume", 1.0), 0.0, 10.0);
        float soundPitch = (float) clamp(sounds == null ? 1.0 : sounds.getDouble("pitch", 1.0), 0.5, 2.0);
        int finishSoundLoops = Math.max(1, sounds == null ? 3 : sounds.getInt("finish-loops", 3));

        ConfigurationSection boundary = yml.getConfigurationSection("boundary-particle");
        boolean boundaryParticleEnabled = boundary == null || boundary.getBoolean("enabled", true);
        int boundaryParticleIntervalTicks = Math.max(1, boundary == null ? 10 : boundary.getInt("interval-ticks", 10));
        int boundaryParticlePoints = Math.max(12, boundary == null ? 64 : boundary.getInt("points", 64));

        Set<EntityType> countedTypes = new HashSet<>();
        List<SpawnMobEntry> spawnEntries = new ArrayList<>();
        for (String raw : yml.getStringList("counted-mobs")) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String trimmed = raw.trim();
            String entityId = trimmed;
            String nbt = "";
            int nbtStart = trimmed.indexOf('{');
            if (nbtStart > 0 && trimmed.endsWith("}")) {
                entityId = trimmed.substring(0, nbtStart).trim();
                nbt = trimmed.substring(nbtStart).trim();
            }

            EntityType type = parseEntityType(entityId);
            if (type != null && type.isAlive()) {
                countedTypes.add(type);
                String normalizedId = normalizeEntityId(entityId, type);
                spawnEntries.add(new SpawnMobEntry(type, normalizedId, nbt));
            }
        }
        if (countedTypes.isEmpty()) {
            countedTypes.add(EntityType.ZOMBIE);
            spawnEntries.add(new SpawnMobEntry(EntityType.ZOMBIE, "minecraft:zombie", ""));
        }

        List<String> victory = new ArrayList<>(yml.getStringList("on-victory"));
        List<String> defeat = new ArrayList<>(yml.getStringList("on-defeat"));

        return new BaseDefendDefinition(id, displayName, world, x, y, z, radius,
                totalRequiredDamage, hpBarCount, timeLimitSeconds, countedTypes,
            spawnEntries, spawnCountPerWave, spawnWaveIntervalSeconds,
            boundaryParticleEnabled, boundaryParticleIntervalTicks, boundaryParticlePoints,
                runningSoundId, victorySoundId, defeatSoundId, soundIntervalTicks, soundVolume, soundPitch, finishSoundLoops,
            victory, defeat);
    }

    private String normalizeEntityId(String text, EntityType fallbackType) {
        if (text == null || text.isBlank()) {
            return fallbackType == null ? "minecraft:zombie" : fallbackType.getKey().toString();
        }
        String trimmed = text.trim().toLowerCase(Locale.ROOT);
        if (!trimmed.contains(":")) {
            trimmed = "minecraft:" + trimmed;
        }
        return trimmed;
    }

    private String normalizeSoundId(String soundId) {
        return soundId == null ? "" : soundId.trim();
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private EntityType parseEntityType(String raw) {
        String text = normalize(raw);
        if (text.startsWith("minecraft:")) {
            text = text.substring("minecraft:".length());
        }
        try {
            return EntityType.valueOf(text.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return null;
        }
    }

    private void registerCommand() {
        if (plugin.getCommand("si-game") == null) {
            plugin.getLogger().warning("Command si-game is missing in plugin.yml");
            return;
        }

        if (command == null) {
            command = new GameCommand(this);
        }

        plugin.getCommand("si-game").setExecutor(command);
        plugin.getCommand("si-game").setTabCompleter(command);
    }

    private void registerListener() {
        listener = new GameListener(this);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }

    private void saveConfig() {
        if (config == null || configFile == null) {
            return;
        }
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save game config: " + e.getMessage());
        }
    }

    private String msg(String key) {
        return messages.getOrDefault(key, "");
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
    }

    public interface CommandFeedback {
        void send(String message);
    }

    private static class BaseDefendDefinition {
        private final String id;
        private final String displayName;
        private final String world;
        private final double centerX;
        private final double centerY;
        private final double centerZ;
        private final double radius;
        private final double totalRequiredDamage;
        private final int hpBarCount;
        private final int timeLimitSeconds;
        private final Set<EntityType> countedTypes;
        private final List<SpawnMobEntry> spawnEntries;
        private final int spawnCountPerWave;
        private final int spawnWaveIntervalSeconds;
        private final boolean boundaryParticleEnabled;
        private final int boundaryParticleIntervalTicks;
        private final int boundaryParticlePoints;
        private final String runningSoundId;
        private final String victorySoundId;
        private final String defeatSoundId;
        private final int soundIntervalTicks;
        private final float soundVolume;
        private final float soundPitch;
        private final int finishSoundLoops;
        private final List<String> victoryCommands;
        private final List<String> defeatCommands;

        private BaseDefendDefinition(String id, String displayName, String world,
                                     double centerX, double centerY, double centerZ, double radius,
                                     double totalRequiredDamage, int hpBarCount, int timeLimitSeconds,
                         Set<EntityType> countedTypes, List<SpawnMobEntry> spawnEntries,
                         int spawnCountPerWave, int spawnWaveIntervalSeconds,
                         boolean boundaryParticleEnabled, int boundaryParticleIntervalTicks, int boundaryParticlePoints,
                                     String runningSoundId, String victorySoundId, String defeatSoundId,
                                     int soundIntervalTicks, float soundVolume, float soundPitch, int finishSoundLoops,
                                     List<String> victoryCommands, List<String> defeatCommands) {
            this.id = id;
            this.displayName = displayName;
            this.world = world;
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
            this.radius = radius;
            this.totalRequiredDamage = totalRequiredDamage;
            this.hpBarCount = hpBarCount;
            this.timeLimitSeconds = timeLimitSeconds;
            this.countedTypes = countedTypes;
            this.spawnEntries = spawnEntries;
            this.spawnCountPerWave = spawnCountPerWave;
            this.spawnWaveIntervalSeconds = spawnWaveIntervalSeconds;
            this.boundaryParticleEnabled = boundaryParticleEnabled;
            this.boundaryParticleIntervalTicks = boundaryParticleIntervalTicks;
            this.boundaryParticlePoints = boundaryParticlePoints;
            this.runningSoundId = runningSoundId;
            this.victorySoundId = victorySoundId;
            this.defeatSoundId = defeatSoundId;
            this.soundIntervalTicks = soundIntervalTicks;
            this.soundVolume = soundVolume;
            this.soundPitch = soundPitch;
            this.finishSoundLoops = finishSoundLoops;
            this.victoryCommands = victoryCommands;
            this.defeatCommands = defeatCommands;
        }
    }

    private static class BaseDefendRuntime {
        private final BaseDefendDefinition definition;
        private int remainingSeconds;
        private double currentDamage;
        private final Set<UUID> countedEntityIds = new HashSet<>();
        private final Map<UUID, Double> playerDamage = new HashMap<>();
        private BukkitTask timerTask;
        private BukkitTask runningSoundTask;
        private BukkitTask spawnTask;
        private BukkitTask boundaryTask;
        private BossBar bossBar;
        private long lastBarsLeft;

        private BaseDefendRuntime(BaseDefendDefinition definition) {
            this.definition = definition;
            this.remainingSeconds = definition.timeLimitSeconds;
            this.currentDamage = 0D;
            this.lastBarsLeft = definition.hpBarCount;
        }
    }

    private static class SpawnMobEntry {
        private final EntityType type;
        private final String entityId;
        private final String nbt;

        private SpawnMobEntry(EntityType type, String entityId, String nbt) {
            this.type = type;
            this.entityId = entityId;
            this.nbt = nbt == null ? "" : nbt;
        }
    }
}
