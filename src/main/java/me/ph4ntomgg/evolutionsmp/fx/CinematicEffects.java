package me.ph4ntomgg.evolutionsmp.fx;

import me.ph4ntomgg.evolutionsmp.domain.EvolutionPath;
import me.ph4ntomgg.evolutionsmp.item.EvolutionItems;
import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CinematicEffects {
    private final JavaPlugin plugin;
    private final EvolutionItems items;
    private final Map<UUID, BukkitTask> activeTasks = new HashMap<>();
    private final Map<UUID, List<ItemDisplay>> activeDisplays = new HashMap<>();

    public CinematicEffects(JavaPlugin plugin) {
        this.plugin = plugin;
        this.items = new EvolutionItems(plugin);
    }

    public void revealPath(Player player, EvolutionPath chosen) {
        UUID id = player.getUniqueId();
        BukkitTask previous = activeTasks.remove(id);
        if (previous != null) previous.cancel();
        removeDisplays(id);

        EvolutionPath[] paths = EvolutionPath.values();
        List<ItemDisplay> displays = new ArrayList<>();
        Location origin = player.getLocation().add(0, 1.55, 0);
        for (int i = 0; i < paths.length; i++) {
            ItemDisplay display = player.getWorld().spawn(origin, ItemDisplay.class);
            display.setItemStack(items.pathIcon(paths[i]));
            display.setBillboard(Display.Billboard.CENTER);
            display.setInvulnerable(true);
            display.setSilent(true);
            display.setPersistent(false);
            display.setGravity(false);
            displays.add(display);
        }
        activeDisplays.put(id, displays);
        player.sendTitle(ChatColor.GOLD + "EVOLUTION AWAKENED", ChatColor.WHITE + chosen.displayName(), 8, 45, 12);
        player.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.1f);

        BukkitRunnable animation = new BukkitRunnable() {
            int tick = 0;
            @Override public void run() {
                if (!player.isOnline() || player.isDead() || tick >= 100) {
                    cleanup(id);
                    cancel();
                    return;
                }
                Location center = player.getLocation().add(0, 1.55, 0);
                double acceleration = 0.075 + tick * 0.0024;
                for (int i = 0; i < paths.length; i++) {
                    ItemDisplay display = displays.get(i);
                    if (!display.isValid()) continue;
                    double angle = i * (Math.PI * 2 / paths.length) + tick * acceleration;
                    double radius;
                    double height = 0.0;
                    if (tick < 62) {
                        radius = 1.28;
                        height = Math.sin(angle * 1.6) * 0.12;
                    } else if (paths[i] == chosen) {
                        radius = Math.max(0.16, 0.42 - (tick - 62) * 0.008);
                        height = 0.15 + Math.sin(tick * 0.25) * 0.08;
                    } else {
                        double progress = Math.min(1.0, (tick - 62) / 15.0);
                        radius = 1.28 + progress * 2.6;
                        height = progress * (0.55 + (i % 3) * 0.2);
                    }
                    Location point = center.clone().add(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
                    display.teleport(point);
                    display.setRotation((float) Math.toDegrees(angle + Math.PI / 2), 0);
                    if (tick >= 62 && paths[i] != chosen) {
                        player.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0.015, 0.015, 0.015, 0);
                        if (tick >= 78) display.remove();
                    }
                }
                if (tick % 5 == 0) {
                    player.getWorld().spawnParticle(Particle.DUST, center, 5, 0.22, 0.22, 0.22, 0,
                            new Particle.DustOptions(color(chosen), 0.85f));
                }
                if (tick == 62) player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.9f, 1.3f);
                if (tick == 80) player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.7f, 1.5f);
                if (tick >= 98) {
                    for (ItemDisplay display : displays) display.remove();
                    cleanup(id);
                    cancel();
                }
                tick++;
            }
        };
        BukkitTask task = animation.runTaskTimer(plugin, 0L, 1L);
        activeTasks.put(id, task);
    }

    private Color color(EvolutionPath path) {
        return switch (path) {
            case HUNT -> Color.fromRGB(220, 55, 55);
            case ALCHEMY -> Color.fromRGB(75, 220, 150);
            case ENCHANTMENT -> Color.fromRGB(135, 90, 240);
            case PROSPECTOR -> Color.fromRGB(245, 190, 55);
            case ENDER -> Color.fromRGB(65, 170, 245);
            case TRADE -> Color.fromRGB(70, 190, 160);
            case CALAMITY -> Color.fromRGB(235, 75, 120);
        };
    }

    private void cleanup(UUID id) {
        activeTasks.remove(id);
        removeDisplays(id);
    }

    private void removeDisplays(UUID id) {
        List<ItemDisplay> displays = activeDisplays.remove(id);
        if (displays != null) displays.forEach(display -> { if (display.isValid()) display.remove(); });
    }

    public void shutdown() {
        activeTasks.values().forEach(BukkitTask::cancel);
        activeTasks.clear();
        activeDisplays.keySet().forEach(this::removeDisplays);
    }
}
