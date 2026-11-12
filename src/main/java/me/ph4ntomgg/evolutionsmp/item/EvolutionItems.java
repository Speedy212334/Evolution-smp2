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
        String model = switch (tier) { case T2 -> "orb_t2"; case T3 -> "orb_t3"; case T4 -> "orb_t4"; default -> throw new IllegalArgumentException("Unsupported orb tier"); };
        Material carrier = switch (tier) { case T2 -> Material.ECHO_SHARD; case T3 -> Material.AMETHYST_SHARD; case T4 -> Material.NETHER_STAR; default -> Material.AIR; };
        ItemStack item = new ItemStack(carrier);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Evolution Orb " + ChatColor.GRAY + "• " + tier.name());
        meta.setLore(List.of(ChatColor.GRAY + "A remnant of lost evolution.", ChatColor.YELLOW + "Value: " + xp + " XP"));
        meta.setItemModel(new NamespacedKey(plugin, model));
        meta.getPersistentDataContainer().set(itemId, PersistentDataType.STRING, "orb_" + tier.name().toLowerCase());
        meta.getPersistentDataContainer().set(orbXp, PersistentDataType.LONG, xp);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack shard() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Evolution Shard");
        meta.setLore(List.of(ChatColor.GRAY + "A concentrated fragment of evolution.", ChatColor.DARK_GRAY + "Used in advanced crafting."));
        meta.setItemModel(new NamespacedKey(plugin, "evolution_shard"));
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
        for (int i = 0; i < ingredients.length; i += 2) recipe.setIngredient((Character) ingredients[i], org.bukkit.inventory.RecipeChoice.exactChoice((ItemStack) ingredients[i + 1]));
        plugin.getServer().addRecipe(recipe);
    }


    public ItemStack fxGem() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setItemModel(new NamespacedKey(plugin, "fx_gem"));
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack pathIcon(me.ph4ntomgg.evolutionsmp.domain.EvolutionPath path) {
        int model = switch (path) {
            case HUNT -> 7901;
            case ALCHEMY -> 7902;
            case ENCHANTMENT -> 7903;
            case PROSPECTOR -> 7904;
            case ENDER -> 7905;
            case TRADE -> 7906;
            case CALAMITY -> 7907;
        };
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + path.displayName());
        meta.setLore(List.of(ChatColor.GRAY + "Select this evolution path."));
        meta.setItemModel(new NamespacedKey(plugin, "path_" + path.name().toLowerCase(java.util.Locale.ROOT)));
        meta.getPersistentDataContainer().set(itemId, PersistentDataType.STRING, "path_" + path.name().toLowerCase());
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack pathChanger() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Evolution Path Changer");
        meta.setLore(List.of(ChatColor.GRAY + "Right-click to choose a new path."));
        meta.setItemModel(new NamespacedKey(plugin, "path_changer"));
        meta.getPersistentDataContainer().set(itemId, PersistentDataType.STRING, "path_changer");
        item.setItemMeta(meta);
        return item;
    }


    /** Migrates legacy custom-model-data items to namespaced item-model components. */
    public void migrateInventory(org.bukkit.entity.Player player) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (applyNamespacedModel(item)) player.getInventory().setItem(slot, item);
        }
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (applyNamespacedModel(offhand)) player.getInventory().setItemInOffHand(offhand);
    }

    private boolean applyNamespacedModel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        String id = meta.getPersistentDataContainer().get(itemId, PersistentDataType.STRING);
        if (id == null) return false;
        boolean known = id.equals("orb_t2") || id.equals("orb_t3") || id.equals("orb_t4")
                || id.equals("evolution_shard") || id.equals("path_changer")
                || id.equals("path_hunt") || id.equals("path_alchemy") || id.equals("path_enchantment")
                || id.equals("path_prospector") || id.equals("path_ender") || id.equals("path_trade")
                || id.equals("path_calamity");
        if (!known) return false;
        meta.setItemModel(new NamespacedKey(plugin, id));
        item.setItemMeta(meta);
        return true;
    }

    public boolean isEvolutionShard(ItemStack item) {
        return item != null && item.getType() == Material.ECHO_SHARD && item.hasItemMeta()
                && "evolution_shard".equals(item.getItemMeta().getPersistentDataContainer()
                        .get(itemId, PersistentDataType.STRING));
    }

    public NamespacedKey orbXpKey() { return orbXp; }
    public NamespacedKey itemIdKey() { return itemId; }
}
