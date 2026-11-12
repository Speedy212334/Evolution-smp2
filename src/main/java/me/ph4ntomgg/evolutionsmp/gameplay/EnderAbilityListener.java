package me.ph4ntomgg.evolutionsmp.gameplay;

import me.ph4ntomgg.evolutionsmp.domain.EvolutionPath;
import me.ph4ntomgg.evolutionsmp.domain.PlayerProgress;
import me.ph4ntomgg.evolutionsmp.service.ProgressService;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class EnderAbilityListener implements Listener {
    private final ProgressService progress;
    private final Map<UUID, Location> lastSafe = new HashMap<>();
    private final NamespacedKey voidStepCooldownKey;

    public EnderAbilityListener(JavaPlugin plugin, ProgressService progress) {
        this.progress = progress;
        this.voidStepCooldownKey = new NamespacedKey(plugin, "void_step_cooldown_until");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (isEnderTierFour(player)) lastSafe.put(player.getUniqueId(), player.getLocation().clone());
    }

    @EventHandler
    public void onPearlLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof EnderPearl pearl) || !(pearl.getShooter() instanceof Player player)) return;
        PlayerProgress p = progress.get(player.getUniqueId());
        if (p.path() != EvolutionPath.ENDER || p.tier().rank() < 2) return;
        int cooldown = p.tier().rank() >= 4 ? 2 : p.tier().rank() >= 3 ? 5 : 10;
        player.setCooldown(Material.ENDER_PEARL, cooldown * 20);
        if (p.tier().rank() >= 3) pearl.setVelocity(pearl.getVelocity().multiply(p.tier().rank() >= 4 ? 1.5 : 1.35));
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (isEnderTierFour(player)) event.setCancelled(true);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null || event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;
        Player player = event.getPlayer();
        if (!isEnderTierFour(player)) return;
        UUID id = player.getUniqueId();
        Location to = event.getTo();
        if (to.getY() <= player.getWorld().getMinHeight() + 4) {
            long cooldownUntil = player.getPersistentDataContainer()
                    .getOrDefault(voidStepCooldownKey, PersistentDataType.LONG, 0L);
            if (cooldownUntil > System.currentTimeMillis()) return;
            Location safe = lastSafe.get(id);
            if (safe != null && safe.getWorld() == player.getWorld() && isSafeLocation(safe)) {
                // A protection plugin may cancel the rescue teleport. Only consume
                // the cooldown after the player actually reaches the safe location.
                if (player.teleport(safe)) {
                    player.getPersistentDataContainer().set(voidStepCooldownKey, PersistentDataType.LONG,
                            System.currentTimeMillis() + 60_000L);
                    player.sendMessage(ChatColor.LIGHT_PURPLE + "Void Step activated. You were pulled back to safety.");
                    player.getWorld().playSound(safe, org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.4f);
                }
            } else {
                lastSafe.remove(id);
            }
            return;
        }
        if (player.isOnGround() && to.getBlock().isPassable() && to.clone().add(0, 1, 0).getBlock().isPassable()
                && to.clone().add(0, -1, 0).getBlock().getType().isSolid()) {
            lastSafe.put(id, to.clone());
        }
    }

    private boolean isSafeLocation(Location location) {
        if (location.getWorld() == null) return false;
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        if (!location.getWorld().isChunkLoaded(chunkX, chunkZ)) return false;
        return location.getBlock().isPassable()
                && location.clone().add(0, 1, 0).getBlock().isPassable()
                && location.clone().add(0, -1, 0).getBlock().getType().isSolid();
    }

    /** Reset Ender-only cooldown and refresh the rescue anchor after a path change. */
    public void resetPathCooldowns(UUID id) {
        Player player = org.bukkit.Bukkit.getPlayer(id);
        if (player != null) player.getPersistentDataContainer().remove(voidStepCooldownKey);
        if (player != null && isEnderTierFour(player)) {
            lastSafe.put(id, player.getLocation().clone());
        } else {
            lastSafe.remove(id);
        }
    }

    private boolean isEnderTierFour(Player player) {
        PlayerProgress p = progress.get(player.getUniqueId());
        return p.path() == EvolutionPath.ENDER && p.tier().rank() >= 4;
    }
}
