package com.simc.modules.protection;

import com.simc.SiMCUniverse;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
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

    private final Map<UUID, Long> protectedUntil = new HashMap<>();

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
        applyProtection(player.getUniqueId(), joinProtectionSeconds);
    }

    public void applySpawnProtection(Player player) {
        if (player == null || spawnProtectionSeconds <= 0) {
            return;
        }
        applyProtection(player.getUniqueId(), spawnProtectionSeconds);
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
            protectedUntil.remove(player.getUniqueId());
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
            protectedUntil.remove(player.getUniqueId());
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

    private void applyProtection(UUID uuid, int seconds) {
        long now = System.currentTimeMillis();
        long newUntil = now + seconds * 1000L;

        Long old = protectedUntil.get(uuid);
        if (old == null || old < newUntil) {
            protectedUntil.put(uuid, newUntil);
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
