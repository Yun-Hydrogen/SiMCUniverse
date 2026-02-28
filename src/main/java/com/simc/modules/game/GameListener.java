package com.simc.modules.game;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GameListener implements Listener {
    private final GameModule module;

    public GameListener(GameModule module) {
        this.module = module;
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        module.onCreatureSpawn(event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player player = resolvePlayerDamager(event.getDamager());
        if (player == null) {
            return;
        }

        module.onEntityDamagedByPlayer(player, event.getEntity(), event.getFinalDamage());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        module.onPlayerJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        module.onPlayerQuit(event.getPlayer());
    }

    private Player resolvePlayerDamager(Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        }

        if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            if (projectile.getShooter() instanceof Player) {
                return (Player) projectile.getShooter();
            }
        }

        return null;
    }
}
