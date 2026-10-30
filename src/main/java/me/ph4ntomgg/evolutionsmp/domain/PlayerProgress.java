package me.ph4ntomgg.evolutionsmp.domain;

import java.util.Objects;
import java.util.UUID;

public record PlayerProgress(UUID playerId, EvolutionPath path, EvolutionTier tier, long experience, boolean apex) {
    public PlayerProgress {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(tier, "tier");
        if (experience < 0) throw new IllegalArgumentException("experience cannot be negative");
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

    public PlayerProgress addExperience(long amount) {
        if (amount < 0) throw new IllegalArgumentException("amount cannot be negative");
        return new PlayerProgress(playerId, path, tier, Math.addExact(experience, amount), apex);
    }

    public PlayerProgress withTier(EvolutionTier newTier, long remainingExperience) {
        return new PlayerProgress(playerId, path, newTier, remainingExperience, newTier == EvolutionTier.APEX);
    }
}
