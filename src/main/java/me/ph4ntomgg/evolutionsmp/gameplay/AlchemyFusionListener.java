package me.ph4ntomgg.evolutionsmp.gameplay;

import me.ph4ntomgg.evolutionsmp.domain.EvolutionPath;
import me.ph4ntomgg.evolutionsmp.domain.PlayerProgress;
import me.ph4ntomgg.evolutionsmp.service.ProgressService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Alchemy's potion fusion system. Tier III+ Alchemists can fuse two ordinary
 * drinkable potions into one potion carrying the union of their effects.
 */
public final class AlchemyFusionListener implements Listener {
    private final ProgressService progress;
    private final NamespacedKey fusedKey;

    public AlchemyFusionListener(JavaPlugin plugin, ProgressService progress) {
        this.progress = progress;
        this.fusedKey = new NamespacedKey(plugin, "alchemy_fused_potion");
    }

    @EventHandler
    public void prepare(PrepareItemCraftEvent event) {
        CraftingInventory inventory = event.getInventory();
        ItemStack[] matrix = inventory.getMatrix();
        List<ItemStack> ingredients = new ArrayList<>();
        boolean hasOtherIngredient = false;
        for (ItemStack item : matrix) {
            if (item == null || item.getType().isAir()) continue;
            if (item.getType() != Material.POTION || !(item.getItemMeta() instanceof PotionMeta)) {
                hasOtherIngredient = true;
                break;
            }
            ingredients.add(item);
        }
        // Leave every ordinary crafting recipe untouched.
        if (hasOtherIngredient || ingredients.size() != 2) return;
        inventory.setResult(createFusion(ingredients.get(0), ingredients.get(1)));
    }

    @EventHandler
    public void craft(InventoryClickEvent event) {
        if (event.getRawSlot() != 0 || !(event.getView().getTopInventory() instanceof CraftingInventory)) return;
        ItemStack result = event.getCurrentItem();
        if (result == null || !result.hasItemMeta()
                || !result.getItemMeta().getPersistentDataContainer().has(fusedKey, PersistentDataType.BYTE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClick().isShiftClick() || event.getCursor() != null && !event.getCursor().getType().isAir()) {
            player.sendMessage(ChatColor.YELLOW + "Take the fusion potion with an empty cursor; shift-click is disabled.");
            return;
        }
        PlayerProgress state = progress.get(player.getUniqueId());
        if (state.path() != EvolutionPath.ALCHEMY || state.tier().rank() < 3) {
            player.sendMessage(ChatColor.RED + "Potion Fusion requires Alchemy Tier III or higher.");
            return;
        }
        CraftingInventory crafting = (CraftingInventory) event.getView().getTopInventory();
        ItemStack[] matrix = crafting.getMatrix();
        int count = 0;
        for (ItemStack ingredient : matrix) {
            if (ingredient != null && !ingredient.getType().isAir()) {
                if (ingredient.getType() != Material.POTION) return;
                count++;
            }
        }
        if (count != 2) return;
        crafting.setResult(null);
        for (int i = 0; i < matrix.length; i++) {
            ItemStack ingredient = matrix[i];
            if (ingredient == null || ingredient.getType().isAir()) continue;
            if (ingredient.getAmount() > 1) ingredient.setAmount(ingredient.getAmount() - 1);
            else matrix[i] = null;
        }
        crafting.setMatrix(matrix);
        ItemStack fused = result.clone();
        fused.setAmount(1);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(fused);
        overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        player.updateInventory();
    }

    private ItemStack createFusion(ItemStack first, ItemStack second) {
        PotionMeta a = (PotionMeta) first.getItemMeta();
        PotionMeta b = (PotionMeta) second.getItemMeta();
        Map<String, PotionEffect> merged = new HashMap<>();
        for (PotionEffect effect : a.getAllEffects()) merge(merged, effect);
        for (PotionEffect effect : b.getAllEffects()) merge(merged, effect);
        if (merged.isEmpty()) return null;

        ItemStack output = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) output.getItemMeta();
        meta.setBasePotionType(PotionType.AWKWARD);
        for (PotionEffect effect : merged.values()) meta.addCustomEffect(effect, true);
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Alchemy Fusion");
        meta.setLore(List.of(
                ChatColor.GRAY + "Two brews. One vessel.",
                ChatColor.DARK_PURPLE + "Fused potion effects",
                ChatColor.GRAY + "Created by an Alchemist"
        ));
        meta.getPersistentDataContainer().set(fusedKey, PersistentDataType.BYTE, (byte) 1);
        output.setItemMeta(meta);
        return output;
    }

    private void merge(Map<String, PotionEffect> effects, PotionEffect candidate) {
        String key = candidate.getType().getKey().toString();
        PotionEffect current = effects.get(key);
        if (current == null || candidate.getAmplifier() > current.getAmplifier()
                || (candidate.getAmplifier() == current.getAmplifier()
                && candidate.getDuration() > current.getDuration())) {
            effects.put(key, candidate);
        }
    }
}
