package me.ph4ntomgg.evolutionsmp.ability;

import me.ph4ntomgg.evolutionsmp.domain.EvolutionPath;
import me.ph4ntomgg.evolutionsmp.domain.PlayerProgress;
import me.ph4ntomgg.evolutionsmp.service.ProgressService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AbilityService implements Listener {
    private static final long GAMBIt_DURATION_MS = 180_000L;
    private static final long ARMOR_LOCK_MS = 1_200_000L;
    private static final long HEART_DURATION_MS = 180_000L;
    private static final long COOLDOWN_MS = 1_800_000L;

    private final JavaPlugin plugin;
    private final ProgressService progress;
    private final File file;
    private final YamlConfiguration state;
    private final Map<UUID, Long> gambitUntil = new HashMap<>();
    private final Map<UUID, Long> armorLockUntil = new HashMap<>();
    private final Map<UUID, Long> heartUntil = new HashMap<>();
    private final Map<UUID, Long> cooldownUntil = new HashMap<>();
    private final Map<UUID, BukkitTask> heartTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> veilTasks = new HashMap<>();
    private final Map<UUID, java.util.Set<UUID>> veilViewers = new HashMap<>();
    private final Map<UUID, Boolean> previousInvisible = new HashMap<>();
    private final Map<UUID, Boolean> previousSilent = new HashMap<>();
    private final Map<UUID, Long> fractureCooldown = new HashMap<>();
    private final Map<UUID, Long> swapCooldown = new HashMap<>();

    public AbilityService(JavaPlugin plugin, ProgressService progress) {
        this.plugin = plugin;
        this.progress = progress;
        this.file = new File(plugin.getDataFolder(), "ability-state.yml");
        this.state = YamlConfiguration.loadConfiguration(file);
        load();
    }

    public boolean activateGambit(Player player) {
        PlayerProgress p = progress.get(player.getUniqueId());
        if (p.path() != EvolutionPath.TRADE || !p.apex()) {
            player.sendMessage(ChatColor.RED + "Idle Death Gambit requires Trade Apex.");
            return false;
        }
        long now = System.currentTimeMillis();
        if (cooldownUntil.getOrDefault(player.getUniqueId(), 0L) > now) {
            player.sendMessage(ChatColor.RED + "Gambit is still on cooldown.");
            return false;
        }
        UUID id = player.getUniqueId();
        gambitUntil.put(id, now + GAMBIt_DURATION_MS);
        cooldownUntil.put(id, now + COOLDOWN_MS);
        save();
        player.sendMessage(ChatColor.DARK_PURPLE + "IDLE DEATH GAMBIT " + ChatColor.GRAY + "— invulnerability for 3 minutes.");
        player.sendTitle(ChatColor.DARK_PURPLE + "IDLE DEATH GAMBIT", ChatColor.LIGHT_PURPLE + "Three minutes. No damage.", 5, 50, 10);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 0.7f);
        Bukkit.getScheduler().runTaskLater(plugin, () -> finishGambit(player), 3_600L);
        return true;
    }

    private void finishGambit(Player player) {
        UUID id = player.getUniqueId();
        if (gambitUntil.remove(id) == null) return;
        destroyAllArmor(player.getInventory());
        armorLockUntil.put(id, System.currentTimeMillis() + ARMOR_LOCK_MS);
        save();
        player.sendTitle(ChatColor.RED + "GAMBIT ENDED", ChatColor.GRAY + "Armor shattered. Armor locked for 20 minutes.", 5, 70, 15);
        player.sendMessage(ChatColor.RED + "All armor in your inventory has been destroyed. You cannot equip armor for 20 minutes.");
    }

    public boolean activateHeart(Player player) {
        PlayerProgress p = progress.get(player.getUniqueId());
        if (p.path() != EvolutionPath.CALAMITY || !p.apex()) {
            player.sendMessage(ChatColor.RED + "Minotaur's Heart requires Calamity Apex.");
            return false;
        }
        long now = System.currentTimeMillis();
        if (cooldownUntil.getOrDefault(player.getUniqueId(), 0L) > now) {
            player.sendMessage(ChatColor.RED + "Heart is still on cooldown.");
            return false;
        }
        UUID id = player.getUniqueId();
        cooldownUntil.put(id, now + COOLDOWN_MS);
        heartUntil.put(id, now + HEART_DURATION_MS);
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 1_200, 3, true, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1_200, 2, true, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 1_200, 1, true, false, true));
        player.sendTitle(ChatColor.RED + "MINOTAUR'S HEART", ChatColor.DARK_RED + "The heart of calamity awakens.", 5, 55, 10);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 0.8f);
        startHeartSymbol(player);
        save();
        return true;
    }

    private void startHeartSymbol(Player player) {
        UUID id = player.getUniqueId();
        BukkitTask old = heartTasks.remove(id);
        if (old != null) old.cancel();
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Long until = heartUntil.get(id);
            if (until == null || until <= System.currentTimeMillis() || !player.isOnline()) {
                heartUntil.remove(id);
                BukkitTask current = heartTasks.remove(id);
                if (current != null) current.cancel();
                save();
                return;
            }
            player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2.25, 0), 1, 0.12, 0.08, 0.12, 0);
        }, 0L, 10L);
        heartTasks.put(id, task);
    }


    public boolean activateVeil(Player player) {
        PlayerProgress p = progress.get(player.getUniqueId());
        if (p.path() != EvolutionPath.HUNT || p.tier().rank() < 4) {
            player.sendMessage(ChatColor.RED + "Veil requires Hunt Tier IV.");
            return false;
        }
        UUID id = player.getUniqueId();
        if (veilTasks.containsKey(id)) {
            player.sendMessage(ChatColor.YELLOW + "Veil is already active.");
            return false;
        }
        previousInvisible.put(id, player.isInvisible());
        previousSilent.put(id, player.isSilent());
        java.util.Set<UUID> viewers = new java.util.HashSet<>();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(player) || !viewer.getWorld().equals(player.getWorld())) continue;
            if (viewer.canSee(player)) viewers.add(viewer.getUniqueId());
            viewer.hidePlayer(plugin, player);
        }
        veilViewers.put(id, viewers);
        player.setInvisible(true);
        player.setSilent(true);
        player.sendTitle(ChatColor.DARK_PURPLE + "VEIL", ChatColor.GRAY + "Your presence fades.", 5, 35, 10);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.5f);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> restoreVeil(player), 600L);
        veilTasks.put(id, task);
        return true;
    }

    private void restoreVeil(Player player) {
        UUID id = player.getUniqueId();
        BukkitTask task = veilTasks.remove(id);
        if (task != null && !task.isCancelled()) task.cancel();
        java.util.Set<UUID> viewers = veilViewers.remove(id);
        if (viewers != null) for (UUID viewerId : viewers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) viewer.showPlayer(plugin, player);
        }
        player.setInvisible(previousInvisible.getOrDefault(id, false));
        player.setSilent(previousSilent.getOrDefault(id, false));
        previousInvisible.remove(id);
        previousSilent.remove(id);
        if (player.isOnline()) player.sendMessage(ChatColor.GRAY + "Veil has ended.");
    }


    public boolean activateVoidSwap(Player player, Player target) {
        PlayerProgress p = progress.get(player.getUniqueId());
        if (p.path() != EvolutionPath.ENDER || p.tier().rank() < 4) {
            player.sendMessage(ChatColor.RED + "Void Swap requires Ender Tier IV.");
            return false;
        }
        if (target == player || !target.isOnline() || !target.getWorld().equals(player.getWorld())
                || target.getLocation().distanceSquared(player.getLocation()) > 400.0) {
            player.sendMessage(ChatColor.RED + "Target must be another player within 20 blocks in your world.");
            return false;
        }
        long now = System.currentTimeMillis();
        if (swapCooldown.getOrDefault(player.getUniqueId(), 0L) > now) {
            player.sendMessage(ChatColor.RED + "Void Swap is still recharging.");
            return false;
        }
        Location first = player.getLocation().clone();
        Location second = target.getLocation().clone();
        if (!safeSwapLocation(first) || !safeSwapLocation(second)) {
            player.sendMessage(ChatColor.RED + "One of the swap locations is unsafe.");
            return false;
        }
        swapCooldown.put(player.getUniqueId(), now + 30_000L);
        player.teleport(second);
        target.teleport(first);
        player.getWorld().spawnParticle(Particle.PORTAL, first.add(0, 1, 0), 45, 0.35, 0.55, 0.35, 0.2);
        target.getWorld().spawnParticle(Particle.PORTAL, second.add(0, 1, 0), 45, 0.35, 0.55, 0.35, 0.2);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.9f, 0.7f);
        target.sendMessage(ChatColor.DARK_PURPLE + "An Ender has swapped your position.");
        return true;
    }

    private boolean safeSwapLocation(Location location) {
        return location.getBlock().isPassable() && location.clone().add(0, 1, 0).getBlock().isPassable()
                && location.clone().add(0, -1, 0).getBlock().getType().isSolid();
    }

    public boolean activateFracture(Player player) {
        PlayerProgress p = progress.get(player.getUniqueId());
        if (p.path() != EvolutionPath.CALAMITY || p.tier().rank() < 4) {
            player.sendMessage(ChatColor.RED + "Calamity Fracture requires Calamity Tier IV.");
            return false;
        }
        long now = System.currentTimeMillis();
        if (fractureCooldown.getOrDefault(player.getUniqueId(), 0L) > now) {
            player.sendMessage(ChatColor.RED + "Fracture is still recharging.");
            return false;
        }
        fractureCooldown.put(player.getUniqueId(), now + 20_000L);
        Location center = player.getLocation();
        for (int i = 0; i < 36; i++) {
            double angle = Math.PI * 2 * i / 36.0;
            Location point = center.clone().add(Math.cos(angle) * 3.0, 0.15, Math.sin(angle) * 3.0);
            player.getWorld().spawnParticle(Particle.CRIT, point, 3, 0.08, 0.08, 0.08, 0.02);
        }
        player.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 0.85f);
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(6, 3, 6)) {
            if (!(entity instanceof LivingEntity living) || entity instanceof Player || !entity.isValid()) continue;
            living.damage(8.0, player);
            Vector direction = living.getLocation().toVector().subtract(center.toVector());
            if (direction.lengthSquared() > 0.01) living.setVelocity(direction.normalize().multiply(1.25).setY(0.45));
        }
        return true;
    }

    @EventHandler
    public void onJoinVeil(PlayerJoinEvent event) {
        Player joining = event.getPlayer();
        for (UUID hiddenId : new java.util.HashSet<>(veilTasks.keySet())) {
            Player hidden = Bukkit.getPlayer(hiddenId);
            if (hidden != null && hidden.isOnline() && !joining.equals(hidden)
                    && joining.getWorld().equals(hidden.getWorld())) {
                veilViewers.computeIfAbsent(hiddenId, ignored -> new java.util.HashSet<>()).add(joining.getUniqueId());
                joining.hidePlayer(plugin, hidden);
            }
        }
    }

    @EventHandler
    public void onQuitVeil(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        for (java.util.Set<UUID> viewers : veilViewers.values()) viewers.remove(id);
        if (veilTasks.containsKey(id)) restoreVeil(event.getPlayer());
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && gambitUntil.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onArmorClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !armorLocked(player)) return;
        if (isArmor(event.getCursor()) || isArmor(event.getCurrentItem()) || event.getSlotType() == org.bukkit.event.inventory.InventoryType.SlotType.ARMOR) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Armor is locked by Gambit for " + remainingMinutes(player) + " more minutes.");
        }
    }

    @EventHandler
    public void onArmorDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !armorLocked(player)) return;
        if (isArmor(event.getOldCursor()) || event.getNewItems().values().stream().anyMatch(this::isArmor)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Armor is locked by Gambit for " + remainingMinutes(player) + " more minutes.");
        }
    }

    @EventHandler
    public void onArmorQuickEquip(PlayerInteractEvent event) {
        if (!armorLocked(event.getPlayer())) return;
        if (event.getItem() != null && isArmor(event.getItem())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Armor is locked by Gambit for " + remainingMinutes(event.getPlayer()) + " more minutes.");
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long gambitEnd = gambitUntil.get(id);
        if (gambitEnd != null) {
            if (gambitEnd > now) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> finishGambit(player), Math.max(1L, (gambitEnd - now) / 50L));
            } else {
                finishGambit(player);
            }
        }
        Long heartEnd = heartUntil.get(id);
        if (heartEnd != null && heartEnd > now) startHeartSymbol(player);
        else heartUntil.remove(id);
    }

    private boolean armorLocked(Player player) {
        long until = armorLockUntil.getOrDefault(player.getUniqueId(), 0L);
        if (until <= System.currentTimeMillis()) {
            armorLockUntil.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    private long remainingMinutes(Player player) {
        long ms = armorLockUntil.getOrDefault(player.getUniqueId(), 0L) - System.currentTimeMillis();
        return Math.max(1L, (ms + 59_999L) / 60_000L);
    }

    private boolean isArmor(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        String name = item.getType().name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS") || name.equals("ELYTRA") || name.equals("TURTLE_HELMET");
    }

    private void destroyAllArmor(PlayerInventory inventory) {
        ItemStack[] contents = inventory.getStorageContents();
        for (int i = 0; i < contents.length; i++) if (isArmor(contents[i])) contents[i] = null;
        inventory.setStorageContents(contents);
        inventory.setHelmet(null);
        inventory.setChestplate(null);
        inventory.setLeggings(null);
        inventory.setBoots(null);
    }

    private void load() {
        for (String key : state.getConfigurationSection("players") == null ? java.util.Set.<String>of() : state.getConfigurationSection("players").getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                String root = "players." + key + ".";
                long gambit = state.getLong(root + "gambit-until", 0);
                long armor = state.getLong(root + "armor-lock-until", 0);
                long heart = state.getLong(root + "heart-until", 0);
                long cooldown = state.getLong(root + "cooldown-until", 0);
                if (gambit > 0) gambitUntil.put(id, gambit);
                if (armor > 0) armorLockUntil.put(id, armor);
                if (heart > 0) heartUntil.put(id, heart);
                if (cooldown > 0) cooldownUntil.put(id, cooldown);
            } catch (IllegalArgumentException ignored) { }
        }
    }

    private void save() {
        state.set("players", null);
        java.util.Set<UUID> ids = new java.util.HashSet<>();
        ids.addAll(gambitUntil.keySet()); ids.addAll(armorLockUntil.keySet());
        ids.addAll(heartUntil.keySet()); ids.addAll(cooldownUntil.keySet());
        for (UUID id : ids) {
            String root = "players." + id + ".";
            state.set(root + "gambit-until", gambitUntil.getOrDefault(id, 0L));
            state.set(root + "armor-lock-until", armorLockUntil.getOrDefault(id, 0L));
            state.set(root + "heart-until", heartUntil.getOrDefault(id, 0L));
            state.set(root + "cooldown-until", cooldownUntil.getOrDefault(id, 0L));
        }
        try { state.save(file); }
        catch (IOException ex) { plugin.getLogger().severe("Could not save ability state: " + ex.getMessage()); }
    }

    public void shutdown() {
        heartTasks.values().forEach(BukkitTask::cancel);
        heartTasks.clear();
        for (UUID id : new java.util.HashSet<>(veilTasks.keySet())) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) restoreVeil(player);
        }
        veilTasks.values().forEach(BukkitTask::cancel);
        veilTasks.clear();
        save();
    }
}
