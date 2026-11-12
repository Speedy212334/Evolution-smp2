package me.ph4ntomgg.evolutionsmp.ability;

import me.ph4ntomgg.evolutionsmp.domain.EvolutionPath;
import me.ph4ntomgg.evolutionsmp.domain.PlayerProgress;
import me.ph4ntomgg.evolutionsmp.service.ProgressService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class OreResonanceService implements Listener {
    private static final int RADIUS = 50;
    private static final int MAX_MARKERS = 128;
    private static final int MAX_GLOBAL_MARKERS = 512;
    private static final long DURATION_TICKS = 1_200L;
    private static final long COOLDOWN_MS = 300_000L;

    private final JavaPlugin plugin;
    private final ProgressService progress;
    private final NamespacedKey cooldownKey;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private int activeMarkerCount;

    public OreResonanceService(JavaPlugin plugin, ProgressService progress) {
        this.plugin = plugin;
        this.progress = progress;
        this.cooldownKey = new NamespacedKey(plugin, "ore_resonance_cooldown");
    }

    public boolean activate(Player player) {
        PlayerProgress state = progress.get(player.getUniqueId());
        if (!state.assigned() || state.path() != EvolutionPath.PROSPECTOR || state.tier().rank() < 4) {
            player.sendMessage(ChatColor.RED + "Ore Resonance requires Prospector Tier IV.");
            return false;
        }
        UUID id = player.getUniqueId();
        if (sessions.containsKey(id)) {
            player.sendMessage(ChatColor.YELLOW + "Ore Resonance is already active.");
            return false;
        }
        long now = System.currentTimeMillis();
        long until = player.getPersistentDataContainer().getOrDefault(cooldownKey, PersistentDataType.LONG, 0L);
        if (until > now) {
            long seconds = Math.max(1L, (until - now + 999L) / 1_000L);
            player.sendMessage(ChatColor.RED + "Ore Resonance is recharging for " + seconds + "s.");
            return false;
        }
        player.getPersistentDataContainer().set(cooldownKey, PersistentDataType.LONG, now + COOLDOWN_MS);

        World world = player.getWorld();
        Location origin = player.getLocation().getBlock().getLocation();
        Set<Long> loadedChunks = new HashSet<>();
        int minChunkX = (origin.getBlockX() - RADIUS) >> 4;
        int maxChunkX = (origin.getBlockX() + RADIUS) >> 4;
        int minChunkZ = (origin.getBlockZ() - RADIUS) >> 4;
        int maxChunkZ = (origin.getBlockZ() + RADIUS) >> 4;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (world.isChunkLoaded(cx, cz)) loadedChunks.add(chunkKey(cx, cz));
            }
        }

        Session session = new Session(id, world, origin, loadedChunks);
        sessions.put(id, session);
        player.sendTitle(ChatColor.AQUA + "ORE RESONANCE", ChatColor.GRAY + "Nearby ores are being revealed.", 5, 35, 10);
        player.sendMessage(ChatColor.AQUA + "Ore Resonance activated. Highlights last 60 seconds.");
        world.playSound(origin, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.9f, 1.25f);
        world.spawnParticle(Particle.END_ROD, origin.clone().add(0.5, 1.0, 0.5), 24, 0.45, 0.4, 0.45, 0.025);

        session.scanTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> scanLayer(session), 1L, 1L);
        session.validationTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> validateMarkers(session), 20L, 20L);
        session.cleanupTask = Bukkit.getScheduler().runTaskLater(plugin, () -> finish(session), DURATION_TICKS);
        return true;
    }

    /** Clears the active scan and cooldown when a Path Changer is used. */
    public void resetPathState(UUID playerId) {
        Session active = sessions.get(playerId);
        if (active != null) finish(active);
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) player.getPersistentDataContainer().remove(cooldownKey);
    }

    private void scanLayer(Session session) {
        Player owner = Bukkit.getPlayer(session.owner);
        if (owner == null || !owner.isOnline() || owner.getWorld() != session.world) {
            finish(session);
            return;
        }
        if (session.nextDy > RADIUS || session.markers.size() >= MAX_MARKERS || activeMarkerCount >= MAX_GLOBAL_MARKERS) {
            session.capacityLimited = session.markers.size() >= MAX_MARKERS || activeMarkerCount >= MAX_GLOBAL_MARKERS;
            if (session.scanTask != null) session.scanTask.cancel();
            session.scanTask = null;
            if (session.markers.isEmpty()) {
                if (session.capacityLimited) {
                    owner.sendMessage(ChatColor.YELLOW + "Ore Resonance could not finish scanning because the server-wide visual safety limit is busy. Try again shortly.");
                } else {
                    owner.sendMessage(ChatColor.GRAY + "No ores were detected in currently loaded chunks within 50 blocks.");
                }
            } else {
                owner.sendMessage(ChatColor.AQUA + "Ore Resonance highlighted " + session.markers.size()
                        + " ore blocks. No coordinates were revealed.");
                if (session.capacityLimited) {
                    owner.sendMessage(ChatColor.GRAY + "The visual safety limit was reached; some nearby ores may not be highlighted.");
                }
            }
            return;
        }

        int dy = session.nextDy;
        int centerX = session.origin.getBlockX();
        int centerY = session.origin.getBlockY();
        int centerZ = session.origin.getBlockZ();
        int y = centerY + dy;
        if (y < session.world.getMinHeight() || y >= session.world.getMaxHeight()) {
            session.nextDy++;
            session.nextDx = -RADIUS;
            return;
        }

        Set<Long> loadedNow = new HashSet<>();
        for (long key : session.loadedChunks) {
            int cx = (int) (key >> 32);
            int cz = (int) key;
            if (session.world.isChunkLoaded(cx, cz)) loadedNow.add(key);
        }

        // Spread each horizontal layer over several ticks. A full 101x101 scan
        // in one tick multiplied across players can create avoidable MSPT spikes.
        int processedColumns = 0;
        int radiusSquared = RADIUS * RADIUS;
        while (session.nextDx <= RADIUS && processedColumns < 20
                && session.markers.size() < MAX_MARKERS && activeMarkerCount < MAX_GLOBAL_MARKERS) {
            int dx = session.nextDx++;
            processedColumns++;
            int remaining = radiusSquared - dx * dx - dy * dy;
            if (remaining < 0) continue;
            int maxDz = (int) Math.sqrt(remaining);
            int x = centerX + dx;
            for (int dz = -maxDz; dz <= maxDz && session.markers.size() < MAX_MARKERS
                    && activeMarkerCount < MAX_GLOBAL_MARKERS; dz++) {
                int z = centerZ + dz;
                if (!loadedNow.contains(chunkKey(x >> 4, z >> 4))) continue;
                var block = session.world.getBlockAt(x, y, z);
                OreStyle style = oreStyle(block.getType());
                if (style == null) continue;
                addMarker(session, block, style);
            }
        }
        if (session.nextDx > RADIUS) {
            session.nextDy++;
            session.nextDx = -RADIUS;
        }
    }

    private void addMarker(Session session, org.bukkit.block.Block block, OreStyle style) {
        Player owner = Bukkit.getPlayer(session.owner);
        if (owner == null || activeMarkerCount >= MAX_GLOBAL_MARKERS) return;
        Location location = block.getLocation();
        BlockDisplay display = session.world.spawn(location, BlockDisplay.class, marker -> {
            marker.setBlock(block.getBlockData());
            marker.setPersistent(false);
            marker.setInvulnerable(true);
            marker.setSilent(true);
            marker.setGravity(false);
            marker.setGlowing(true);
            marker.setBrightness(new Display.Brightness(15, 15));
            marker.setShadowRadius(0.0f);
            marker.setViewRange(1.0f);
        });

        Team team = teamFor(session, owner.getScoreboard(), style);
        team.addEntry(display.getUniqueId().toString());
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(owner)) viewer.hideEntity(plugin, display);
        }
        session.markers.add(new Marker(display, team, location.clone(), block.getType()));
        activeMarkerCount++;
        // The marker and its accent particles are both private to the activating player.
        owner.spawnParticle(Particle.DUST, location.clone().add(0.5, 0.5, 0.5), 2,
                0.18, 0.18, 0.18, 0.0,
                new Particle.DustOptions(style.rgb, 0.8f));
    }

    private void validateMarkers(Session session) {
        Player owner = Bukkit.getPlayer(session.owner);
        if (owner == null || !owner.isOnline() || owner.getWorld() != session.world) {
            finish(session);
            return;
        }
        var iterator = session.markers.iterator();
        while (iterator.hasNext()) {
            Marker marker = iterator.next();
            if (!marker.display.isValid()
                    || !session.world.isChunkLoaded(marker.location.getBlockX() >> 4, marker.location.getBlockZ() >> 4)
                    || session.world.getBlockAt(marker.location).getType() != marker.originalType) {
                removeMarker(marker);
                iterator.remove();
            }
        }
    }

    private void removeMarker(Marker marker) {
        if (marker.display.isValid()) marker.display.remove();
        marker.team.removeEntry(marker.display.getUniqueId().toString());
        activeMarkerCount = Math.max(0, activeMarkerCount - 1);
    }

    private Team teamFor(Session session, Scoreboard scoreboard, OreStyle style) {
        Team existing = session.teams.get(style.team);
        if (existing != null) return existing;

        // Never borrow or recolour a scoreboard team owned by another plugin.
        // Team names are limited to 16 characters, so use a compact owner/style key.
        String ownerKey = session.owner.toString().replace("-", "").substring(0, 10);
        String styleKey = Integer.toUnsignedString(style.team.hashCode(), 36);
        if (styleKey.length() > 5) styleKey = styleKey.substring(0, 5);
        String teamName = "e" + styleKey + ownerKey;
        Team team = scoreboard.getTeam(teamName);
        while (team != null) {
            // A collision is extraordinarily unlikely, but never mutate a foreign team.
            String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 4);
            teamName = "e" + styleKey.substring(0, Math.min(4, styleKey.length())) + randomSuffix;
            team = scoreboard.getTeam(teamName);
        }
        team = scoreboard.registerNewTeam(teamName);
        team.setColor(style.color);
        session.teams.put(style.team, team);
        return team;
    }

    private OreStyle oreStyle(Material material) {
        String name = material.name();
        if (name.equals("ANCIENT_DEBRIS")) return new OreStyle("evo_ore_debris", ChatColor.DARK_RED, org.bukkit.Color.fromRGB(155, 35, 50));
        if (name.contains("DIAMOND")) return new OreStyle("evo_ore_diamond", ChatColor.AQUA, org.bukkit.Color.fromRGB(85, 225, 255));
        if (name.contains("LAPIS")) return new OreStyle("evo_ore_lapis", ChatColor.BLUE, org.bukkit.Color.fromRGB(35, 75, 220));
        if (name.contains("REDSTONE")) return new OreStyle("evo_ore_redstone", ChatColor.RED, org.bukkit.Color.fromRGB(245, 45, 45));
        if (name.contains("EMERALD")) return new OreStyle("evo_ore_emerald", ChatColor.GREEN, org.bukkit.Color.fromRGB(45, 230, 105));
        if (name.contains("GOLD")) return new OreStyle("evo_ore_gold", ChatColor.GOLD, org.bukkit.Color.fromRGB(255, 205, 55));
        if (name.contains("IRON")) return new OreStyle("evo_ore_iron", ChatColor.WHITE, org.bukkit.Color.fromRGB(220, 225, 235));
        if (name.contains("COPPER")) return new OreStyle("evo_ore_copper", ChatColor.GOLD, org.bukkit.Color.fromRGB(225, 125, 75));
        if (name.contains("COAL")) return new OreStyle("evo_ore_coal", ChatColor.DARK_GRAY, org.bukkit.Color.fromRGB(95, 105, 115));
        if (name.contains("QUARTZ")) return new OreStyle("evo_ore_quartz", ChatColor.WHITE, org.bukkit.Color.fromRGB(245, 225, 210));
        return null;
    }

    private long chunkKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private void finish(Session session) {
        if (sessions.remove(session.owner) == null) return;
        if (session.scanTask != null) session.scanTask.cancel();
        if (session.cleanupTask != null) session.cleanupTask.cancel();
        if (session.validationTask != null) session.validationTask.cancel();
        for (Marker marker : session.markers) removeMarker(marker);
        session.markers.clear();
        for (Team team : session.teams.values()) {
            if (team.getEntries().isEmpty()) team.unregister();
        }
        session.teams.clear();
        Player owner = Bukkit.getPlayer(session.owner);
        if (owner != null && owner.isOnline()) {
            owner.sendMessage(ChatColor.GRAY + "Ore Resonance has faded.");
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player joining = event.getPlayer();
        for (Session session : sessions.values()) {
            if (joining.getUniqueId().equals(session.owner)) continue;
            for (Marker marker : session.markers) {
                if (marker.display.isValid()) joining.hideEntity(plugin, marker.display);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Session session = sessions.get(event.getPlayer().getUniqueId());
        if (session != null) finish(session);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Session session = sessions.get(event.getEntity().getUniqueId());
        if (session != null) finish(session);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Session session = sessions.get(event.getPlayer().getUniqueId());
        if (session != null) finish(session);
    }

    public void shutdown() {
        for (Session session : new ArrayList<>(sessions.values())) finish(session);
    }

    private record OreStyle(String team, ChatColor color, org.bukkit.Color rgb) { }
    private record Marker(BlockDisplay display, Team team, Location location, Material originalType) { }

    private static final class Session {
        private final UUID owner;
        private final World world;
        private final Location origin;
        private final Set<Long> loadedChunks;
        private final List<Marker> markers = new ArrayList<>();
        private final Map<String, Team> teams = new HashMap<>();
        private int nextDy = -RADIUS;
        private int nextDx = -RADIUS;
        private boolean capacityLimited;
        private BukkitTask scanTask;
        private BukkitTask cleanupTask;
        private BukkitTask validationTask;

        private Session(UUID owner, World world, Location origin, Set<Long> loadedChunks) {
            this.owner = owner;
            this.world = world;
            this.origin = origin;
            this.loadedChunks = loadedChunks;
        }
    }
}
