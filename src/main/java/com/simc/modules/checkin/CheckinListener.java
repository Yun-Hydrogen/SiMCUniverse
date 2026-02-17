package com.simc.modules.checkin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class CheckinListener implements Listener {
    private final CheckinModule module;

    public CheckinListener(CheckinModule module) {
        this.module = module;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (!(event.getView().getTopInventory().getHolder() instanceof CheckinModule.CheckinHolder)) {
            return;
        }

        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        module.handleGuiClick(player, event.getView().getTopInventory(), event.getSlot());
    }
}
