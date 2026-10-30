package me.ph4ntomgg.evolutionsmp;

import me.ph4ntomgg.evolutionsmp.command.EvolutionCommand;
import me.ph4ntomgg.evolutionsmp.command.EvolutionFXCommand;
import me.ph4ntomgg.evolutionsmp.fx.CustomAnimationManager;
import me.ph4ntomgg.evolutionsmp.ability.AbilityService;
import me.ph4ntomgg.evolutionsmp.domain.PlayerProgress;
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

public final class EvolutionSMP extends JavaPlugin implements Listener {
    private ProgressService progress;
    private CinematicEffects effects;
    private AbilityService abilities;
    private CustomAnimationManager animations;
    private PathPassiveListener passives;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        progress = new ProgressService(new ProgressRepository(this));
        effects = new CinematicEffects(this);
        animations = new CustomAnimationManager(this);
        animations.reload();
        abilities = new AbilityService(this, progress);
        EvolutionItems items = new EvolutionItems(this);
        items.registerRecipes();
        RecipeMenu recipes = new RecipeMenu(items);
        EvolutionMainMenu mainMenu = new EvolutionMainMenu(progress, items);
        CurseRemovalMenu curseMenu = new CurseRemovalMenu(progress);
        PathSelectionMenu menu = new PathSelectionMenu(this, progress, effects);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new GameplayListener(this, progress), this);
        getServer().getPluginManager().registerEvents(new AlchemyFusionListener(this, progress), this);
        getServer().getPluginManager().registerEvents(new EnderAbilityListener(this, progress), this);
        passives = new PathPassiveListener(this, progress);
        getServer().getPluginManager().registerEvents(passives, this);
        getServer().getPluginManager().registerEvents(abilities, this);
        getServer().getPluginManager().registerEvents(new RecipeMenuListener(), this);
        getServer().getPluginManager().registerEvents(new PathChangerListener(items, menu), this);
        getServer().getPluginManager().registerEvents(new EvolutionMainMenuListener(progress, menu, recipes, effects), this);
        getServer().getPluginManager().registerEvents(curseMenu, this);

        if (getCommand("evo") == null || getCommand("evofx") == null || getCommand("enderchest") == null) {
            getLogger().severe("Command descriptors are missing; disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getCommand("evo").setExecutor(new EvolutionCommand(progress, menu, effects, abilities, recipes, items, mainMenu, curseMenu));
        getCommand("evofx").setExecutor(new EvolutionFXCommand(animations, effects, progress));
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
        if (animations != null) animations.stopAll();
        if (passives != null) passives.shutdown();
        getLogger().info("EvolutionSMP 1.0.0 stopped; services flushed.");
    }
}