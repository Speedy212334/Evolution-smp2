package me.ph4ntomgg.evolutionsmp.ui;

import me.ph4ntomgg.evolutionsmp.domain.EvolutionTier;
import me.ph4ntomgg.evolutionsmp.item.EvolutionItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class RecipeMenu {
    public static final String TITLE = ChatColor.DARK_PURPLE + "Evolution Crafting";
    private final EvolutionItems items;

    public RecipeMenu(EvolutionItems items) { this.items = items; }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, TITLE);
        inventory.setItem(11, items.orb(EvolutionTier.T2));
        inventory.setItem(13, items.orb(EvolutionTier.T3));
        inventory.setItem(15, items.orb(EvolutionTier.T4));
        inventory.setItem(22, items.shard());
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta meta = info.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Crafting information");
        meta.setLore(List.of(ChatColor.GRAY + "Select an item to open a crafting table.", ChatColor.GRAY + "T4 does not require a previous tier orb."));
        info.setItemMeta(meta);
        inventory.setItem(4, info);
        player.openInventory(inventory);
    }
}
