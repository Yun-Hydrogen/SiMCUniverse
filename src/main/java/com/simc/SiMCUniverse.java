package com.simc;

import com.simc.commands.SimcCommand;
import com.simc.managers.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class SiMCUniverse extends JavaPlugin {
    private static SiMCUniverse instance;
    private PluginManager pluginManager;

    public static SiMCUniverse getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        pluginManager = new PluginManager(this);
        pluginManager.initialize();

        if (getCommand("simc") != null) {
            SimcCommand simcCommand = new SimcCommand(this);
            getCommand("simc").setExecutor(simcCommand);
            getCommand("simc").setTabCompleter(simcCommand);
        }

        getLogger().info("SiMCUniverse enabled. Version: " + getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        if (pluginManager != null) {
            pluginManager.shutdown();
        }
        getLogger().info("SiMCUniverse disabled.");
        instance = null;
    }

    public PluginManager getPluginManagerInstance() {
        return pluginManager;
    }
}