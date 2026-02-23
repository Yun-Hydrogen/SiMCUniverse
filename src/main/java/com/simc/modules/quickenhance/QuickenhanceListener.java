package com.simc.modules.quickenhance;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class QuickenhanceListener implements Listener {
    private final QuickenhanceModule module;

    public QuickenhanceListener(QuickenhanceModule module) {
        this.module = module;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        ItemStack stack = event.getItem().getItemStack();
        if (stack == null || stack.getType() != Material.ENCHANTED_BOOK) {
            return;
        }
        module.notifyPickup((Player) event.getEntity());
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (event.getClick() != ClickType.RIGHT) {
            return;
        }

        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getWhoClicked().getInventory()) {
            return;
        }

        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        if (cursor == null || cursor.getType() != Material.ENCHANTED_BOOK) {
            return;
        }

        if (current == null || current.getType() == Material.AIR) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        boolean applied = module.applyBook(player, cursor, current);
        if (!applied) {
            return;
        }

        event.setCancelled(true);

        int amount = cursor.getAmount();
        if (amount <= 1) {
            player.setItemOnCursor(new ItemStack(Material.AIR));
        } else {
            cursor.setAmount(amount - 1);
            player.setItemOnCursor(cursor);
        }

        event.setCurrentItem(current);
    }
}
