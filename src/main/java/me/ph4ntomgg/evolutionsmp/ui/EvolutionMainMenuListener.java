package me.ph4ntomgg.evolutionsmp.ui;

import me.ph4ntomgg.evolutionsmp.domain.PlayerProgress;
import me.ph4ntomgg.evolutionsmp.domain.ProgressionRules;
import me.ph4ntomgg.evolutionsmp.fx.CinematicEffects;
import me.ph4ntomgg.evolutionsmp.guide.EvolutionGuide;
import me.ph4ntomgg.evolutionsmp.service.ProgressService;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class EvolutionMainMenuListener implements Listener {
    private final ProgressService progress;
    private final PathSelectionMenu paths;
    private final RecipeMenu recipes;
    private final CinematicEffects effects;

    public EvolutionMainMenuListener(ProgressService progress, PathSelectionMenu paths, RecipeMenu recipes, CinematicEffects effects) {
        this.progress = progress; this.paths = paths; this.recipes = recipes; this.effects = effects;
    }

    @EventHandler
    public void click(InventoryClickEvent event) {
        if (!EvolutionMainMenu.TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        switch (event.getRawSlot()) {
            case 10 -> {
                player.closeInventory();
                if (player.hasPermission("evolution.admin")) paths.open(player, player);
                else player.sendMessage(ChatColor.GRAY + "Ask an administrator to assign your path, or use an Evolution Path Changer.");
            }
            case 12 -> {
                PlayerProgress p = progress.get(player.getUniqueId());
                player.closeInventory();
                player.sendMessage(ChatColor.GOLD + "Evolution: " + (p.path() == null ? "Unassigned" : p.path().displayName()) + " • " + p.tier());
                player.sendMessage(ChatColor.GRAY + "XP: " + p.experience());
            }
            case 14 -> {
                player.closeInventory();
                EvolutionGuide.send(player, "abilities");
            }
            case 16 -> {
                player.closeInventory();
                recipes.open(player);
            }
            case 28 -> {
                PlayerProgress p = progress.get(player.getUniqueId());
                player.closeInventory();
                if (!p.assigned()) player.sendMessage(ChatColor.GRAY + "Your evolution has not begun.");
                else if (p.apex()) player.sendMessage(ChatColor.GOLD + "Apex achieved. You have reached the peak of evolution.");
                else player.sendMessage(ChatColor.GRAY + "XP: " + p.experience() + " / " + ProgressionRules.thresholdForNextTier(p.tier()) + " toward the next tier.");
            }
            case 30 -> {
                player.closeInventory();
                EvolutionGuide.send(player, null);
            }
            case 32 -> {
                player.closeInventory();
                PlayerProgress p = progress.get(player.getUniqueId());
                if (p.path() == null) player.sendMessage(ChatColor.RED + "You do not have an evolution path yet.");
                else effects.revealPath(player, p.path());
            }
            case 34 -> {
                player.closeInventory();
                if (player.hasPermission("evolution.admin")) player.sendMessage(ChatColor.YELLOW + "/evo start <player> • /evo exp <player> <amount> • /evo tier <player> <tier> • /evo pathchanger <player>");
                else player.sendMessage(ChatColor.RED + "Administrator tools are restricted.");
            }
            default -> { }
        }
    }
}
