package me.ph4ntomgg.evolutionsmp.command;

import me.ph4ntomgg.evolutionsmp.fx.CinematicEffects;
import me.ph4ntomgg.evolutionsmp.fx.CustomAnimationManager;
import me.ph4ntomgg.evolutionsmp.service.ProgressService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.stream.Collectors;

public final class EvolutionFXCommand implements CommandExecutor {
    private final CustomAnimationManager animations;
    private final CinematicEffects effects;
    private final ProgressService progress;

    public EvolutionFXCommand(CustomAnimationManager animations, CinematicEffects effects, ProgressService progress) {
        this.animations = animations; this.effects = effects; this.progress = progress;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("evolution.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "EvolutionFX: /evofx list | play <id> <player> [seconds] | stop <player> | reload | reveal <player>");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> sender.sendMessage(ChatColor.LIGHT_PURPLE + "Animations: " +
                    animations.animationIds().stream().sorted().collect(Collectors.joining(", ")));
            case "reload" -> sender.sendMessage(ChatColor.GREEN + "Loaded " + animations.reload() + " animation definitions.");
            case "stop" -> {
                if (args.length != 2) { sender.sendMessage(ChatColor.RED + "Usage: /evofx stop <player>"); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage(ChatColor.RED + "Player is not online."); return true; }
                animations.stop(target);
                sender.sendMessage(ChatColor.GREEN + "Stopped custom animations for " + target.getName() + ".");
            }
            case "play" -> {
                if (args.length < 3 || args.length > 4) { sender.sendMessage(ChatColor.RED + "Usage: /evofx play <id> <player> [seconds]"); return true; }
                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) { sender.sendMessage(ChatColor.RED + "Player is not online."); return true; }
                Integer seconds = null;
                if (args.length == 4) {
                    try { seconds = Integer.parseInt(args[3]); }
                    catch (NumberFormatException ex) { sender.sendMessage(ChatColor.RED + "Seconds must be a whole number."); return true; }
                    if (seconds < 1 || seconds > 600) { sender.sendMessage(ChatColor.RED + "Duration must be between 1 and 600 seconds."); return true; }
                }
                if (!animations.play(target, args[1], seconds)) sender.sendMessage(ChatColor.RED + "Unknown animation. Use /evofx list.");
                else sender.sendMessage(ChatColor.GREEN + "Playing " + args[1] + " on " + target.getName() + ".");
            }
            case "reveal" -> {
                if (args.length != 2) { sender.sendMessage(ChatColor.RED + "Usage: /evofx reveal <player>"); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage(ChatColor.RED + "Player is not online."); return true; }
                var state = progress.get(target.getUniqueId());
                if (state.path() == null) { sender.sendMessage(ChatColor.RED + "Player has no path."); return true; }
                effects.revealPath(target, state.path());
            }
            default -> sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /evofx.");
        }
        return true;
    }
}
