package me.ph4ntomgg.evolutionsmp.fx;

import me.ph4ntomgg.evolutionsmp.domain.EvolutionPath;
import me.ph4ntomgg.evolutionsmp.item.EvolutionItems;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Multi-stage path awakening cinematic.
 *
 * Seven unique path sigils rise, accelerate around the player, and six launch
 * away on different trajectories. The chosen sigil remains, locks above the
 * player's head, then resolves in a path-coloured finale. Five small gold
 * crystals form a second, tighter helix and collapse into the chosen sigil.
 *
 * All entities are non-persistent and are removed on completion, quit/death
 * cleanup, world changes, replacement animations, and plugin shutdown.
 */
public final class CinematicEffects {
    private static final int PATH_COUNT = 7;
    private static final int GEM_COUNT = 5;
    private static final int ORBIT_END_TICK = 72;
    private static final int TOTAL_TICKS = 112;
    private static final double TWO_PI = Math.PI * 2.0;
    private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));

    private final JavaPlugin plugin;
    private final EvolutionItems items;
    private final Map<UUID, BukkitTask> activeTasks = new HashMap<>();
    private final Map<UUID, List<ItemDisplay>> activeDisplays = new HashMap<>();

    public CinematicEffects(JavaPlugin plugin) {
        this.plugin = plugin;
        this.items = new EvolutionItems(plugin);
    }

    public void revealPath(Player player, EvolutionPath chosen) {
        if (player == null || !player.isOnline() || chosen == null) return;

        UUID id = player.getUniqueId();
        stop(id);

        World world = player.getWorld();
        EvolutionPath[] paths = EvolutionPath.values();
        if (paths.length != PATH_COUNT) {
            plugin.getLogger().warning("Path reveal expects exactly seven Evolution paths; found " + paths.length);
            return;
        }

        List<ItemDisplay> pathDisplays = new ArrayList<>(PATH_COUNT);
        List<ItemDisplay> gemDisplays = new ArrayList<>(GEM_COUNT);
        List<ItemDisplay> allDisplays = new ArrayList<>(PATH_COUNT + GEM_COUNT);
        Location spawn = player.getLocation().clone().add(0.0, 0.9, 0.0);

        try {
            for (EvolutionPath path : paths) {
                ItemDisplay display = spawnDisplay(world, spawn, items.pathIcon(path), 0.78f, true);
                pathDisplays.add(display);
                allDisplays.add(display);
            }
            for (int i = 0; i < GEM_COUNT; i++) {
                // The resource pack turns this vanilla carrier into the FX Gold Gem.
                ItemDisplay display = spawnDisplay(world, spawn,
                        new ItemStack(Material.GOLD_INGOT), 0.38f, false);
                gemDisplays.add(display);
                allDisplays.add(display);
            }
        } catch (RuntimeException exception) {
            allDisplays.forEach(entity -> { if (entity.isValid()) entity.remove(); });
            plugin.getLogger().warning("Could not start the path reveal: " + exception.getMessage());
            return;
        }

        activeDisplays.put(id, allDisplays);
        player.sendTitle(
                ChatColor.GOLD + "EVOLUTION AWAKENED",
                ChatColor.WHITE + chosen.displayName(),
                8, 54, 14
        );
        player.sendMessage(ChatColor.DARK_PURPLE + "✦ " + ChatColor.LIGHT_PURPLE
                + "Your path: " + ChatColor.WHITE + chosen.displayName()
                + ChatColor.DARK_PURPLE + " ✦");
        world.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.85f, 0.68f);
        world.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.55f, 0.72f);

        BukkitRunnable animation = new BukkitRunnable() {
            private int tick;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()
                        || player.getWorld() != world || tick >= TOTAL_TICKS) {
                    cleanup(id);
                    cancel();
                    return;
                }

                Location center = player.getLocation().clone().add(0.0, 1.15, 0.0);
                Location ground = player.getLocation().clone().add(0.0, 0.08, 0.0);
                double spin = tick * 0.145 + tick * tick * 0.00135;
                double lift = smooth(Math.min(1.0, tick / 18.0));

                // The ground seal expands in two counter-rotating layers.
                if (tick % 5 == 0 && tick < 94) {
                    double ringRadius = 1.0 + lift * 0.9 + 0.12 * Math.sin(tick * 0.11);
                    drawDustRing(ground, color(chosen), 1.05f, ringRadius, 32, spin);
                    drawDustRing(ground.clone().add(0.0, 0.025, 0.0),
                            Color.fromRGB(220, 235, 255), 0.72f,
                            ringRadius * 0.62, 24, -spin * 1.22);
                }

                // Three ascending particle strands make the centre feel charged.
                if (tick < ORBIT_END_TICK && tick % 2 == 0) {
                    for (int strand = 0; strand < 3; strand++) {
                        double angle = -spin * 1.4 + strand * TWO_PI / 3.0;
                        double y = 0.18 + ((tick * 0.105 + strand * 0.9) % 2.15);
                        double radius = 0.24 + 0.24 * Math.sin((y / 2.15) * Math.PI);
                        Location spark = center.clone().add(
                                Math.cos(angle) * radius, y - 0.8, Math.sin(angle) * radius);
                        world.spawnParticle(strand == 1 ? Particle.ENCHANT : Particle.END_ROD,
                                spark, 1, 0.0, 0.0, 0.0, 0.0);
                    }
                }

                for (int i = 0; i < pathDisplays.size(); i++) {
                    ItemDisplay display = pathDisplays.get(i);
                    if (!display.isValid()) continue;
                    EvolutionPath path = paths[i];

                    if (tick < ORBIT_END_TICK) {
                        // All seven icons rise into a fast, accelerating ring around the head.
                        double angle = i * TWO_PI / PATH_COUNT + spin;
                        double radius = (2.05 - 0.63 * lift)
                                + 0.07 * Math.sin(tick * 0.16 + i * 0.8);
                        double height = 0.05 + 0.38 * lift
                                + Math.sin(angle * 1.55 + tick * 0.055) * 0.19;
                        Location point = center.clone().add(
                                Math.cos(angle) * radius, height, Math.sin(angle) * radius);
                        display.teleport(point);
                        display.setRotation((float) Math.toDegrees(angle + Math.PI / 2.0),
                                (float) (Math.sin(tick * 0.09 + i) * 7.0));
                        setDisplayTransform(display,
                                0.72f + 0.08f * (float) Math.sin(tick * 0.12 + i),
                                (float) (spin * 0.8 + i * 0.4),
                                (float) (Math.sin(tick * 0.1 + i) * 0.12));
                        if (tick % 4 == 0) {
                            world.spawnParticle(pathParticle(path), point, 1,
                                    0.015, 0.015, 0.015, 0.0);
                        }
                    } else if (path == chosen) {
                        // The winner spirals inward and settles directly above the player.
                        double progress = smooth(Math.min(1.0, (tick - ORBIT_END_TICK) / 17.0));
                        double oldAngle = i * TWO_PI / PATH_COUNT + spin;
                        double x = Math.cos(oldAngle) * (1.42 * (1.0 - progress));
                        double z = Math.sin(oldAngle) * (1.42 * (1.0 - progress));
                        double y = 0.3 + progress * 1.05
                                + Math.sin(tick * 0.24) * (0.12 * (1.0 - progress));
                        Location point = center.clone().add(x, y, z);
                        display.teleport(point);
                        display.setRotation((float) (tick * 13.0), (float) (Math.sin(tick * 0.18) * 8.0));
                        float pulse = 0.86f + 0.1f * (float) Math.sin(tick * 0.32);
                        setDisplayTransform(display, pulse, tick * 0.11f, 0.08f);
                        if (tick >= 91 && tick % 3 == 0) {
                            world.spawnParticle(pathParticle(chosen), point, 2,
                                    0.035, 0.035, 0.035, 0.01);
                        }
                    } else {
                        // Every rejected path is launched along its own direction, not stacked.
                        int stagger = i % 3;
                        double progress = Math.max(0.0,
                                Math.min(1.0, (tick - (ORBIT_END_TICK + stagger * 2.0)) / 17.0));
                        if (progress >= 1.0) {
                            display.remove();
                            continue;
                        }
                        double angle = i * GOLDEN_ANGLE + 0.35;
                        double distance = 1.35 + 4.35 * easeOut(progress);
                        double x = Math.cos(angle) * distance;
                        double z = Math.sin(angle) * distance;
                        double y = 0.28 + progress * (1.2 + (i % 3) * 0.32)
                                + Math.sin(progress * Math.PI) * 0.42;
                        Location point = center.clone().add(x, y, z);
                        display.teleport(point);
                        display.setRotation((float) Math.toDegrees(angle + tick * 0.19),
                                (float) (-18.0 * progress));
                        setDisplayTransform(display, 0.78f * (1.0f - 0.28f * (float) progress),
                                (float) (tick * 0.24 + i), 0.24f);
                        if (tick % 2 == 0) {
                            world.spawnParticle(pathParticle(path), point, 2,
                                    0.025, 0.025, 0.025, 0.01);
                            world.spawnParticle(Particle.END_ROD, point, 1,
                                    0.0, 0.0, 0.0, 0.0);
                        }
                    }
                }

                // The five Gold Gems trace a tighter counter-rotating helix, then converge.
                for (int i = 0; i < gemDisplays.size(); i++) {
                    ItemDisplay gem = gemDisplays.get(i);
                    if (!gem.isValid()) continue;
                    double angle = -spin * 1.32 + i * TWO_PI / GEM_COUNT;
                    if (tick < 86) {
                        double radius = 0.62 + 0.2 * Math.sin(tick * 0.12 + i);
                        double y = -0.08 + ((tick * 0.08 + i * 0.38) % 1.25);
                        Location point = center.clone().add(
                                Math.cos(angle) * radius, y, Math.sin(angle) * radius);
                        gem.teleport(point);
                        setDisplayTransform(gem, 0.34f + 0.035f * (float) Math.sin(tick * 0.2 + i),
                                (float) (angle + tick * 0.16), 0.18f);
                        if (tick % 3 == 0) {
                            world.spawnParticle(Particle.ELECTRIC_SPARK, point, 1,
                                    0.01, 0.01, 0.01, 0.0);
                        }
                    } else {
                        double progress = smooth(Math.min(1.0, (tick - 86) / 12.0));
                        double x = Math.cos(angle) * (0.68 * (1.0 - progress));
                        double z = Math.sin(angle) * (0.68 * (1.0 - progress));
                        Location point = center.clone().add(x, 1.2 + progress * 0.12, z);
                        gem.teleport(point);
                        setDisplayTransform(gem, Math.max(0.04f, 0.34f * (1.0f - (float) progress)),
                                (float) (angle + tick * 0.22), 0.2f);
                        if (tick % 2 == 0) {
                            world.spawnParticle(Particle.END_ROD, point, 1,
                                    0.015, 0.015, 0.015, 0.0);
                        }
                        if (progress >= 1.0) gem.remove();
                    }
                }

                // Layered core pulses: restrained during orbit, powerful at the reveal.
                if (tick < 88 && tick % 4 == 0) {
                    Location core = center.clone().add(0.0, 0.22, 0.0);
                    world.spawnParticle(Particle.ENCHANT, core, 4,
                            0.22, 0.35, 0.22, 0.02);
                    world.spawnParticle(Particle.PORTAL, core, 2,
                            0.18, 0.25, 0.18, 0.025);
                }
                if (tick == 74) {
                    world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.95f, 1.35f);
                    world.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.7f, 1.05f);
                }
                if (tick == 94) {
                    drawDustRing(ground, color(chosen), 1.45f, 2.45, 64, -0.1);
                    drawDustRing(ground.clone().add(0, 0.04, 0),
                            accentColor(chosen), 1.1f, 1.55, 52, 0.18);
                    world.spawnParticle(Particle.TOTEM_OF_UNDYING,
                            center.clone().add(0, 0.5, 0), 34, 0.42, 0.72, 0.42, 0.08);
                    world.spawnParticle(Particle.FIREWORK,
                            center.clone().add(0, 0.7, 0), 26, 0.5, 0.78, 0.5, 0.07);
                    world.spawnParticle(pathParticle(chosen),
                            center.clone().add(0, 1.1, 0), 48, 0.35, 0.65, 0.35, 0.055);
                    world.playSound(center, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, pathPitch(chosen));
                    world.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.25f);
                }
                if (tick >= 94 && tick % 3 == 0) {
                    Location chosenPoint = center.clone().add(0, 1.35, 0);
                    world.spawnParticle(Particle.DUST, chosenPoint, 5,
                            0.14, 0.16, 0.14, 0.0,
                            new Particle.DustOptions(color(chosen), 1.25f));
                    world.spawnParticle(pathParticle(chosen), chosenPoint, 1,
                            0.025, 0.025, 0.025, 0.0);
                }

                tick++;
                if (tick >= TOTAL_TICKS) {
                    cleanup(id);
                    cancel();
                }
            }
        };

        BukkitTask task = animation.runTaskTimer(plugin, 0L, 1L);
        activeTasks.put(id, task);
    }

    private ItemDisplay spawnDisplay(World world, Location location, ItemStack item,
                                     float scale, boolean glowing) {
        ItemDisplay display = world.spawn(location, ItemDisplay.class, entity -> {
            entity.setItemStack(item);
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setPersistent(false);
            entity.setInvulnerable(true);
            entity.setSilent(true);
            entity.setGravity(false);
            entity.setGlowing(glowing);
            entity.setShadowRadius(0.0f);
            entity.setShadowStrength(0.0f);
            entity.setInterpolationDelay(0);
            entity.setInterpolationDuration(1);
            entity.setTeleportDuration(1);
            entity.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 1, 0, 0),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f(0, 1, 0, 0)
            ));
        });
        return display;
    }

    private void setDisplayTransform(ItemDisplay display, float scale, float rotation, float tilt) {
        display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(rotation, 0, 1, 0),
                new Vector3f(scale, scale, scale),
                new AxisAngle4f(tilt, 1, 0, 0)
        ));
    }

    private void drawDustRing(Location center, Color color, float size,
                              double radius, int points, double rotation) {
        World world = center.getWorld();
        if (world == null) return;
        Particle.DustOptions dust = new Particle.DustOptions(color, size);
        int safePoints = Math.max(12, Math.min(72, points));
        for (int i = 0; i < safePoints; i++) {
            double angle = rotation + TWO_PI * i / safePoints;
            Location point = center.clone().add(
                    Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
            world.spawnParticle(Particle.DUST, point, 1,
                    0.0, 0.0, 0.0, 0.0, dust);
        }
    }

    private Particle pathParticle(EvolutionPath path) {
        return switch (path) {
            case HUNT -> Particle.CRIT;
            case ALCHEMY -> Particle.WITCH;
            case ENCHANTMENT -> Particle.ENCHANT;
            case PROSPECTOR -> Particle.ELECTRIC_SPARK;
            case ENDER -> Particle.REVERSE_PORTAL;
            case TRADE -> Particle.HAPPY_VILLAGER;
            case CALAMITY -> Particle.SOUL_FIRE_FLAME;
        };
    }

    private Color color(EvolutionPath path) {
        return switch (path) {
            case HUNT -> Color.fromRGB(255, 76, 52);
            case ALCHEMY -> Color.fromRGB(91, 255, 145);
            case ENCHANTMENT -> Color.fromRGB(172, 92, 255);
            case PROSPECTOR -> Color.fromRGB(255, 196, 55);
            case ENDER -> Color.fromRGB(72, 210, 255);
            case TRADE -> Color.fromRGB(75, 255, 202);
            case CALAMITY -> Color.fromRGB(255, 48, 88);
        };
    }

    private Color accentColor(EvolutionPath path) {
        return switch (path) {
            case HUNT -> Color.fromRGB(255, 224, 164);
            case ALCHEMY -> Color.fromRGB(220, 255, 170);
            case ENCHANTMENT -> Color.fromRGB(220, 190, 255);
            case PROSPECTOR -> Color.fromRGB(255, 245, 160);
            case ENDER -> Color.fromRGB(185, 240, 255);
            case TRADE -> Color.fromRGB(185, 255, 225);
            case CALAMITY -> Color.fromRGB(255, 180, 195);
        };
    }

    private float pathPitch(EvolutionPath path) {
        return switch (path) {
            case HUNT -> 1.25f;
            case ALCHEMY -> 1.0f;
            case ENCHANTMENT -> 1.4f;
            case PROSPECTOR -> 0.8f;
            case ENDER -> 0.65f;
            case TRADE -> 1.1f;
            case CALAMITY -> 0.55f;
        };
    }

    private double smooth(double value) {
        double t = Math.max(0.0, Math.min(1.0, value));
        return t * t * (3.0 - 2.0 * t);
    }

    private double easeOut(double value) {
        double t = Math.max(0.0, Math.min(1.0, value));
        return 1.0 - Math.pow(1.0 - t, 3.0);
    }

    private void stop(UUID id) {
        BukkitTask previous = activeTasks.remove(id);
        if (previous != null) previous.cancel();
        removeDisplays(id);
    }

    private void cleanup(UUID id) {
        activeTasks.remove(id);
        removeDisplays(id);
    }

    private void removeDisplays(UUID id) {
        List<ItemDisplay> displays = activeDisplays.remove(id);
        if (displays != null) {
            for (ItemDisplay display : displays) {
                if (display.isValid()) display.remove();
            }
        }
    }

    public void shutdown() {
        for (UUID id : new ArrayList<>(activeTasks.keySet())) stop(id);
        for (UUID id : new ArrayList<>(activeDisplays.keySet())) removeDisplays(id);
        activeTasks.clear();
        activeDisplays.clear();
    }
}
