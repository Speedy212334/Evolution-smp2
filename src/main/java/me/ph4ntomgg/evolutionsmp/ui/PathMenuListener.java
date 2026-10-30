package me.ph4ntomgg.evolutionsmp.ui;

import me.ph4ntomgg.evolutionsmp.domain.EvolutionPath;
import me.ph4ntomgg.evolutionsmp.service.ProgressService;
import me.ph4ntomgg.evolutionsmp.fx.CinematicEffects;
import me.ph4ntomgg.evolutionsmp.item.EvolutionItems;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public final class PathMenuListener implements Listener {
    private final JavaPlugin plugin;
    private final UUID target;
    private final UUID viewer;
    private final ProgressService progress;
    private final CinematicEffects effects;
    private final boolean consumeToken;

    public PathMenuListener(JavaPlugin plugin, UUID target, UUID viewer, ProgressService progress, CinematicEffects effects, boolean consumeToken) {
        this.plugin = plugin; this.target = target; this.viewer = viewer; this.progress = progress; this.effects = effects; this.consumeToken = consumeToken;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!PathSelectionMenu.TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || !player.getUniqueId().equals(viewer)) return;
        int slot = event.getRawSlot();
        if (slot < 10 || slot > 16) return;
        EvolutionPath[] paths = EvolutionPath.values();
        int index = slot - 10;
        if (index >= paths.length) return;
        if (consumeToken && !consumePathChanger(player)) { player.sendMessage(ChatColor.RED + "The path changer is missing."); player.closeInventory(); return; }
        PlayerProgressAssign.assign(plugin, target, player, paths[index], progress, effects);
        player.closeInventory();
        org.bukkit.event.HandlerList.unregisterAll(this);
    }

    private boolean consumePathChanger(Player player) {
        var key = new EvolutionItems(plugin).itemIdKey();
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || !item.hasItemMeta()) continue;
            String id = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
            if (!"path_changer".equals(id)) continue;
            if (item.getAmount() <= 1) player.getInventory().setItem(i, null);
            else item.setAmount(item.getAmount() - 1);
            return true;
        }
        return false;
    }
}
