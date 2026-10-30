package me.ph4ntomgg.evolutionfx;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Configurable, server-driven animations built from client-rendered ItemDisplay entities.
 *
 * These are custom visual animations rather than vanilla player/mob animation states.
 * The configured item-model IDs resolve to models supplied by the matching resource pack.
 */
public final class CustomAnimationManager {
    private static final Set<String> SHAPES = Set.of(
            "spin", "orbit", "helix", "figure-eight", "rise", "slam", "pulse", "vortex"
    );
    private static final int MAX_COPIES = 16;
    private static final int MAX_RUNTIME_TICKS = 12_000;

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, AnimationDefinition> definitions = new HashMap<>();
    private final Map<UUID, List<AnimationSession>> active = new HashMap<>();

    public CustomAnimationManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "animations.yml");
    }

    /** Reads animations.yml. Existing customized files are never overwritten. */
    public int reload() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder for animations.yml.");
        }
        if (!file.exists()) {
            plugin.saveResource("animations.yml", false);
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("animations");
        Map<String, AnimationDefinition> loaded = new HashMap<>();
        if (section != null) {
            for (String rawId : section.getKeys(false)) {
                String id = rawId.toLowerCase(Locale.ROOT);
                ConfigurationSection cfg = section.getConfigurationSection(rawId);
                if (cfg == null) continue;

                String rawShape = cfg.getString("shape", "orbit").toLowerCase(Locale.ROOT);
                if (!SHAPES.contains(rawShape)) {
                    plugin.getLogger().warning("Animation '" + id + "' has unknown shape '" + rawShape + "'; using orbit.");
                    rawShape = "orbit";
                }

                Material material = Material.matchMaterial(cfg.getString("material", "PAPER"));
                if (material == null || !material.isItem()) {
                    plugin.getLogger().warning("Animation '" + id + "' has invalid item material; using PAPER.");
                    material = Material.PAPER;
                }

                NamespacedKey itemModel = null;
                String modelText = cfg.getString("item-model", "").trim();
                if (!modelText.isEmpty()) {
                    itemModel = NamespacedKey.fromString(modelText);
                    if (itemModel == null) {
                        plugin.getLogger().warning("Animation '" + id + "' has invalid item-model key '" + modelText + "'.");
                    }
                }

                Particle particle = null;
                String particleText = cfg.getString("particle", "END_ROD").trim().toUpperCase(Locale.ROOT);
                if (!particleText.isEmpty() && !particleText.equalsIgnoreCase("none")) {
                    try {
                        particle = Particle.valueOf(particleText);
                    } catch (IllegalArgumentException ignored) {
                        plugin.getLogger().warning("Animation '" + id + "' has unknown particle '" + particleText + "'; particles disabled.");
                    }
                }

                Sound sound = null;
                String soundText = cfg.getString("sound", "").trim();
                if (!soundText.isEmpty() && !soundText.equalsIgnoreCase("none")) {
                    try {
                        sound = Sound.valueOf(soundText.toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException ignored) {
                        plugin.getLogger().warning("Animation '" + id + "' has unknown sound '" + soundText + "'; sound disabled.");
                    }
                }

                int cycleTicks = Math.max(4, Math.min(1200, cfg.getInt("cycle-ticks", 80)));
                int copies = Math.max(1, Math.min(MAX_COPIES, cfg.getInt("copies", 1)));
                double scale = clamp(cfg.getDouble("scale", 0.55), 0.05, 4.0);
                double radius = clamp(cfg.getDouble("radius", 1.0), 0.0, 8.0);
                double height = clamp(cfg.getDouble("height", 1.5), -2.0, 8.0);
                double bob = clamp(cfg.getDouble("bob", 0.12), 0.0, 2.0);
                double rotationSpeed = clamp(cfg.getDouble("rotation-speed", 0.12), -1.5, 1.5);
                double tilt = clamp(cfg.getDouble("tilt", 0.0), -1.5, 1.5);
                int modelData = Math.max(0, cfg.getInt("custom-model-data", 0));
                int particleCount = Math.max(0, Math.min(16, cfg.getInt("particle-count", 1)));
                int particleInterval = Math.max(1, Math.min(100, cfg.getInt("particle-interval-ticks", 2)));
                int soundInterval = Math.max(1, Math.min(1200, cfg.getInt("sound-interval-ticks", 40)));
                float volume = (float) clamp(cfg.getDouble("sound-volume", 0.35), 0.0, 2.0);
                float pitch = (float) clamp(cfg.getDouble("sound-pitch", 1.0), 0.5, 2.0);
                boolean glow = cfg.getBoolean("glowing", false);

                loaded.put(id, new AnimationDefinition(
                        id, material, itemModel, modelData, rawShape, cycleTicks,
                        cfg.getBoolean("loop", false), copies, radius, height, bob,
                        scale, rotationSpeed, tilt, particle, particleCount,
                        particleInterval, sound, soundInterval, volume, pitch, glow
                ));
            }
        }

        definitions.clear();
        definitions.putAll(loaded);
        plugin.getLogger().info("Loaded " + definitions.size() + " custom animation(s) from animations.yml.");
        return definitions.size();
    }

    public Set<String> animationIds() {
        return new HashSet<>(definitions.keySet());
    }

    /**
     * Starts an animation around a player. A supplied run duration can bound a looping
     * effect; without one, a looping animation runs until /evofx stop or /evofx cancel.
     */
    public boolean play(Player target, String requestedId, Integer runSeconds) {
        if (target == null || !target.isOnline()) return false;
        String id = requestedId == null ? "" : requestedId.toLowerCase(Locale.ROOT);
        AnimationDefinition definition = definitions.get(id);
        if (definition == null) return false;

        stop(target);
        World world = target.getWorld();
        ItemStack item = createItem(definition);
        List<ItemDisplay> displays = new ArrayList<>();
        Location spawnAt = target.getLocation().clone().add(0, definition.height, 0);

        for (int i = 0; i < definition.copies; i++) {
            ItemDisplay display = world.spawn(spawnAt, ItemDisplay.class, entity -> {
                entity.setItemStack(item.clone());
                entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                entity.setBillboard(Display.Billboard.FIXED);
                entity.setPersistent(false);
                entity.setGravity(false);
                entity.setInvulnerable(true);
                entity.setSilent(true);
                entity.setGlowing(definition.glowing);
                entity.setInterpolationDelay(0);
                entity.setInterpolationDuration(1);
                entity.setTeleportDuration(1);
                entity.setTransformation(transformation(0, (float) definition.scale, definition.tilt));
            });
            displays.add(display);
        }

        int runLimit = runSeconds == null
                ? (definition.loop ? -1 : definition.cycleTicks)
                : Math.min(MAX_RUNTIME_TICKS, Math.max(1, runSeconds) * 20);
        AnimationSession session = new AnimationSession(target.getUniqueId(), displays);
        BukkitRunnable task = new BukkitRunnable() {
            private int age;

            @Override public void run() {
                if (!target.isOnline() || target.isDead() || target.getWorld() != world
                        || displays.stream().noneMatch(ItemDisplay::isValid)
                        || (runLimit >= 0 && age >= runLimit)) {
                    finish(session);
                    cancel();
                    return;
                }

                Location base = target.getLocation().clone();
                for (int i = 0; i < displays.size(); i++) {
                    ItemDisplay display = displays.get(i);
                    if (!display.isValid()) continue;

                    double cycleAge = age % definition.cycleTicks;
                    double progress = cycleAge / definition.cycleTicks;
                    double angle = age * definition.rotationSpeed + (Math.PI * 2 * i / definition.copies);
                    double x = 0;
                    double z = 0;
                    double y = definition.height;
                    double dynamicScale = definition.scale;

                    switch (definition.shape) {
                        case "spin" -> { /* rotates in place */ }
                        case "orbit" -> {
                            x = Math.cos(angle) * definition.radius;
                            z = Math.sin(angle) * definition.radius;
                            y += Math.sin(angle * 1.7) * definition.bob;
                        }
                        case "helix" -> {
                            x = Math.cos(angle) * definition.radius;
                            z = Math.sin(angle) * definition.radius;
                            y += (progress - 0.5) * Math.max(0.8, definition.radius * 1.8)
                                    + Math.sin(angle * 2.0) * definition.bob;
                        }
                        case "figure-eight" -> {
                            x = Math.sin(angle) * definition.radius;
                            z = Math.sin(angle) * Math.cos(angle) * definition.radius * 2.0;
                            y += Math.sin(angle * 2.0) * definition.bob;
                        }
                        case "rise" -> {
                            y += progress * Math.max(1.0, definition.radius * 2.0);
                            dynamicScale *= (float) (1.0 - 0.55 * progress);
                        }
                        case "slam" -> {
                            y += (1.0 - smooth(progress)) * Math.max(1.0, definition.radius * 2.0);
                            dynamicScale *= (float) (0.85 + 0.15 * Math.sin(progress * Math.PI));
                        }
                        case "pulse" -> {
                            y += Math.sin(angle) * definition.bob;
                            dynamicScale *= 0.68 + 0.32 * (0.5 + 0.5 * Math.sin(angle));
                        }
                        case "vortex" -> {
                            double expandingRadius = definition.radius * (0.12 + 0.88 * smooth(progress));
                            x = Math.cos(angle) * expandingRadius;
                            z = Math.sin(angle) * expandingRadius;
                            y += (progress - 0.5) * definition.radius;
                        }
                        default -> { /* validated on load; safe visual fallback */ }
                    }

                    Location at = base.clone().add(x, y, z);
                    display.setInterpolationDelay(0);
                    display.setInterpolationDuration(1);
                    display.setTeleportDuration(1);
                    display.teleport(at);
                    display.setTransformation(transformation((float) angle, (float) dynamicScale, definition.tilt));

                    if (definition.particle != null && definition.particleCount > 0
                            && age % definition.particleIntervalTicks == 0) {
                        spawnParticle(world, at, definition);
                    }
                }

                if (definition.sound != null && age % definition.soundIntervalTicks == 0) {
                    world.playSound(base, definition.sound, definition.soundVolume, definition.soundPitch);
                }
                age++;
            }
        };
        session.task = task;
        active.computeIfAbsent(target.getUniqueId(), ignored -> new ArrayList<>()).add(session);
        task.runTaskTimer(plugin, 0L, 1L);
        return true;
    }

    public void stop(Player target) {
        if (target != null) stop(target.getUniqueId());
    }

    public void stop(UUID playerId) {
        List<AnimationSession> sessions = active.get(playerId);
        if (sessions == null) return;
        for (AnimationSession session : new ArrayList<>(sessions)) finish(session);
    }

    public void stopAll() {
        for (UUID playerId : new ArrayList<>(active.keySet())) stop(playerId);
    }

    private void finish(AnimationSession session) {
        if (session.task != null) {
            try { session.task.cancel(); } catch (IllegalStateException ignored) { /* not scheduled yet */ }
        }
        List<AnimationSession> playerSessions = active.get(session.playerId);
        if (playerSessions != null) {
            playerSessions.remove(session);
            if (playerSessions.isEmpty()) active.remove(session.playerId);
        }
        for (ItemDisplay display : session.displays) {
            if (display.isValid()) display.remove();
        }
    }

    private ItemStack createItem(AnimationDefinition definition) {
        ItemStack item = new ItemStack(definition.material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (definition.itemModel != null) meta.setItemModel(definition.itemModel);
            if (definition.customModelData > 0) meta.setCustomModelData(definition.customModelData);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void spawnParticle(World world, Location at, AnimationDefinition definition) {
        if (definition.particle == Particle.DUST) {
            world.spawnParticle(Particle.DUST, at, definition.particleCount,
                    0.025, 0.025, 0.025,
                    new Particle.DustOptions(Color.fromRGB(180, 125, 255), 0.8f));
            return;
        }
        world.spawnParticle(definition.particle, at, definition.particleCount,
                0.025, 0.025, 0.025, 0.01);
    }

    private Transformation transformation(float rotation, float scale, double tilt) {
        return new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(rotation, 0, 1, 0),
                new Vector3f(scale, scale, scale),
                new AxisAngle4f((float) tilt, 1, 0, 0)
        );
    }

    private static double smooth(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record AnimationDefinition(
            String id, Material material, NamespacedKey itemModel, int customModelData,
            String shape, int cycleTicks, boolean loop, int copies, double radius,
            double height, double bob, double scale, double rotationSpeed, double tilt,
            Particle particle, int particleCount, int particleIntervalTicks,
            Sound sound, int soundIntervalTicks, float soundVolume, float soundPitch,
            boolean glowing
    ) {}

    private static final class AnimationSession {
        private final UUID playerId;
        private final List<ItemDisplay> displays;
        private BukkitRunnable task;

        private AnimationSession(UUID playerId, List<ItemDisplay> displays) {
            this.playerId = playerId;
            this.displays = displays;
        }
    }
}
