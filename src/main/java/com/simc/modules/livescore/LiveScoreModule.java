package com.simc.modules.livescore;

import com.simc.SiMCUniverse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LiveScoreModule {
    private static final String DISPLAY_NAME = "生存分";

    private final SiMCUniverse plugin;
    private final Map<UUID, Integer> scores = new HashMap<>();

    private File configFile;
    private YamlConfiguration config;

    private boolean tabDisplayEnabled;
    private int gainIntervalSeconds;
    private int gainAmount;

    private boolean deathMultiplierEnabled;
    private double deathMultiplierValue;
    private String deathMultiplierRounding;

    private boolean paused;

    private int taskId = -1;
    private int secondCounter = 0;

    public LiveScoreModule(SiMCUniverse plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        loadConfig();
        registerCommand();
        registerListener();
        startTask();
        updateTabDisplay();
        plugin.getLogger().info("LiveScore module initialized.");
    }

    public void shutdown() {
        saveScoresToConfig();
        stopTask();
        clearTabDisplay();
        plugin.getLogger().info("LiveScore module shutdown completed.");
    }

    public void reload() {
        loadConfig();
        secondCounter = 0;
        stopTask();
        startTask();
        updateTabDisplay();
    }

    public int getScore(UUID uuid) {
        return scores.getOrDefault(uuid, 0);
    }

    public void setScore(UUID uuid, int score) {
        scores.put(uuid, score);
        saveScoresToConfig();
        updateTabDisplay();
    }

    public void addScore(UUID uuid, int delta) {
        setScore(uuid, getScore(uuid) + delta);
    }

    public void resetAllScores() {
        scores.clear();
        saveScoresToConfig();
        updateTabDisplay();
    }

    public void pause() {
        paused = true;
        config.set("runtime.paused", true);
        saveConfigFile();
    }

    public void start() {
        paused = false;
        config.set("runtime.paused", false);
        saveConfigFile();
    }

    public List<Map.Entry<UUID, Integer>> getRanking() {
        List<Map.Entry<UUID, Integer>> ranking = new ArrayList<>(scores.entrySet());
        ranking.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        return ranking;
    }

    public void handlePlayerDeath(Player player) {
        if (!deathMultiplierEnabled) {
            return;
        }

        int current = getScore(player.getUniqueId());
        double reduced = current * deathMultiplierValue;

        int finalScore;
        switch (deathMultiplierRounding.toLowerCase()) {
            case "ceil":
                finalScore = (int) Math.ceil(reduced);
                break;
            case "floor":
                finalScore = (int) Math.floor(reduced);
                break;
            default:
                finalScore = (int) Math.round(reduced);
                break;
        }

        setScore(player.getUniqueId(), finalScore);
    }

    private void registerCommand() {
        if (plugin.getCommand("si-livescore") == null) {
            plugin.getLogger().warning("Command si-livescore is missing in plugin.yml");
            return;
        }
        LiveScoreCommand command = new LiveScoreCommand(this);
        plugin.getCommand("si-livescore").setExecutor(command);
        plugin.getCommand("si-livescore").setTabCompleter(command);
    }

    private void registerListener() {
        plugin.getServer().getPluginManager().registerEvents(new LiveScoreListener(this), plugin);
    }

    private void loadConfig() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder.");
        }

        configFile = new File(plugin.getDataFolder(), "livescore.yml");
        if (!configFile.exists()) {
            plugin.saveResource("livescore.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        tabDisplayEnabled = config.getBoolean("tab-display.enabled", true);

        gainIntervalSeconds = Math.max(1, config.getInt("gain.interval-seconds", 60));
        gainAmount = Math.max(1, config.getInt("gain.amount", 1));

        deathMultiplierEnabled = config.getBoolean("death-multiplier.enabled", false);
        deathMultiplierValue = clamp(config.getDouble("death-multiplier.value", 1.0), 0.0, 1.0);
        deathMultiplierRounding = config.getString("death-multiplier.rounding", "round");

        paused = config.getBoolean("runtime.paused", false);

        loadScoresFromConfig();
        saveConfigFile();
    }

    private void loadScoresFromConfig() {
        scores.clear();
        ConfigurationSection section = config.getConfigurationSection("scores");
        if (section == null) {
            return;
        }

        for (String uuidText : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidText);
                int score = section.getInt(uuidText, 0);
                scores.put(uuid, score);
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Invalid UUID in livescore.yml scores: " + uuidText);
            }
        }
    }

    private void saveScoresToConfig() {
        config.set("scores", null);
        for (Map.Entry<UUID, Integer> entry : scores.entrySet()) {
            config.set("scores." + entry.getKey(), entry.getValue());
        }
        saveConfigFile();
    }

    private void saveConfigFile() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save livescore.yml: " + e.getMessage());
        }
    }

    private void startTask() {
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tickEverySecond, 20L, 20L);
    }

    private void stopTask() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    private void tickEverySecond() {
        if (tabDisplayEnabled) {
            updateTabDisplay();
        }

        if (paused) {
            return;
        }

        secondCounter++;
        if (secondCounter < gainIntervalSeconds) {
            return;
        }

        secondCounter = 0;
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            scores.put(uuid, getScore(uuid) + gainAmount);
        }
        saveScoresToConfig();

        if (tabDisplayEnabled) {
            updateTabDisplay();
        }
    }

    private void updateTabDisplay() {
        if (!tabDisplayEnabled) {
            clearTabDisplay();
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            String display = player.getName() + " §7[§a" + DISPLAY_NAME + ":" + getScore(player.getUniqueId()) + "§7]";
            try {
                player.setPlayerListName(display);
            } catch (IllegalArgumentException ignored) {
                player.setPlayerListName(player.getName());
            }
        }
    }

    private void clearTabDisplay() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setPlayerListName(player.getName());
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public String getPlayerName(UUID uuid) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        String name = offlinePlayer.getName();
        return (name == null || name.isBlank()) ? uuid.toString().substring(0, 8) : name;
    }
}
