package me.ph4ntomgg.evolutionsmp.ui;

import me.ph4ntomgg.evolutionsmp.domain.PlayerProgress;
import me.ph4ntomgg.evolutionsmp.service.ProgressService;
import me.ph4ntomgg.evolutionsmp.item.EvolutionItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class EvolutionMainMenu {
    public static final String TITLE = ChatColor.DARK_PURPLE + "EvolutionSMP";
    private final ProgressService progress;
    private final EvolutionItems items;

    public EvolutionMainMenu(ProgressService progress, EvolutionItems items) {
        this.progress = progress; this.items = items;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, TITLE);
        put(inv, 10, Material.COMPASS, "Path Selection", "Admin selection or use a Path Changer item.");
        put(inv, 12, Material.NETHER_STAR, "Evolution", "View your current tier and progression.");
        put(inv, 14, Material.BLAZE_POWDER, "Abilities", "View your path abilities and Apex powers.");
        put(inv, 16, Material.BOOK, "Recipes", "Browse Evolution Orbs and Shards.");
        put(inv, 28, Material.PAPER, "Statistics", "View your current XP and next tier threshold.");
        put(inv, 30, Material.KNOWLEDGE_BOOK, "Guide", "Open the curated Evolution guide.");
        put(inv, 32, Material.END_CRYSTAL, "Cinematic Reveal", "Replay your seven-sigil path reveal.");
        put(inv, 34, Material.REDSTONE, "Admin Tools", "Administration commands and path assignment.");
        player.openInventory(inv);
    }

    private void put(Inventory inv, int slot, Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + name);
        meta.setLore(List.of(ChatColor.GRAY + lore));
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    public PlayerProgress status(Player player) {
        return progress.get(player.getUniqueId());
    }

    public EvolutionItems items() { return items; }
}
