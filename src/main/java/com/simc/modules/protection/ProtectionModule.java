package com.simc.modules.protection;

import com.simc.SiMCUniverse;
import com.simc.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.bukkit.configuration.file.YamlConfiguration;

public class ProtectionModule {
    private final SiMCUniverse plugin;

    private boolean enabled;
    private File moduleFolder;
    private File configFile;
    private YamlConfiguration config;

    private int joinProtectionSeconds = 45;
    private int spawnProtectionSeconds = 120;
    private String messageJoinStart;
    private String messageSpawnStart;
    private String messageProtectionEnd;
    private Sound startSound = Sound.UI_TOAST_IN;
    private Sound endSound = Sound.UI_TOAST_CHALLENGE_COMPLETE;

    private final Map<UUID, Long> protectedUntil = new HashMap<>();
    private int tickerTaskId = -1;

    private ProtectionCommand command;
    private ProtectionListener listener;

    public ProtectionModule(SiMCUniverse plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        if (enabled) {
            return;
        }

        loadAll();
        registerCommand();
        registerListener();
        startTicker();
        enabled = true;
        plugin.getLogger().info("Protection module initialized.");
    }

    public void shutdown() {
        if (!enabled) {
            return;
        }

        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }

        stopTicker();

        protectedUntil.clear();
        enabled = false;
        plugin.getLogger().info("Protection module shutdown completed.");
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

    public void applyJoinProtection(Player player) {
        if (player == null || joinProtectionSeconds <= 0) {
            return;
        }
        applyProtection(player, joinProtectionSeconds, messageJoinStart);
    }

    public void applySpawnProtection(Player player) {
        if (player == null || spawnProtectionSeconds <= 0) {
            return;
        }
        applyProtection(player, spawnProtectionSeconds, messageSpawnStart);
    }

    public boolean isProtected(Player player) {
        if (player == null) {
            return false;
        }

        Long until = protectedUntil.get(player.getUniqueId());
        if (until == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (until <= now) {
            onProtectionEnd(player);
            return false;
        }
        return true;
    }

    public long getRemainingSeconds(Player player) {
        if (player == null) {
            return 0;
        }

        Long until = protectedUntil.get(player.getUniqueId());
        if (until == null) {
            return 0;
        }

        long remainMillis = until - System.currentTimeMillis();
        if (remainMillis <= 0) {
            onProtectionEnd(player);
            return 0;
        }

        return (remainMillis + 999) / 1000;
    }

    public int getJoinProtectionSeconds() {
        return joinProtectionSeconds;
    }

    public int getSpawnProtectionSeconds() {
        return spawnProtectionSeconds;
    }

    public boolean setProtectionSeconds(String type, int seconds) {
        int value = Math.max(0, seconds);
        String key = normalize(type);

        if ("joinprotection".equals(key)) {
            joinProtectionSeconds = value;
            config.set("join-protection-seconds", value);
            saveConfig();
            return true;
        }

        if ("spawnprotection".equals(key)) {
            spawnProtectionSeconds = value;
            config.set("spawn-protection-seconds", value);
            saveConfig();
            return true;
        }

        return false;
    }

    private void applyProtection(Player player, int seconds, String startMessage) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long newUntil = now + seconds * 1000L;

        Long old = protectedUntil.get(uuid);
        if (old == null || old < newUntil) {
            protectedUntil.put(uuid, newUntil);
        }

        sendMessage(player, startMessage, seconds);
        playConfiguredSound(player, startSound);
    }

    private void startTicker() {
        stopTicker();
        tickerTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tickProtection, 20L, 20L);
    }

    private void stopTicker() {
        if (tickerTaskId != -1) {
            Bukkit.getScheduler().cancelTask(tickerTaskId);
            tickerTaskId = -1;
        }
    }

    private void tickProtection() {
        if (protectedUntil.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> iterator = protectedUntil.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            if (entry.getValue() > now) {
                continue;
            }

            iterator.remove();
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                sendMessage(player, messageProtectionEnd, 0);
                playConfiguredSound(player, endSound);
            }
        }
    }

    private void onProtectionEnd(Player player) {
        if (player == null) {
            return;
        }

        if (protectedUntil.remove(player.getUniqueId()) != null) {
            sendMessage(player, messageProtectionEnd, 0);
            playConfiguredSound(player, endSound);
        }
    }

    private void sendMessage(Player player, String raw, int seconds) {
        if (player == null || raw == null || raw.isBlank()) {
            return;
        }
        player.sendMessage(Utils.colorize(raw.replace("%seconds%", String.valueOf(seconds))));
    }

    private void playConfiguredSound(Player player, Sound sound) {
        if (player == null || sound == null) {
            return;
        }
        try {
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (Exception ignored) {
        }
    }

    private Sound parseSound(String name, Sound fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        try {
            return Sound.valueOf(name.trim().toUpperCase());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private void registerCommand() {
        if (plugin.getCommand("si-protection") == null) {
            plugin.getLogger().warning("Command si-protection is missing in plugin.yml");
            return;
        }

        if (command == null) {
            command = new ProtectionCommand(this);
        }
        plugin.getCommand("si-protection").setExecutor(command);
        plugin.getCommand("si-protection").setTabCompleter(command);
    }

    private void registerListener() {
        listener = new ProtectionListener(this);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }

    private void loadAll() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder.");
        }

        moduleFolder = new File(plugin.getDataFolder(), "protection");
        if (!moduleFolder.exists() && !moduleFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create protection module folder.");
        }

        configFile = new File(moduleFolder, "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("protection/config.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        joinProtectionSeconds = Math.max(0, config.getInt("join-protection-seconds", 45));
        spawnProtectionSeconds = Math.max(0, config.getInt("spawn-protection-seconds", 120));
        messageJoinStart = config.getString("messages.join-start", "&a你已获得加入保护，持续 &e%seconds%&a 秒。");
        messageSpawnStart = config.getString("messages.spawn-start", "&b你已获得重生保护，持续 &e%seconds%&b 秒。");
        messageProtectionEnd = config.getString("messages.protection-end", "&e你的保护已结束。");
        startSound = parseSound(config.getString("sounds.start", "UI_TOAST_IN"), Sound.UI_TOAST_IN);
        endSound = parseSound(config.getString("sounds.end", "UI_TOAST_CHALLENGE_COMPLETE"), Sound.UI_TOAST_CHALLENGE_COMPLETE);

        saveConfig();
    }

    private void saveConfig() {
        if (config == null || configFile == null) {
            return;
        }

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save protection config: " + e.getMessage());
        }
    }

    public String normalize(String text) {
        return text == null ? "" : text.toLowerCase().trim();
    }
}
