package com.simc.managers;

import com.simc.SiMCUniverse;
import com.simc.listeners.PlayerListener;
import com.simc.modules.killscore.KillScoreModule;
import com.simc.modules.livescore.LiveScoreModule;

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
}