package com.simc.managers;

public class PluginManager {
    private static PluginManager instance;
    
    public static PluginManager getInstance() {
        if (instance == null) {
            instance = new PluginManager();
        }
        return instance;
    }
    
    public void initialize() {
        // Initialize your managers here
    }
}