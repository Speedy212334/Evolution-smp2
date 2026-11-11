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
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
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
    private final Map<UUID, BukkitTask> gambitTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> veilTasks = new HashMap<>();
    private final Map<UUID, Long> veilCooldownUntil = new HashMap<>();
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
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> finishGambit(player), 3_600L);
        gambitTasks.put(id, task);
        return true;
    }

    private void finishGambit(Player player) {
        UUID id = player.getUniqueId();
        BukkitTask task = gambitTasks.remove(id);
        if (task != null && !task.isCancelled()) task.cancel();
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
        long now = System.currentTimeMillis();
        long cooldownEnd = veilCooldownUntil.getOrDefault(id, 0L);
        if (cooldownEnd > now) {
            long seconds = Math.max(1L, (cooldownEnd - now + 999L) / 1_000L);
            player.sendMessage(ChatColor.RED + "Veil is recharging for " + seconds + "s.");
            return false;
        }
        veilCooldownUntil.put(id, now + 60_000L);
        save();
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
        // Teleports can be cancelled by protection plugins. Treat the swap as a
        // transaction: do not consume cooldown unless both sides reach their destination.
        if (!player.teleport(second)) {
            player.sendMessage(ChatColor.RED + "Void Swap was blocked by another plugin.");
            return false;
        }
        if (!target.teleport(first)) {
            boolean rolledBack = player.teleport(first);
            if (!rolledBack) plugin.getLogger().warning("Void Swap rollback failed for " + player.getUniqueId());
            player.sendMessage(ChatColor.RED + "Void Swap could not move the target; no cooldown was consumed.");
            return false;
        }
        swapCooldown.put(player.getUniqueId(), now + 30_000L);
        Location firstFx = first.clone().add(0, 1, 0);
        Location secondFx = second.clone().add(0, 1, 0);
        player.getWorld().spawnParticle(Particle.PORTAL, firstFx, 18, 0.2, 0.35, 0.2, 0.08);
        target.getWorld().spawnParticle(Particle.PORTAL, secondFx, 18, 0.2, 0.35, 0.2, 0.08);
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
        Location center = player.getLocation().clone();
        org.bukkit.World world = player.getWorld();
        player.sendMessage(ChatColor.DARK_RED + "Fracture is charging...");
        world.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 0.65f);

        // A readable expanding ground ring telegraphs the shockwave before impact.
        Bukkit.getScheduler().runTaskTimer(plugin, new org.bukkit.scheduler.BukkitRunnable() {
            private int tick;

            @Override public void run() {
                if (!player.isOnline() || player.getWorld() != world || tick > 8) {
                    cancel();
                    return;
                }
                double radius = 1.0 + tick * 0.65;
                for (int i = 0; i < 48; i++) {
                    double angle = Math.PI * 2.0 * i / 48.0;
                    Location point = center.clone().add(Math.cos(angle) * radius, 0.12, Math.sin(angle) * radius);
                    world.spawnParticle(Particle.DUST, point, 1, 0.015, 0.01, 0.015, 0.0,
                            new Particle.DustOptions(org.bukkit.Color.fromRGB(178, 28, 64), 1.15f));
                    if (tick % 2 == 0) {
                        world.spawnParticle(Particle.ELECTRIC_SPARK, point, 1, 0.02, 0.02, 0.02, 0.0);
                    }
                }

                if (tick == 8) {
                    world.spawnParticle(Particle.EXPLOSION, center.clone().add(0, 0.25, 0), 2, 0.35, 0.12, 0.35, 0.0);
                    world.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.9f, 0.8f);
                    for (org.bukkit.entity.Entity entity : world.getNearbyEntities(center, 6.0, 3.0, 6.0)) {
                        if (!(entity instanceof LivingEntity living) || entity == player || !entity.isValid()) continue;
                        living.damage(8.0, player);
                        Vector direction = living.getLocation().toVector().subtract(center.toVector());
                        if (direction.lengthSquared() < 0.01) {
                            direction = player.getLocation().getDirection().clone().setY(0);
                        }
                        if (direction.lengthSquared() < 0.01) direction = new Vector(1, 0, 0);
                        living.setVelocity(direction.normalize().multiply(1.7).setY(0.65));
                    }
                }
                tick++;
            }
        }, 0L, 1L);
        return true;
    }

    @EventHandler
    public void onJoinVeil(PlayerJoinEvent event) {
        Player joining = event.getPlayer();
        for (UUID hiddenId : new java.util.HashSet<>(veilTasks.keySet())) {
            Player hidden = Bukkit.getPlayer(hiddenId);
            if (hidden != null && hidden.isOnline() && !joining.equals(hidden)
                    && joining.getWorld().equals(hidden.getWorld())) {
                if (joining.canSee(hidden)) {
                    veilViewers.computeIfAbsent(hiddenId, ignored -> new java.util.HashSet<>()).add(joining.getUniqueId());
                    joining.hidePlayer(plugin, hidden);
                }
            }
        }
    }

    @EventHandler
    public void onQuitVeil(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        for (java.util.Set<UUID> viewers : veilViewers.values()) viewers.remove(id);
        BukkitTask gambitTask = gambitTasks.remove(id);
        if (gambitTask != null) gambitTask.cancel();
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
        ItemStack hotbarItem = event.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY
                && event.getHotbarButton() >= 0
                ? player.getInventory().getItem(event.getHotbarButton()) : null;
        if (isArmor(event.getCursor()) || isArmor(event.getCurrentItem()) || isArmor(hotbarItem)
                || event.getSlotType() == org.bukkit.event.inventory.InventoryType.SlotType.ARMOR) {
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

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVeilAttack(EntityDamageByEntityEvent event) {
        Player attacker = null;
        if (event.getDamager() instanceof Player player) attacker = player;
        else if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player player) attacker = player;
        if (attacker != null && veilTasks.containsKey(attacker.getUniqueId())) event.setCancelled(true);
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVeilProjectile(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Player player
                && veilTasks.containsKey(player.getUniqueId())) event.setCancelled(true);
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVeilBlockBreak(BlockBreakEvent event) {
        if (veilTasks.containsKey(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVeilBlockPlace(BlockPlaceEvent event) {
        if (veilTasks.containsKey(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVeilEntityInteract(PlayerInteractEntityEvent event) {
        if (veilTasks.containsKey(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVeilDrop(PlayerDropItemEvent event) {
        if (veilTasks.containsKey(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler
    public void onVeilWorldChange(PlayerChangedWorldEvent event) {
        Player changed = event.getPlayer();
        for (UUID hiddenId : new java.util.HashSet<>(veilTasks.keySet())) {
            Player hidden = Bukkit.getPlayer(hiddenId);
            if (hidden == null) continue;
            java.util.Set<UUID> viewers = veilViewers.computeIfAbsent(hiddenId, ignored -> new java.util.HashSet<>());
            if (hidden.equals(changed)) {
                for (Player viewer : Bukkit.getOnlinePlayers()) {
                    if (viewer.equals(hidden) || !viewer.getWorld().equals(hidden.getWorld()) || !viewer.canSee(hidden)) continue;
                    viewers.add(viewer.getUniqueId());
                    viewer.hidePlayer(plugin, hidden);
                }
            } else if (changed.getWorld().equals(hidden.getWorld()) && changed.canSee(hidden)) {
                viewers.add(changed.getUniqueId());
                changed.hidePlayer(plugin, hidden);
            }
        }
    }

    @EventHandler
    public void onArmorQuickEquip(PlayerInteractEvent event) {
        if (veilTasks.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            return;
        }
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
                BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin,
                        () -> finishGambit(player), Math.max(1L, (gambitEnd - now) / 50L));
                gambitTasks.put(id, task);
            } else {
                finishGambit(player);
            }
        }
        Long heartEnd = heartUntil.get(id);
        if (heartEnd != null && heartEnd > now) startHeartSymbol(player);
        else heartUntil.remove(id);
    }

    @EventHandler
    public void onArmorHandSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!armorLocked(player)) return;
        if (isArmor(event.getMainHandItem()) || isArmor(event.getOffHandItem())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Armor is locked by Gambit for " + remainingMinutes(player) + " more minutes.");
        }
    }

    @EventHandler
    public void onArmorDispenser(BlockDispenseArmorEvent event) {
        if (event.getTargetEntity() instanceof Player player && armorLocked(player) && isArmor(event.getItem())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Armor is locked by Gambit for " + remainingMinutes(player) + " more minutes.");
        }
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
        if (isArmor(inventory.getItemInOffHand())) inventory.setItemInOffHand(null);
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
                long veilCooldown = state.getLong(root + "veil-cooldown-until", 0);
                if (gambit > 0) gambitUntil.put(id, gambit);
                if (armor > 0) armorLockUntil.put(id, armor);
                if (heart > 0) heartUntil.put(id, heart);
                if (cooldown > 0) cooldownUntil.put(id, cooldown);
                if (veilCooldown > 0) veilCooldownUntil.put(id, veilCooldown);
            } catch (IllegalArgumentException ignored) { }
        }
    }

    private void save() {
        state.set("players", null);
        java.util.Set<UUID> ids = new java.util.HashSet<>();
        ids.addAll(gambitUntil.keySet()); ids.addAll(armorLockUntil.keySet());
        ids.addAll(heartUntil.keySet()); ids.addAll(cooldownUntil.keySet()); ids.addAll(veilCooldownUntil.keySet());
        for (UUID id : ids) {
            String root = "players." + id + ".";
            state.set(root + "gambit-until", gambitUntil.getOrDefault(id, 0L));
            state.set(root + "armor-lock-until", armorLockUntil.getOrDefault(id, 0L));
            state.set(root + "heart-until", heartUntil.getOrDefault(id, 0L));
            state.set(root + "cooldown-until", cooldownUntil.getOrDefault(id, 0L));
            state.set(root + "veil-cooldown-until", veilCooldownUntil.getOrDefault(id, 0L));
        }
        try { state.save(file); }
        catch (IOException ex) { plugin.getLogger().severe("Could not save ability state: " + ex.getMessage()); }
    }

    /** Clears temporary ability state when a player changes evolution paths. */
    public void resetPathCooldowns(UUID id) {
        // Reset cooldowns, not active effects or their consequences. A Path Changer
        // must not let a player escape Gambit's delayed armor destruction/lock.
        cooldownUntil.remove(id);
        veilCooldownUntil.remove(id);
        fractureCooldown.remove(id);
        swapCooldown.remove(id);
        Player player = Bukkit.getPlayer(id);
        if (player != null) player.setCooldown(Material.ENDER_PEARL, 0);
        if (veilTasks.containsKey(id)) {
            if (player != null) restoreVeil(player);
            else {
                BukkitTask veilTask = veilTasks.remove(id);
                if (veilTask != null) veilTask.cancel();
                veilViewers.remove(id);
                previousInvisible.remove(id);
                previousSilent.remove(id);
            }
        }
        save();
    }

    public void shutdown() {
        gambitTasks.values().forEach(BukkitTask::cancel);
        gambitTasks.clear();
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
