package me.ph4ntomgg.evolutionsmp.persistence;

import me.ph4ntomgg.evolutionsmp.domain.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class ProgressRepository {
    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration data;

    public ProgressRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
        reload();
    }

    public synchronized void reload() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            throw new IllegalStateException("Could not create plugin data directory");
        }
        data = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized PlayerProgress load(UUID id) {
        String root = "players." + id;
        String pathName = data.getString(root + ".path");
        EvolutionPath path = pathName == null ? null : EvolutionPath.fromId(pathName).orElse(null);
        EvolutionTier tier;
        try { tier = EvolutionTier.valueOf(data.getString(root + ".tier", "T1")); }
        catch (IllegalArgumentException ex) { tier = EvolutionTier.T1; }
        boolean apex = data.getBoolean(root + ".apex", false);
        if (apex) tier = EvolutionTier.APEX;
        long xp = Math.max(0L, data.getLong(root + ".xp", 0L));
        if (apex && path == null) apex = false;
        if (tier == EvolutionTier.APEX && !apex) tier = EvolutionTier.T4;
        return new PlayerProgress(id, path, tier, xp, apex);
    }

    public synchronized void save(PlayerProgress progress) {
        String root = "players." + progress.playerId();
        data.set(root + ".path", progress.path() == null ? null : progress.path().name());
        data.set(root + ".tier", progress.tier().name());
        data.set(root + ".xp", progress.experience());
        data.set(root + ".apex", progress.apex());
        try { data.save(file); }
        catch (IOException ex) { plugin.getLogger().severe("Could not save player data: " + ex.getMessage()); }
    }
}
