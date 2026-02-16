package com.simc;

import org.bukkit.plugin.java.JavaPlugin;
import com.simc.managers.PluginManager;
import com.simc.listeners.PlayerListener;

public class SiMCUniverse extends JavaPlugin {
    
    @Override
    public void onEnable() {
        
        // Initialize managers
        PluginManager.getInstance().initialize();
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        
        getLogger().info("SiMCUniverse has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("SiMCUniverse has been disabled!");
    }
    
}