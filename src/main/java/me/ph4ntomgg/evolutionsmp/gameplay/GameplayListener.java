package me.ph4ntomgg.evolutionsmp.gameplay;

import me.ph4ntomgg.evolutionsmp.domain.*;
import me.ph4ntomgg.evolutionsmp.service.ProgressService;
import me.ph4ntomgg.evolutionsmp.item.EvolutionItems;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.Set;

public final class GameplayListener implements Listener {
    private static final Set<Material> ORES = EnumSet.of(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE, Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE, Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE, Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE, Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE, Material.ANCIENT_DEBRIS
    );
    private final ProgressService progress;
    private final NamespacedKey orbXp;
    private final EvolutionItems items;

    public GameplayListener(JavaPlugin plugin, ProgressService progress) {
        this.progress = progress;
        this.items = new EvolutionItems(plugin);
        this.orbXp = items.orbXpKey();
    }

    @EventHandler
    public void onOreBreak(BlockBreakEvent event) {
        if (ORES.contains(event.getBlock().getType())) award(event.getPlayer(), EvolutionPath.PROSPECTOR, 8);
    }

    @EventHandler
    public void onMobKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null || event.getEntityType() == EntityType.PLAYER) return;
        if (event.getEntityType() == EntityType.ENDER_DRAGON || event.getEntityType() == EntityType.WITHER) {
            award(killer, EvolutionPath.HUNT, 40);
        } else if (event.getEntityType().isAlive()) {
            award(killer, EvolutionPath.HUNT, 12);
        }
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        award(event.getEnchanter(), EvolutionPath.ENCHANTMENT, Math.max(5, event.getExpLevelCost() * 3L));
    }

    @EventHandler
    public void onPotionConsume(PlayerItemConsumeEvent event) {
        Material type = event.getItem().getType();
        if (type == Material.POTION || type == Material.SPLASH_POTION || type == Material.LINGERING_POTION) {
            award(event.getPlayer(), EvolutionPath.ALCHEMY, 10);
        }
    }

    @EventHandler
    public void onEnderPearlTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            award(event.getPlayer(), EvolutionPath.ENDER, 8);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && event.getFinalDamage() > 0) {
            award(player, EvolutionPath.CALAMITY, 2);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        PlayerProgress state = progress.get(player.getUniqueId());
        if (!state.assigned() || state.apex() || state.tier().rank() < 2) return;
        long value = switch (state.tier()) {
            case T2 -> 250;
            case T3 -> 600;
            case T4 -> 1_200;
            default -> 0;
        };
        if (value > 0) {
            ItemStack orb = items.orb(state.tier());
            event.getDrops().add(orb);
        }
        progress.setTier(player.getUniqueId(), EvolutionTier.T1);
        player.sendMessage(ChatColor.RED + "Your evolution has regressed to T1.");
    }

    @EventHandler
    public void onOrbPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack stack = event.getItem().getItemStack();
        if (!stack.hasItemMeta()) return;
        Long value = stack.getItemMeta().getPersistentDataContainer().get(orbXp, PersistentDataType.LONG);
        if (value == null) return;
        PlayerProgress state = progress.get(player.getUniqueId());
        if (!state.assigned()) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You need an evolution path before collecting an Evolution Orb.");
            return;
        }
        progress.award(player.getUniqueId(), value);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Evolution Orb absorbed: +" + value + " XP");
    }

    private void award(Player player, EvolutionPath expectedPath, long amount) {
        PlayerProgress state = progress.get(player.getUniqueId());
        if (state.path() == expectedPath && !state.apex()) progress.award(player.getUniqueId(), amount);
    }
}
