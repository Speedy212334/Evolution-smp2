package me.ph4ntomgg.evolutionsmp.domain;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ProgressionRulesTest {
    @Test void unassignedPlayersDoNotGainEvolutionXp() {
        PlayerProgress p = PlayerProgress.unassigned(UUID.randomUUID());
        assertEquals(p, ProgressionRules.applyExperience(p, 500));
    }

    @Test void xpAdvancesAcrossMultipleTiersAndCarriesRemainder() {
        PlayerProgress p = PlayerProgress.unassigned(UUID.randomUUID()).withPath(EvolutionPath.HUNT);
        PlayerProgress advanced = ProgressionRules.applyExperience(p, 4_500);
        assertEquals(EvolutionTier.T3, advanced.tier());
        assertEquals(0, advanced.experience());
    }

    @Test void t4DoesNotAutoGrantApex() {
        PlayerProgress p = PlayerProgress.unassigned(UUID.randomUUID()).withPath(EvolutionPath.ENDER)
                .withTier(EvolutionTier.T4, 20_000);
        PlayerProgress advanced = ProgressionRules.applyExperience(p, 100_000);
        assertEquals(EvolutionTier.T4, advanced.tier());
        assertFalse(advanced.apex());
    }
}
