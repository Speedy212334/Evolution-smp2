package me.ph4ntomgg.evolutionsmp.ui;

import me.ph4ntomgg.evolutionsmp.domain.EvolutionPath;
import me.ph4ntomgg.evolutionsmp.service.ProgressService;
import me.ph4ntomgg.evolutionsmp.fx.CinematicEffects;
import me.ph4ntomgg.evolutionsmp.item.EvolutionItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

public final class PathSelectionMenu {
    public static final String TITLE = ChatColor.DARK_PURPLE + "Choose Your Evolution Path";
    private final JavaPlugin plugin;
    private final ProgressService progress;
    private final CinematicEffects effects;
    private final PathMenuListener listener;

    public PathSelectionMenu(JavaPlugin plugin, ProgressService progress, CinematicEffects effects) {
        this.plugin = plugin; this.progress = progress; this.effects = effects;
        this.listener = new PathMenuListener(plugin, progress, effects);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }

    public void open(Player target, Player viewer) { open(target, viewer, false); }

    public void open(Player target, Player viewer, boolean consumeToken) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        EvolutionPath[] paths = EvolutionPath.values();
        for (int i = 0; i < paths.length; i++) {
            EvolutionPath path = paths[i];
            ItemStack item = new EvolutionItems(plugin).pathIcon(path);
            ItemMeta meta = item.getItemMeta();
            meta.setLore(List.of(ChatColor.GRAY + "Select this path for the target player."));
            item.setItemMeta(meta);
            inv.setItem(new int[]{10,11,12,13,14,15,16}[i], item);
        }
        listener.track(target, viewer, consumeToken);
        viewer.openInventory(inv);
    }
}
