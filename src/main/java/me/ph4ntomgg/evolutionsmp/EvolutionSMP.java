package me.ph4ntomgg.evolutionsmp;

import me.ph4ntomgg.evolutionsmp.command.EvolutionCommand;
import me.ph4ntomgg.evolutionsmp.ability.AbilityService;
import me.ph4ntomgg.evolutionsmp.ability.OreResonanceService;
import me.ph4ntomgg.evolutionsmp.domain.PlayerProgress;
import me.ph4ntomgg.evolutionsmp.domain.ProgressionRules;
import me.ph4ntomgg.evolutionsmp.fx.CinematicEffects;
import me.ph4ntomgg.evolutionsmp.gameplay.GameplayListener;
import me.ph4ntomgg.evolutionsmp.gameplay.AlchemyFusionListener;
import me.ph4ntomgg.evolutionsmp.gameplay.EnderAbilityListener;
import me.ph4ntomgg.evolutionsmp.gameplay.PathPassiveListener;
import me.ph4ntomgg.evolutionsmp.persistence.ProgressRepository;
import me.ph4ntomgg.evolutionsmp.service.ProgressService;
import me.ph4ntomgg.evolutionsmp.ui.PathSelectionMenu;
import me.ph4ntomgg.evolutionsmp.ui.RecipeMenu;
import me.ph4ntomgg.evolutionsmp.ui.RecipeMenuListener;
import me.ph4ntomgg.evolutionsmp.ui.PathChangerListener;
import me.ph4ntomgg.evolutionsmp.ui.EvolutionMainMenu;
import me.ph4ntomgg.evolutionsmp.ui.EvolutionMainMenuListener;
import me.ph4ntomgg.evolutionsmp.ui.CurseRemovalMenu;
import me.ph4ntomgg.evolutionsmp.item.EvolutionItems;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class EvolutionSMP extends JavaPlugin implements Listener {
    private ProgressService progress;
    private CinematicEffects effects;
    private AbilityService abilities;
    private OreResonanceService resonance;
    private PathPassiveListener passives;
    private BukkitTask progressFlushTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        progress = new ProgressService(new ProgressRepository(this),
                getConfig().getLong("progression.thresholds.t2", ProgressionRules.DEFAULT_T2_XP),
                getConfig().getLong("progression.thresholds.t3", ProgressionRules.DEFAULT_T3_XP),
                getConfig().getLong("progression.thresholds.t4", ProgressionRules.DEFAULT_T4_XP));
        progressFlushTask = getServer().getScheduler().runTaskTimer(this, progress::flush, 100L, 100L);
        effects = new CinematicEffects(this);
        abilities = new AbilityService(this, progress);
        resonance = new OreResonanceService(this, progress);
        passives = new PathPassiveListener(this, progress);
        getServer().getPluginManager().registerEvents(passives, this);
        EvolutionItems items = new EvolutionItems(this);
        items.registerRecipes();
        RecipeMenu recipes = new RecipeMenu(items);
        EvolutionMainMenu mainMenu = new EvolutionMainMenu(progress, items);
        CurseRemovalMenu curseMenu = new CurseRemovalMenu(progress);
        EnderAbilityListener enderAbilities = new EnderAbilityListener(this, progress);
        PathSelectionMenu menu = new PathSelectionMenu(this, progress, effects, abilities, resonance, passives, enderAbilities);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new GameplayListener(this, progress), this);
        getServer().getPluginManager().registerEvents(new AlchemyFusionListener(this, progress), this);
        getServer().getPluginManager().registerEvents(enderAbilities, this);
        getServer().getPluginManager().registerEvents(abilities, this);
        abilities.resumeOnlinePlayers();
        getServer().getPluginManager().registerEvents(resonance, this);
        getServer().getPluginManager().registerEvents(new RecipeMenuListener(), this);
        getServer().getPluginManager().registerEvents(new PathChangerListener(items, menu), this);
        getServer().getPluginManager().registerEvents(new EvolutionMainMenuListener(progress, menu, recipes, effects), this);
        getServer().getPluginManager().registerEvents(curseMenu, this);

        if (getCommand("evo") == null || getCommand("enderchest") == null) {
            getLogger().severe("Command descriptors are missing; disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getCommand("evo").setExecutor(new EvolutionCommand(progress, menu, effects, abilities, resonance, recipes, items, mainMenu, curseMenu));
        getCommand("enderchest").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player player)) { sender.sendMessage("Players only."); return true; }
            var state = progress.get(player.getUniqueId());
            if (state.path() != me.ph4ntomgg.evolutionsmp.domain.EvolutionPath.ENDER
                    || state.tier().rank() < me.ph4ntomgg.evolutionsmp.domain.EvolutionTier.T4.rank()) {
                player.sendMessage(ChatColor.RED + "Ender Storage requires Ender Tier IV.");
                return true;
            }
            player.openInventory(player.getEnderChest());
            return true;
        });
        getLogger().info("EvolutionSMP 1.0.0 enabled. Seven evolution paths registered.");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        new EvolutionItems(this).migrateInventory(event.getPlayer());
        PlayerProgress state = progress.get(event.getPlayer().getUniqueId());
        if (state.path() == null) {
            event.getPlayer().sendMessage(ChatColor.GRAY + "Your evolution has not begun. An administrator will assign your path.");
        }
    }

    @Override
    public void onDisable() {
        if (progressFlushTask != null) progressFlushTask.cancel();
        if (progress != null) progress.flush();
        if (effects != null) effects.shutdown();
        if (abilities != null) abilities.shutdown();
        if (resonance != null) resonance.shutdown();
        if (passives != null) passives.shutdown();
        getLogger().info("EvolutionSMP 1.0.0 stopped; services flushed.");
    }
}