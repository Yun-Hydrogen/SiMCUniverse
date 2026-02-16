package com.simc.modules.killscore;

import com.simc.SiMCUniverse;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class KillScoreModule {
    private final SiMCUniverse plugin;
    private final Map<UUID, Integer> scores = new HashMap<>();
    private final Map<String, Integer> killRules = new HashMap<>();

    private File configFile;
    private YamlConfiguration config;

    private String killScoreName;
    private boolean scoreboardEnabled;
    private boolean deathMultiplierEnabled;
    private double deathMultiplierValue;
    private String deathMultiplierRounding;

    private int scoreboardTaskId = -1;

    public KillScoreModule(SiMCUniverse plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        loadConfig();
        registerCommand();
        registerListener();
        startScoreboardTask();
        plugin.getLogger().info("KillScore module initialized.");
    }

    public void shutdown() {
        saveScoresToConfig();
        stopScoreboardTask();
        plugin.getLogger().info("KillScore module shutdown completed.");
    }

    public void reload() {
        loadConfig();
        stopScoreboardTask();
        startScoreboardTask();
        updateScoreboards();
    }

    public int getScore(UUID uuid) {
        return scores.getOrDefault(uuid, 0);
    }

    public void setScore(UUID uuid, int score) {
        scores.put(uuid, score);
        saveScoresToConfig();
        updateScoreboards();
    }

    public void addScore(UUID uuid, int delta) {
        setScore(uuid, getScore(uuid) + delta);
    }

    public void resetAllScores() {
        scores.clear();
        saveScoresToConfig();
        updateScoreboards();
    }

    public String getKillScoreName() {
        return killScoreName;
    }

    public String formatText(String text) {
        return text.replace("%killscore_name%", killScoreName);
    }

    public void handleKill(Player killer, NamespacedKey key) {
        String namespacedId = key.toString();
        int delta = killRules.getOrDefault(namespacedId, 0);
        if (delta <= 0) {
            return;
        }
        addScore(killer.getUniqueId(), delta);
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
        if (plugin.getCommand("si-killscore") == null) {
            plugin.getLogger().warning("Command si-killscore is missing in plugin.yml");
            return;
        }
        KillScoreCommand command = new KillScoreCommand(this);
        plugin.getCommand("si-killscore").setExecutor(command);
        plugin.getCommand("si-killscore").setTabCompleter(command);
    }

    private void registerListener() {
        plugin.getServer().getPluginManager().registerEvents(new KillScoreListener(this), plugin);
    }

    private void loadConfig() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder.");
        }

        configFile = new File(plugin.getDataFolder(), "killscore.yml");
        if (!configFile.exists()) {
            plugin.saveResource("killscore.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        killScoreName = config.getString("killscore-name", "击杀分");
        scoreboardEnabled = config.getBoolean("scoreboard.enabled", true);

        deathMultiplierEnabled = config.getBoolean("death-multiplier.enabled", false);
        deathMultiplierValue = clamp(config.getDouble("death-multiplier.value", 1.0), 0.0, 1.0);
        deathMultiplierRounding = config.getString("death-multiplier.rounding", "round");

        killRules.clear();
        ConfigurationSection killsSection = config.getConfigurationSection("kills");
        if (killsSection != null) {
            for (String key : killsSection.getKeys(false)) {
                int value = killsSection.getInt(key, 0);
                if (value > 0) {
                    killRules.put(key, value);
                }
            }
        }

        loadScoresFromConfig();
        saveConfigFile();
    }

    private void loadScoresFromConfig() {
        scores.clear();
        ConfigurationSection scoreSection = config.getConfigurationSection("scores");
        if (scoreSection == null) {
            return;
        }

        for (String uuidText : scoreSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidText);
                int score = scoreSection.getInt(uuidText, 0);
                scores.put(uuid, score);
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Invalid UUID in killscore.yml scores: " + uuidText);
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
            plugin.getLogger().severe("Failed to save killscore.yml: " + e.getMessage());
        }
    }

    private void startScoreboardTask() {
        if (!scoreboardEnabled) {
            return;
        }

        scoreboardTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::updateScoreboards, 20L, 40L);
    }

    private void stopScoreboardTask() {
        if (scoreboardTaskId != -1) {
            Bukkit.getScheduler().cancelTask(scoreboardTaskId);
            scoreboardTaskId = -1;
        }
    }

    public void updateScoreboards() {
        if (!scoreboardEnabled) {
            return;
        }

        List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>(scores.entrySet());
        sorted.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        for (Player player : Bukkit.getOnlinePlayers()) {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective objective = scoreboard.registerNewObjective("si_killscore", "dummy", "§e" + killScoreName + "§f排行榜");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);

            String selfLine = "§7你: §a" + getScore(player.getUniqueId());
            objective.getScore(selfLine).setScore(15);

            int line = 14;
            int rank = 1;

            for (Map.Entry<UUID, Integer> entry : sorted) {
                if (rank > 10 || line <= 1) {
                    break;
                }

                String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                if (name == null || name.isBlank()) {
                    name = entry.getKey().toString().substring(0, 8);
                }

                String row = "§f" + rank + ". §b" + name + " §7" + entry.getValue();
                if (row.length() > 40) {
                    row = row.substring(0, 40);
                }

                objective.getScore(row).setScore(line);
                line--;
                rank++;
            }
            player.setScoreboard(scoreboard);
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
