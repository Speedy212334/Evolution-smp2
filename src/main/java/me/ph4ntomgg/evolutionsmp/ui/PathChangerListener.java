package me.ph4ntomgg.evolutionsmp.ui;

import me.ph4ntomgg.evolutionsmp.item.EvolutionItems;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
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
        if (event.getItem() == null || !event.getItem().hasItemMeta()) return;
        String id = event.getItem().getItemMeta().getPersistentDataContainer()
                .get(items.itemIdKey(), PersistentDataType.STRING);
        if (!"path_changer".equals(id)) return;
        event.setCancelled(true);
        menu.open(event.getPlayer(), event.getPlayer(), true);
    }
}
