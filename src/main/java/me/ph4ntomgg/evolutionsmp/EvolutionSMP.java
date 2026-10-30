package me.ph4ntomgg.evolutionsmp;

import org.bukkit.plugin.java.JavaPlugin;

public final class EvolutionSMP extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info("EvolutionSMP 1.0.0 starting.");
        getLogger().info("Core and FX will share this plugin lifecycle and one distributable JAR.");
    }

    @Override
    public void onDisable() {
        getLogger().info("EvolutionSMP 1.0.0 stopped; lifecycle services must be cleaned up here.");
    }
}
