package me.ph4ntomgg.evolutionsmp.service;

import me.ph4ntomgg.evolutionsmp.domain.*;
import me.ph4ntomgg.evolutionsmp.persistence.ProgressRepository;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ProgressService {
    private final ProgressRepository repository;
    private final long t2Xp;
    private final long t3Xp;
    private final long t4Xp;
    private final Map<UUID, PlayerProgress> cache = new ConcurrentHashMap<>();

    public ProgressService(ProgressRepository repository) { this(repository, ProgressionRules.DEFAULT_T2_XP, ProgressionRules.DEFAULT_T3_XP, ProgressionRules.DEFAULT_T4_XP); }

    public ProgressService(ProgressRepository repository, long t2Xp, long t3Xp, long t4Xp) {
        this.repository = repository;
        this.t2Xp = Math.max(1, t2Xp);
        this.t3Xp = Math.max(1, t3Xp);
        this.t4Xp = Math.max(1, t4Xp);
    }

    public long thresholdForNextTier(EvolutionTier tier) {
        return ProgressionRules.thresholdForNextTier(tier, t2Xp, t3Xp, t4Xp);
    }

    public PlayerProgress get(UUID id) {
        return cache.computeIfAbsent(id, repository::load);
    }

    public PlayerProgress assign(UUID id, EvolutionPath path) {
        PlayerProgress updated = get(id).withPath(path);
        put(updated);
        return updated;
    }

    public PlayerProgress award(UUID id, long amount) {
        PlayerProgress current = get(id);
        if (!current.assigned()) return current;
        PlayerProgress updated = ProgressionRules.applyExperience(current, amount, t2Xp, t3Xp, t4Xp);
        put(updated);
        return updated;
    }

    public PlayerProgress setTier(UUID id, EvolutionTier tier) {
        PlayerProgress current = get(id);
        if (!current.assigned()) throw new IllegalStateException("Player has no evolution path");
        PlayerProgress updated = current.withTier(tier, 0);
        put(updated);
        return updated;
    }

    public void put(PlayerProgress progress) {
        cache.put(progress.playerId(), progress);
        repository.save(progress);
    }

    public void flush() { cache.values().forEach(repository::save); }
}
