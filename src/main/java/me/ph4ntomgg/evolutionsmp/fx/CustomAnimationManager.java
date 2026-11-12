package me.ph4ntomgg.evolutionsmp.fx;

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
import org.bukkit.util.Vector;
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
            "spin", "orbit", "helix", "figure-eight", "rise", "slam", "pulse", "vortex", "physics-burst"
    );
    private static final int MAX_COPIES = 16;
    private static final int MAX_GLOBAL_DISPLAYS = 256;
    private static final int MAX_PARTICLES_PER_ANIMATION_TICK = 48;
    private static final int MAX_RUNTIME_TICKS = 12_000;

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, AnimationDefinition> definitions = new HashMap<>();
    private final Map<UUID, List<AnimationSession>> active = new HashMap<>();
    private int activeDisplayCount;

    public CustomAnimationManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "animations.yml");
    }

    /** Reads animations.yml. Existing customized files are never overwritten. */
    public int reload() {
        stopAll();
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
                    if (itemModel == null || !itemModel.getNamespace().equals("evolutionsmp")) {
                        plugin.getLogger().warning("Animation '" + id + "' must use an item-model key in the "
                                + "evolutionsmp namespace; model disabled.");
                        itemModel = null;
                    }
                }

                Particle particle = null;
                String particleText = cfg.getString("particle", "END_ROD").trim().toUpperCase(Locale.ROOT);
                if (!particleText.isEmpty() && !particleText.equalsIgnoreCase("none")) {
                    try {
                        particle = Particle.valueOf(particleText);
                        // The generic particle path below only supplies a numeric speed.
                        // Data-bearing particle types (BLOCK, ITEM, TRAIL, VIBRATION, etc.)
                        // need their own payload and would otherwise throw at runtime.
                        if (particle != Particle.DUST && particle != Particle.DUST_COLOR_TRANSITION
                                && particle.getDataType() != Void.class) {
                            plugin.getLogger().warning("Animation '" + id + "' uses data-bearing particle '"
                                    + particleText + "' without a supported payload; particles disabled.");
                            particle = null;
                        }
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
                int soundInterval = Math.max(10, Math.min(1200, cfg.getInt("sound-interval-ticks", 40)));
                float volume = (float) clamp(cfg.getDouble("sound-volume", 0.35), 0.0, 2.0);
                float pitch = (float) clamp(cfg.getDouble("sound-pitch", 1.0), 0.5, 2.0);
                boolean glow = cfg.getBoolean("glowing", false);
                double physicsGravity = clamp(cfg.getDouble("physics-gravity", 0.035), 0.0, 0.2);
                double physicsDrag = clamp(cfg.getDouble("physics-drag", 0.985), 0.80, 1.0);
                double physicsBounce = clamp(cfg.getDouble("physics-bounce", 0.42), 0.0, 0.9);
                int physicsLifeTicks = Math.max(4, Math.min(200, cfg.getInt("physics-life-ticks", 48)));
                double physicsInitialSpeed = clamp(cfg.getDouble("physics-initial-speed", 0.16), 0.0, 1.25);
                double physicsUpwardSpeed = clamp(cfg.getDouble("physics-upward-speed", 0.22), 0.0, 1.25);
                double particleSpread = clamp(cfg.getDouble("particle-spread", 0.025), 0.0, 1.0);
                double particleSpeed = clamp(cfg.getDouble("particle-speed", 0.01), 0.0, 2.0);
                float particleSize = (float) clamp(cfg.getDouble("particle-size", 0.8), 0.1, 4.0);
                Color particleColor = parseColor(cfg.getString("particle-color", "B47DFF"), Color.fromRGB(180, 125, 255));
                Color particleColorEnd = parseColor(cfg.getString("particle-color-end", "FFFFFF"), Color.WHITE);
                boolean trailEnabled = cfg.getBoolean("trail-enabled", false);
                Color trailColor = parseColor(cfg.getString("trail-color", "75E9FF"), Color.fromRGB(117, 233, 255));
                int trailDuration = Math.max(2, Math.min(100, cfg.getInt("trail-duration-ticks", 24)));
                boolean ringEnabled = cfg.getBoolean("ring-enabled", false);
                int ringPoints = Math.max(12, Math.min(48, cfg.getInt("ring-points", 32)));
                int ringLayers = Math.max(1, Math.min(3, cfg.getInt("ring-layers", 2)));
                int ringInterval = Math.max(1, Math.min(20, cfg.getInt("ring-interval-ticks", 2)));
                double ringRadius = clamp(cfg.getDouble("ring-radius", 2.0), 0.25, 8.0);
                double ringHeight = clamp(cfg.getDouble("ring-height", 0.06), -1.0, 2.0);
                float ringSize = (float) clamp(cfg.getDouble("ring-size", 1.1), 0.1, 4.0);
                Color ringColor = parseColor(cfg.getString("ring-color", "75E9FF"), Color.fromRGB(117, 233, 255));

                loaded.put(id, new AnimationDefinition(
                        id, material, itemModel, modelData, rawShape, cycleTicks,
                        cfg.getBoolean("loop", false), copies, radius, height, bob,
                        scale, rotationSpeed, tilt, particle, particleCount,
                        particleInterval, sound, soundInterval, volume, pitch, glow,
                        physicsGravity, physicsDrag, physicsBounce, physicsLifeTicks,
                        physicsInitialSpeed, physicsUpwardSpeed, particleSpread, particleSpeed,
                        particleSize, particleColor, particleColorEnd,
                        trailEnabled, trailColor, trailDuration,
                        ringEnabled, ringPoints, ringLayers, ringInterval,
                        ringRadius, ringHeight, ringSize, ringColor
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

        int existingDisplays = active.getOrDefault(target.getUniqueId(), List.of()).stream()
                .mapToInt(session -> session.displays.size()).sum();
        if (activeDisplayCount - existingDisplays + definition.copies > MAX_GLOBAL_DISPLAYS) return false;
        stop(target);
        World world = target.getWorld();
        ItemStack item = createItem(definition);
        List<ItemDisplay> displays = new ArrayList<>();
        Location spawnAt = target.getLocation().clone().add(0, definition.height, 0);

        try {
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
        } catch (RuntimeException exception) {
            for (ItemDisplay display : displays) if (display.isValid()) display.remove();
            plugin.getLogger().warning("Could not spawn animation '" + id + "': " + exception.getMessage());
            return false;
        }

        Vector[] physicsVelocities = new Vector[displays.size()];
        Location[] physicsPositions = new Location[displays.size()];
        for (int i = 0; i < displays.size(); i++) {
            double angle = (Math.PI * 2.0 * i / displays.size()) + 0.31;
            double radialSpeed = definition.physicsInitialSpeed * (0.72 + (i % 4) * 0.12);
            physicsVelocities[i] = new Vector(Math.cos(angle) * radialSpeed,
                    definition.physicsUpwardSpeed + (i % 5) * 0.055,
                    Math.sin(angle) * radialSpeed);
            physicsPositions[i] = spawnAt.clone().add(
                    Math.cos(angle) * 0.08, (i % 3) * 0.035, Math.sin(angle) * 0.08);
        }

        int runLimit = definition.loop
                ? (runSeconds == null ? -1 : (int) Math.min(MAX_RUNTIME_TICKS,
                        Math.max(1L, (long) runSeconds) * 20L))
                : definition.cycleTicks;
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
                int particlesRemaining = MAX_PARTICLES_PER_ANIMATION_TICK;
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
                        case "physics-burst" -> { /* integrated below using velocity, gravity, drag and block collision */ }
                        case "vortex" -> {
                            double expandingRadius = definition.radius * (0.12 + 0.88 * smooth(progress));
                            x = Math.cos(angle) * expandingRadius;
                            z = Math.sin(angle) * expandingRadius;
                            y += (progress - 0.5) * definition.radius;
                        }
                        default -> { /* validated on load; safe visual fallback */ }
                    }

                    if (definition.shape.equals("physics-burst")) {
                        if (age >= definition.physicsLifeTicks) {
                            display.remove();
                            continue;
                        }
                        Vector velocity = physicsVelocities[i];
                        velocity.setY(velocity.getY() - definition.physicsGravity);
                        velocity.multiply(definition.physicsDrag);
                        Location next = advancePhysics(world, physicsPositions[i], velocity,
                                definition.physicsBounce);
                        if (next == null) {
                            display.remove();
                            continue;
                        }
                        physicsPositions[i] = next;
                        x = next.getX() - base.getX();
                        y = next.getY() - base.getY();
                        z = next.getZ() - base.getZ();
                        dynamicScale *= Math.max(0.12, 1.0 - (double) age / definition.physicsLifeTicks);
                    }
                    Location at = base.clone().add(x, y, z);
                    display.setInterpolationDelay(0);
                    display.setInterpolationDuration(1);
                    display.setTeleportDuration(1);
                    display.teleport(at);
                    display.setTransformation(transformation((float) angle, (float) dynamicScale, definition.tilt));

                    if (definition.particle != null && definition.particleCount > 0
                            && age % definition.particleIntervalTicks == 0 && particlesRemaining > 0) {
                        int count = Math.min(definition.particleCount, particlesRemaining);
                        spawnParticle(world, at, definition, count);
                        particlesRemaining -= count;
                    }
                    if (definition.trailEnabled && definition.shape.equals("physics-burst")
                            && age % definition.particleIntervalTicks == 0 && particlesRemaining > 0) {
                        Location destination = base.clone().add(0.0, 1.0, 0.0);
                        world.spawnParticle(Particle.TRAIL, at, 1, 0.0, 0.0, 0.0,
                                new Particle.Trail(destination, definition.trailColor,
                                        definition.trailDurationTicks));
                        particlesRemaining--;
                    }
                }

                if (definition.ringEnabled && age % definition.ringIntervalTicks == 0
                        && particlesRemaining >= definition.ringPoints) {
                    // Three travelling sigil waves per cycle create a staged charge/release
                    // instead of a single ring that merely grows once and stays there.
                    double cyclePhase = (age % definition.cycleTicks) / (double) definition.cycleTicks;
                    double wavePhase = (cyclePhase * 3.0) % 1.0;
                    double waveEnvelope = Math.sin(Math.PI * wavePhase);
                    double ringProgress = smooth(wavePhase);
                    for (int layer = 0; layer < definition.ringLayers; layer++) {
                        if (particlesRemaining < definition.ringPoints) break;
                        double layerScale = 0.54 + layer * 0.46;
                        double direction = layer % 2 == 0 ? 1.0 : -0.78;
                        double radius = definition.ringRadius * layerScale * (0.22 + 0.78 * ringProgress);
                        double rotation = age * definition.rotationSpeed * direction + layer * 0.31;
                        double lift = definition.ringHeight + layer * 0.035
                                + Math.sin(age * 0.11 + layer * Math.PI) * 0.025;
                        Location ringCenter = base.clone().add(0.0, lift, 0.0);
                        float pulseSize = (float) (definition.ringSize * (0.72 + 0.28 * waveEnvelope));
                        Particle.DustOptions dust = new Particle.DustOptions(definition.ringColor,
                                Math.max(0.1f, pulseSize - layer * 0.10f));
                        for (int pointIndex = 0; pointIndex < definition.ringPoints && particlesRemaining > 0; pointIndex++) {
                            double ringAngle = rotation + Math.PI * 2.0 * pointIndex / definition.ringPoints;
                            double angularAccent = pointIndex % 4 == 0 ? 1.035 : 1.0;
                            Location point = ringCenter.clone().add(
                                    Math.cos(ringAngle) * radius * angularAccent,
                                    Math.sin(ringAngle * 3.0 + age * 0.08) * 0.018,
                                    Math.sin(ringAngle) * radius * angularAccent);
                            world.spawnParticle(Particle.DUST, point, 1, 0.0, 0.0, 0.0, 0.0, dust);
                            particlesRemaining--;
                        }
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
        activeDisplayCount += displays.size();
        try {
            task.runTaskTimer(plugin, 0L, 1L);
        } catch (RuntimeException exception) {
            finish(session);
            plugin.getLogger().warning("Could not schedule animation '" + id + "': " + exception.getMessage());
            return false;
        }
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
        if (session.finished) return;
        session.finished = true;
        activeDisplayCount = Math.max(0, activeDisplayCount - session.displays.size());
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

    private void spawnParticle(World world, Location at, AnimationDefinition definition, int count) {
        double spread = definition.particleSpread;
        if (definition.particle == Particle.DUST) {
            world.spawnParticle(Particle.DUST, at, count, spread, spread, spread,
                    new Particle.DustOptions(definition.particleColor, definition.particleSize));
            return;
        }
        if (definition.particle == Particle.DUST_COLOR_TRANSITION) {
            world.spawnParticle(Particle.DUST_COLOR_TRANSITION, at, count, spread, spread, spread,
                    new Particle.DustTransition(definition.particleColor, definition.particleColorEnd,
                            definition.particleSize));
            return;
        }
        world.spawnParticle(definition.particle, at, count, spread, spread, spread,
                definition.particleSpeed);
    }

    private Location advancePhysics(World world, Location origin, Vector velocity, double bounce) {
        if (origin == null || origin.getWorld() != world) return null;
        double maxDelta = Math.max(Math.abs(velocity.getX()),
                Math.max(Math.abs(velocity.getY()), Math.abs(velocity.getZ())));
        int steps = Math.max(1, Math.min(12, (int) Math.ceil(maxDelta / 0.22)));
        Location current = origin.clone();
        boolean grounded = false;

        for (int step = 0; step < steps; step++) {
            double dx = velocity.getX() / steps;
            double dy = velocity.getY() / steps;
            double dz = velocity.getZ() / steps;
            Location candidate = current.clone().add(dx, 0, 0);
            if (isPassableAndLoaded(world, candidate)) current.setX(candidate.getX());
            else velocity.setX(-velocity.getX() * bounce);

            candidate = current.clone().add(0, velocity.getY() / steps, 0);
            if (isPassableAndLoaded(world, candidate)) current.setY(candidate.getY());
            else {
                if (velocity.getY() < 0) {
                    grounded = true;
                    velocity.setY(Math.abs(velocity.getY()) > 0.055
                            ? Math.abs(velocity.getY()) * bounce : 0.0);
                } else {
                    velocity.setY(-velocity.getY() * bounce);
                }
            }

            candidate = current.clone().add(0, 0, velocity.getZ() / steps);
            if (isPassableAndLoaded(world, candidate)) current.setZ(candidate.getZ());
            else velocity.setZ(-velocity.getZ() * bounce);
        }

        if (grounded) {
            velocity.setX(velocity.getX() * 0.72);
            velocity.setZ(velocity.getZ() * 0.72);
        }
        if (velocity.lengthSquared() < 0.0009) {
            velocity.setX(0);
            velocity.setY(0);
            velocity.setZ(0);
        }
        return current;
    }

    private boolean isPassableAndLoaded(World world, Location location) {
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        if (y < world.getMinHeight() || y >= world.getMaxHeight()) return false;
        if (!world.isChunkLoaded(x >> 4, z >> 4)) return false;
        return world.getBlockAt(x, y, z).isPassable();
    }

    private static Color parseColor(String value, Color fallback) {
        if (value == null) return fallback;
        String hex = value.trim().replace("#", "");
        if (!hex.matches("[0-9a-fA-F]{6}")) return fallback;
        try {
            return Color.fromRGB(Integer.parseInt(hex, 16));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
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
            boolean glowing, double physicsGravity, double physicsDrag,
            double physicsBounce, int physicsLifeTicks, double physicsInitialSpeed,
            double physicsUpwardSpeed, double particleSpread, double particleSpeed,
            float particleSize, Color particleColor, Color particleColorEnd,
            boolean trailEnabled, Color trailColor, int trailDurationTicks,
            boolean ringEnabled, int ringPoints, int ringLayers, int ringIntervalTicks,
            double ringRadius, double ringHeight, float ringSize, Color ringColor
    ) {}

    private static final class AnimationSession {
        private final UUID playerId;
        private final List<ItemDisplay> displays;
        private BukkitRunnable task;
        private boolean finished;

        private AnimationSession(UUID playerId, List<ItemDisplay> displays) {
            this.playerId = playerId;
            this.displays = displays;
        }
    }
}
