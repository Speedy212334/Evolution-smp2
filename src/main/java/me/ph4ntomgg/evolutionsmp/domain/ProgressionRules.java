package me.ph4ntomgg.evolutionsmp.domain;

import java.util.Optional;

public final class ProgressionRules {
    public static final long DEFAULT_T2_XP = 1_000;
    public static final long DEFAULT_T3_XP = 3_500;
    public static final long DEFAULT_T4_XP = 9_000;

    private ProgressionRules() {}

    public static long thresholdForNextTier(EvolutionTier tier) {
        return thresholdForNextTier(tier, DEFAULT_T2_XP, DEFAULT_T3_XP, DEFAULT_T4_XP);
    }

    public static long thresholdForNextTier(EvolutionTier tier, long t2, long t3, long t4) {
        return switch (tier) {
            case T1 -> t2;
            case T2 -> t3;
            case T3 -> t4;
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
        return applyExperience(current, amount, DEFAULT_T2_XP, DEFAULT_T3_XP, DEFAULT_T4_XP);
    }

    public static PlayerProgress applyExperience(PlayerProgress current, long amount, long t2, long t3, long t4) {
        if (!current.assigned() || current.apex()) return current;
        if (amount < 0 || t2 < 1 || t3 < 1 || t4 < 1) throw new IllegalArgumentException("XP values must be positive");
        PlayerProgress updated = current.addExperience(amount);
        while (!updated.apex() && updated.assigned()) {
            long threshold = thresholdForNextTier(updated.tier(), t2, t3, t4);
            if (updated.experience() < threshold) break;
            EvolutionTier next = nextTier(updated.tier()).orElseThrow();
            updated = updated.withTier(next, updated.experience() - threshold);
        }
        return updated;
    }
}
