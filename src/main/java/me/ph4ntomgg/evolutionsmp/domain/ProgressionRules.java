package me.ph4ntomgg.evolutionsmp.domain;

import java.util.Optional;

public final class ProgressionRules {
    private static final long T2_XP = 1_000;
    private static final long T3_XP = 3_500;
    private static final long T4_XP = 9_000;

    private ProgressionRules() {}

    public static long thresholdForNextTier(EvolutionTier tier) {
        return switch (tier) {
            case T1 -> T2_XP;
            case T2 -> T3_XP;
            case T3 -> T4_XP;
            case T4, APEX -> Long.MAX_VALUE;
        };
    }

    public static Optional<EvolutionTier> nextTier(EvolutionTier tier) {
        return switch (tier) {
            case T1 -> Optional.of(EvolutionTier.T2);
            case T2 -> Optional.of(EvolutionTier.T3);
            case T3 -> Optional.of(EvolutionTier.T4);
            case T4, APEX -> Optional.empty();
        };
    }

    public static PlayerProgress applyExperience(PlayerProgress current, long amount) {
        if (!current.assigned() || current.apex()) return current;
        PlayerProgress updated = current.addExperience(amount);
        while (!updated.apex() && updated.assigned()) {
            long threshold = thresholdForNextTier(updated.tier());
            if (updated.experience() < threshold) break;
            EvolutionTier next = nextTier(updated.tier()).orElseThrow();
            updated = updated.withTier(next, updated.experience() - threshold);
        }
        return updated;
    }
}
