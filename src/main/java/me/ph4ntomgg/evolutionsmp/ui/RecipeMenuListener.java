package me.ph4ntomgg.evolutionsmp.ui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class RecipeMenuListener implements Listener {
    @EventHandler
    public void click(InventoryClickEvent event) {
        if (!RecipeMenu.TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getRawSlot();
        if (slot == 11 || slot == 13 || slot == 15 || slot == 22) {
            player.closeInventory();
            player.openWorkbench(null, true);
        }
    }

    @EventHandler
    public void drag(InventoryDragEvent event) {
        if (RecipeMenu.TITLE.equals(event.getView().getTitle())) event.setCancelled(true);
    }
}
