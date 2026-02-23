package com.simc.modules.shop;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.EquipmentSlot;

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

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!module.isEnabled() || !module.isQuickOpenEnabled()) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (!module.isQuickOpenTriggerItem(event.getItem())) {
            return;
        }

        event.setCancelled(true);
        module.openShop(event.getPlayer(), 1);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!module.isEnabled() || !module.isQuickOpenEnabled()) {
            return;
        }

        String tip = module.getQuickOpenJoinTip();
        if (tip != null && !tip.isBlank()) {
            event.getPlayer().sendMessage(tip);
        }
    }
}
