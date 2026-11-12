package me.ph4ntomgg.evolutionsmp.gameplay;

import me.ph4ntomgg.evolutionsmp.domain.EvolutionPath;
import me.ph4ntomgg.evolutionsmp.domain.PlayerProgress;
import me.ph4ntomgg.evolutionsmp.service.ProgressService;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class PathPassiveListener implements Listener {
    private final JavaPlugin plugin;
    private final ProgressService progress;
    private final BukkitTask refreshTask;
    private final NamespacedKey originalEfficiencyKey;
    private final Map<UUID, Set<PotionEffectType>> managedEffects = new HashMap<>();

    public PathPassiveListener(JavaPlugin plugin, ProgressService progress) {
        this.plugin = plugin;
        this.progress = progress;
        this.originalEfficiencyKey = new NamespacedKey(plugin, "prospector_original_efficiency");
        this.refreshTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::refreshAll, 20L, 40L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        refresh(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        clearManagedEffects(player);
        restoreInventoryEfficiency(player);
    }

    private void refreshAll() {
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) refresh(player);
    }

    public void refreshPlayer(Player player) {
        if (player != null && player.isOnline()) refresh(player);
    }

    private void refresh(Player player) {
        UUID id = player.getUniqueId();
        PlayerProgress p = progress.get(id);
        Set<PotionEffectType> desired = new HashSet<>();
        if (p.assigned()) {
            if (p.path() == EvolutionPath.HUNT) desired.add(PotionEffectType.SPEED);
            if (p.path() == EvolutionPath.CALAMITY && p.tier().rank() >= 3) desired.add(PotionEffectType.STRENGTH);
            if (p.path() == EvolutionPath.TRADE && p.tier().rank() >= 4) {
                desired.add(PotionEffectType.HERO_OF_THE_VILLAGE);
                desired.add(PotionEffectType.LUCK);
            }
        }

        Set<PotionEffectType> managed = managedEffects.get(id);
        if (managed != null) {
            for (PotionEffectType type : new HashSet<>(managed)) {
                if (!desired.contains(type)) player.removePotionEffect(type);
            }
            managed.retainAll(desired);
            if (managed.isEmpty()) managedEffects.remove(id);
        }

        if (p.assigned() && p.path() == EvolutionPath.HUNT) {
            maintain(player, PotionEffectType.SPEED, p.tier().rank() >= 3 ? 1 : 0);
        }
        if (p.assigned() && p.path() == EvolutionPath.CALAMITY && p.tier().rank() >= 3) {
            maintain(player, PotionEffectType.STRENGTH, 0);
        }
        if (p.assigned() && p.path() == EvolutionPath.TRADE && p.tier().rank() >= 4) {
            maintain(player, PotionEffectType.HERO_OF_THE_VILLAGE, 1);
            maintain(player, PotionEffectType.LUCK, 1);
        }

        if (p.assigned() && p.path() == EvolutionPath.PROSPECTOR && p.tier().rank() >= 4) {
            applyProspectorEfficiency(player);
        } else {
            restoreInventoryEfficiency(player);
        }
    }

    private void maintain(Player player, PotionEffectType type, int amplifier) {
        PotionEffect current = player.getPotionEffect(type);
        boolean managed = managedEffects.getOrDefault(player.getUniqueId(), Set.of()).contains(type);
        if (current == null || current.getAmplifier() < amplifier
                || current.getAmplifier() == amplifier && managed && current.getDuration() < 60) {
            player.addPotionEffect(new PotionEffect(type, 100, amplifier, true, false, false), true);
            managedEffects.computeIfAbsent(player.getUniqueId(), ignored -> new HashSet<>()).add(type);
        }
    }

    private void applyProspectorEfficiency(Player player) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item == null || item.getType().isAir() || !item.getType().name().endsWith("_PICKAXE")) continue;
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;
            var data = meta.getPersistentDataContainer();
            if (!data.has(originalEfficiencyKey, PersistentDataType.INTEGER)) {
                data.set(originalEfficiencyKey, PersistentDataType.INTEGER,
                        meta.getEnchantLevel(Enchantment.EFFICIENCY));
            }
            if (meta.getEnchantLevel(Enchantment.EFFICIENCY) != 10) {
                meta.addEnchant(Enchantment.EFFICIENCY, 10, true);
            }
            item.setItemMeta(meta);
        }
    }

    private boolean restoreEfficiencyItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        var data = meta.getPersistentDataContainer();
        Integer original = data.get(originalEfficiencyKey, PersistentDataType.INTEGER);
        if (original == null) return false;
        meta.removeEnchant(Enchantment.EFFICIENCY);
        if (original > 0) meta.addEnchant(Enchantment.EFFICIENCY, original, true);
        data.remove(originalEfficiencyKey);
        item.setItemMeta(meta);
        return true;
    }

    private void restoreInventoryEfficiency(Player player) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (restoreEfficiencyItem(item)) player.getInventory().setItem(slot, item);
        }
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (restoreEfficiencyItem(offhand)) player.getInventory().setItemInOffHand(offhand);
    }

    private void clearManagedEffects(Player player) {
        Set<PotionEffectType> managed = managedEffects.remove(player.getUniqueId());
        if (managed != null) managed.forEach(player::removePotionEffect);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Item dropped = event.getItemDrop();
        ItemStack stack = dropped.getItemStack();
        if (restoreEfficiencyItem(stack)) dropped.setItemStack(stack);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player) || isProspectorTierFour(player)) return;
        ItemStack stack = event.getItem().getItemStack();
        if (restoreEfficiencyItem(stack)) event.getItem().setItemStack(stack);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryTransfer(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        boolean leavingPlayerInventory = event.isShiftClick()
                || event.getClickedInventory() != null && event.getClickedInventory() != player.getInventory()
                || event.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY;
        if (!leavingPlayerInventory) return;

        ItemStack current = event.getCurrentItem();
        if (restoreEfficiencyItem(current)) event.setCurrentItem(current);
        ItemStack cursor = event.getCursor();
        if (restoreEfficiencyItem(cursor)) event.setCursor(cursor);
        if (event.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY && event.getHotbarButton() >= 0) {
            int slot = event.getHotbarButton();
            ItemStack hotbar = player.getInventory().getItem(slot);
            if (restoreEfficiencyItem(hotbar)) player.getInventory().setItem(slot, hotbar);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        boolean entersTopInventory = event.getRawSlots().stream()
                .anyMatch(rawSlot -> rawSlot < event.getView().getTopInventory().getSize());
        if (!entersTopInventory) return;
        ItemStack cursor = event.getOldCursor();
        if (isManagedEfficiencyItem(cursor)) event.setCancelled(true);
    }

    private boolean isManagedEfficiencyItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(originalEfficiencyKey, PersistentDataType.INTEGER);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        for (ItemStack drop : event.getDrops()) restoreEfficiencyItem(drop);
        // Also handle keep-inventory deaths, where the enchanted tools are not dropped.
        restoreInventoryEfficiency(event.getEntity());
    }

    private boolean isProspectorTierFour(Player player) {
        PlayerProgress p = progress.get(player.getUniqueId());
        return p.assigned() && p.path() == EvolutionPath.PROSPECTOR && p.tier().rank() >= 4;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void damageReduction(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerProgress p = progress.get(player.getUniqueId());
        if (p.path() == EvolutionPath.HUNT && p.tier().rank() >= 2
                && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setDamage(event.getDamage() * 0.45);
        }
        if (p.path() == EvolutionPath.CALAMITY && p.tier().rank() >= 2) {
            switch (event.getCause()) {
                case FIRE, FIRE_TICK, LAVA, BLOCK_EXPLOSION, ENTITY_EXPLOSION, SONIC_BOOM ->
                        event.setDamage(event.getDamage() * 0.8);
                default -> { }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void calamityToll(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || event.getRegainReason() != EntityRegainHealthEvent.RegainReason.SATIATED) return;
        PlayerProgress p = progress.get(player.getUniqueId());
        if (p.path() == EvolutionPath.CALAMITY && p.tier().rank() >= 1) {
            event.setAmount(event.getAmount() * 0.9);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void autoSmelt(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        PlayerProgress p = progress.get(player.getUniqueId());
        if (p.path() != EvolutionPath.PROSPECTOR || p.tier().rank() < 2
                || player.getInventory().getItemInMainHand().containsEnchantment(Enchantment.SILK_TOUCH)) return;
        boolean ore = event.getBlockState().getType().name().contains("ORE")
                || event.getBlockState().getType() == Material.ANCIENT_DEBRIS;
        double chance = switch (p.tier().rank()) { case 1 -> 0.10; case 2 -> 0.20; case 3 -> 0.30; default -> 0.50; };
        for (Item dropped : event.getItems()) {
            Material replacement = switch (dropped.getItemStack().getType()) {
                case RAW_IRON -> Material.IRON_INGOT;
                case RAW_GOLD -> Material.GOLD_INGOT;
                case RAW_COPPER -> Material.COPPER_INGOT;
                default -> null;
            };
            if (replacement != null) dropped.getItemStack().setType(replacement);
            if (ore && ThreadLocalRandom.current().nextDouble() < chance) {
                dropped.getItemStack().setAmount(dropped.getItemStack().getAmount() + (p.tier().rank() >= 4 ? 2 : 1));
            }
        }
    }

    public void shutdown() {
        refreshTask.cancel();
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            clearManagedEffects(player);
            restoreInventoryEfficiency(player);
        }
    }
}
