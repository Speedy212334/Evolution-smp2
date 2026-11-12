package me.ph4ntomgg.evolutionsmp.domain;

import java.util.Objects;
import java.util.UUID;

public record PlayerProgress(UUID playerId, EvolutionPath path, EvolutionTier tier, long experience, boolean apex) {
    public PlayerProgress {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(tier, "tier");
        if (experience < 0) throw new IllegalArgumentException("experience cannot be negative");
        if ((tier == EvolutionTier.T4 || tier == EvolutionTier.APEX) && experience != 0) {
            throw new IllegalArgumentException("T4 and Apex players cannot retain progression XP");
        }
        if (path == null && (tier != EvolutionTier.T1 || experience != 0 || apex)) {
            throw new IllegalArgumentException("Unassigned players must remain at T1 with zero XP");
        }
        if (apex && path == null) throw new IllegalArgumentException("Apex players must have a path");
        if (apex && tier != EvolutionTier.APEX) throw new IllegalArgumentException("Apex flag and tier must agree");
        if (!apex && tier == EvolutionTier.APEX) throw new IllegalArgumentException("Apex tier requires Apex flag");
    }

    public static PlayerProgress unassigned(UUID id) {
        return new PlayerProgress(id, null, EvolutionTier.T1, 0, false);
    }

    public boolean assigned() { return path != null; }

    public PlayerProgress withPath(EvolutionPath newPath) {
        return new PlayerProgress(playerId, Objects.requireNonNull(newPath), EvolutionTier.T1, 0, false);
    }

    /** Path Changers preserve the earned tier but reset path-specific XP progress. */
    public PlayerProgress withPathPreservingTier(EvolutionPath newPath) {
        return new PlayerProgress(playerId, Objects.requireNonNull(newPath), tier, 0, apex);
    }

    public PlayerProgress addExperience(long amount) {
        if (amount < 0) throw new IllegalArgumentException("amount cannot be negative");
        return new PlayerProgress(playerId, path, tier, Math.addExact(experience, amount), apex);
    }

    public PlayerProgress withTier(EvolutionTier newTier, long remainingExperience) {
        return new PlayerProgress(playerId, path, newTier, remainingExperience, newTier == EvolutionTier.APEX);
    }
}
