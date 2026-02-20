package com.simc.listeners;

import com.simc.SiMCUniverse;
import com.simc.utils.Utils;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

public class PlayerListener implements Listener {
    private final SiMCUniverse plugin;

    public PlayerListener(SiMCUniverse plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (plugin == null || !plugin.getConfig().getBoolean("join-tip.enabled", true)) {
            return;
        }

        List<String> lines = plugin.getConfig().getStringList("join-tip.messages");
        if (lines == null || lines.isEmpty()) {
            return;
        }

        for (String line : lines) {
            if (line != null && !line.isBlank()) {
                event.getPlayer().sendMessage(Utils.colorize(line));
            }
        }
    }
}