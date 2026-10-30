package me.ph4ntomgg.evolutionsmp.gameplay;

import me.ph4ntomgg.evolutionsmp.domain.EvolutionPath;
import me.ph4ntomgg.evolutionsmp.domain.PlayerProgress;
import me.ph4ntomgg.evolutionsmp.service.ProgressService;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class PathPassiveListener implements Listener {
    private final ProgressService progress;
    private final BukkitTask refreshTask;

    public PathPassiveListener(JavaPlugin plugin, ProgressService progress) {
        this.progress = progress;
        this.refreshTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::refreshAll, 20L, 40L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        refresh(event.getPlayer());
    }

    private void refreshAll() {
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) refresh(player);
    }

    private void refresh(Player player) {
        PlayerProgress p = progress.get(player.getUniqueId());
        if (!p.assigned()) return;
        if (p.path() == EvolutionPath.HUNT) {
            if (p.tier().rank() >= 3) maintain(player, PotionEffectType.SPEED, 1);
            else if (p.tier().rank() >= 1) maintain(player, PotionEffectType.SPEED, 0);
        } else if (p.path() == EvolutionPath.PROSPECTOR && p.tier().rank() >= 3) {
            maintain(player, PotionEffectType.HASTE, 5);
        } else if (p.path() == EvolutionPath.CALAMITY && p.tier().rank() >= 3) {
            maintain(player, PotionEffectType.STRENGTH, 0);
        } else if (p.path() == EvolutionPath.TRADE && p.tier().rank() >= 4) {
            maintain(player, PotionEffectType.HERO_OF_THE_VILLAGE, 1);
            maintain(player, PotionEffectType.LUCK, 1);
        }
    }

    private void maintain(Player player, PotionEffectType type, int amplifier) {
        PotionEffect current = player.getPotionEffect(type);
        if (current == null || current.getAmplifier() < amplifier
                || current.getAmplifier() == amplifier && current.getDuration() < 60
                && current.isAmbient() && !current.hasParticles() && !current.hasIcon()) {
            player.addPotionEffect(new PotionEffect(type, 100, amplifier, true, false, false), true);
        }
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
    }
}
