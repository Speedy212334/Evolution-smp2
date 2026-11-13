package me.ph4ntomgg.evolutionsmp.fx;

import me.ph4ntomgg.evolutionsmp.fx.CustomAnimationManager;
import me.ph4ntomgg.evolutionsmp.fx.EvolutionFXCommand;
import me.ph4ntomgg.evolutionsmp.fx.EvolutionFXListener;
import org.bukkit.plugin.java.JavaPlugin;

/** Optional standalone visual-effects plugin for EvolutionSMP servers. */
public final class EvolutionFXPlugin extends JavaPlugin {
    private CustomAnimationManager animations;

    @Override
    public void onEnable() {
        animations = new CustomAnimationManager(this);
        int loaded = animations.reload();
        getServer().getPluginManager().registerEvents(new EvolutionFXListener(animations), this);
        if (getCommand("evofx") == null) {
            getLogger().severe("Missing /evofx command descriptor; disabling EvolutionFX.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getCommand("evofx").setExecutor(new EvolutionFXCommand(animations));
        getLogger().info("EvolutionFX enabled; loaded " + loaded + " animation definitions.");
    }

    @Override
    public void onDisable() {
        if (animations != null) animations.stopAll();
    }
}
