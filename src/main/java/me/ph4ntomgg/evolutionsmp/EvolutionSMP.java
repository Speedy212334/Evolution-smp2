package me.ph4ntomgg.evolutionsmp;

import me.ph4ntomgg.evolutionsmp.command.EvolutionCommand;
import me.ph4ntomgg.evolutionsmp.ability.AbilityService;
import me.ph4ntomgg.evolutionsmp.domain.PlayerProgress;
import me.ph4ntomgg.evolutionsmp.fx.CinematicEffects;
import me.ph4ntomgg.evolutionsmp.gameplay.GameplayListener;
import me.ph4ntomgg.evolutionsmp.persistence.ProgressRepository;
import me.ph4ntomgg.evolutionsmp.service.ProgressService;
import me.ph4ntomgg.evolutionsmp.ui.PathSelectionMenu;
import me.ph4ntomgg.evolutionsmp.ui.RecipeMenu;
import me.ph4ntomgg.evolutionsmp.ui.RecipeMenuListener;
import me.ph4ntomgg.evolutionsmp.item.EvolutionItems;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class EvolutionSMP extends JavaPlugin implements Listener {
    private ProgressService progress;
    private CinematicEffects effects;
    private AbilityService abilities;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        progress = new ProgressService(new ProgressRepository(this));
        effects = new CinematicEffects(this);
        abilities = new AbilityService(this, progress);
        EvolutionItems items = new EvolutionItems(this);
        items.registerRecipes();
        RecipeMenu recipes = new RecipeMenu(items);
        PathSelectionMenu menu = new PathSelectionMenu(this, progress, effects);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new GameplayListener(this, progress), this);
        getServer().getPluginManager().registerEvents(abilities, this);
        getServer().getPluginManager().registerEvents(new RecipeMenuListener(), this);

        if (getCommand("evo") == null || getCommand("evofx") == null) {
            getLogger().severe("Command descriptors are missing; disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getCommand("evo").setExecutor(new EvolutionCommand(progress, menu, effects, abilities, recipes));
        getCommand("evofx").setExecutor((sender, command, label, args) -> {
            if (!sender.hasPermission("evolution.admin")) { sender.sendMessage(ChatColor.RED + "No permission."); return true; }
            if (args.length != 1) { sender.sendMessage(ChatColor.RED + "Usage: /evofx <player>"); return true; }
            org.bukkit.entity.Player target = getServer().getPlayerExact(args[0]);
            if (target == null) { sender.sendMessage(ChatColor.RED + "That player is not online."); return true; }
            var state = progress.get(target.getUniqueId());
            if (state.path() == null) { sender.sendMessage(ChatColor.RED + "Player has no path."); return true; }
            effects.revealPath(target, state.path());
            return true;
        });
        getLogger().info("EvolutionSMP 1.0.0 enabled. Seven paths registered; unified FX lifecycle active.");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PlayerProgress state = progress.get(event.getPlayer().getUniqueId());
        if (state.path() == null) {
            event.getPlayer().sendMessage(ChatColor.GRAY + "Your evolution has not begun. An administrator will assign your path.");
        }
    }

    @Override
    public void onDisable() {
        if (progress != null) progress.flush();
        if (effects != null) effects.shutdown();
        if (abilities != null) abilities.shutdown();
        getLogger().info("EvolutionSMP 1.0.0 stopped; services flushed.");
    }
}