package com.simc.modules.shop;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class ShopListener implements Listener {
    private final ShopModule module;

    public ShopListener(ShopModule module) {
        this.module = module;
    }

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (!module.isEnabled() || !module.isAliasShopEnabled()) {
            return;
        }

        String msg = event.getMessage();
        String lower = msg.toLowerCase();
        if (lower.equals("/shop") || lower.startsWith("/shop ")) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            module.processAliasCommand(player, msg);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (!module.isShopInventory(event.getView().getTopInventory())) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        int page = module.getOpenInventoryPage(event.getView().getTopInventory());
        module.handleInventoryClick(player, page, event.getSlot());
    }
}
