package me.ph4ntomgg.evolutionsmp.gameplay;

import me.ph4ntomgg.evolutionsmp.domain.*;
import me.ph4ntomgg.evolutionsmp.service.ProgressService;
import me.ph4ntomgg.evolutionsmp.item.EvolutionItems;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.inventory.MerchantRecipe;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.block.BrewingStartEvent;
import org.bukkit.event.inventory.BrewingStandFuelEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.block.Block;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
    private final JavaPlugin plugin;
    private final NamespacedKey orbXp;
    private final EvolutionItems items;
    private final Map<String, RecentBrewer> recentBrewers = new HashMap<>();

    public GameplayListener(JavaPlugin plugin, ProgressService progress) {
        this.progress = progress;
        this.plugin = plugin;
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
        if (killer == null) return;
        EntityType type = event.getEntityType();
        if (type == EntityType.PLAYER) {
            award(killer, EvolutionPath.HUNT, 40);
            return;
        }
        if (event.getEntity() instanceof Monster) award(killer, EvolutionPath.HUNT, 12);
        if (type == EntityType.WITHER || type == EntityType.ENDER_DRAGON
                || type == EntityType.WARDEN || type == EntityType.ELDER_GUARDIAN) {
            award(killer, EvolutionPath.CALAMITY, 120);
        }
    }

    @EventHandler
    public void recordBrewingStandUser(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null
                || event.getClickedBlock().getType() != Material.BREWING_STAND) return;
        recentBrewers.put(blockKey(event.getClickedBlock()), new RecentBrewer(event.getPlayer().getUniqueId(), System.currentTimeMillis()));
    }

    @EventHandler
    public void onBrewComplete(BrewEvent event) {
        RecentBrewer recent = recentBrewers.remove(blockKey(event.getBlock()));
        if (recent == null || System.currentTimeMillis() - recent.at() > 120_000L) return;
        Player brewer = org.bukkit.Bukkit.getPlayer(recent.playerId());
        if (brewer != null) award(brewer, EvolutionPath.ALCHEMY, 25);
    }


    @EventHandler
    public void onBrewingStart(BrewingStartEvent event) {
        RecentBrewer recent = recentBrewers.get(blockKey(event.getBlock()));
        if (recent == null || System.currentTimeMillis() - recent.at() > 120_000L) return;
        Player brewer = org.bukkit.Bukkit.getPlayer(recent.playerId());
        if (brewer == null) return;
        PlayerProgress p = progress.get(brewer.getUniqueId());
        if (p.path() != EvolutionPath.ALCHEMY) return;
        int base = event.getBrewingTime();
        int adjusted = switch (p.tier().rank()) {
            case 1 -> (int) Math.ceil(base * 0.75);
            case 2 -> (int) Math.ceil(base * 0.55);
            case 3 -> (int) Math.ceil(base * 0.35);
            default -> 1;
        };
        event.setBrewingTime(Math.max(1, adjusted));
    }

    @EventHandler
    public void onBrewingFuel(BrewingStandFuelEvent event) {
        RecentBrewer recent = recentBrewers.get(blockKey(event.getBlock()));
        if (recent == null || System.currentTimeMillis() - recent.at() > 120_000L) return;
        Player brewer = org.bukkit.Bukkit.getPlayer(recent.playerId());
        if (brewer == null) return;
        PlayerProgress p = progress.get(brewer.getUniqueId());
        if (p.path() == EvolutionPath.ALCHEMY && p.tier().rank() >= 1) {
            event.setFuelPower(Math.max(30, event.getFuelPower()));
        }
    }

    @EventHandler
    public void onHarmfulPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player) || event.getNewEffect() == null) return;
        PlayerProgress p = progress.get(player.getUniqueId());
        if (p.path() != EvolutionPath.ALCHEMY || p.tier().rank() < 2) return;
        String effect = event.getNewEffect().getType().getKey().getKey();
        if (Set.of("poison", "wither", "slowness", "weakness", "blindness", "hunger",
                "mining_fatigue", "nausea", "darkness", "levitation").contains(effect)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onTrade(PlayerTradeEvent event) {
        Player player = event.getPlayer();
        PlayerProgress p = progress.get(player.getUniqueId());
        if (p.path() != EvolutionPath.TRADE) return;
        progress.award(player.getUniqueId(), 15);
        MerchantRecipe adjusted = new MerchantRecipe(event.getTrade());
        if (p.tier().rank() >= 2) {
            int discount = p.tier().rank() >= 4 ? 2 : 1;
            adjusted.setSpecialPrice(Math.max(-5, adjusted.getSpecialPrice() - discount));
            event.setTrade(adjusted);
        }
        if (p.tier().rank() >= 3) {
            double preserveChance = p.tier().rank() >= 4 ? 0.35 : 0.20;
            if (ThreadLocalRandom.current().nextDouble() < preserveChance) event.setIncreaseTradeUses(false);
        }
    }

    @EventHandler
    public void onDimensionChange(PlayerChangedWorldEvent event) {
        if (event.getPlayer().getWorld().getEnvironment() == org.bukkit.World.Environment.THE_END) {
            award(event.getPlayer(), EvolutionPath.ENDER, 25);
        }
    }

    private String blockKey(Block block) {
        return block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private record RecentBrewer(UUID playerId, long at) { }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        Player player = event.getEnchanter();
        award(player, EvolutionPath.ENCHANTMENT, Math.max(5, event.getExpLevelCost() * 3L));
        PlayerProgress p = progress.get(player.getUniqueId());
        if (p.path() == EvolutionPath.ENCHANTMENT) {
            double chance = p.tier().rank() >= 4 ? 0.30 : p.tier().rank() >= 2 ? 0.20 : 0.10;
            if (ThreadLocalRandom.current().nextDouble() < chance) {
                player.getInventory().addItem(new ItemStack(Material.LAPIS_LAZULI, 1))
                        .values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
                player.sendMessage(ChatColor.AQUA + "Arcane Reserve refunded one lapis lazuli.");
            }
        }
    }

    @EventHandler
    public void onPotionConsume(PlayerItemConsumeEvent event) {
        Material type = event.getItem().getType();
        if (type != Material.POTION && type != Material.SPLASH_POTION && type != Material.LINGERING_POTION) return;
        Player player = event.getPlayer();
        award(player, EvolutionPath.ALCHEMY, 10);
        PlayerProgress p = progress.get(player.getUniqueId());
        if (p.path() != EvolutionPath.ALCHEMY || p.tier().rank() < 3
                || !(event.getItem().getItemMeta() instanceof PotionMeta meta)) return;
        double multiplier = p.tier().rank() >= 4 ? 2.0 : 1.5;
        java.util.List<PotionEffect> expected = new java.util.ArrayList<>(meta.getAllEffects());
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            for (PotionEffect effect : expected) {
                PotionEffect active = player.getPotionEffect(effect.getType());
                if (active == null) continue;
                int duration = Math.min(24_000, (int) Math.ceil(active.getDuration() * multiplier));
                player.addPotionEffect(new PotionEffect(active.getType(), duration, active.getAmplifier(),
                        active.isAmbient(), active.hasParticles(), active.hasIcon()), true);
            }
        });
    }

    @EventHandler
    public void onEnderPearlTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            award(event.getPlayer(), EvolutionPath.ENDER, 8);
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
