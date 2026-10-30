package me.ph4ntomgg.evolutionsmp.ui;

import me.ph4ntomgg.evolutionsmp.domain.EvolutionPath;
import me.ph4ntomgg.evolutionsmp.service.ProgressService;
import me.ph4ntomgg.evolutionsmp.fx.CinematicEffects;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

final class PlayerProgressAssign {
    private PlayerProgressAssign() {}
    static void assign(JavaPlugin plugin, UUID targetId, Player viewer, EvolutionPath path, ProgressService progress, CinematicEffects effects) {
        progress.assign(targetId, path);
        Player target = Bukkit.getPlayer(targetId);
        if (target != null) {
            target.sendMessage(ChatColor.GREEN + "Your evolution path is now " + path.displayName() + ".");
            effects.revealPath(target, path);
        }
        viewer.sendMessage(ChatColor.GREEN + "Assigned " + path.displayName() + " to " + (target == null ? targetId : target.getName()) + ".");
    }
}
