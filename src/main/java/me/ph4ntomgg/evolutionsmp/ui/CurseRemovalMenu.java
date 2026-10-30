package me.ph4ntomgg.evolutionsmp.ui;

import me.ph4ntomgg.evolutionsmp.domain.EvolutionPath;
import me.ph4ntomgg.evolutionsmp.domain.EvolutionTier;
import me.ph4ntomgg.evolutionsmp.service.ProgressService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

public final class CurseRemovalMenu implements Listener {
    public static final String TITLE = ChatColor.DARK_PURPLE + "Curse Removal";
    private final ProgressService progress;
    private final Map<UUID, EnumSet<Curse>> selections = new HashMap<>();

    public CurseRemovalMenu(ProgressService progress) { this.progress = progress; }

    public void open(Player player) {
        var state = progress.get(player.getUniqueId());
        if (state.path() != EvolutionPath.ENCHANTMENT || state.tier().rank() < EvolutionTier.T4.rank()) {
            player.sendMessage(ChatColor.RED + "Curse Removal requires Enchantment Tier IV.");
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) {
            player.sendMessage(ChatColor.RED + "Hold the item you want to cleanse in your main hand.");
            return;
        }
        selections.put(player.getUniqueId(), EnumSet.noneOf(Curse.class));
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        inv.setItem(13, held.clone());
        inv.setItem(11, toggle(Curse.BINDING, false));
        inv.setItem(15, toggle(Curse.VANISHING, false));
        inv.setItem(22, button(Material.LIME_CONCRETE, ChatColor.GREEN + "Confirm Removal", "Apply selected curse removals to the held item."));
        player.openInventory(inv);
    }

    @EventHandler
    public void click(InventoryClickEvent event) {
        if (!TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        EnumSet<Curse> selected = selections.get(player.getUniqueId());
        if (selected == null) return;
        int slot = event.getRawSlot();
        if (slot == 11) {
            toggle(selected, Curse.BINDING);
            event.getInventory().setItem(11, toggle(Curse.BINDING, selected.contains(Curse.BINDING)));
        } else if (slot == 15) {
            toggle(selected, Curse.VANISHING);
            event.getInventory().setItem(15, toggle(Curse.VANISHING, selected.contains(Curse.VANISHING)));
        } else if (slot == 22) {
            ItemStack held = player.getInventory().getItemInMainHand();
            if (held.getType().isAir()) {
                player.sendMessage(ChatColor.RED + "The held item is missing.");
                player.closeInventory();
                return;
            }
            ItemMeta meta = held.getItemMeta();
            boolean removed = false;
            if (selected.contains(Curse.BINDING)) removed |= meta.removeEnchant(Enchantment.BINDING_CURSE);
            if (selected.contains(Curse.VANISHING)) removed |= meta.removeEnchant(Enchantment.VANISHING_CURSE);
            if (removed) {
                held.setItemMeta(meta);
                player.sendMessage(ChatColor.GREEN + "Selected curses removed.");
            } else player.sendMessage(ChatColor.YELLOW + "No selected curse was present on the held item.");
            selections.remove(player.getUniqueId());
            player.closeInventory();
        }
    }

    @EventHandler
    public void close(InventoryCloseEvent event) {
        if (TITLE.equals(event.getView().getTitle())) selections.remove(event.getPlayer().getUniqueId());
    }

    private void toggle(EnumSet<Curse> set, Curse curse) {
        if (!set.add(curse)) set.remove(curse);
    }

    private ItemStack toggle(Curse curse, boolean enabled) {
        String label = curse == Curse.BINDING ? "Curse of Binding" : "Curse of Vanishing";
        return button(enabled ? Material.LIME_DYE : Material.GRAY_DYE,
                (enabled ? ChatColor.GREEN : ChatColor.GRAY) + label,
                enabled ? "Selected for removal." : "Click to select for removal.");
    }

    private ItemStack button(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of(ChatColor.GRAY + lore));
        item.setItemMeta(meta);
        return item;
    }

    private enum Curse { BINDING, VANISHING }
}
