package me.ph4ntomgg.evolutionsmp.ui;

import me.ph4ntomgg.evolutionsmp.domain.EvolutionPath;
import me.ph4ntomgg.evolutionsmp.service.ProgressService;
import me.ph4ntomgg.evolutionsmp.fx.CinematicEffects;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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

    public PathSelectionMenu(JavaPlugin plugin, ProgressService progress, CinematicEffects effects) {
        this.plugin = plugin; this.progress = progress; this.effects = effects;
    }

    public void open(Player target, Player viewer) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        EvolutionPath[] paths = EvolutionPath.values();
        for (int i = 0; i < paths.length; i++) {
            EvolutionPath path = paths[i];
            Material icon = switch (path) {
                case HUNT -> Material.IRON_SWORD;
                case ALCHEMY -> Material.BREWING_STAND;
                case ENCHANTMENT -> Material.ENCHANTING_TABLE;
                case PROSPECTOR -> Material.DIAMOND_PICKAXE;
                case ENDER -> Material.ENDER_PEARL;
                case TRADE -> Material.EMERALD;
                case CALAMITY -> Material.WITHER_SKELETON_SKULL;
            };
            ItemStack item = new ItemStack(icon);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + path.displayName());
            meta.setLore(List.of(ChatColor.GRAY + "Select this path for the target player."));
            item.setItemMeta(meta);
            inv.setItem(new int[]{10,11,12,13,14,15,16}[i], item);
        }
        plugin.getServer().getPluginManager().registerEvents(new PathMenuListener(plugin, target.getUniqueId(), viewer.getUniqueId(), progress, effects), plugin);
        viewer.openInventory(inv);
    }
}
