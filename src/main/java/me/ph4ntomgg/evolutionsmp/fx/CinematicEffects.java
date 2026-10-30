package me.ph4ntomgg.evolutionsmp.fx;

import me.ph4ntomgg.evolutionsmp.domain.EvolutionPath;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public final class CinematicEffects {
    private final JavaPlugin plugin;
    private final List<BukkitTask> activeTasks = new ArrayList<>();

    public CinematicEffects(JavaPlugin plugin) { this.plugin = plugin; }

    public void revealPath(Player player, EvolutionPath path) {
        Location center = player.getLocation().add(0, 1.15, 0);
        Particle.DustOptions dust = new Particle.DustOptions(color(path), 1.25f);
        for (int i = 0; i < 7; i++) {
            double angle = (Math.PI * 2 * i) / 7.0;
            Location point = center.clone().add(Math.cos(angle) * 1.15, 0, Math.sin(angle) * 1.15);
            player.getWorld().spawnParticle(Particle.DUST, point, 8, 0.08, 0.08, 0.08, dust);
        }
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, center, 28, 0.45, 0.65, 0.45, 0.08);
        player.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.15f);
        player.sendTitle(ChatColor.GOLD + "EVOLUTION AWAKENED", ChatColor.WHITE + path.displayName(), 10, 55, 15);
    }

    private Color color(EvolutionPath path) {
        return switch (path) {
            case HUNT -> Color.fromRGB(220, 55, 55);
            case ALCHEMY -> Color.fromRGB(75, 220, 150);
            case ENCHANTMENT -> Color.fromRGB(135, 90, 240);
            case PROSPECTOR -> Color.fromRGB(245, 190, 55);
            case ENDER -> Color.fromRGB(155, 75, 220);
            case TRADE -> Color.fromRGB(70, 190, 220);
            case CALAMITY -> Color.fromRGB(235, 235, 235);
        };
    }

    public void shutdown() {
        activeTasks.forEach(BukkitTask::cancel);
        activeTasks.clear();
    }
}
