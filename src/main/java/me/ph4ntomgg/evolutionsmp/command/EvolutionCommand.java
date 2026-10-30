package me.ph4ntomgg.evolutionsmp.command;

import me.ph4ntomgg.evolutionsmp.domain.*;
import me.ph4ntomgg.evolutionsmp.ability.AbilityService;
import me.ph4ntomgg.evolutionsmp.fx.CinematicEffects;
import me.ph4ntomgg.evolutionsmp.service.ProgressService;
import me.ph4ntomgg.evolutionsmp.ui.PathSelectionMenu;
import me.ph4ntomgg.evolutionsmp.ui.RecipeMenu;
import me.ph4ntomgg.evolutionsmp.ui.EvolutionMainMenu;
import me.ph4ntomgg.evolutionsmp.guide.EvolutionGuide;
import me.ph4ntomgg.evolutionsmp.item.EvolutionItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class EvolutionCommand implements CommandExecutor {
    private final ProgressService progress;
    private final AbilityService abilities;
    private final RecipeMenu recipes;
    private final EvolutionItems items;
    private final EvolutionMainMenu mainMenu;
    private final PathSelectionMenu menu;
    private final CinematicEffects effects;

    public EvolutionCommand(ProgressService progress, PathSelectionMenu menu, CinematicEffects effects, AbilityService abilities, RecipeMenu recipes, EvolutionItems items, EvolutionMainMenu mainMenu) {
        this.progress = progress; this.menu = menu; this.effects = effects; this.abilities = abilities; this.recipes = recipes; this.items = items; this.mainMenu = mainMenu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) mainMenu.open(player);
            else sender.sendMessage("Use /evo help for commands.");
            return true;
        }
        if (args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(ChatColor.GOLD + "EvolutionSMP 1.0.0 " + ChatColor.GRAY + "Commands:");
            sender.sendMessage(ChatColor.YELLOW + "/evo status");
            sender.sendMessage(ChatColor.YELLOW + "/evo recipes");
            sender.sendMessage(ChatColor.YELLOW + "/evo guide [paths|tiers|death|orbs|abilities|xp]");
            sender.sendMessage(ChatColor.YELLOW + "/evo pathchanger <player> " + ChatColor.GRAY + "(admin)");
            sender.sendMessage(ChatColor.YELLOW + "/evo gambit " + ChatColor.GRAY + "(Trade Apex)");
            sender.sendMessage(ChatColor.YELLOW + "/evo heart " + ChatColor.GRAY + "(Calamity Apex)");
            sender.sendMessage(ChatColor.YELLOW + "/evo start <player> " + ChatColor.GRAY + "(admin: open path selection)");
            sender.sendMessage(ChatColor.YELLOW + "/evo exp <player> <amount> " + ChatColor.GRAY + "(admin)");
            sender.sendMessage(ChatColor.YELLOW + "/evo tier <player> <T1|T2|T3|T4|APEX> " + ChatColor.GRAY + "(admin)");
            sender.sendMessage(ChatColor.YELLOW + "/evofx <player> " + ChatColor.GRAY + "(admin: replay path reveal)");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "pathchanger" -> {
                if (!sender.hasPermission("evolution.admin")) { sender.sendMessage(ChatColor.RED + "No permission."); return true; }
                if (args.length != 2) { sender.sendMessage(ChatColor.RED + "Usage: /evo pathchanger <player>"); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage(ChatColor.RED + "That player is not online."); return true; }
                target.getInventory().addItem(items.pathChanger()).values().forEach(item -> target.getWorld().dropItemNaturally(target.getLocation(), item));
                sender.sendMessage(ChatColor.GREEN + "Gave a Path Changer to " + target.getName() + ".");
            }
            case "guide" -> {
                EvolutionGuide.send(sender, args.length > 1 ? args[1] : null);
            }
            case "menu" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
                mainMenu.open(player);
            }
            case "recipes" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
                recipes.open(player);
            }
            case "gambit" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
                abilities.activateGambit(player);
            }
            case "heart" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
                abilities.activateHeart(player);
            }
            case "status" -> {
                if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
                PlayerProgress p = progress.get(player.getUniqueId());
                sender.sendMessage(ChatColor.GOLD + "Evolution Status");
                sender.sendMessage(ChatColor.GRAY + "Path: " + (p.path() == null ? "Unassigned" : p.path().displayName()));
                sender.sendMessage(ChatColor.GRAY + "Tier: " + p.tier().name());
                sender.sendMessage(ChatColor.GRAY + "XP: " + p.experience());
            }
            case "start" -> {
                if (!sender.hasPermission("evolution.admin")) { sender.sendMessage(ChatColor.RED + "No permission."); return true; }
                if (args.length < 2) { sender.sendMessage(ChatColor.RED + "Usage: /evo start <player>"); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage(ChatColor.RED + "That player is not online."); return true; }
                if (!(sender instanceof Player viewer)) { sender.sendMessage("Run this command in-game so you can use the selection menu."); return true; }
                menu.open(target, viewer);
            }
            case "exp" -> {
                if (!sender.hasPermission("evolution.admin")) { sender.sendMessage(ChatColor.RED + "No permission."); return true; }
                if (args.length != 3) { sender.sendMessage(ChatColor.RED + "Usage: /evo exp <player> <amount>"); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage(ChatColor.RED + "That player is not online."); return true; }
                try {
                    long amount = Long.parseLong(args[2]);
                    if (amount < 1) throw new NumberFormatException();
                    PlayerProgress updated = progress.award(target.getUniqueId(), amount);
                    sender.sendMessage(ChatColor.GREEN + "Awarded " + amount + " XP. " + updated.tier());
                    target.sendMessage(ChatColor.GREEN + "You received " + amount + " evolution XP.");
                } catch (NumberFormatException ex) { sender.sendMessage(ChatColor.RED + "Amount must be a positive whole number."); }
            }
            case "tier" -> {
                if (!sender.hasPermission("evolution.admin")) { sender.sendMessage(ChatColor.RED + "No permission."); return true; }
                if (args.length != 3) { sender.sendMessage(ChatColor.RED + "Usage: /evo tier <player> <T1|T2|T3|T4|APEX>"); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage(ChatColor.RED + "That player is not online."); return true; }
                try {
                    EvolutionTier tier = EvolutionTier.valueOf(args[2].toUpperCase(Locale.ROOT));
                    PlayerProgress updated = progress.setTier(target.getUniqueId(), tier);
                    sender.sendMessage(ChatColor.GREEN + target.getName() + " is now " + updated.tier() + ".");
                } catch (IllegalArgumentException ex) { sender.sendMessage(ChatColor.RED + "Unknown tier or player has no path."); }
            }
            case "reveal" -> {
                if (!sender.hasPermission("evolution.admin")) { sender.sendMessage(ChatColor.RED + "No permission."); return true; }
                if (args.length != 2) { sender.sendMessage(ChatColor.RED + "Usage: /evo reveal <player>"); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage(ChatColor.RED + "That player is not online."); return true; }
                PlayerProgress p = progress.get(target.getUniqueId());
                if (p.path() == null) { sender.sendMessage(ChatColor.RED + "Player has no path."); return true; }
                effects.revealPath(target, p.path());
            }
            default -> sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /evo help.");
        }
        return true;
    }
}
