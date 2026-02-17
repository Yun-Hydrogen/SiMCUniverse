package com.simc.modules.random;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.block.SignChangeEvent;

public class RandomListener implements Listener {
    private final RandomModule module;

    public RandomListener(RandomModule module) {
        this.module = module;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (!module.isRandomInventory(event.getView().getTopInventory())) {
            return;
        }

        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        module.handleGuiClick(player, event.getView().getTopInventory(), event.getSlot());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        if (!module.isRandomInventory(event.getInventory())) {
            return;
        }

        Player player = (Player) event.getPlayer();
        module.cancelAnimation(player.getUniqueId());

        Bukkit.getScheduler().runTaskLater(module.getPlugin(), () -> {
            if (!player.isOnline()) {
                return;
            }
            if (!module.isRandomInventory(player.getOpenInventory().getTopInventory())) {
                module.stopCustomMusic(player);
            }
        }, 1L);
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        if (!module.hasPendingDraw(player.getUniqueId())) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(module.getPlugin(), () -> module.handleSignComplete(player), 1L);
    }

    @EventHandler
    public void onAsyncChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!module.hasPendingDraw(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        Bukkit.getScheduler().runTask(module.getPlugin(), () -> module.onChatSubmit(player));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        module.clearPlayerTempState(event.getPlayer().getUniqueId());
        module.stopCustomMusic(event.getPlayer());
    }

    @EventHandler
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        module.handleResourcePackStatus(event.getPlayer(), event.getStatus().name());
    }
}
