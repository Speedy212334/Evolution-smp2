package me.ph4ntomgg.evolutionsmp.item;

import me.ph4ntomgg.evolutionsmp.domain.EvolutionTier;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class EvolutionItems {
    private final JavaPlugin plugin;
    private final NamespacedKey itemId;
    private final NamespacedKey orbXp;

    public EvolutionItems(JavaPlugin plugin) {
        this.plugin = plugin;
        this.itemId = new NamespacedKey(plugin, "evolution_item");
        this.orbXp = new NamespacedKey(plugin, "evolution_orb_xp");
    }

    public ItemStack orb(EvolutionTier tier) {
        if (tier == EvolutionTier.T1 || tier == EvolutionTier.APEX) throw new IllegalArgumentException("Only T2-T4 have normal evolution orbs");
        long xp = switch (tier) { case T2 -> 250L; case T3 -> 600L; case T4 -> 1_200L; default -> 0L; };
        int model = switch (tier) { case T2 -> 8102; case T3 -> 8103; case T4 -> 8104; default -> 0; };
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Evolution Orb " + ChatColor.GRAY + "• " + tier.name());
        meta.setLore(List.of(ChatColor.GRAY + "A remnant of lost evolution.", ChatColor.YELLOW + "Value: " + xp + " XP"));
        meta.setCustomModelData(model);
        meta.getPersistentDataContainer().set(itemId, PersistentDataType.STRING, "orb_" + tier.name().toLowerCase());
        meta.getPersistentDataContainer().set(orbXp, PersistentDataType.LONG, xp);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack shard() {
        ItemStack item = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Evolution Shard");
        meta.setLore(List.of(ChatColor.GRAY + "A concentrated fragment of evolution.", ChatColor.DARK_GRAY + "Used in advanced crafting."));
        meta.setCustomModelData(8110);
        meta.getPersistentDataContainer().set(itemId, PersistentDataType.STRING, "evolution_shard");
        item.setItemMeta(meta);
        return item;
    }

    public void registerRecipes() {
        register("evolution_shard", shard(), new String[]{"EAE", "ADA", "EAE"},
                'E', new ItemStack(Material.ECHO_SHARD), 'A', new ItemStack(Material.AMETHYST_BLOCK),
                'D', new ItemStack(Material.DIAMOND_BLOCK));
        register("orb_t2", orb(EvolutionTier.T2), new String[]{"IDI", "DED", "IDI"},
                'I', new ItemStack(Material.IRON_BLOCK), 'D', new ItemStack(Material.DIAMOND_BLOCK),
                'E', new ItemStack(Material.EMERALD_BLOCK));
        register("orb_t3", orb(EvolutionTier.T3), new String[]{"NTN", "TBT", "NTN"},
                'N', new ItemStack(Material.NETHERITE_INGOT), 'T', new ItemStack(Material.TOTEM_OF_UNDYING),
                'B', new ItemStack(Material.BEACON));
        register("orb_t4", orb(EvolutionTier.T4), new String[]{"TST", "SWS", "TST"},
                'T', new ItemStack(Material.TOTEM_OF_UNDYING), 'S', shard(),
                'W', new ItemStack(Material.NETHER_STAR));
    }

    private void register(String id, ItemStack result, String[] shape, Object... ingredients) {
        NamespacedKey key = new NamespacedKey(plugin, id);
        plugin.getServer().removeRecipe(key);
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(shape);
        for (int i = 0; i < ingredients.length; i += 2) recipe.setIngredient((Character) ingredients[i], (ItemStack) ingredients[i + 1]);
        plugin.getServer().addRecipe(recipe);
    }

    public NamespacedKey orbXpKey() { return orbXp; }
    public NamespacedKey itemIdKey() { return itemId; }
}
