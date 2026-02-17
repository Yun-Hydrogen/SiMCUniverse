package com.simc.managers;

import com.simc.SiMCUniverse;
import com.simc.listeners.PlayerListener;
import com.simc.modules.killscore.KillScoreModule;
import com.simc.modules.livescore.LiveScoreModule;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class PluginManager {
    private final SiMCUniverse plugin;
    private KillScoreModule killScoreModule;
    private LiveScoreModule liveScoreModule;

    public PluginManager(SiMCUniverse plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        registerListeners();
        initializeModules();
        plugin.getLogger().info("PluginManager initialized.");
    }

    public void shutdown() {
        if (killScoreModule != null) {
            killScoreModule.shutdown();
        }
        if (liveScoreModule != null) {
            liveScoreModule.shutdown();
        }
        plugin.getLogger().info("PluginManager shutdown completed.");
    }

    private void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(new PlayerListener(), plugin);
    }

    private void initializeModules() {
        killScoreModule = new KillScoreModule(plugin);
        killScoreModule.initialize();

        liveScoreModule = new LiveScoreModule(plugin);
        liveScoreModule.initialize();
    }

    public KillScoreModule getKillScoreModule() {
        return killScoreModule;
    }

    public LiveScoreModule getLiveScoreModule() {
        return liveScoreModule;
    }

    public Map<String, Boolean> listModuleStatus() {
        Map<String, Boolean> result = new LinkedHashMap<>();
        result.put("killscore", killScoreModule != null && killScoreModule.isEnabled());
        result.put("livescore", liveScoreModule != null && liveScoreModule.isEnabled());
        return result;
    }

    public boolean enableModule(String moduleName) {
        String key = normalize(moduleName);
        if ("killscore".equals(key) && killScoreModule != null) {
            killScoreModule.enable();
            return true;
        }
        if ("livescore".equals(key) && liveScoreModule != null) {
            liveScoreModule.enable();
            return true;
        }
        return false;
    }

    public boolean disableModule(String moduleName) {
        String key = normalize(moduleName);
        if ("killscore".equals(key) && killScoreModule != null) {
            killScoreModule.disable();
            return true;
        }
        if ("livescore".equals(key) && liveScoreModule != null) {
            liveScoreModule.disable();
            return true;
        }
        return false;
    }

    public boolean reloadModule(String moduleName) {
        String key = normalize(moduleName);
        if ("killscore".equals(key) && killScoreModule != null) {
            killScoreModule.reload();
            return true;
        }
        if ("livescore".equals(key) && liveScoreModule != null) {
            liveScoreModule.reload();
            return true;
        }
        return false;
    }

    public void reloadPluginSelf() {
        plugin.reloadConfig();
        if (killScoreModule != null) {
            killScoreModule.reload();
        }
        if (liveScoreModule != null) {
            liveScoreModule.reload();
        }
    }

    private String normalize(String moduleName) {
        return moduleName == null ? "" : moduleName.toLowerCase(Locale.ROOT).trim();
    }
}