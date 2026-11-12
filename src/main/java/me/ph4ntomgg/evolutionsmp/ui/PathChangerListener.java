package me.ph4ntomgg.evolutionsmp.ui;

import me.ph4ntomgg.evolutionsmp.item.EvolutionItems;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataType;

public final class PathChangerListener implements Listener {
    private final EvolutionItems items;
    private final PathSelectionMenu menu;

    public PathChangerListener(EvolutionItems items, PathSelectionMenu menu) {
        this.items = items;
        this.menu = menu;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        // Prefer main hand when both hands hold a Path Changer, but still support
        // an offhand-only Path Changer without opening the menu twice.
        if (event.getHand() == EquipmentSlot.OFF_HAND
                && isPathChanger(event.getPlayer().getInventory().getItemInMainHand())) return;
        if (!isPathChanger(event.getItem())) return;
        event.setCancelled(true);
        menu.open(event.getPlayer(), event.getPlayer(), true);
    }

    private boolean isPathChanger(org.bukkit.inventory.ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        String id = item.getItemMeta().getPersistentDataContainer()
                .get(items.itemIdKey(), PersistentDataType.STRING);
        return "path_changer".equals(id);
    }
}
