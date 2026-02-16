package com.simc.modules.livescore;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class LiveScoreListener implements Listener {
    private final LiveScoreModule module;

    public LiveScoreListener(LiveScoreModule module) {
        this.module = module;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        module.handlePlayerDeath(player);
    }
}
